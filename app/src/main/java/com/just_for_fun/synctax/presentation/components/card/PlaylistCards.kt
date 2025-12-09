package com.just_for_fun.synctax.presentation.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Playlist
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistCardLarger(
    playlist: Playlist,
    onClick: () -> Unit
) {
//    2x4 height x width
    val roundedShape = RoundedCornerShape(12.dp)
    val cardHeight = 250.dp // Define a fixed height for the card

    Card(
        onClick = onClick,
        // The container color is not used because the background is the image
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight) // Set the card height
            .clip(roundedShape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Full-bleed Thumbnail Background
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = "Playlist cover art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box( // Fallback for no thumbnail
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF202020)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No cover art icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Optional: Gradient Overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // 2. Content Column (Title, Description, User)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween // Push the controls to the bottom
            ) {
                // Top Section (Playlist Tag and Menu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PLAYLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(60.dp)) // Spacing for central alignment

                // Middle Section (Name, Description, User Info)
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!playlist.description.isNullOrEmpty()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Fallback using Song Count and Platform/Creation Date
                        val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(playlist.createdAt))
                        Text(
                            text = "${playlist.songCount} songs • ${playlist.platform} • Created $dateString",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Display User Information (Hardcoded for example since it's not in Playlist Entity)
                    Text(
                        text = "Created by John5525", // Placeholder: Replace with actual user info
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 3. Playlist Description Bar (at the bottom)
                PlaylistDescriptionBar(playlist)
            }
        }
    }
}


@Composable
fun PlaylistCardMedium(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val roundedShape = RoundedCornerShape(12.dp)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(roundedShape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail Background
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = "Playlist cover art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF202020)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No cover art icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Overlay for text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            // Playlist name at the bottom
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun PlaylistCardSmall(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val roundedShape = RoundedCornerShape(12.dp)
    val cardHeight = 120.dp // Smaller height for compact view

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(roundedShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side: Thumbnail
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (playlist.thumbnailUrl != null) {
                    AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = "Playlist cover art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF202020)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No cover art icon",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Right side: Details
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val description = if (!playlist.description.isNullOrEmpty()) {
                    playlist.description
                } else {
                    val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(playlist.createdAt))
                    "${playlist.songCount} songs • ${playlist.platform} • Created $dateString"
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistDescriptionBar(
    playlist: Playlist
) {
    val description = if (!playlist.description.isNullOrEmpty()) {
        playlist.description
    } else {
        // Fallback using Song Count and Platform/Creation Date
        val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(playlist.createdAt))
        "${playlist.songCount} songs • ${playlist.platform} • Created $dateString"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = durationMs / 60000
    val seconds = (durationMs % 60000) / 1000
    return String.format("%d:%02d", minutes, seconds)
}