package com.kurupdevs.mynotes.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.R
import com.kurupdevs.mynotes.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("note_id") ?: return
        val action = intent.getStringExtra("rem_action")
        val app = context.applicationContext as NotesApp
        val c = app.container
        scope.launch {
            val note = c.notes.getNote(noteId) ?: return@launch
            when (action) {
                "complete" -> c.notes.completeReminder(noteId)
                "snooze" -> c.notes.snoozeReminder(noteId, System.currentTimeMillis() + 60 * 60 * 1000)
                else -> {
                    showNotification(context, noteId, note.title.ifEmpty { "Untitled note" })
                    // handle repeat
                    val repeat = note.reminderRepeat
                    if (repeat == "daily" || repeat == "weekly") {
                        val next = note.reminderAt!! + if (repeat == "daily") 24 * 3600_000L else 7 * 24 * 3600_000L
                        c.notes.setReminder(noteId, next, repeat)
                    } else {
                        c.notes.completeReminder(noteId)
                    }
                }
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(noteId.hashCode())
        }
    }

    private fun showNotification(context: Context, noteId: String, title: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = PendingIntent.getActivity(
            context, noteId.hashCode() + 1,
            Intent(context, MainActivity::class.java).putExtra("open_note", noteId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val complete = PendingIntent.getBroadcast(
            context, noteId.hashCode() + 2,
            Intent(context, ReminderReceiver::class.java)
                .setAction("com.kurupdevs.mynotes.ACTION_REMINDER")
                .putExtra("note_id", noteId).putExtra("rem_action", "complete"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context, noteId.hashCode() + 3,
            Intent(context, ReminderReceiver::class.java)
                .setAction("com.kurupdevs.mynotes.ACTION_REMINDER")
                .putExtra("note_id", noteId).putExtra("rem_action", "snooze"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Reminder from My Notes")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Done", complete)
            .addAction(0, "Snooze 1h", snooze)
            .build()
        nm.notify(noteId.hashCode(), notif)
    }
}
