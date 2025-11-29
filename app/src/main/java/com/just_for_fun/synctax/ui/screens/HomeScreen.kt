package com.just_for_fun.synctax.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.ui.components.onboarding.DirectorySelectionDialog
import com.just_for_fun.synctax.ui.components.section.EmptyMusicState
import com.just_for_fun.synctax.ui.components.section.FilterChipsRow
import com.just_for_fun.synctax.ui.components.section.OnlineHistorySection
import com.just_for_fun.synctax.ui.components.section.SectionHeader
import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.ui.components.section.SpeedDialSection
import com.just_for_fun.synctax.ui.components.utils.SortOption
import com.just_for_fun.synctax.ui.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.ui.dynamic.DynamicGreetingSection
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    userPreferences: UserPreferences,
    onTrainClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val scanPaths by userPreferences.scanPaths.collectAsState()

    // Directory selection dialog state
    var showDirectorySelectionDialog by remember { mutableStateOf(false) }

    // Sorting state for All Songs section
    var currentSortOption by remember { mutableStateOf(SortOption.TITLE_ASC) }

    val listState = rememberLazyListState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }

    // Detect scroll to bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = layoutInfo.totalItemsCount
                if (lastVisibleItemIndex >= totalItems - 1 && !hasScrolledToBottom && totalItems > 0) {
                    hasScrolledToBottom = true
                    // Refresh album art when user scrolls to bottom
                    homeViewModel.refreshAlbumArt()
                } else if (lastVisibleItemIndex < totalItems - 2) {
                    hasScrolledToBottom = false
                }
            }
    }

    // Sort songs based on current sort option
    val sortedSongs = remember(uiState.allSongs, currentSortOption) {
        when (currentSortOption) {
            SortOption.TITLE_ASC -> uiState.allSongs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> uiState.allSongs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> uiState.allSongs.sortedBy { it.artist.lowercase() }
            SortOption.ARTIST_DESC -> uiState.allSongs.sortedByDescending { it.artist.lowercase() }
            SortOption.RELEASE_YEAR_DESC -> uiState.allSongs.sortedByDescending {
                it.releaseYear ?: 0
            }

            SortOption.RELEASE_YEAR_ASC -> uiState.allSongs.sortedBy { it.releaseYear ?: 0 }
            SortOption.ADDED_TIMESTAMP_DESC -> uiState.allSongs.sortedByDescending { it.addedTimestamp }
            SortOption.ADDED_TIMESTAMP_ASC -> uiState.allSongs.sortedBy { it.addedTimestamp }
            SortOption.DURATION_DESC -> uiState.allSongs.sortedByDescending { it.duration }
            SortOption.DURATION_ASC -> uiState.allSongs.sortedBy { it.duration }
            SortOption.NAME_ASC -> uiState.allSongs.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> uiState.allSongs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST -> uiState.allSongs.sortedBy { it.artist.lowercase() }
            SortOption.DATE_ADDED_OLDEST -> uiState.allSongs.sortedBy { it.addedTimestamp }
            SortOption.DATE_ADDED_NEWEST -> uiState.allSongs.sortedByDescending { it.addedTimestamp }
            SortOption.CUSTOM -> uiState.allSongs
        }
    }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    // FIXED: Update album colors when current song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Set up callback for refreshing downloaded songs check
    LaunchedEffect(homeViewModel, playerViewModel) {
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
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    uiState.allSongs.isEmpty() -> {
                        // Show directory selection dialog when no songs are scanned
                        LaunchedEffect(Unit) {
                            if (scanPaths.isEmpty() && !showDirectorySelectionDialog) {
                                showDirectorySelectionDialog = true
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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

                    else -> {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(600)) +
                                    slideInVertically(animationSpec = tween(600)) { it / 4 }
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                // Greeting Section with dynamic colors
                                if (userName.isNotEmpty()) {
                                    item {
                                        DynamicGreetingSection(
                                            userName = userName,
                                            albumColors = albumColors
                                        )
                                    }
                                }

                                // Filter Chips
                                item {
                                    FilterChipsRow()
                                }

                                // Quick Picks Section - Now shows online listening history
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
                                        onPlayAll = {
                                            uiState.onlineHistory.firstOrNull()?.let { firstHistory ->
                                                playerViewModel.playUrl(
                                                    url = firstHistory.watchUrl,
                                                    title = firstHistory.title,
                                                    artist = firstHistory.artist,
                                                    durationMs = 0L
                                                )
                                            }
                                        },
                                        currentVideoId = if (playerState.currentSong?.id?.startsWith("online:") == true)
                                            playerState.currentSong?.id?.removePrefix("online:")
                                        else null
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(15.dp))
                                }

                                // Listen Again Section
                                @OptIn(ExperimentalFoundationApi::class)
                                item {
                                    SectionHeader(
                                        title = "Listen again",
                                        subtitle = null,
                                        onViewAllClick = null
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
                                        items(pages.size) { pageIndex ->
                                            Column(
                                                modifier = Modifier
                                                    .fillParentMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                pages[pageIndex].forEach { song ->
                                                    SongCard(
                                                        song = song,
                                                        onClick = {
                                                            playerViewModel.playSong(
                                                                song,
                                                                uiState.allSongs
                                                            )
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .animateItem()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Speed Dial Section
                                item {
                                    SpeedDialSection(
                                        songs = uiState.speedDialSongs,
                                        onSongClick = { song ->
                                            playerViewModel.playSong(song, uiState.speedDialSongs)
                                        },
                                        userInitial = userInitial,
                                        currentSong = playerState.currentSong
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(15.dp))
                                }

                                item {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(25.dp))
                                }

                                item {
                                    QuickAccessGrid(
                                        songs = uiState.quickAccessSongs,
                                        onSongClick = { song ->
                                            playerViewModel.playSong(song, uiState.quickAccessSongs)
                                        },
                                        currentSong = playerState.currentSong
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(25.dp))
                                }

                                item {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Bottom padding for mini player
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Snackbar for errors
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { homeViewModel.dismissError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }

                // Training indicator
                AnimatedVisibility(
                    visible = uiState.isTraining,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.DarkGray
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Training ML models...",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Guide Overlay
                var showGuide by remember {
                    mutableStateOf(
                        userPreferences.shouldShowGuide(
                            UserPreferences.GUIDE_HOME
                        )
                    )
                }
                if (showGuide) {
                    com.just_for_fun.synctax.ui.guide.GuideOverlay(
                        steps = com.just_for_fun.synctax.ui.guide.GuideContent.homeScreenGuide,
                        onDismiss = {
                            showGuide = false
                            userPreferences.setGuideShown(UserPreferences.GUIDE_HOME)
                            // Show directory selection dialog if no directories are selected yet
                            if (scanPaths.isEmpty()) {
                                showDirectorySelectionDialog = true
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