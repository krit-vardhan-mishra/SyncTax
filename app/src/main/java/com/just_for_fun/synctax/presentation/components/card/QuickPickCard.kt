package com.just_for_fun.synctax.presentation.components.card

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.player.AnimatedWaveform
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickPickCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onAddToQueue: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    // Create comprehensive options for the dialog
    val dialogOptions = remember(song, onAddToQueue, onToggleFavorite, onAddToPlaylist, isFavorite) {
        mutableListOf<DialogOption>().apply {
            // Play Now option
            add(DialogOption(
                id = "play_now",
                title = "Play Now",
                subtitle = "Play this song immediately",
                icon = {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onClick
            ))
            
            // Add to Queue option
            onAddToQueue?.let {
                add(DialogOption(
                    id = "add_to_queue",
                    title = "Add to Queue",
                    subtitle = "Add to end of current queue",
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Toggle Favorite option
            onToggleFavorite?.let {
                add(DialogOption(
                    id = "toggle_favorite",
                    title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    subtitle = if (isFavorite) "Remove from your liked songs" else "Add to your liked songs",
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Add to Playlist option
            onAddToPlaylist?.let {
                add(DialogOption(
                    id = "add_to_playlist",
                    title = "Add to Playlist",
                    subtitle = "Save to a playlist",
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
        }
    }

    // Determine the active color (Primary if playing, otherwise transparent/default)
    val activeColor = MaterialTheme.colorScheme.primary
    val activeShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .width(160.dp)
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showOptionsDialog = true
                }
            )
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Album art container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // 1. Add a Border if playing
                .then(
                    if (isPlaying) {
                        Modifier
                            .shadow(
                                elevation = 12.dp,
                                shape = activeShape,
                                ambientColor = activeColor,
                                spotColor = activeColor
                            )
                            .border(
                                width = 2.dp,
                                color = activeColor,
                                shape = activeShape
                            )
                    } else {
                        Modifier
                    }
                ),
            shape = activeShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // The Image Layer
                if (song.albumArtUri.isNullOrEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Overlay Layer: If playing, darken image and show Icon
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)), // Dim effect
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedWaveform(activeColor)
                    }
                }
            }
        }

        // Song info
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                // 3. Text Color: Change to Primary if playing, stay White if not
                color = if (isPlaying) activeColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    
    // Bottom options dialog
    BottomOptionsDialog(
        song = song,
        isVisible = showOptionsDialog,
        onDismiss = { showOptionsDialog = false },
        options = dialogOptions
    )
}
