package com.kurupdevs.mynotes.voice

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Singleton audio player for voice-note playback (home cards + editor). */
@OptIn(UnstableApi::class)
class AudioPlayer private constructor(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playingId.value = if (state == Player.STATE_ENDED) {
                    _playingId.value?.let { null }
                } else _playingId.value
                _isPlaying.value = isPlaying
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun toggle(id: String, url: String) {
        if (_playingId.value == id && player.isPlaying) {
            player.pause()
            return
        }
        if (_playingId.value != id) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            _playingId.value = id
        }
        player.play()
    }

    fun stop() {
        player.stop()
        _playingId.value = null
    }

    fun seekTo(ms: Long) = player.seekTo(ms)
    fun duration(): Long = if (player.duration > 0) player.duration else 0L
    fun position(): Long = player.currentPosition
    fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    companion object {
        @Volatile private var instance: AudioPlayer? = null
        fun get(context: Context): AudioPlayer =
            instance ?: synchronized(this) {
                instance ?: AudioPlayer(context).also { instance = it }
            }
    }
}
