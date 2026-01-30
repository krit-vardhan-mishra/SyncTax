package com.just_for_fun.synctax.presentation.components.card

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.painter.ColorPainter
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.createSongOptions
import java.io.File

/**
 * Helper function to get MediaStore URI from file path
 */
private fun getUriFromFilePath(context: android.content.Context, filePath: String): Uri? {
    val projection = arrayOf(MediaStore.Audio.Media._ID)
    val selection = "${MediaStore.Audio.Media.DATA} = ?"
    val selectionArgs = arrayOf(filePath)
    
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        }
    }
    return null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onDelete: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onAddNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    isFavorite: Boolean = false,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    artistColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Create options for the dialog
    val dialogOptions = remember(song, onAddToPlaylist, onAddNext, onAddToQueue, onToggleFavorite, onDelete, isFavorite) {
        mutableListOf<com.just_for_fun.synctax.presentation.components.player.DialogOption>().apply {
            // Play Now option
            add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                id = "play_now",
                title = "Play Now",
                subtitle = "Play this song immediately",
                icon = {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onClick
            ))
            
            // Add to Queue option
            onAddToQueue?.let {
                add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "add_to_queue",
                    title = "Add to Queue",
                    subtitle = "Add to end of current queue",
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Play Next option
            onAddNext?.let {
                add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "add_next",
                    title = "Play Next",
                    subtitle = "Add to queue after current song",
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Add to Favorites option
            onToggleFavorite?.let {
                add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "toggle_favorite",
                    title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    subtitle = if (isFavorite) "Remove from your liked songs" else "Add to your liked songs",
                    icon = {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Add to Playlist option
            onAddToPlaylist?.let {
                add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "add_to_playlist",
                    title = "Add to Playlist",
                    subtitle = "Save to a playlist",
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { it(song) }
                ))
            }
            
            // Delete option
            onDelete?.let {
                add(com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "delete",
                    title = "Delete",
                    subtitle = "Remove from device",
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    destructive = true,
                    onClick = { showDeleteDialog = true }
                ))
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showOptionsDialog = true
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
            color = backgroundColor
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
                // Optimized image loading with placeholder and crossfade
                val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song.albumArtUri)
                        .size(56, 56)  // Downsample to exact display size
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(placeholderColor),
                    error = ColorPainter(placeholderColor),
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
                    titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album ?: "Unknown Album"}",
                style = MaterialTheme.typography.bodySmall,
                color = artistColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More options
        Box {
            IconButton(
                onClick = { showOptionsDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            var deleted = false
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Android 10+ - Use MediaStore API
                                try {
                                    val uri = getUriFromFilePath(context, song.filePath)
                                    if (uri != null) {
                                        deleted = context.contentResolver.delete(uri, null, null) > 0
                                        if (deleted) {
                                            android.util.Log.d("SongCard", "Deleted via MediaStore: ${song.filePath}")
                                        } else {
                                            android.util.Log.w("SongCard", "MediaStore delete returned 0 rows")
                                        }
                                    } else {
                                        android.util.Log.w("SongCard", "Could not find MediaStore URI for: ${song.filePath}")
                                        // Fallback to direct file deletion
                                        if (file.exists()) {
                                            deleted = file.delete()
                                            android.util.Log.d("SongCard", "Fallback file.delete() result: $deleted")
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    android.util.Log.e("SongCard", "SecurityException with MediaStore, trying direct delete", e)
                                    if (file.exists()) {
                                        deleted = file.delete()
                                    }
                                }
                            } else {
                                // Android 9 and below - Direct file deletion
                                if (file.exists()) {
                                    deleted = file.delete()
                                    if (deleted) {
                                        android.util.Log.d("SongCard", "Deleted file: ${file.absolutePath}")
                                    } else {
                                        android.util.Log.w("SongCard", "Failed to delete file: ${file.absolutePath}")
                                    }
                                } else {
                                    android.util.Log.w("SongCard", "File does not exist: ${file.path}")
                                }
                            }
                            
                            if (deleted || !file.exists()) {
                                onDelete?.invoke(song)
                            } else {
                                android.util.Log.e("SongCard", "Failed to delete file: ${song.filePath}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SongCard", "Error deleting file: ${song.filePath}", e)
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

    // Bottom options dialog
    BottomOptionsDialog(
        song = song,
        isVisible = showOptionsDialog,
        onDismiss = { showOptionsDialog = false },
        options = dialogOptions
    )
}
