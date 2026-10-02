package com.kurupdevs.mynotes.data.repo

import android.content.Context
import com.kurupdevs.mynotes.data.cloud.CloudinaryUploader
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.local.NoteFts
import com.kurupdevs.mynotes.data.local.NotesDatabase
import com.kurupdevs.mynotes.data.model.Attachment
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.model.Collaborator
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.data.model.NoteType
import com.kurupdevs.mynotes.data.model.SyncStatus
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class NotesRepository(
    private val context: Context,
    private val db: NotesDatabase,
    private val auth: AuthRepository,
    private val uploader: CloudinaryUploader,
    private val reminders: ReminderScheduler,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit
) {
    private val notes = db.noteDao()
    private val labels = db.labelDao()
    private val fts = db.ftsDao()

    fun uid(): String = auth.currentUid() ?: "local"

    fun home() = notes.observeHome(uid())
    fun favorites() = notes.observeFavorites(uid())
    fun archived() = notes.observeArchived(uid())
    fun trash() = notes.observeTrash(uid())
    fun remindersFlow() = notes.observeReminders(uid())
    fun allRemindersFlow() = notes.observeAllReminders(uid())
    fun noteById(id: String) = notes.observeById(id, uid())
    fun labelsFlow() = labels.observe(uid())

    suspend fun getNote(id: String): NoteEntity? = notes.getById(id, uid())

    suspend fun search(query: String, includeArchivedTrash: Boolean = false): List<NoteEntity> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.length >= 2 }
            .map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }.filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        val q = terms.joinToString(" OR ") { "\"$it\"*" }
        val ids = fts.searchNoteIds(q)
        if (ids.isEmpty()) return emptyList()
        val byId = db.noteDao().getAllVisible(uid()).associateBy { it.id }
        var result = ids.mapNotNull { byId[it] }
        if (!includeArchivedTrash) result = result.filter { it.deletedAt == null && !it.archived }
        return result
    }

    private suspend fun touch(e: NoteEntity, status: SyncStatus = SyncStatus.PENDING_UPLOAD): NoteEntity {
        val now = System.currentTimeMillis()
        val updated = e.copy(updatedAt = now, updatedBy = uid(), version = e.version + 1, syncStatus = status.name)
        notes.upsert(updated)
        indexFts(updated)
        onChanged()
        return updated
    }

    private suspend fun indexFts(e: NoteEntity) {
        if (e.deletedAt != null) {
            fts.delete(e.id); return
        }
        val blocks = Jsons.blocks(e.blocksJson)
        val atts = Jsons.attachments(e.attachmentsJson)
        val sb = StringBuilder()
        sb.append(e.title).append(' ')
        blocks.forEach { sb.append(it.text).append(' ') }
        atts.forEach { sb.append(it.caption).append(' ').append(it.ocrText).append(' ') }
        fts.upsert(NoteFts(noteId = e.id, uid = uid(), content = sb.toString()))
    }

    suspend fun createNote(
        type: NoteType = NoteType.TEXT,
        title: String = "",
        blocks: List<Block> = emptyList(),
        color: NoteColor = NoteColor.DEFAULT
    ): NoteEntity {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val e = NoteEntity(
            id = id, uid = uid(), ownerId = uid(), title = title, type = type.name,
            blocksJson = Jsons.blocksToJson(blocks.ifEmpty { listOf(Block(UUID.randomUUID().toString(), BlockKind.P)) }),
            color = color.key, createdAt = now, updatedAt = now, updatedBy = uid(),
            syncStatus = SyncStatus.PENDING_UPLOAD.name
        )
        notes.upsert(e)
        indexFts(e)
        onChanged()
        return e
    }

    suspend fun updateBlocks(id: String, blocks: List<Block>, title: String? = null, atts: List<Attachment>? = null) {
        val e = notes.getById(id, uid()) ?: return
        var updated = e.copy(blocksJson = Jsons.blocksToJson(blocks), title = title ?: e.title)
        if (atts != null) updated = updated.copy(attachmentsJson = Jsons.attachmentsToJson(atts))
        touch(updated)
    }

    suspend fun updateTitle(id: String, title: String) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(title = title))
    }

    suspend fun setColor(id: String, color: NoteColor) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(color = color.key))
    }

    suspend fun togglePin(id: String): Boolean {
        val e = notes.getById(id, uid()) ?: return false
        val pinnedCount = notes.getLive(uid()).count { it.pinned }
        if (!e.pinned && pinnedCount >= 20) return false
        val now = System.currentTimeMillis()
        touch(e.copy(pinned = !e.pinned, pinnedAt = if (!e.pinned) now else 0))
        return true
    }

    suspend fun toggleFavorite(id: String) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(favorite = !e.favorite))
    }

    suspend fun archive(id: String, arch: Boolean) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(archived = arch))
        if (arch) reminders.cancel(id) else e.reminderAt?.let {
            if (!e.reminderDone && it > System.currentTimeMillis()) reminders.schedule(id, it, e.reminderRepeat)
        }
    }

    suspend fun trash(id: String) {
        val e = notes.getById(id, uid()) ?: return
        val now = System.currentTimeMillis()
        touch(e.copy(deletedAt = now, trashedWasPinned = e.pinned, trashedWasArchived = e.archived, pinned = false))
        reminders.cancel(id)
    }

    suspend fun restore(id: String) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(deletedAt = null, pinned = e.trashedWasPinned, archived = e.trashedWasArchived))
        e.reminderAt?.let {
            if (!e.reminderDone && it > System.currentTimeMillis() && !e.trashedWasArchived)
                reminders.schedule(id, it, e.reminderRepeat)
        }
    }

    suspend fun deleteForever(id: String) {
        val e = notes.getById(id, uid()) ?: return
        // assets: best-effort Cloudinary purge is unsigned-impossible; delete local copies
        Jsons.attachments(e.attachmentsJson).forEach { a ->
            if (a.localPath.isNotEmpty()) runCatching { File(a.localPath).delete() }
        }
        notes.deleteHard(id, uid())
        fts.delete(id)
        reminders.cancel(id)
        scope.launch(Dispatchers.IO) { SyncEngine.pushDelete(context, db, auth, id, e.ownerId) }
    }

    suspend fun emptyTrash() {
        trashIds().forEach { deleteForever(it) }
    }

    private suspend fun trashIds(): List<String> =
        db.noteDao().getExpiredTrash(uid(), Long.MAX_VALUE).map { it.id }

    suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        db.noteDao().getExpiredTrash(uid(), cutoff).forEach { deleteForever(it.id) }
    }

    suspend fun toggleTodo(id: String, blockId: String) {
        val e = notes.getById(id, uid()) ?: return
        val blocks = Jsons.blocks(e.blocksJson).map {
            if (it.id == blockId && it.kind == BlockKind.TODO) it.copy(checked = !it.checked) else it
        }
        touch(e.copy(blocksJson = Jsons.blocksToJson(blocks)))
    }

    suspend fun setReminder(id: String, at: Long?, repeat: String?) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(reminderAt = at, reminderRepeat = repeat, reminderDone = false))
        reminders.cancel(id)
        if (at != null && at > System.currentTimeMillis()) reminders.schedule(id, at, repeat)
    }

    suspend fun completeReminder(id: String) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(reminderDone = true))
        reminders.cancel(id)
    }

    suspend fun snoozeReminder(id: String, newAt: Long) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(reminderAt = newAt, reminderDone = false))
        reminders.schedule(id, newAt, e.reminderRepeat)
    }

    suspend fun setLabels(id: String, labelIds: List<String>) {
        val e = notes.getById(id, uid()) ?: return
        touch(e.copy(labelsJson = Jsons.labelsToJson(labelIds)))
    }

    suspend fun setLocked(id: String, locked: Boolean) {
        val e = notes.getById(id, uid()) ?: return
        notes.upsert(e.copy(locked = locked))
    }

    suspend fun duplicate(id: String): NoteEntity? {
        val e = notes.getById(id, uid()) ?: return null
        val now = System.currentTimeMillis()
        val copy = e.copy(
            id = UUID.randomUUID().toString(), title = e.title + " (copy)",
            pinned = false, pinnedAt = 0, favorite = false, archived = false,
            reminderAt = null, collaboratorsJson = "{}",
            createdAt = now, updatedAt = now, updatedBy = uid(), version = 1,
            syncStatus = SyncStatus.PENDING_UPLOAD.name, conflictJson = null
        )
        notes.upsert(copy)
        indexFts(copy)
        onChanged()
        return copy
    }

    // ---- labels ----
    suspend fun createLabel(name: String, color: String): Boolean {
        if (labels.count(uid()) >= 50) return false
        if (labels.getAll(uid()).any { it.name.equals(name.trim(), ignoreCase = true) }) return false
        labels.upsert(LabelEntity(UUID.randomUUID().toString(), uid(), name.trim(), color, System.currentTimeMillis()))
        onChanged()
        return true
    }

    suspend fun renameLabel(id: String, name: String): Boolean {
        val all = labels.getAll(uid())
        if (all.any { it.id != id && it.name.equals(name.trim(), ignoreCase = true) }) return false
        val l = all.firstOrNull { it.id == id } ?: return false
        labels.upsert(l.copy(name = name.trim(), syncStatus = SyncStatus.PENDING_UPLOAD.name))
        onChanged()
        return true
    }

    suspend fun recolorLabel(id: String, color: String) {
        val l = labels.getAll(uid()).firstOrNull { it.id == id } ?: return
        labels.upsert(l.copy(color = color, syncStatus = SyncStatus.PENDING_UPLOAD.name))
        onChanged()
    }

    suspend fun deleteLabel(id: String) {
        labels.delete(id, uid())
        // strip from notes
        db.noteDao().getLive(uid()).forEach { n ->
            val ls = Jsons.labels(n.labelsJson)
            if (id in ls) touch(n.copy(labelsJson = Jsons.labelsToJson(ls - id)))
        }
        onChanged()
    }

    // ---- attachments ----
    suspend fun attachImage(id: String, src: File): Attachment? {
        val e = notes.getById(id, uid()) ?: return null
        return try {
            val res = uploader.uploadImage(src, uid(), id)
            val att = Attachment(
                id = UUID.randomUUID().toString(), kind = "image", url = res.url,
                localPath = src.absolutePath, cloudinaryPublicId = res.publicId,
                mime = "image/webp", sizeBytes = res.sizeBytes, width = res.width, height = res.height
            )
            val atts = Jsons.attachments(e.attachmentsJson) + att
            val blocks = Jsons.blocks(e.blocksJson).toMutableList()
            blocks.add(Block(UUID.randomUUID().toString(), BlockKind.IMG, attachmentId = att.id, order = blocks.size))
            touch(e.copy(attachmentsJson = Jsons.attachmentsToJson(atts), blocksJson = Jsons.blocksToJson(blocks)))
            att
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun updateAttachment(id: String, att: Attachment) {
        val e = notes.getById(id, uid()) ?: return
        val atts = Jsons.attachments(e.attachmentsJson).map { if (it.id == att.id) att else it }
        touch(e.copy(attachmentsJson = Jsons.attachmentsToJson(atts)))
    }

    suspend fun attachAudio(id: String, src: File, durationMs: Long): Attachment? {
        val e = notes.getById(id, uid()) ?: return null
        return try {
            val res = uploader.uploadAudio(src, uid(), id)
            val att = Attachment(
                id = UUID.randomUUID().toString(), kind = "audio", url = res.url,
                localPath = src.absolutePath, cloudinaryPublicId = res.publicId,
                mime = "audio/mp4", sizeBytes = res.sizeBytes, durationMs = durationMs
            )
            val atts = Jsons.attachments(e.attachmentsJson) + att
            val blocks = Jsons.blocks(e.blocksJson).toMutableList()
            blocks.add(Block(UUID.randomUUID().toString(), BlockKind.AUDIO, attachmentId = att.id, order = blocks.size))
            touch(e.copy(attachmentsJson = Jsons.attachmentsToJson(atts), blocksJson = Jsons.blocksToJson(blocks)))
            att
        } catch (t: Throwable) {
            null
        }
    }

    // ---- share ----
    suspend fun shareWith(id: String, targetUid: String, role: String): Boolean {
        val e = notes.getById(id, uid()) ?: return false
        if (e.ownerId != uid()) return false
        val collabs = Jsons.collaborators(e.collaboratorsJson).toMutableMap()
        collabs[targetUid] = Collaborator(role, System.currentTimeMillis())
        touch(e.copy(collaboratorsJson = Jsons.collaboratorsToJson(collabs)))
        SyncEngine.writeInboxRef(context, db, auth, targetUid, id, uid(), role)
        return true
    }

    suspend fun unshare(id: String, targetUid: String) {
        val e = notes.getById(id, uid()) ?: return
        if (e.ownerId != uid()) return
        val collabs = Jsons.collaborators(e.collaboratorsJson).toMutableMap()
        collabs.remove(targetUid)
        touch(e.copy(collaboratorsJson = Jsons.collaboratorsToJson(collabs)))
        SyncEngine.deleteInboxRef(context, auth, targetUid, id)
    }

    suspend fun stopSharing(id: String) {
        val e = notes.getById(id, uid()) ?: return
        if (e.ownerId != uid()) return
        Jsons.collaborators(e.collaboratorsJson).keys.forEach { SyncEngine.deleteInboxRef(context, auth, it, id) }
        touch(e.copy(collaboratorsJson = "{}"))
    }

    suspend fun leaveShared(id: String) {
        val e = notes.getById(id, uid()) ?: return
        notes.deleteHard(id, uid())
        fts.delete(id)
    }

    suspend fun resolveConflict(id: String, keepLocal: Boolean) {
        val e = notes.getById(id, uid()) ?: return
        if (keepLocal) {
            touch(e.copy(conflictJson = null))
        } else {
            val remote = e.conflictJson ?: return
            val now = System.currentTimeMillis()
            val resolved = e.copy(
                blocksJson = remote, conflictJson = null, updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD.name, version = e.version + 1
            )
            notes.upsert(resolved)
            indexFts(resolved)
            onChanged()
        }
    }

    // ---- export lives in ExportManager (uses DAO directly) ----
}
