package com.just_for_fun.synctax.ui.utils

import androidx.compose.ui.graphics.Color

data class AlbumColors(
    val vibrant: Color,
    val darkVibrant: Color,
    val lightVibrant: Color,
    val muted: Color,
    val dominant: Color,
    val blackColor: Color,
) {
    companion object {
        // Black-based fallback when album colors are not available
        fun default() = AlbumColors(
            vibrant = Color(0xFF6200EE).copy(alpha = 0.15f),
            darkVibrant = Color(0xFF3700B3).copy(alpha = 0.25f),
            lightVibrant = Color(0xFFBB86FC).copy(alpha = 0.08f),
            muted = Color(0xFF03DAC6).copy(alpha = 0.12f),
            dominant = Color(0xFF6200EE).copy(alpha = 0.18f),
            blackColor =  Color(0xFF000000),
        )
    }
}