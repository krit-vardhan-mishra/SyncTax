package com.just_for_fun.synctax.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.IOException
// import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.player.MusicPlayer
import com.just_for_fun.synctax.widget.MusicWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.Build
import android.media.AudioAttributes

/**
 * Foreground service to keep music playing even when the app is in the background.
 * This ensures continuous playback and shows a persistent notification.
 */
class MusicService : Service() {

    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var currentSong: Song? = null
    private var isPlaying = false
    private var currentPosition = 0L
    private var duration = 0L
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus - pause playback
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss of audio focus - pause playback
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Temporary loss with ducking allowed - lower volume (handled by system)
                // We'll just pause to avoid conflict
                onPause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained audio focus - optionally resume playback
                // We won't auto-resume, user needs to press play
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SEEK_TO = "ACTION_SEEK_TO"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        setupMediaSession()
        notificationManager = MusicNotificationManager(this, mediaSession, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> onPlay()
            ACTION_PAUSE -> onPause()
            ACTION_NEXT -> onNext()
            ACTION_PREVIOUS -> onPrevious()
            ACTION_STOP -> onStop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        abandonAudioFocus()
        serviceScope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Called when the app is removed from recent tasks (swiped away).
        // Stop playback and ensure the service stops to avoid background drain.
        // Send a stop broadcast to the ViewModel/receiver so it can clean up playback state.
        sendBroadcast(Intent(ACTION_STOP))
        // Remove foreground notification and stop the service
        stopForeground(true)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            // Set media button receiver
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    this@MusicService.onPlay()
                }

                override fun onPause() {
                    this@MusicService.onPause()
                }

                override fun onSkipToNext() {
                    this@MusicService.onNext()
                }

                override fun onSkipToPrevious() {
                    this@MusicService.onPrevious()
                }

                override fun onStop() {
                    this@MusicService.onStop()
                }

                override fun onSeekTo(pos: Long) {
                    this@MusicService.onSeekTo(pos)
                }
            })

            // Set initial state
            setPlaybackState(createPlaybackState())
        }
    }

    private fun createPlaybackState(): PlaybackStateCompat {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                     PlaybackStateCompat.ACTION_PAUSE or
                     PlaybackStateCompat.ACTION_PLAY_PAUSE or
                     PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                     PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                     PlaybackStateCompat.ACTION_SEEK_TO or
                     PlaybackStateCompat.ACTION_STOP

        return PlaybackStateCompat.Builder()
            .setState(state, currentPosition, 1.0f)
            .setActions(actions)
            .build()
    }

    private fun updateMediaMetadata(song: Song?) {
        if (song == null) {
            mediaSession.setMetadata(null)
            return
        }

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album ?: "")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .build()

        // Load album art asynchronously
        serviceScope.launch {
            val bitmap = loadAlbumArt(song.albumArtUri)
            if (bitmap != null) {
                val metadataWithArt = MediaMetadataCompat.Builder(metadata)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
                    .build()
                mediaSession.setMetadata(metadataWithArt)
            } else {
                mediaSession.setMetadata(metadata)
            }
        }
    }

    private fun loadAlbumArt(albumArtUri: String?): Bitmap? {
        if (albumArtUri.isNullOrEmpty()) return null

        return try {
            val uri = Uri.parse(albumArtUri)
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                // Resize bitmap for metadata
                val maxSize = 512
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize

                Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
        } catch (e: IOException) {
            null
        }
    }

    fun updatePlaybackState(song: Song?, isPlaying: Boolean, position: Long, duration: Long) {
        // Request audio focus when starting playback
        if (isPlaying && !this.isPlaying) {
            if (!requestAudioFocus()) {
                // If we can't get audio focus, don't start playback
                return
            }
        }
        
        // Abandon audio focus when stopping playback
        if (!isPlaying && this.isPlaying) {
            abandonAudioFocus()
        }

        this.currentSong = song
        this.isPlaying = isPlaying
        this.currentPosition = position
        this.duration = duration

        updateMediaMetadata(song)
        mediaSession.setPlaybackState(createPlaybackState())

        if (song != null) {
            startForeground(MusicNotificationManager.NOTIFICATION_ID, notificationManager.createNotification(song, isPlaying, position, duration))
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        // Update widget
        MusicWidgetProvider.updateWidget(
            context = this,
            songTitle = song?.title,
            songArtist = song?.artist,
            albumArtUri = song?.albumArtUri,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            shuffleOn = false // You can pass the actual shuffle state if available
        )
    }

    // Action handlers - send broadcasts to ViewModel
    private fun onPlay() {
        sendBroadcast(Intent(ACTION_PLAY))
    }

    private fun onPause() {
        sendBroadcast(Intent(ACTION_PAUSE))
    }

    private fun onNext() {
        sendBroadcast(Intent(ACTION_NEXT))
    }

    private fun onPrevious() {
        sendBroadcast(Intent(ACTION_PREVIOUS))
    }

    private fun onStop() {
        sendBroadcast(Intent(ACTION_STOP))
    }

    private fun onSeekTo(position: Long) {
        sendBroadcast(Intent(ACTION_SEEK_TO).apply {
            putExtra("position", position)
        })
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(true)
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}