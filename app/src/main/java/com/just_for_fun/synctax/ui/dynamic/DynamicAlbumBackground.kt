package com.just_for_fun.synctax.ui.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.just_for_fun.synctax.ui.theme.LightMainBackground
import com.just_for_fun.synctax.ui.theme.MainBackground
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun DynamicAlbumBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Theme-aware background color
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) MainBackground else LightMainBackground

    Box(
        modifier = modifier
            .background(
                backgroundColor
            )
    ) {
        content()
    }
}