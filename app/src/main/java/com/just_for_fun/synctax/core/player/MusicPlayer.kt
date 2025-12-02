package com.just_for_fun.synctax.core.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayer(context: Context) {

    companion object {
        private const val TAG = "MusicPlayer"
    }

    // Use cached data source for better performance
    private val cachedDataSourceFactory = StreamCache.createCachedDataSourceFactory(context)
    private val mediaSourceFactory = DefaultMediaSourceFactory(cachedDataSourceFactory)
    
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
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

    private var lastEmittedPosition = 0L
    private val positionUpdateThreshold = 1000L // Only update if position changed by 1 second
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                val currentPos = exoPlayer.currentPosition
                // Only emit if position changed significantly to reduce recompositions
                if (kotlin.math.abs(currentPos - lastEmittedPosition) >= positionUpdateThreshold) {
                    _currentPosition.value = currentPos
                    lastEmittedPosition = currentPos
                }
                mainHandler.postDelayed(this, 500) // Update every 500ms (reduced frequency)
            }
        }
        mainHandler.post(positionUpdateRunnable!!)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        positionUpdateRunnable = null
    }

    fun prepare(filePath: String, songId: String) {
        // Reset isEnded flag BEFORE setting new media item to prevent stale state
        _playerState.value = _playerState.value.copy(
            isEnded = false
        )
        
        val mediaItem = MediaItem.fromUri(filePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        _playerState.value = _playerState.value.copy(
            currentSongId = songId,
            duration = exoPlayer.duration,
            isEnded = false
        )
    }

    /**
     * Prepare a media item with a custom cache key for cached playback.
     * Use this for online songs to leverage pre-cached audio data.
     * 
     * @param uri The stream URL or file path
     * @param songId The song ID for state tracking
     * @param cacheKey Optional cache key (usually video ID for online songs)
     */
    fun prepareWithCacheKey(uri: String, songId: String, cacheKey: String? = null) {
        _playerState.value = _playerState.value.copy(isEnded = false)
        
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)
        
        // Set custom cache key if provided (for online songs)
        if (cacheKey != null) {
            mediaItemBuilder.setCustomCacheKey(cacheKey)
            Log.d(TAG, "Preparing with cache key: $cacheKey")
        }
        
        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        
        _playerState.value = _playerState.value.copy(
            currentSongId = songId,
            duration = exoPlayer.duration,
            isEnded = false
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

    /**
     * Check if the player has a media source prepared and ready to play.
     * This is useful to detect if a restored song needs stream URL extraction.
     */
    fun isSourcePrepared(): Boolean {
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            exoPlayer.playbackState != Player.STATE_IDLE && 
            exoPlayer.playbackState != Player.STATE_ENDED &&
            exoPlayer.mediaItemCount > 0
        } else {
            _playerState.value.currentSongId != null && !_playerState.value.isEnded
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