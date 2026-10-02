package com.kurupdevs.mynotes.di

import android.content.Context
import androidx.room.Room
import com.kurupdevs.mynotes.data.cloud.CloudinaryUploader
import com.kurupdevs.mynotes.data.local.NotesDatabase
import com.kurupdevs.mynotes.data.repo.AuthRepository
import com.kurupdevs.mynotes.data.repo.NotesRepository
import com.kurupdevs.mynotes.data.repo.PrefsRepository
import com.kurupdevs.mynotes.data.repo.SyncEngine
import com.kurupdevs.mynotes.export.ExportManager
import com.kurupdevs.mynotes.lock.NoteLocker
import com.kurupdevs.mynotes.reminders.ReminderScheduler
import com.kurupdevs.mynotes.voice.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val db: NotesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java, "mynotes.db")
        .fallbackToDestructiveMigration()
        .build()

    val prefs = PrefsRepository(context)
    val auth = AuthRepository(context)
    val reminders = ReminderScheduler(context)
    val uploader = CloudinaryUploader(context)
    val locker = NoteLocker(context)
    val player: AudioPlayer by lazy { AudioPlayer.get(context) }
    val export = ExportManager(context, db, auth)

    val notes: NotesRepository = NotesRepository(
        context, db, auth, uploader, reminders, appScope,
        onChanged = { SyncEngine.schedulePush(context) }
    )

    fun start() {
        appScope.launch(Dispatchers.IO) {
            val uid = auth.ensureSignedIn()
            notes.purgeExpiredTrash()
            SyncEngine.start(this@AppContainer)
        }
    }
}
