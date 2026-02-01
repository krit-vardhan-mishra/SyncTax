package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Redesigned with Material 3 Expressive (Big shapes, centered layout, bold type).
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
    scrimAlpha: Float = 0.4f,
    songTitle: String? = null,
    songArtist: String? = null,
    songThumbnail: String? = null,
    customHeader: @Composable (() -> Unit)? = null
) {
    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlayerSurface,
        scrimColor = Color.Black.copy(alpha = scrimAlpha),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        // Expressive Spring Animation
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    slideInVertically(initialOffsetY = { it / 4 }, animationSpec = spring(dampingRatio = 0.8f)),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Ensures everything is centered
            ) {
                // --- EXPRESSIVE HEADER SECTION ---
                // Logic Flow: Custom -> Song Details (Entity or Manual) -> Generic Title/Desc

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (customHeader != null) {
                            customHeader()
                        } else {
                            // 1. Resolve Display Data
                            val displayTitle = song?.title ?: songTitle ?: title
                            val displaySubtitle = song?.artist ?: songArtist ?: description
                            val displayArt = song?.albumArtUri ?: songThumbnail

                            // 2. Display Artwork (if available, or generic placeholder if it's a song context)
                            if (displayArt != null || song != null || songTitle != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = PlayerTextSecondary.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .size(90.dp)
                                        .padding(bottom = 16.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    if (displayArt != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(displayArt)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = displayTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Fallback icon centered
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                contentDescription = null,
                                                tint = PlayerTextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Display Title
                            if (displayTitle != null) {
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = PlayerTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            // 4. Display Subtitle (Artist or Description)
                            if (displaySubtitle != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displaySubtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PlayerTextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // --- OPTIONS LIST ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(options) { option ->
                        // Staggered entry animation
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            ) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = Spring.StiffnessLow
                                )
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
            }
        }
    }
}

/**
 * Individual option item in the dialog
 * Icons are now perfectly centered within their container.
 */
@Composable
private fun OptionItem(
    option: DialogOption,
    onClick: () -> Unit
) {
    // Determine colors based on state
    val containerColor = if (option.enabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
    }

    val contentColor = if (option.enabled) {
        if (option.destructive) MaterialTheme.colorScheme.error else PlayerTextPrimary
    } else {
        PlayerTextSecondary.copy(alpha = 0.5f)
    }

    Surface(
        onClick = onClick,
        enabled = option.enabled,
        shape = RoundedCornerShape(24.dp), // Full pill shape
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container (Tonal look) - Perfectly Centered
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (option.destructive)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else
                            Color.Red.copy(alpha = 0.3f) // Changed to Red as requested
                    )
            ) {
                // The icon lambda is called directly in the center of the Box
                option.icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )

                option.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlayerTextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            // Arrow indicator (subtle)
            if (option.enabled) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = PlayerTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
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
            subtitle = "Start listening immediately",
            icon = {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            onClick = onPlayNow
        ),

        DialogOption(
            id = "add_to_queue",
            title = "Add to Queue",
            subtitle = "Play next in current session",
            icon = {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            onClick = onAddToQueue
        ),

        DialogOption(
            id = "add_to_playlist",
            title = "Add to Playlist",
            subtitle = "Save to your collection",
            icon = {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            onClick = onAddToPlaylist
        ),

        DialogOption(
            id = "download",
            title = if (isDownloaded) "Downloaded" else if (isDownloading) "Downloading..." else "Download",
            subtitle = if (isDownloaded) "Available offline" else if (isDownloading) "Please wait..." else "Listen without data",
            icon = {
                Icon(
                    if (isDownloaded) Icons.Rounded.DownloadDone
                    else if (isDownloading) Icons.Rounded.Downloading
                    else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            enabled = !isDownloaded && !isDownloading,
            onClick = onDownload
        ),

        DialogOption(
            id = "share",
            title = "Share",
            subtitle = "Send link to friends",
            icon = {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            onClick = onShare
        ),

        DialogOption(
            id = "view_details",
            title = "View Details",
            subtitle = "File info & tags",
            icon = {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            onClick = onViewDetails
        )
    ) + (onRemove?.let {
        listOf(
        DialogOption(
            id = "remove",
            title = "Remove",
            subtitle = "Delete from context",
            icon = {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            destructive = true,
            onClick = it
        )
        )
    } ?: emptyList())
}