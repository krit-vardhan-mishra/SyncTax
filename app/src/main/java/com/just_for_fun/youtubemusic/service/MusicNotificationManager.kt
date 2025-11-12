package com.just_for_fun.youtubemusic.service

import android.R.attr.description
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import java.io.IOException
import androidx.media.app.NotificationCompat.MediaStyle
import com.just_for_fun.youtubemusic.MainActivity
import com.just_for_fun.youtubemusic.R
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages music playback notifications
 */
class MusicNotificationManager(
    private val context: Context,
    private val mediaSession: MediaSessionCompat,
    private val serviceScope: CoroutineScope
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        song: Song?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, MusicService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MusicService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, MusicService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, MusicService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(song?.title ?: "YouTube Music")
            .setContentText(song?.artist ?: "No song playing")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                R.drawable.ic_previous,
                "Previous",
                previousIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                R.drawable.ic_next,
                "Next",
                nextIntent
            )

        // Load album art for large icon
        song?.albumArtUri?.let { uri ->
            serviceScope.launch {
                val bitmap = loadAlbumArt(uri)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    // Update the notification
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }

        return builder.build()
    }

    private fun loadAlbumArt(albumArtUri: String?): Bitmap? {
        if (albumArtUri.isNullOrEmpty()) return null

        return try {
            val uri = Uri.parse(albumArtUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                // Resize bitmap for notification
                val maxSize = 512
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize

                Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}