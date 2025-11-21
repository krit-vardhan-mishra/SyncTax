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
import android.widget.RemoteViews
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.    net.HttpURLConnection
import java.net.URL

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_SHUFFLE = "ACTION_SHUFFLE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_SONG_ALBUM_ART = "extra_song_album_art"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_SHUFFLE_ON = "extra_shuffle_on"

        private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        fun updateWidget(
            context: Context,
            songTitle: String?,
            songArtist: String?,
            albumArtUri: String?,
            isPlaying: Boolean,
            position: Long,
            duration: Long,
            shuffleOn: Boolean
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                putExtra(EXTRA_SONG_TITLE, songTitle)
                putExtra(EXTRA_SONG_ARTIST, songArtist)
                putExtra(EXTRA_SONG_ALBUM_ART, albumArtUri)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_POSITION, position)
                putExtra(EXTRA_DURATION, duration)
                putExtra(EXTRA_SHUFFLE_ON, shuffleOn)
            }

            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                "",
                "",
                null,
                false,
                0L,
                0L,
                false
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
        updateAppWidget(context, appWidgetManager, appWidgetId, "", "", null, false, 0L, 0L, false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
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
                // Handle shuffle action
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MusicWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE)
                val songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST)
                val albumArtUri = intent.getStringExtra(EXTRA_SONG_ALBUM_ART)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                val shuffleOn = intent.getBooleanExtra(EXTRA_SHUFFLE_ON, false)

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        songTitle ?: "",
                        songArtist ?: "",
                        albumArtUri,
                        isPlaying,
                        position,
                        duration,
                        shuffleOn
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

            // Choose layout based on size
            return when {
                width < 150 || height < 80 -> R.layout.widget_music_small
                height < 120 -> R.layout.widget_music_medium
                else -> R.layout.widget_music_circular // Large layout
            }
        }
        return R.layout.widget_music_medium // Default
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        songTitle: String,
        songArtist: String,
        albumArtUri: String?,
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        shuffleOn: Boolean
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
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
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

        // Equalizer (if exists)
        try {
            val equalizerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_equalizer,
                PendingIntent.getActivity(
                    context, 5, equalizerIntent,
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
                } else {
                    val uri = Uri.parse(albumArtUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                            Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicWidgetProvider", "Error loading album art", e)
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