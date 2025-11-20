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
        color = androidx.compose.ui.graphics.Color(0xFF8e8a8aff), // Fixed section color
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}
