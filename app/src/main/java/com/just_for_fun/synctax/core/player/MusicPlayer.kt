package com.just_for_fun.synctax.core.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayer(context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.value = _playerState.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isEnded = playbackState == Player.STATE_ENDED
                )
                
                // Update duration when player is ready
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        _playerState.value = _playerState.value.copy(duration = duration)
                    }
                }
            }
        })
    }

    private var positionUpdateRunnable: Runnable? = null

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                _currentPosition.value = exoPlayer.currentPosition
                mainHandler.postDelayed(this, 100) // Update every 100ms
            }
        }
        mainHandler.post(positionUpdateRunnable!!)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        positionUpdateRunnable = null
    }

    fun prepare(filePath: String, songId: String) {
        val mediaItem = MediaItem.fromUri(filePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        _playerState.value = _playerState.value.copy(
            currentSongId = songId,
            duration = exoPlayer.duration
        )
    }

    fun play() {
        exoPlayer.play()
        _playerState.value = _playerState.value.copy(
            playStartTime = System.currentTimeMillis()
        )
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        // Don't immediately set _currentPosition here - let position updates handle it
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0.0f, 1.0f)
        exoPlayer.volume = clampedVolume
        _volume.value = clampedVolume
    }

    fun getVolume(): Float {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            exoPlayer.volume
        } else {
            _volume.value
        }
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun setPlaylist(playlist: List<String>, startIndex: Int = 0) {
        val mediaItems = playlist.map { MediaItem.fromUri(it) }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
    }

    fun getCurrentPosition(): Long {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            exoPlayer.currentPosition
        } else {
            _currentPosition.value
        }
    }

    fun getDuration(): Long {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            exoPlayer.duration
        } else {
            _playerState.value.duration
        }
    }

    fun isPlaying(): Boolean {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            exoPlayer.isPlaying
        } else {
            _playerState.value.isPlaying
        }
    }

    fun release() {
        mainHandler.post {
            exoPlayer.release()
        }
    }
}

data class PlayerState(
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val duration: Long = 0L,
    val playStartTime: Long = 0L
)