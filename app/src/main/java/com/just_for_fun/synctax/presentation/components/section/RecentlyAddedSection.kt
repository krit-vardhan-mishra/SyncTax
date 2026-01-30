package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.utils.PolyShapes

/**
 * Section showing recently added songs with Material 3 Expressive Design.
 * Features consistent organic shapes (Cookie9) and bold typography.
 */
@Composable
fun RecentlyAddedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        ExpressiveSectionHeader(
            title = "Fresh Tunes",
            subtitle = "Recently added to your library",
            onViewAllClick = onViewAllClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(songs.take(10)) { song ->
                ExpressiveSongCard(
                    song = song,
                    shape = PolyShapes.Cookie9, // Unified shape for all items
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSongCard(
    song: Song,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        // 1. Album Art Container with Dynamic Shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // The Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album art for ${song.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape) // Apply the passed shape here
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            // Subtle Gradient Overlay for depth
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape) // Apply the same shape to the overlay
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f)),
                            startY = size.height / 2,
                            endY = size.height
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(gradient, blendMode = BlendMode.Overlay)
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Text Content
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpressiveSectionHeader(
    title: String,
    subtitle: String,
    onViewAllClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onViewAllClick != null) {
            IconButton(
                onClick = onViewAllClick,
                modifier = Modifier
                    .clip(PolyShapes.FullCircle)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View All",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private val PolyShapes.FullCircle
    get() = this.Circle