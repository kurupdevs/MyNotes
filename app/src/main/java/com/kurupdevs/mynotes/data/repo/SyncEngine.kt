package com.kurupdevs.mynotes.data.repo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.local.NoteFts
import com.kurupdevs.mynotes.data.model.SyncStatus
import com.kurupdevs.mynotes.data.remote.FirestoreMapper
import com.kurupdevs.mynotes.data.remote.Jsons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NotesApp
        return try {
            SyncEngine.pushAll(app.container)
            Result.success()
        } catch (t: Throwable) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}

object SyncEngine {
    private var notesReg: ListenerRegistration? = null
    private var inboxReg: ListenerRegistration? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(c: com.kurupdevs.mynotes.di.AppContainer) {
        ioScope.launch {
            c.auth.authState().collect { acct ->
                notesReg?.remove(); inboxReg?.remove()
                notesReg = null; inboxReg = null
                if (acct == null || acct.isAnonymous) return@collect
                attachListeners(c, acct.uid)
            }
        }
        // daily trash purge + periodic push as safety net
        val wm = WorkManager.getInstance(c.context)
        wm.enqueueUniquePeriodicWork(
            "sync-periodic",
            ExistingWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }

    private fun attachListeners(c: com.kurupdevs.mynotes.di.AppContainer, uid: String) {
        val fs = FirebaseFirestore.getInstance()
        notesReg = fs.collection("users").document(uid).collection("notes")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                ioScope.launch { applyRemote(c, uid, snap.documents.mapNotNull { d ->
                    runCatching { FirestoreMapper.toEntity(uid, d) }.getOrNull()
                }) }
            }
        inboxReg = fs.collection("users").document(uid).collection("inbox")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                ioScope.launch {
                    for (change in snap.documentChanges) {
                        val d = change.document.data
                        val ownerId = d["ownerId"] as? String ?: continue
                        val noteId = d["noteId"] as? String ?: change.document.id
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                runCatching {
                                    val noteDoc = fs.collection("users").document(ownerId)
                                        .collection("notes").document(noteId).get().await()
                                    if (noteDoc.exists()) {
                                        FirestoreMapper.toEntity(uid, noteDoc)?.let { e ->
                                            c.db.noteDao().upsert(e.copy(syncStatus = SyncStatus.SYNCED.name))
                                            indexFts(c, e)
                                        }
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val local = c.db.noteDao().getById(noteId, uid)
                                if (local != null && local.ownerId != uid) {
                                    c.db.noteDao().deleteHard(noteId, uid)
                                    c.db.ftsDao().delete(noteId)
                                }
                            }
                        }
                    }
                }
            }
    }

    private suspend fun applyRemote(c: com.kurupdevs.mynotes.di.AppContainer, uid: String, remote: List<NoteEntity>) {
        val dao = c.db.noteDao()
        val meta = c.db.syncMetaDao()
        for (r in remote) {
            val local = dao.getById(r.id, uid)
            val lastSeen = meta.get("remote_seen_${r.id}")?.toLongOrNull() ?: 0L
            if (local == null) {
                dao.upsert(r)
                indexFts(c, r)
            } else if (local.syncStatus == SyncStatus.PENDING_UPLOAD.name && r.updatedAt > lastSeen && r.updatedAt != local.updatedAt) {
                // both sides changed -> conflict, keep local visible, stash remote
                dao.upsert(local.copy(syncStatus = SyncStatus.CONFLICT.name, conflictJson = r.blocksJson))
            } else if (r.updatedAt > local.updatedAt && local.syncStatus != SyncStatus.PENDING_UPLOAD.name) {
                // preserve local-only flags (locked) and local attachments' localPath/ocr
                val merged = mergeLocalBits(local, r)
                dao.upsert(merged)
                indexFts(c, merged)
                c.reminders.rescheduleIfNeeded(merged)
            }
            meta.put(com.kurupdevs.mynotes.data.local.SyncMeta("remote_seen_${r.id}", r.updatedAt.toString()))
        }
        // remote deletions: docs missing remotely but SYNCED locally -> hard delete locally
        val remoteIds = remote.map { it.id }.toSet()
        dao.getLive(uid).filter { it.syncStatus == SyncStatus.SYNCED.name && it.ownerId == uid && it.id !in remoteIds }
            .forEach {
                dao.deleteHard(it.id, uid)
                c.db.ftsDao().delete(it.id)
            }
    }

    private fun mergeLocalBits(local: NoteEntity, remote: NoteEntity): NoteEntity {
        val localAtts = Jsons.attachments(local.attachmentsJson).associateBy { it.id }
        val mergedAtts = Jsons.attachments(remote.attachmentsJson).map { ra ->
            val la = localAtts[ra.id]
            if (la != null) ra.copy(localPath = la.localPath, caption = la.caption, ocrText = la.ocrText) else ra
        }
        return remote.copy(
            locked = local.locked,
            attachmentsJson = Jsons.attachmentsToJson(mergedAtts),
            syncStatus = SyncStatus.SYNCED.name
        )
    }

    private suspend fun indexFts(c: com.kurupdevs.mynotes.di.AppContainer, e: NoteEntity) {
        if (e.deletedAt != null) {
            c.db.ftsDao().delete(e.id); return
        }
        val sb = StringBuilder().append(e.title).append(' ')
        Jsons.blocks(e.blocksJson).forEach { sb.append(it.text).append(' ') }
        Jsons.attachments(e.attachmentsJson).forEach { sb.append(it.caption).append(' ').append(it.ocrText).append(' ') }
        c.db.ftsDao().upsert(NoteFts(e.id, e.uid, sb.toString()))
    }

    fun schedulePush(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync-push", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    suspend fun pushAll(c: com.kurupdevs.mynotes.di.AppContainer) {
        val uid = c.auth.currentUid() ?: return
        if (c.auth.isAnonymous()) return
        if (!networkOk(c.context, c.prefs)) return
        val dao = c.db.noteDao()
        val fs = FirebaseFirestore.getInstance()
        val pending = dao.getPending(uid)
        for (p in pending) {
            when (p.syncStatus) {
                SyncStatus.PENDING_UPLOAD.name -> {
                    val ref = fs.collection("users").document(p.ownerId).collection("notes").document(p.id)
                    // editor-role collaborators may only touch content fields
                    ref.set(FirestoreMapper.toMap(p)).await()
                    dao.upsert(p.copy(syncStatus = SyncStatus.SYNCED.name))
                }
                SyncStatus.PENDING_DELETE.name -> {
                    fs.collection("users").document(p.ownerId).collection("notes").document(p.id).delete().await()
                    dao.deleteHard(p.id, uid)
                    c.db.ftsDao().delete(p.id)
                }
                else -> Unit // CONFLICT waits for user
            }
        }
        c.prefs.setLastSyncAt(System.currentTimeMillis())
    }

    suspend fun pushDelete(context: Context, db: com.kurupdevs.mynotes.data.local.NotesDatabase, auth: AuthRepository, noteId: String, ownerId: String) {
        if (auth.isAnonymous()) return
        runCatching {
            FirebaseFirestore.getInstance().collection("users").document(ownerId)
                .collection("notes").document(noteId).delete().await()
        }
    }

    fun writeInboxRef(context: Context, db: com.kurupdevs.mynotes.data.local.NotesDatabase, auth: AuthRepository, targetUid: String, noteId: String, ownerId: String, role: String) {
        ioScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("users").document(targetUid)
                    .collection("inbox").document(noteId)
                    .set(mapOf("ownerId" to ownerId, "noteId" to noteId, "role" to role,
                        "sharedAt" to com.google.firebase.Timestamp.now(),
                        "noteUpdatedAt" to com.google.firebase.Timestamp.now()))
                    .await()
            }
        }
    }

    fun deleteInboxRef(context: Context, auth: AuthRepository, targetUid: String, noteId: String) {
        ioScope.launch {
            runCatching {
                FirebaseFirestore.getInstance().collection("users").document(targetUid)
                    .collection("inbox").document(noteId).delete().await()
            }
        }
    }

    private suspend fun networkOk(context: Context, prefs: PrefsRepository): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (prefs.syncWifiOnly.first() && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false
        return prefs.autoSync.first()
    }
}
