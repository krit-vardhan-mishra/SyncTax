package com.just_for_fun.synctax.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.service.MusicService

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
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val songTitle = context.getString(R.string.app_name)
        val songArtist = ""
        val albumArtUri: String? = null
        val isPlaying = false
        val position = 0L
        val duration = 0L
        val shuffleOn = false

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                songTitle,
                songArtist,
                albumArtUri,
                isPlaying,
                position,
                duration,
                shuffleOn
            )
        }
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
                // Handle shuffle action - this would need to be implemented in MusicService
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
            val views = RemoteViews(context.packageName, R.layout.widget_music_circular)

            // Set gray background when no song is playing
            if (songTitle.isEmpty() || songTitle == context.getString(R.string.app_name)) {
                // Use proper color integer (ARGB format)
                views.setInt(R.id.widget_background_circle, "setBackgroundColor", 0xFF777777.toInt())
            } else {
                // Keep the default circular background (semi-transparent black)
                views.setInt(R.id.widget_background_circle, "setBackgroundColor", 0x4D000000.toInt())
            }

            // Set up click intent to open the app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            // Set up control intents
            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)

            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)

            val previousIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PREVIOUS
            }
            val previousPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_previous, previousPendingIntent)

            val shuffleIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_SHUFFLE
            }
            val shufflePendingIntent = PendingIntent.getBroadcast(
                context,
                4,
                shuffleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_shuffle, shufflePendingIntent)

            // Update widget content
            if (songTitle.isNotEmpty()) {
                views.setTextViewText(R.id.widget_song_title, songTitle)
                views.setTextViewText(R.id.widget_artist, songArtist)

                // Update progress
                val progress = if (duration > 0) ((position.toFloat() / duration.toFloat()) * 100).toInt() else 0
                views.setProgressBar(R.id.widget_progress, 100, progress, false)

                // Update duration text
                views.setTextViewText(R.id.widget_duration, formatTime(duration))
            } else {
                views.setTextViewText(R.id.widget_song_title, "No song playing")
                views.setTextViewText(R.id.widget_artist, "")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setTextViewText(R.id.widget_duration, "0:00")
            }

            // Update play/pause icon
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            // Log error and create a simple fallback widget
            e.printStackTrace()
            val fallbackViews = RemoteViews(context.packageName, R.layout.widget_music_circular)
            fallbackViews.setTextViewText(R.id.widget_song_title, "Widget Error")
            fallbackViews.setTextViewText(R.id.widget_artist, e.message ?: "Unknown error")
            appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
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