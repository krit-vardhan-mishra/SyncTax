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
        color = if (useAccent) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}