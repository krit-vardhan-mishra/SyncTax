package com.just_for_fun.synctax.ui.dynamic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun DynamicSectionBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    useAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = if (useAccent) {
            albumColors.dominant.copy(alpha = 0.15f)
        } else {
            albumColors.muted.copy(alpha = 0.08f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}
