package com.just_for_fun.synctax.ui.components.card

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onDelete: ((Song) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album art with animation
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            if (song.albumArtUri.isNullOrEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
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

            // Playing indicator overlay
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PlayingIndicator()
                }
            }
        }

        // Song info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album ?: "Unknown Album"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More options
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        showDeleteDialog = true
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song") },
            text = { 
                Text("Are you sure you want to permanently delete \"${song.title}\"? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // Delete the file permanently
                        try {
                            val file = File(song.filePath)
                            if (file.exists()) {
                                file.delete()
                            }
                            onDelete?.invoke(song)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}