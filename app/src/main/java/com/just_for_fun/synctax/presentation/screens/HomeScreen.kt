package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.card.AlbumCard
import com.just_for_fun.synctax.presentation.components.card.ArtistCard
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.components.chips.FilterChipsRow
import com.just_for_fun.synctax.presentation.components.onboarding.DirectorySelectionDialog
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.components.section.EmptyMusicState
import com.just_for_fun.synctax.presentation.components.section.EmptyRecommendationsPrompt
import com.just_for_fun.synctax.presentation.components.section.MostPlayedSection
import com.just_for_fun.synctax.presentation.components.section.HistorySection
import com.just_for_fun.synctax.presentation.components.section.QuickAccessGrid
import com.just_for_fun.synctax.presentation.components.section.QuickShortcutsRow
import com.just_for_fun.synctax.presentation.components.section.RecommendationSkeleton
import com.just_for_fun.synctax.presentation.components.section.RecommendationsSection
import com.just_for_fun.synctax.presentation.components.section.SavedPlaylistsSection
import com.just_for_fun.synctax.presentation.components.section.SectionHeader
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.components.section.SpeedDialSection
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.components.utils.ExtraLargeSpacer
import com.just_for_fun.synctax.presentation.components.utils.LargeSpacer
import com.just_for_fun.synctax.presentation.components.utils.SectionDivider
import com.just_for_fun.synctax.presentation.components.utils.SectionSpacer
import com.just_for_fun.synctax.presentation.components.utils.SmallSpacer
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
    onNavigateToStats: () -> Unit = {},
    onNavigateToListenedArtists: () -> Unit = {},
    onNavigateToArtistDetail: (String) -> Unit = {},
    onNavigateToAlbumDetail: (albumName: String, artistName: String) -> Unit = { _, _ -> },
    onNavigateToSavedSongs: () -> Unit = {}
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

    // Fetch artist photos once when artists list changes
    LaunchedEffect(uiState.artists, uiState.onlineHistory) {
        val artistNames = mutableListOf<String>()

        // Collect offline artist names - split by comma and clean up
        uiState.artists.take(5).forEach { artist ->
            artist.name.split(",")
                .map { it.trim() }
                .map { name ->
                    // Clean up common prefixes/suffixes
                    name.removePrefix("and ")
                        .removePrefix("& ")
                        .removePrefix("feat. ")
                        .removePrefix("ft. ")
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .forEach { artistName ->
                    artistNames.add(artistName)
                }
        }

        // Collect online artist names - split by comma and clean up
        uiState.onlineHistory
            .groupBy { it.artist }
            .keys
            .take(5)
            .forEach { artistString ->
                artistString.split(",")
                    .map { it.trim() }
                    .map { name ->
                        // Clean up common prefixes/suffixes
                        name.removePrefix("and ")
                            .removePrefix("& ")
                            .removePrefix("feat. ")
                            .removePrefix("ft. ")
                            .trim()
                    }
            }

        // Fetch photos for unique artists
        val uniqueArtists = artistNames.distinct().take(5)
        if (uniqueArtists.isNotEmpty()) {
            homeViewModel.fetchAllArtistPhotos(uniqueArtists)
        }
    }

    // Collect error messages from player view model - moved to global level
    // LaunchedEffect(Unit) {
    //     playerViewModel.errorMessages.collect { message ->
    //         SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, message)
    //     }
    // }

    // Show error from uiState via Snackbar (auto-dismiss)
    LaunchedEffect(uiState.error) {
        uiState.error?.takeIf { it.isNotBlank() }?.let { error ->
            SnackbarUtils.showGlobalSnackbar(message = error)
            homeViewModel.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                SimpleDynamicMusicTopAppBar(
                    title = "Synctax",
                    albumColors = albumColors,
                    showShuffleButton = true,
                    showRefreshButton = false,
                    showProfileButton = true,
                    onShuffleClick = {
                        if (uiState.allSongs.isNotEmpty()) {
                            // Use simple random shuffle on Home screen app bar (per RECOMMENDATION_LOGIC_NEW.md)
                            // Intelligent shuffle is used during playback via toggleShuffle()
                            playerViewModel.playSongShuffled(uiState.allSongs)
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

                // Pull-to-refresh state
                var isRefreshing by remember { mutableStateOf(false) }
                val pullToRefreshState = rememberPullToRefreshState()

                // Update isRefreshing when scanning completes and refresh artist photos
                LaunchedEffect(uiState.isScanning) {
                    if (!uiState.isScanning && isRefreshing) {
                        // Refresh complete - re-fetch artist photos
                        val artistNames = mutableListOf<String>()

                        // Collect offline artist names - split by comma and clean up
                        uiState.artists.take(5).forEach { artist ->
                            artist.name.split(",")
                                .map { it.trim() }
                                .map { name ->
                                    // Clean up common prefixes/suffixes
                                    name.removePrefix("and ")
                                        .removePrefix("& ")
                                        .removePrefix("feat. ")
                                        .removePrefix("ft. ")
                                        .trim()
                                }
                                .filter { it.isNotEmpty() }
                                .forEach { artistName ->
                                    artistNames.add(artistName)
                                }
                        }

                        // Collect online artist names - split by comma and clean up
                        uiState.onlineHistory
                            .groupBy { it.artist }
                            .keys
                            .take(5)
                            .forEach { artistString ->
                                artistString.split(",")
                                    .map { it.trim() }
                                    .map { name ->
                                        // Clean up common prefixes/suffixes
                                        name.removePrefix("and ")
                                            .removePrefix("& ")
                                            .removePrefix("feat. ")
                                            .removePrefix("ft. ")
                                            .trim()
                                    }
                                    .filter { it.isNotEmpty() }
                                    .forEach { artistName ->
                                        artistNames.add(artistName)
                                    }
                            }

                        val uniqueArtists = artistNames.distinct().take(5)
                        if (uniqueArtists.isNotEmpty()) {
                            homeViewModel.refreshArtistPhotos(uniqueArtists)
                        }
                        isRefreshing = false
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(animationSpec = tween(600)) { it / 4 }
                ) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            homeViewModel.forceRefreshLibrary()
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                                color = Color(0xFFFF0033) // Red color matching app theme
                            )
                        }
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

                            // Shortcuts Section
                            item(key = "shortcuts", contentType = "shortcuts") {
                                QuickShortcutsRow(
                                    onTrainClick = onTrainClick,
                                    onHistoryClick = onNavigateToHistory,
                                    onStatsClick = onNavigateToStats,
                                    onSavedSongsClick = onNavigateToSavedSongs,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }

                            // Artist Section
                            if (uiState.artists.isNotEmpty() || uiState.onlineHistory.isNotEmpty()) {
                                item(key = "artists", contentType = "artists") {
                                    SectionHeader(
                                        title = "Artists",
                                        onViewAllClick = onNavigateToListenedArtists,
                                        titleColor = sectionTitleColor
                                    )

                                    // Combine offline and online artists with cached photos
                                    val offlineArtists = uiState.artists.take(5).map {
                                        com.just_for_fun.synctax.presentation.model.ArtistUiModel(
                                            name = it.name,
                                            songCount = it.songCount,
                                            isOnline = false,
                                            imageUrl = uiState.artistPhotos[it.name] // Use cached photo
                                        )
                                    }

                                    // Extract unique online artists from history with cached photos
                                    val onlineArtists = uiState.onlineHistory
                                        .groupBy { it.artist }
                                        .map { (name, history) ->
                                            com.just_for_fun.synctax.presentation.model.ArtistUiModel(
                                                name = name,
                                                songCount = history.size,
                                                isOnline = true,
                                                imageUrl = uiState.artistPhotos[name] // Use cached photo
                                            )
                                        }
                                        .sortedByDescending { it.songCount }
                                        .take(5)

                                    // Merge and show up to 5 total (Offline first, then Online) (Modified per user request)
                                    val displayArtists =
                                        (offlineArtists + onlineArtists).distinctBy { it.name }
                                            .take(5)

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        items(displayArtists) { artist ->
                                            ArtistCard(
                                                artist = artist,
                                                isLoading = uiState.isLoadingArtistPhotos && artist.imageUrl == null,
                                                onClick = {
                                                    onNavigateToArtistDetail(artist.name)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Album Section
                            if (uiState.albums.isNotEmpty()) {
                                item(key = "albums", contentType = "albums") {
                                    SectionHeader(
                                        title = "Albums",
                                        onViewAllClick = null, // TODO: Navigate to all albums
                                        titleColor = sectionTitleColor
                                    )

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        items(uiState.albums.take(10)) { album ->
                                            AlbumCard(
                                                album = album,
                                                onClick = {
                                                    onNavigateToAlbumDetail(album.name, album.artist)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Picks Section - Now shows online listening history
                            if (selectedFilter == "All" || selectedFilter == "Quick Picks") {
                                item {
                                    HistorySection(
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
                                    val dialogOptions =
                                        remember(selectedSong, uiState.favoriteSongs) {
                                            selectedSong?.let { song ->
                                                val isFavorite =
                                                    uiState.favoriteSongs.any { it.id == song.id }
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
                                                                    Icons.AutoMirrored.Filled.QueueMusic,
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

                                // Only show divider if recommendations will be displayed
                                val shouldShowRecommendations = when {
                                    isLoadingRecommendations -> true
                                    recommendations != null -> true
                                    hasEnoughHistory -> true
                                    else -> false
                                }

                                if (shouldShowRecommendations) {
                                    item {
                                        SectionDivider()
                                    }
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
                                                    recommendationViewModel.getRecommendationReason(
                                                        song
                                                    )
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

                                // Only show divider if Speed Dial section will be displayed
                                if ((selectedFilter == "All" || selectedFilter == "Speed Dial") && uiState.speedDialSongs.isNotEmpty()) {
                                    item {
                                        SectionDivider()
                                    }
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
                            if (selectedFilter == "All" && uiState.allSongs.isNotEmpty() && uiState.quickAccessSongs.isNotEmpty()) {
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
                    }  // End PullToRefreshBox
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
    }
}
