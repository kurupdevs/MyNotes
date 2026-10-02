package com.kurupdevs.mynotes.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kurupdevs.mynotes.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Foreground-service voice recorder. Activity binds via the companion state flows;
 * recording state survives rotation and backgrounding.
 */
class VoiceRecordService : Service() {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> { stopRecording(); stopSelf() }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return
        val dir = File(cacheDir, "audio").apply { mkdirs() }
        outFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44100)
            setAudioChannels(1)
            setOutputFile(outFile!!.absolutePath)
            setMaxDuration(10 * 60 * 1000)
            prepare()
            start()
        }
        startForeground(NOTIF_ID, buildNotif(true))
        _state.value = RecState(recording = true, paused = false, file = outFile, elapsedMs = 0)
        startTime = System.currentTimeMillis()
        pausedTotal = 0
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= 24) recorder?.pause()
        pausedAt = System.currentTimeMillis()
        _state.value = _state.value.copy(recording = true, paused = true)
        stopForeground(STOP_FOREGROUND_DETACH)
        startForeground(NOTIF_ID, buildNotif(false))
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= 24) recorder?.resume()
        pausedTotal += System.currentTimeMillis() - pausedAt
        _state.value = _state.value.copy(paused = false)
    }

    private fun stopRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        val elapsed = System.currentTimeMillis() - startTime - pausedTotal
        _state.value = _state.value.copy(recording = false, paused = false, elapsedMs = elapsed)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CHANNEL, "Voice recording", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotif(active: Boolean): android.app.Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (active) "Recording voice note" else "Recording paused")
            .setContentText("My Notes")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        runCatching { recorder?.release() }
        recorder = null
        super.onDestroy()
    }

    data class RecState(
        val recording: Boolean = false,
        val paused: Boolean = false,
        val file: File? = null,
        val elapsedMs: Long = 0
    )

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        private const val CHANNEL = "voice_rec"
        private const val NOTIF_ID = 4101
        private val _state = MutableStateFlow(RecState())
        val state: StateFlow<RecState> = _state
        private var startTime = 0L
        private var pausedAt = 0L
        private var pausedTotal = 0L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VoiceRecordService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, VoiceRecordService::class.java).setAction(ACTION_STOP))
        }

        fun pause(context: Context) {
            context.startService(Intent(context, VoiceRecordService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, VoiceRecordService::class.java).setAction(ACTION_RESUME))
        }
    }
}
