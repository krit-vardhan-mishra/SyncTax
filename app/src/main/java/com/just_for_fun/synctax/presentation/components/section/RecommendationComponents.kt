package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.core.service.RecommendationService
import com.just_for_fun.synctax.presentation.utils.shimmer

/**
 * Main recommendations section for the Home screen.
 * Displays a 3x3 grid of 9 recommendations.
 */
@Composable
fun RecommendationsSection(
    recommendations: RecommendationService.RecommendationResult,
    onSongClick: (OnlineSearchResult) -> Unit,
    onViewAllClick: () -> Unit,
    getRecommendationReason: (OnlineSearchResult) -> String,
    modifier: Modifier = Modifier,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    Column(modifier = modifier) {
        // Header with View All button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Online Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onViewAllClick) {
                Text("View All")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View All",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // 3x3 Grid of 9 recommendations - don't filter by duration since online results may not have it
        val gridRecommendations = (recommendations.artistBased +
                recommendations.similarSongs +
                recommendations.discovery)
            .distinctBy { it.id }
            .shuffled()
            .take(9)

        if (gridRecommendations.isNotEmpty()) {
            RecommendationsQuickAccessGrid(
                songs = gridRecommendations,
                onSongClick = onSongClick,
                modifier = Modifier.height(400.dp),
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recommendations available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Quick Access Grid for Online Recommendations - 3x3 Grid of random recommendation thumbnails
 * Adapted from QuickAccessGrid but for OnlineSearchResult
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsQuickAccessGrid(
    songs: List<OnlineSearchResult>,
    onSongClick: (OnlineSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    currentSong: OnlineSearchResult? = null,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (songs.isEmpty()) {
            EmptyRecommendationsStateCard()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(songs.take(9)) { song ->
                    RecommendationSpeedDialItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        isPlaying = song.id == currentSong?.id,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationSpeedDialItem(
    song: OnlineSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showOptionsDialog by remember { mutableStateOf(false) }

    // Use ElevatedCard for a nice pop off the background
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showOptionsDialog = true
                }
            )
            .then(
                if (isPlaying) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Background Image or Fallback Color
            if (song.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback container for no image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                }
            }

            // 2. Gradient Overlay (Scrim)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // 3. Playing Overlay
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }

            // 4. Text Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                )
            }
        }
    }

    // Create options for the dialog
    val dialogOptions = remember(song, onAddToQueue, onAddToPlaylist) {
        mutableListOf<com.just_for_fun.synctax.presentation.components.player.DialogOption>().apply {
            // Play option
            add(
                com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "play",
                    title = "Play",
                    subtitle = "Play this song",
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onClick
                )
            )

            // Add to Queue option
            onAddToQueue?.let {
                add(
                    com.just_for_fun.synctax.presentation.components.player.DialogOption(
                        id = "add_to_queue",
                        title = "Add to Queue",
                        subtitle = "Add to current playlist",
                        icon = {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { it(song) }
                    )
                )
            }

            // Add to Playlist option
            onAddToPlaylist?.let {
                add(
                    com.just_for_fun.synctax.presentation.components.player.DialogOption(
                        id = "add_to_playlist",
                        title = "Add to Playlist",
                        subtitle = "Save to a playlist",
                        icon = {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { it(song) }
                    )
                )
            }
        }
    }

    // Bottom options dialog
    com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog(
        song = null, // No song entity for online results
        isVisible = showOptionsDialog,
        onDismiss = { showOptionsDialog = false },
        options = dialogOptions,
        title = "Song Options",
        description = "Choose an action for this song",
        songTitle = song.title,
        songArtist = song.author,
        songThumbnail = song.thumbnailUrl
    )
}

@Composable
private fun EmptyRecommendationsStateCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Recommendations Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Start listening to music and we'll suggest personalized recommendations here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Category section for detail screen with horizontal scroll.
 * Redesigned to match the app's design pattern with ElevatedCard and gradient overlays.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecommendationCategorySection(
    title: String,
    songs: List<OnlineSearchResult>,
    onSongClick: (OnlineSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    if (songs.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 12.dp)) {
        // Section Header
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal scrolling list of recommendation cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = songs,
                key = { it.id }
            ) { song ->
                RecommendationCarouselCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist
                )
            }
        }
    }
}

/**
 * Carousel-style card inspired by OnlineHistoryCarousel design.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecommendationCarouselCard(
    song: OnlineSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showOptionsDialog by remember { mutableStateOf(false) }

    // Create options for the dialog
    val dialogOptions = remember(song, onAddToQueue, onAddToPlaylist) {
        mutableListOf<com.just_for_fun.synctax.presentation.components.player.DialogOption>().apply {
            // Play option
            add(
                com.just_for_fun.synctax.presentation.components.player.DialogOption(
                    id = "play",
                    title = "Play",
                    subtitle = "Play this song",
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onClick
                )
            )

            // Add to Queue option
            onAddToQueue?.let {
                add(
                    com.just_for_fun.synctax.presentation.components.player.DialogOption(
                        id = "add_to_queue",
                        title = "Add to Queue",
                        subtitle = "Add to current playlist",
                        icon = {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { it(song) }
                    )
                )
            }

            // Add to Playlist option
            onAddToPlaylist?.let {
                add(
                    com.just_for_fun.synctax.presentation.components.player.DialogOption(
                        id = "add_to_playlist",
                        title = "Add to Playlist",
                        subtitle = "Save to a playlist",
                        icon = {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { it(song) }
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showOptionsDialog = true
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Shadow overlay on bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            startY = 120f
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(180.dp)
        )

        Text(
            text = song.author ?: "Unknown Artist",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(180.dp)
        )
    }

    // Bottom options dialog
    com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog(
        song = null,
        isVisible = showOptionsDialog,
        onDismiss = { showOptionsDialog = false },
        options = dialogOptions,
        title = "Song Options",
        description = "Choose an action for this song",
        songTitle = song.title,
        songArtist = song.author,
        songThumbnail = song.thumbnailUrl
    )
}

/**
 * Loading skeleton for recommendations.
 */
@Composable
fun RecommendationSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }

        // 3x3 Grid skeleton
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp),
            userScrollEnabled = false
        ) {
            items(9) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmer()
                    )
                }
            }
        }
    }
}

/**
 * Empty state prompt when no recommendations available.
 */
@Composable
fun EmptyRecommendationsPrompt(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Start listening to get personalized recommendations",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Play some songs and we'll suggest music you'll love",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onExploreClick) {
                Text("Explore Music")
            }
        }
    }
}

/**
 * Song list item for shuffle batch display.
 */
@Composable
fun SongListItem(
    song: OnlineSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.author ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play button
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play"
            )
        }
    }
}