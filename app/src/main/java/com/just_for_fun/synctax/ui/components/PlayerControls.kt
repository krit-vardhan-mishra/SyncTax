package com.just_for_fun.synctax.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlin.math.abs

@Composable
internal fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(song) {
        visible = true
    }

    // Swipe gesture state for album art area
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RectangleShape,
            tonalElevation = 3.dp
        ) {
            Column {
                // Progress bar
                if (duration > 0) {
                    LinearProgressIndicator(
                        progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable area for song info (opening full player) with swipe on album art
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album art container with swipe gesture
                        Box(
                            modifier = Modifier
                                .offset(x = albumArtOffsetX.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            when {
                                                albumArtOffsetX > swipeThreshold -> {
                                                    // Swiped right - Previous song
                                                    onPreviousClick()
                                                }
                                                albumArtOffsetX < -swipeThreshold -> {
                                                    // Swiped left - Next song
                                                    onNextClick()
                                                }
                                            }
                                            albumArtOffsetX = 0f
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            albumArtOffsetX += dragAmount
                                        }
                                    )
                                }
                        ) {
                            AnimatedAlbumArt(
                                albumArtUri = song.albumArtUri,
                                isPlaying = isPlaying,
                                title = song.title
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Song info - clickable to open full player
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onClick)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Controls area (NOT covered by the main clickable above)
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedPlayPauseButton(
                            isPlaying = isPlaying,
                            onClick = {
                                // Prevent event propagation and ensure click is handled
                                onPlayPauseClick()
                            }
                        )

                        IconButton(
                            onClick = {
                                // Prevent event propagation and ensure click is handled
                                onNextClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedAlbumArt(
    albumArtUri: String?,
    isPlaying: Boolean,
    title: String
) {
    Surface(
        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (albumArtUri.isNullOrEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            coil.compose.AsyncImage(
                model = albumArtUri,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun AnimatedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Crossfade(targetState = isPlaying, label = "play_pause") { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}