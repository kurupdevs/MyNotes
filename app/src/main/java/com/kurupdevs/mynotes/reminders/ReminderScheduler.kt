package com.kurupdevs.mynotes.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kurupdevs.mynotes.data.local.NoteEntity

class ReminderScheduler(private val context: Context) {
    private val alarm: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarm.canScheduleExactAlarms() else true

    fun schedule(noteId: String, at: Long, repeat: String? = null) {
        val pi = pending(noteId)
        try {
            if (canScheduleExact()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(noteId: String) {
        alarm.cancel(pending(noteId))
    }

    private fun pending(noteId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.kurupdevs.mynotes.ACTION_REMINDER"
            putExtra("note_id", noteId)
        }
        return PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun rescheduleIfNeeded(e: NoteEntity) {
        val at = e.reminderAt
        if (at != null && !e.reminderDone && !e.archived && e.deletedAt == null && at > System.currentTimeMillis()) {
            schedule(e.id, at, e.reminderRepeat)
        } else {
            cancel(e.id)
        }
    }
}
