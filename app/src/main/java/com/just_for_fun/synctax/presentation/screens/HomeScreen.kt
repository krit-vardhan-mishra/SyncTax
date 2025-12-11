package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.components.chips.FilterChipsRow
import com.just_for_fun.synctax.presentation.components.onboarding.DirectorySelectionDialog
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.components.section.EmptyMusicState
import com.just_for_fun.synctax.presentation.components.section.EmptyRecommendationsPrompt
import com.just_for_fun.synctax.presentation.components.section.OnlineHistorySection
import com.just_for_fun.synctax.presentation.components.section.RecommendationsSection
import com.just_for_fun.synctax.presentation.components.section.RecommendationSkeleton
import com.just_for_fun.synctax.presentation.components.section.SavedPlaylistsSection
import com.just_for_fun.synctax.presentation.components.section.SectionHeader
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.components.section.SpeedDialSection
import com.just_for_fun.synctax.presentation.components.section.MostPlayedSection
import com.just_for_fun.synctax.presentation.components.utils.SortOption
import com.just_for_fun.synctax.presentation.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.presentation.dynamic.DynamicGreetingSection
import com.just_for_fun.synctax.presentation.guide.GuideContent
import com.just_for_fun.synctax.presentation.guide.GuideOverlay
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

// Composable helper functions for consistent spacing
@Composable
private fun SectionSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SmallSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun LargeSpacer() {
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ExtraLargeSpacer() {
    Spacer(modifier = Modifier.height(25.dp))
}

@Composable
private fun BottomPaddingSpacer() {
    Spacer(modifier = Modifier.height(80.dp))
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = AppColors.divider,
        thickness = 1.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    recommendationViewModel: RecommendationViewModel = viewModel(),
    userPreferences: UserPreferences,
    onTrainClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylist: (Int) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToOnlineSongs: () -> Unit = {},
    onNavigateToRecommendations: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val scanPaths by userPreferences.scanPaths.collectAsState()

    // Recommendation state
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isLoadingRecommendations by recommendationViewModel.isLoading.collectAsState()
    val hasEnoughHistory by recommendationViewModel.hasEnoughHistory.collectAsState()

    // Directory selection dialog state
    var showDirectorySelectionDialog by remember { mutableStateOf(false) }

    // Sorting state for All Songs section
    var currentSortOption by remember { mutableStateOf(SortOption.TITLE_ASC) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Theme-aware colors from AppColors (no need for isDarkTheme check)
    val backgroundColor = AppColors.homeBackground
    val cardBackgroundColor = AppColors.homeCardBackground
    val sectionTitleColor = AppColors.homeSectionTitle
    val sectionSubtitleColor = AppColors.homeSectionSubtitle
    val greetingTextColor = AppColors.homeGreetingText

    // Detect scroll to bottom - use remember to avoid recreating on every recomposition
    LaunchedEffect(Unit) { // Remove listState dependency to prevent constant relaunching
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            Pair(
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                layoutInfo.totalItemsCount
            )
        }
            .distinctUntilChanged() // Only emit when values actually change
            .collect { (lastVisibleItemIndex, totalItems) ->
                if (lastVisibleItemIndex >= totalItems - 1 && !hasScrolledToBottom && totalItems > 0) {
                    hasScrolledToBottom = true
                    // Refresh album art when user scrolls to bottom
                    homeViewModel.refreshAlbumArt()
                } else if (lastVisibleItemIndex < totalItems - 2) {
                    hasScrolledToBottom = false
                }
            }
    }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    // Update album colors when current song changes
    val currentAlbumArtUri = playerState.currentSong?.albumArtUri
    LaunchedEffect(currentAlbumArtUri) {
        // Only update if URI actually changed (LaunchedEffect already handles this, but explicit variable makes intent clear)
        dynamicBgViewModel.updateAlbumArt(currentAlbumArtUri)
    }

    // Set up callback for refreshing downloaded songs check (only once)
    LaunchedEffect(Unit) {
        homeViewModel.onSongsRefreshed = { songs ->
            playerViewModel.refreshDownloadedSongsCheck(songs)
        }
    }

    // Show content when songs are loaded
    LaunchedEffect(uiState.allSongs) {
        if (uiState.allSongs.isNotEmpty()) {
            isVisible = true
        }
    }

    // Collect error messages from player view model
    LaunchedEffect(Unit) {
        playerViewModel.errorMessages.collect { message ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, message)
        }
    }

    // Show error from uiState via Snackbar (auto-dismiss)
    LaunchedEffect(uiState.error) {
        uiState.error?.takeIf { it.isNotBlank() }?.let { error ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, error)
            homeViewModel.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                SimpleDynamicMusicTopAppBar(
                    title = "Music",
                    albumColors = albumColors,
                    showShuffleButton = true,
                    showRefreshButton = true,
                    showProfileButton = true,
                    onShuffleClick = {
                        if (uiState.allSongs.isNotEmpty()) {
                            // Use smart shuffle with recommendations on Home screen
                            playerViewModel.shufflePlayWithRecommendations(uiState.allSongs)
                        }
                    },
                    onRefreshClick = { homeViewModel.scanMusic() },
                    onTrainClick = onTrainClick,
                    onOpenSettings = onOpenSettings,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToStats = onNavigateToStats,
                    userPreferences = userPreferences,
                    userName = userName,
                    userInitial = userInitial
                )
            }
        ) { paddingValues ->
            DynamicAlbumBackground(
                albumColors = albumColors,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show directory selection dialog only once on first app launch
                LaunchedEffect(Unit) {
                    if (!userPreferences.isDirectorySelectionShown() && uiState.allSongs.isEmpty() && scanPaths.isEmpty()) {
                        showDirectorySelectionDialog = true
                        userPreferences.setDirectorySelectionShown()
                    }
                }

                // Set visibility to true even when no local songs (for online history display)
                LaunchedEffect(Unit) {
                    isVisible = true
                }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(animationSpec = tween(600)) { it / 4 }
                ) {
                    OptimizedLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Greeting Section with dynamic colors
                        if (userName.isNotEmpty()) {
                            item(
                                key = "header",
                                contentType = "header"
                            ) {
                                DynamicGreetingSection(
                                    userName = userName,
                                    albumColors = albumColors,
                                    greetingTextColor = greetingTextColor,
                                    userNameColor = sectionTitleColor,
                                    subGreetingColor = sectionSubtitleColor
                                )
                            }
                        }

                        // Filter Chips
                        item(
                            key = "filter_chips",
                            contentType = "filter"
                        ) {
                            FilterChipsRow(
                                selectedChip = selectedFilter,
                                onChipSelected = { chip -> selectedFilter = chip }
                            )
                        }

                        // Quick Picks Section - Now shows online listening history
                        if (selectedFilter == "All" || selectedFilter == "Quick Picks") {
                            item {
                                OnlineHistorySection(
                                    history = uiState.onlineHistory,
                                    onHistoryClick = { history ->
                                        playerViewModel.playUrl(
                                            url = history.watchUrl,
                                            title = history.title,
                                            artist = history.artist,
                                            durationMs = 0L
                                        )
                                    },
                                    onViewAllClick = onNavigateToOnlineSongs,
                                    currentVideoId = if (playerState.currentSong?.id?.startsWith(
                                            "online:"
                                        ) == true
                                    )
                                        playerState.currentSong?.id?.removePrefix("online:")
                                    else null,
                                    onRemoveFromHistory = { history ->
                                        homeViewModel.deleteOnlineHistory(history.videoId)
                                    }
                                )
                            }
                        }

                        if (selectedFilter == "All" || selectedFilter == "Quick Picks") {
                            item {
                                SmallSpacer()
                            }
                        }

                        // Listen Again Section (only show if there are local songs)
                        if ((selectedFilter == "All" || selectedFilter == "Listen Again") && uiState.allSongs.isNotEmpty()) {
                            @OptIn(ExperimentalFoundationApi::class)
                            item(
                                key = "listen_again",
                                contentType = "section"
                            ) {
                                // State for bottom sheet dialog
                                var showOptionsDialog by remember { mutableStateOf(false) }
                                var selectedSong by remember {
                                    mutableStateOf<com.just_for_fun.synctax.data.local.entities.Song?>(
                                        null
                                    )
                                }
                                val haptic = LocalHapticFeedback.current

                                SectionHeader(
                                    title = "Listen again",
                                    subtitle = null,
                                    onViewAllClick = null,
                                    titleColor = sectionTitleColor,
                                    subtitleColor = sectionSubtitleColor
                                )

                                val songsPerPage = 4
                                val shuffledSongs = remember(uiState.allSongs) {
                                    uiState.allSongs.take(16).shuffled()
                                }
                                val pages = shuffledSongs.chunked(songsPerPage)

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(
                                        count = pages.size,
                                        key = { pageIndex -> "listen_again_page_$pageIndex" }
                                    ) { pageIndex ->
                                        Column(
                                            modifier = Modifier
                                                .fillParentMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pages[pageIndex].forEach { song ->
                                                SimpleSongCard(
                                                    song = song,
                                                    onClick = {
                                                        playerViewModel.playSong(
                                                            song,
                                                            uiState.allSongs
                                                        )
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        selectedSong = song
                                                        showOptionsDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Create options for the dialog
                                val dialogOptions = remember(selectedSong, uiState.favoriteSongs) {
                                    selectedSong?.let { song ->
                                        val isFavorite = uiState.favoriteSongs.any { it.id == song.id }
                                        mutableListOf<DialogOption>().apply {
                                            // Play option
                                            add(
                                                DialogOption(
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
                                                    onClick = {
                                                        playerViewModel.playSong(
                                                            song,
                                                            uiState.allSongs
                                                        )
                                                    }
                                                )
                                            )
                                            
                                            // Add to Queue option
                                            add(
                                                DialogOption(
                                                    id = "add_to_queue",
                                                    title = "Add to Queue",
                                                    subtitle = "Add to end of current queue",
                                                    icon = {
                                                        Icon(
                                                            Icons.Filled.QueueMusic,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        playerViewModel.addToQueue(song)
                                                    }
                                                )
                                            )
                                            
                                            // Add to Favorites option
                                            add(
                                                DialogOption(
                                                    id = "toggle_favorite",
                                                    title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                                    subtitle = if (isFavorite) "Remove from your liked songs" else "Add to your liked songs",
                                                    icon = {
                                                        Icon(
                                                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                            contentDescription = null,
                                                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        homeViewModel.toggleFavorite(song.id)
                                                    }
                                                )
                                            )
                                        }
                                    } ?: emptyList()
                                }

                                // Bottom options dialog
                                BottomOptionsDialog(
                                    song = selectedSong,
                                    isVisible = showOptionsDialog,
                                    onDismiss = { showOptionsDialog = false },
                                    options = dialogOptions,
                                    title = "Song Options",
                                    description = "Choose an action for this song",
                                    songTitle = selectedSong?.title ?: "",
                                    songArtist = selectedSong?.artist ?: ""
                                )
                            }
                        }

                        // Online Recommendations Section
                        if (selectedFilter == "All" || selectedFilter == "Quick Picks") {
                            item {
                                SmallSpacer()
                            }

                            item {
                                SectionDivider()
                            }

                            item(
                                key = "recommendations",
                                contentType = "recommendations"
                            ) {
                                when {
                                    isLoadingRecommendations -> {
                                        RecommendationSkeleton()
                                    }

                                    recommendations != null -> {
                                        RecommendationsSection(
                                            recommendations = recommendations!!,
                                            onSongClick = { song ->
                                                playerViewModel.playOnlineSongWithRecommendations(
                                                    videoId = song.id,
                                                    title = song.title,
                                                    artist = song.author ?: "Unknown Artist",
                                                    thumbnailUrl = song.thumbnailUrl
                                                )
                                                recommendationViewModel.trackInteraction(
                                                    song.id,
                                                    "played",
                                                    recommendationViewModel.getRecommendationReason(
                                                        song
                                                    )
                                                )
                                            },
                                            onViewAllClick = onNavigateToRecommendations,
                                            getRecommendationReason = { song ->
                                                recommendationViewModel.getRecommendationReason(song)
                                            }
                                        )
                                    }

                                    hasEnoughHistory -> {
                                        // Has history but no recommendations loaded yet
                                        EmptyRecommendationsPrompt(
                                            onExploreClick = { recommendationViewModel.loadRecommendations() }
                                        )
                                    }
                                    // Don't show anything if no listening history
                                }
                            }
                        }

                        if (selectedFilter == "All" || selectedFilter == "Quick Picks") {
                            item {
                                SmallSpacer()
                            }

                            item {
                                SectionDivider()
                            }

                            item {
                                SmallSpacer()
                            }
                        }

                        // Speed Dial Section (only show if there are local songs)
                        if ((selectedFilter == "All" || selectedFilter == "Speed Dial") && uiState.speedDialSongs.isNotEmpty()) {
                            item(
                                key = "speed_dial",
                                contentType = "section"
                            ) {
                                SpeedDialSection(
                                    songs = uiState.speedDialSongs,
                                    onSongClick = { song ->
                                        playerViewModel.playSong(
                                            song,
                                            uiState.speedDialSongs
                                        )
                                    },
                                    userInitial = userInitial,
                                    currentSong = playerState.currentSong
                                )
                            }
                        }

                        // Saved Playlists Section (only show if there are saved playlists)
                        if ((selectedFilter == "All" || selectedFilter == "Playlists") && uiState.savedPlaylists.isNotEmpty()) {
                            item {
                                SavedPlaylistsSection(
                                    playlists = uiState.savedPlaylists,
                                    onPlaylistClick = { playlist ->
                                        onNavigateToPlaylist(playlist.playlistId)
                                    },
                                    onViewAllClick = onNavigateToPlaylists
                                )
                            }
                        }

                        // Most Played Section (only show if there are most played songs)
                        if ((selectedFilter == "All" || selectedFilter == "Speed Dial") && uiState.mostPlayedSongs.isNotEmpty()) {
                            item {
                                SmallSpacer()
                            }

                            item(
                                key = "most_played",
                                contentType = "section"
                            ) {
                                MostPlayedSection(
                                    songs = uiState.mostPlayedSongs,
                                    onSongClick = { song ->
                                        playerViewModel.playSong(song, uiState.allSongs)
                                    },
                                    onSongLongClick = { _ ->
                                        // Could add options dialog here if needed
                                    }
                                )
                                SectionSpacer()
                            }
                        }

                        // Divider and Quick Access (only show if there are local songs)
                        if (selectedFilter == "All" && uiState.allSongs.isNotEmpty()) {
                            item {
                                SmallSpacer()
                            }

                            item {
                                SectionDivider()
                            }

                            item {
                                SmallSpacer()
                            }
                        }

                        // Quick Access Grid (only show if there are local songs)
                        if (uiState.quickAccessSongs.isNotEmpty()) {
                            item {
                                QuickAccessGrid(
                                    songs = uiState.quickAccessSongs,
                                    onSongClick = { song ->
                                        playerViewModel.playSong(
                                            song,
                                            uiState.quickAccessSongs
                                        )
                                    },
                                    currentSong = playerState.currentSong
                                )
                            }

                            item {
                                ExtraLargeSpacer()
                            }

                            item {
                                SectionDivider()
                            }

                            item {
                                SmallSpacer()
                            }
                        }

                        // Empty Music State - Show when no local songs available
                        if (uiState.allSongs.isEmpty()) {
                            item {
                                LargeSpacer()
                            }

                            item {
                                SectionDivider()
                            }

                            item {
                                EmptyMusicState(
                                    onScanClick = {
                                        if (scanPaths.isEmpty()) {
                                            showDirectorySelectionDialog = true
                                        } else {
                                            homeViewModel.scanMusic()
                                        }
                                    },
                                    isScanning = uiState.isScanning
                                )
                            }
                        }

                        // Bottom padding for mini player
                        item {
                            BottomPaddingSpacer()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
//                // Training indicator
//                AnimatedVisibility(
//                    visible = uiState.isTraining,
//                    enter = fadeIn() + slideInVertically { it },
//                    exit = fadeOut() + slideOutVertically { it },
//                    modifier = Modifier.align(Alignment.BottomCenter)
//                ) {
//                    Card(
//                        modifier = Modifier.padding(16.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = cardBackgroundColor
//                        )
//                    ) {
//                        Row(
//                            modifier = Modifier.padding(16.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(24.dp),
//                                strokeWidth = 2.dp,
//                                color = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                            Text(
//                                "Training ML models...",
//                                color = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                        }
//                    }
//                }

                // Guide Overlay
                var showGuide by remember {
                    mutableStateOf(
                        userPreferences.shouldShowGuide(
                            UserPreferences.GUIDE_HOME
                        )
                    )
                }
                if (showGuide) {
                    GuideOverlay(
                        steps = GuideContent.homeScreenGuide,
                        onDismiss = {
                            showGuide = false
                            userPreferences.setGuideShown(UserPreferences.GUIDE_HOME)
                            // Show directory selection dialog only if it hasn't been shown before
                            if (!userPreferences.isDirectorySelectionShown() && scanPaths.isEmpty()) {
                                showDirectorySelectionDialog = true
                                userPreferences.setDirectorySelectionShown()
                            }
                        }
                    )
                }

                // Directory Selection Dialog
                if (showDirectorySelectionDialog) {
                    DirectorySelectionDialog(
                        userPreferences = userPreferences,
                        onScanClick = {
                            showDirectorySelectionDialog = false
                            homeViewModel.scanMusic()
                        },
                        onDismiss = {
                            showDirectorySelectionDialog = false
                        }
                    )
                }
            }
        }

        // Snackbar positioned above mini player
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (playerState.currentSong != null) 90.dp else 16.dp)
        )
    }
}
