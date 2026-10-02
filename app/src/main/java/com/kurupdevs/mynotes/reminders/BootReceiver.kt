package com.kurupdevs.mynotes.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kurupdevs.mynotes.NotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as NotesApp
                val c = app.container
                val uid = c.auth.ensureSignedIn()
                c.db.noteDao().getAllVisible(uid)
                    .filter { it.reminderAt != null && !it.reminderDone && !it.archived && it.reminderAt > System.currentTimeMillis() }
                    .forEach { c.reminders.schedule(it.id, it.reminderAt!!, it.reminderRepeat) }
                c.notes.purgeExpiredTrash()
            } finally {
                pending.finish()
            }
        }
    }
}
