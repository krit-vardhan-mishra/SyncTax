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
import android.widget.RemoteViews
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MusicInfoWidgetProvider : AppWidgetProvider() {

    companion object {
        private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "", "", "", null, false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicInfoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val songTitle = intent.getStringExtra(MusicWidgetProvider.EXTRA_SONG_TITLE)
            val songArtist = intent.getStringExtra(MusicWidgetProvider.EXTRA_SONG_ARTIST)
            val songAlbum = intent.getStringExtra(MusicWidgetProvider.EXTRA_SONG_ALBUM)
            val albumArtUri = intent.getStringExtra(MusicWidgetProvider.EXTRA_SONG_ALBUM_ART)
            val isOnline = intent.getBooleanExtra(MusicWidgetProvider.EXTRA_IS_ONLINE, false)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    songTitle ?: "",
                    songArtist ?: "",
                    songAlbum ?: "",
                    albumArtUri,
                    isOnline
                )
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        songTitle: String,
        songArtist: String,
        songAlbum: String,
        albumArtUri: String?,
        isOnline: Boolean
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_info)

            // Set up click intent
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

            // Update content
            val displayTitle = if (songTitle.isNotEmpty()) songTitle else "No song playing"
            views.setTextViewText(R.id.widget_song_title, displayTitle)
            views.setTextViewText(R.id.widget_artist, songArtist)
            
            // Play count (Placeholder for now as it's not in broadcast)
            // We could fetch it from DB if we had the song ID, but we only have title/artist
            views.setTextViewText(R.id.widget_play_count, "Now Playing")

            // Load album art
            if (!albumArtUri.isNullOrEmpty()) {
                widgetScope.launch {
                    val bitmap = loadAlbumArt(context, albumArtUri)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.app_icon_main)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e("MusicInfoWidgetProvider", "Error updating widget", e)
        }
    }

    private suspend fun loadAlbumArt(context: Context, albumArtUri: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val targetSize = 300
                if (albumArtUri.startsWith("http")) {
                    val url = URL(albumArtUri)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.inputStream.use { inputStream ->
                        // Use inSampleSize for memory-efficient decoding
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                        options.inJustDecodeBounds = false
                        
                        connection.inputStream.use { secondStream ->
                            BitmapFactory.decodeStream(secondStream, null, options)?.let { bitmap ->
                                if (bitmap.width > targetSize || bitmap.height > targetSize) {
                                    Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true).also {
                                        if (it != bitmap) bitmap.recycle()
                                    }
                                } else bitmap
                            }
                        }
                    }
                } else if (albumArtUri.startsWith("/") || albumArtUri.contains(":\\")) {
                    // Handle absolute file paths (from downloaded songs)
                    val file = File(albumArtUri)
                    if (file.exists()) {
                        // Use BitmapFactory.Options for memory-efficient decoding
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                        options.inJustDecodeBounds = false
                        
                        BitmapFactory.decodeFile(file.absolutePath, options)?.let { bitmap ->
                            if (bitmap.width > targetSize || bitmap.height > targetSize) {
                                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true).also {
                                    if (it != bitmap) bitmap.recycle()
                                }
                            } else bitmap
                        }
                    } else {
                        null
                    }
                } else {
                    val uri = Uri.parse(albumArtUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                            Bitmap.createScaledBitmap(bitmap, 300, 300, true)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicInfoWidgetProvider", "Error loading album art: $albumArtUri", e)
                null
            }
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
