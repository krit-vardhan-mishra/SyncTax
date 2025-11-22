package com.just_for_fun.synctax.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.service.MusicService
import com.just_for_fun.synctax.core.data.preferences.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_SHUFFLE = "ACTION_SHUFFLE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_SONG_ALBUM = "extra_song_album"
        const val EXTRA_SONG_ALBUM_ART = "extra_song_album_art"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_SHUFFLE_ON = "extra_shuffle_on"
        const val EXTRA_IS_ONLINE = "extra_is_online"
        const val EXTRA_LYRICS = "extra_lyrics"

        private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        // Lyrics caching to prevent continuous refresh
        private var lastLyricsUpdate = 0L
        private var cachedLyrics: String? = null
        private const val LYRICS_UPDATE_INTERVAL = 2000L // 2 seconds minimum between lyrics updates

        fun updateWidget(
            context: Context,
            songTitle: String?,
            songArtist: String?,
            songAlbum: String?,
            albumArtUri: String?,
            isPlaying: Boolean,
            position: Long,
            duration: Long,
            shuffleOn: Boolean,
            lyrics: String? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val isOnline = albumArtUri?.startsWith("http") == true || songTitle?.startsWith("http") == true

            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                putExtra(EXTRA_SONG_TITLE, songTitle)
                putExtra(EXTRA_SONG_ARTIST, songArtist)
                putExtra(EXTRA_SONG_ALBUM, songAlbum)
                putExtra(EXTRA_SONG_ALBUM_ART, albumArtUri)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_DURATION, duration)
                putExtra(EXTRA_SHUFFLE_ON, shuffleOn)
                putExtra(EXTRA_IS_ONLINE, isOnline)
                putExtra(EXTRA_LYRICS, lyrics)
            }

            context.sendBroadcast(intent)

            // Also update MusicInfoWidgetProvider
            val intent2 = Intent(context, MusicInfoWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(EXTRA_SONG_TITLE, songTitle)
                putExtra(EXTRA_SONG_ARTIST, songArtist)
                putExtra(EXTRA_SONG_ALBUM, songAlbum)
                putExtra(EXTRA_SONG_ALBUM_ART, albumArtUri)
                putExtra(EXTRA_IS_ONLINE, isOnline)
            }

            context.sendBroadcast(intent2)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val widgetPrefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                widgetPrefs.getSongTitle(),
                widgetPrefs.getSongArtist(),
                widgetPrefs.getSongAlbum(),
                widgetPrefs.getAlbumArtUri(),
                widgetPrefs.isPlaying(),
                widgetPrefs.getPosition(),
                widgetPrefs.getDuration(),
                widgetPrefs.isShuffleOn(),
                widgetPrefs.getAlbumArtUri()?.startsWith("http") == true || widgetPrefs.getSongTitle().startsWith("http")
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Widget was resized, update with new layout
        // We don't have the current state here, so we might need to persist it or wait for next update
        // For now, just re-apply default state to switch layout
        val widgetPrefs = WidgetPreferences(context)
        updateAppWidget(
            context, 
            appWidgetManager, 
            appWidgetId, 
            widgetPrefs.getSongTitle(),
            widgetPrefs.getSongArtist(),
            widgetPrefs.getSongAlbum(),
            widgetPrefs.getAlbumArtUri(),
            widgetPrefs.isPlaying(),
            widgetPrefs.getPosition(),
            widgetPrefs.getDuration(),
            widgetPrefs.isShuffleOn(),
            widgetPrefs.getAlbumArtUri()?.startsWith("http") == true || widgetPrefs.getSongTitle().startsWith("http")
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val widgetPrefs = WidgetPreferences(context)
                val isCurrentlyPlaying = widgetPrefs.isPlaying()
                
                val action = if (isCurrentlyPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    this.action = action
                }
                context.startService(serviceIntent)
            }
            ACTION_NEXT -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_NEXT
                }
                context.startService(serviceIntent)
            }
            ACTION_PREVIOUS -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PREVIOUS
                }
                context.startService(serviceIntent)
            }
            ACTION_SHUFFLE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_SHUFFLE
                }
                context.startService(serviceIntent)
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MusicWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE)
                val songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST)
                val songAlbum = intent.getStringExtra(EXTRA_SONG_ALBUM)
                val albumArtUri = intent.getStringExtra(EXTRA_SONG_ALBUM_ART)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                val shuffleOn = intent.getBooleanExtra(EXTRA_SHUFFLE_ON, false)
                val isOnline = intent.getBooleanExtra(EXTRA_IS_ONLINE, false)
                val lyrics = intent.getStringExtra(EXTRA_LYRICS)

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        songTitle ?: "",
                        songArtist ?: "",
                        songAlbum ?: "",
                        albumArtUri,
                        isPlaying,
                        position,
                        duration,
                        shuffleOn,
                        isOnline,
                        lyrics
                    )
                }
            }
        }
    }

    private fun getLayoutForSize(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            // Breakpoints (approximate dp values)
            // Cells: 1x1 ~ 60x60, 2x2 ~ 130x130, 4x1 ~ 270x60, 4x2 ~ 270x130, 4x4 ~ 270x270

            return when {
                // Big Widget (4x4+) -> Lyrics support
                width >= 300 && height >= 250 -> R.layout.widget_music_big
                
                // Large Widget -> Album art focus
                width >= 250 && height >= 150 -> R.layout.widget_music_large
                
                // Circular Widget (2x2 approx) -> Circular design
                width in 110..200 && height in 110..200 -> R.layout.widget_music_circular
                
                // Large Widget -> Album art focus (replaces medium)
                width >= 200 && height >= 100 -> R.layout.widget_music_large
                
                // Small Widget (4x1 or smaller) -> Minimal
                else -> R.layout.widget_music_small
            }
        }
        return R.layout.widget_music_large // Default
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        songTitle: String,
        songArtist: String,
        songAlbum: String,
        albumArtUri: String?,
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        shuffleOn: Boolean,
        isOnline: Boolean,
        lyrics: String? = null
    ) {
        try {
            // Get appropriate layout based on widget size
            val layoutId = getLayoutForSize(context, appWidgetManager, appWidgetId)
            val views = RemoteViews(context.packageName, layoutId)

            // Set up click intent to open the app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            // Set up control intents
            setupControlButtons(context, views)

            // Update widget content
            val displayTitle = if (songTitle.isNotEmpty()) songTitle else "No song playing"
            val displayArtist = if (songArtist.isNotEmpty()) {
                songArtist
            } else if (songTitle.isEmpty()) {
                "Tap to open app"
            } else {
                ""
            }
            
            views.setTextViewText(R.id.widget_song_title, displayTitle)
            views.setTextViewText(R.id.widget_artist, displayArtist)

            // Update Album Name (if view exists)
            try {
                if (isOnline) {
                    views.setTextViewText(R.id.widget_album, "") // Hide album for online songs
                } else {
                    views.setTextViewText(R.id.widget_album, songAlbum)
                }
            } catch (e: Exception) {
                // View might not exist
            }

            // Update Lyrics Placeholder (if view exists) with caching to prevent continuous refresh
            try {
                // Determine the text to display based on lyrics input
                val lyricsTextToDisplay = when {
                    lyrics != null && lyrics.isNotEmpty() -> lyrics
                    isOnline -> "" // Blank space for online
                    else -> "Lyrics not available for this song.\n\n(Lyrics feature coming soon)" // Placeholder
                }
                
                // Check if this is actual lyrics content (not placeholder or empty)
                val isActualLyrics = lyrics != null && lyrics.isNotEmpty()
                
                // Only update lyrics if text has changed
                if (lyricsTextToDisplay != cachedLyrics) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Always update immediately if:
                    // 1. We're getting actual lyrics (prioritize real content)
                    // 2. Enough time has passed since last update (throttle placeholder/empty updates)
                    val shouldUpdateLyrics = isActualLyrics || (currentTime - lastLyricsUpdate > LYRICS_UPDATE_INTERVAL)
                    
                    if (shouldUpdateLyrics) {
                        views.setTextViewText(R.id.widget_lyrics, lyricsTextToDisplay)
                        cachedLyrics = lyricsTextToDisplay
                        lastLyricsUpdate = currentTime
                    }
                }
            } catch (e: Exception) {
                // View might not exist in this layout
                android.util.Log.d("MusicWidgetProvider", "Lyrics view not available in this layout")
            }

            // Update progress
            val progress = if (duration > 0) {
                ((position.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            
            // Only set progress bar if it exists in this layout
            try {
                views.setProgressBar(R.id.widget_progress, 100, progress, false)
                views.setTextViewText(R.id.widget_current_time, formatTime(position))
                views.setTextViewText(R.id.widget_duration, formatTime(duration))
            } catch (e: Exception) {
                // Progress bar doesn't exist in small layout
            }

            // Update play/pause icon
            val playPauseIcon = if (isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
            views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

            // Load album art asynchronously
            if (!albumArtUri.isNullOrEmpty()) {
                widgetScope.launch {
                    val bitmap = loadAlbumArt(context, albumArtUri)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
            }

            // Apply the update
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e("MusicWidgetProvider", "Error updating widget", e)
        }
    }

    private fun setupControlButtons(context: Context, views: RemoteViews) {
        // Play/Pause
        val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        views.setOnClickPendingIntent(
            R.id.widget_play_pause,
            PendingIntent.getBroadcast(
                context, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Next (if exists)
        try {
            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_next,
                PendingIntent.getBroadcast(
                    context, 2, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } catch (e: Exception) {
            // Button doesn't exist in this layout
        }

        // Previous (if exists)
        try {
            val previousIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PREVIOUS
            }
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                PendingIntent.getBroadcast(
                    context, 3, previousIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } catch (e: Exception) {
            // Button doesn't exist in this layout
        }

        // Shuffle (if exists)
        try {
            val shuffleIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_SHUFFLE
            }
            views.setOnClickPendingIntent(
                R.id.widget_equalizer,
                PendingIntent.getBroadcast(
                    context, 4, shuffleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } catch (e: Exception) {
            // Button doesn't exist in this layout
        }
    }

    private suspend fun loadAlbumArt(context: Context, albumArtUri: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (albumArtUri.startsWith("http")) {
                    val url = URL(albumArtUri)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.inputStream.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                            Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                        }
                    }
                } else if (albumArtUri.startsWith("/") || albumArtUri.contains(":\\")) {
                    // Handle absolute file paths (from downloaded songs)
                    val file = File(albumArtUri)
                    if (file.exists()) {
                        file.inputStream().use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                                Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                            }
                        }
                    } else {
                        null
                    }
                } else {
                    val uri = Uri.parse(albumArtUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                            Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicWidgetProvider", "Error loading album art: $albumArtUri", e)
                null
            }
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val sec = (ms / 1000)
        val min = sec / 60
        val rem = sec % 60
        return String.format("%d:%02d", min, rem)
    }
}