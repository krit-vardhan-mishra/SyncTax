package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.AppColors

// Composable for displaying recommended songs
@Composable
fun RecommendedSongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit
) {
    // Theme-aware colors from AppColors
    val surfaceColor = AppColors.playerSurfaceVariant.copy(alpha = 0.3f)
    val indexColor = AppColors.playerTextSecondary
    val thumbnailBackground = AppColors.playerSurfaceVariant
    val titleColor = AppColors.playerTextPrimary
    val artistColor = AppColors.playerTextSecondary
    val playIconColor = AppColors.playerAccent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = indexColor,
                modifier = Modifier.width(32.dp)
            )

            // Thumbnail
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(thumbnailBackground, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play icon
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = playIconColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
