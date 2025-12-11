package com.just_for_fun.synctax.presentation.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.utils.AlbumColors

@Composable
fun DynamicAlbumBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(AppColors.mainBackground)
    ) {
        content()
    }
}
