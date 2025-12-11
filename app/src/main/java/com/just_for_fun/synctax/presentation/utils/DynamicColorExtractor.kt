package com.just_for_fun.synctax.presentation.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object DynamicColorExtractor {
    suspend fun extractColorsFromAlbumArt(
        context: Context,
        albumArtUri: String?
    ): AlbumColors = withContext(Dispatchers.IO) {
        if (albumArtUri.isNullOrEmpty()) {
            return@withContext AlbumColors.default()
        }

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(Uri.parse(albumArtUri))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                
                AlbumColors(
                    vibrant = palette.vibrantSwatch?.let {
                        Color(it.rgb).copy(alpha = 0.15f)
                    } ?: Color(0xFF6200EE).copy(alpha = 0.15f),

                    darkVibrant = palette.darkVibrantSwatch?.let {
                        Color(it.rgb).copy(alpha = 0.25f)
                    } ?: Color(0xFF3700B3).copy(alpha = 0.25f),

                    lightVibrant = palette.lightVibrantSwatch?.let {
                        Color(it.rgb).copy(alpha = 0.08f)
                    } ?: Color(0xFFBB86FC).copy(alpha = 0.08f),

                    muted = palette.mutedSwatch?.let {
                        Color(it.rgb).copy(alpha = 0.12f)
                    } ?: Color(0xFF03DAC6).copy(alpha = 0.12f),

                    dominant = palette.dominantSwatch?.let {
                        Color(it.rgb).copy(alpha = 0.18f)
                    } ?: Color(0xFF6200EE).copy(alpha = 0.18f),

                    blackColor = palette.dominantSwatch?.let {
                        Color(it.rgb)
                    } ?: Color(0xFF000000)
                )
            } else {
                AlbumColors.default()
            }
        } catch (e: Exception) {
            AlbumColors.default()
        }
    }
}
