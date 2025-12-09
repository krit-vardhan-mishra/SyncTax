package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurface
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextPrimary
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextSecondary

/**
 * Data class for dialog options
 */
data class DialogOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: @Composable () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Bottom dialog for displaying multiple options based on selection context
 *
 * @param song The selected song (can be null for general options)
 * @param isVisible Whether the dialog is visible
 * @param onDismiss Callback when dialog is dismissed
 * @param options List of options to display
 * @param title Optional title for the dialog
 * @param description Optional description for the dialog
 * @param scrimAlpha Transparency level for the scrim (0.0 to 1.0)
 * @param songTitle Alternative song title if Song entity not available
 * @param songArtist Alternative song artist if Song entity not available
 * @param songThumbnail Alternative song thumbnail URL if Song entity not available
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomOptionsDialog(
    song: Song? = null,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    options: List<DialogOption>,
    title: String? = null,
    description: String? = null,
    scrimAlpha: Float = 0.6f,
    songTitle: String? = null,
    songArtist: String? = null,
    songThumbnail: String? = null
) {
    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = PlayerSurface,
        scrimColor = Color.Black.copy(alpha = scrimAlpha)
    ) {
        // Custom opening/closing animation for content
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 300, delayMillis = 100)
            ) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(durationMillis = 400, delayMillis = 100)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 200)
            ) + slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(durationMillis = 250)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // General header with title and description (if no song)
                if (song == null && (title != null || description != null)) {
                    title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = PlayerTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = PlayerTextSecondary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = PlayerTextSecondary.copy(alpha = 0.2f)
                    )
                }

                // Alternative song header if song entity not provided but details are
                if (song == null && songTitle != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song thumbnail
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PlayerTextSecondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (songThumbnail != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(songThumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = songTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = PlayerTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            songArtist?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PlayerTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = PlayerTextSecondary.copy(alpha = 0.2f)
                    )
                }

                // Header with song info if available
                song?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song thumbnail placeholder
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PlayerTextSecondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (it.albumArtUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(it.albumArtUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = it.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = PlayerTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = it.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlayerTextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = PlayerTextSecondary.copy(alpha = 0.2f)
                    )
                }

                // Options list with staggered animation
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options) { option ->
                        val index = options.indexOf(option)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = 100 + (index * 50)
                                )
                            ) + slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = 100 + (index * 50)
                                )
                            ),
                            exit = fadeOut(
                                animationSpec = tween(durationMillis = 150)
                            ) + slideOutVertically(
                                targetOffsetY = { 30 },
                                animationSpec = tween(durationMillis = 150)
                            )
                        ) {
                            OptionItem(
                                option = option,
                                onClick = {
                                    option.onClick()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Individual option item in the dialog
 */
@Composable
private fun OptionItem(
    option: DialogOption,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = option.enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (option.destructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                } else {
                    PlayerTextSecondary.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    option.icon()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (option.enabled) {
                        if (option.destructive) MaterialTheme.colorScheme.error else PlayerTextPrimary
                    } else {
                        PlayerTextSecondary.copy(alpha = 0.5f)
                    },
                    fontWeight = FontWeight.Medium
                )

                option.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlayerTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            // Arrow indicator
            if (option.enabled) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = PlayerTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Helper function to create common dialog options for songs
 */
fun createSongOptions(
    song: Song,
    onPlayNow: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onViewDetails: () -> Unit,
    onRemove: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false
): List<DialogOption> {
    return listOf(
        DialogOption(
            id = "play_now",
            title = "Play Now",
            subtitle = "Play this song immediately",
            icon = {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onPlayNow
        ),

        DialogOption(
            id = "add_to_queue",
            title = "Add to Queue",
            subtitle = "Add to current playlist",
            icon = {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onAddToQueue
        ),

        DialogOption(
            id = "add_to_playlist",
            title = "Add to Playlist",
            subtitle = "Save to a playlist",
            icon = {
                Icon(
                    Icons.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onAddToPlaylist
        ),

        DialogOption(
            id = "download",
            title = if (isDownloaded) "Downloaded" else if (isDownloading) "Downloading..." else "Download",
            subtitle = if (isDownloaded) "Already downloaded" else if (isDownloading) "In progress" else "Save offline",
            icon = {
                Icon(
                    if (isDownloaded) Icons.Rounded.DownloadDone
                    else if (isDownloading) Icons.Rounded.Downloading
                    else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = if (isDownloaded) MaterialTheme.colorScheme.primary else PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            enabled = !isDownloaded && !isDownloading,
            onClick = onDownload
        ),

        DialogOption(
            id = "share",
            title = "Share",
            subtitle = "Share this song",
            icon = {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onShare
        ),

        DialogOption(
            id = "view_details",
            title = "View Details",
            subtitle = "Song information",
            icon = {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = PlayerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onViewDetails
        )
    ) + (onRemove?.let {
        listOf(
            DialogOption(
                id = "remove",
                title = "Remove",
                subtitle = "Remove from current context",
                icon = {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                },
                destructive = true,
                onClick = it
            )
        )
    } ?: emptyList())
}