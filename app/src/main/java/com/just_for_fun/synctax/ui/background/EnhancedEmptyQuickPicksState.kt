package com.just_for_fun.synctax.ui.background

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.theme.LightEmptyStateIcon
import com.just_for_fun.synctax.ui.theme.LightEmptyStateText
import com.just_for_fun.synctax.ui.theme.LightQuickPickCardBackground
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun EnhancedEmptyQuickPicksState(
    albumColors: AlbumColors,
    trainingDataSize: Int = 0
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val backgroundColor = if (isDarkTheme) Color(0x8E252526) else LightQuickPickCardBackground.copy(alpha = 0.8f)
    val iconColor = if (isDarkTheme) Color.LightGray else LightEmptyStateIcon
    val textColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else LightEmptyStateText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = MaterialTheme.shapes.large,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconColor
            )
            Text(
                text = "The app is analyzing your listening habits to find the best songs for you. Training data: $trainingDataSize plays.",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Listen to your favorite songs and the app will learn your taste",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}