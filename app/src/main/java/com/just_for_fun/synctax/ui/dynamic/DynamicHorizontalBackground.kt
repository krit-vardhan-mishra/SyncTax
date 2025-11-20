package com.just_for_fun.synctax.ui.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun DynamicHorizontalBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                androidx.compose.ui.graphics.Color(0xFF2a2a2a) // Fixed screen background
            )
    ) {
        content()
    }
}