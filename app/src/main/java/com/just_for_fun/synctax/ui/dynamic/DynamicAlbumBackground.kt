package com.just_for_fun.synctax.ui.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.just_for_fun.synctax.ui.utils.AlbumColors
import com.just_for_fun.synctax.ui.theme.MainBackground

@Composable
fun DynamicAlbumBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                MainBackground
            )
    ) {
        content()
    }
}