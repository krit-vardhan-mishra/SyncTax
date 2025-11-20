package com.just_for_fun.synctax.ui.background

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SectionBackground(
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