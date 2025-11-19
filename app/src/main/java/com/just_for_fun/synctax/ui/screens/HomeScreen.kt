package com.just_for_fun.synctax.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.ui.dynamic.DynamicGreetingSection
import com.just_for_fun.synctax.ui.dynamic.DynamicSectionBackground
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.ui.components.section.EmptyMusicState
import com.just_for_fun.synctax.ui.components.section.FilterChipsRow
import com.just_for_fun.synctax.ui.components.section.QuickPicksSection
import com.just_for_fun.synctax.ui.components.section.SectionHeader
import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.ui.components.utils.SortOption
import com.just_for_fun.synctax.ui.components.section.SpeedDialSection
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    userPreferences: UserPreferences,
    onSearchClick: () -> Unit = {},
    onTrainClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

    // Sorting state for All Songs section
    var currentSortOption by remember { mutableStateOf(SortOption.TITLE_ASC) }

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

    // FIXED: Trigger animation after composition
    LaunchedEffect(Unit) {
        isVisible = true
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
                    showSearchButton = true,
                    showProfileButton = true,
                    onShuffleClick = {
                        if (uiState.allSongs.isNotEmpty()) {
                            playerViewModel.shufflePlay(uiState.allSongs)
                        }
                    },
                    onRefreshClick = { homeViewModel.scanMusic() },
                    onSearchClick = onSearchClick,
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyMusicState(
                                onScanClick = { homeViewModel.scanMusic() },
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
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                // Greeting Section with dynamic colors
                                if (userName.isNotEmpty()) {
                                    item {
                                        DynamicGreetingSection (
                                            userName = userName,
                                            albumColors = albumColors
                                        )
                                    }
                                }

                                // Filter Chips
                                item {
                                    FilterChipsRow()
                                }

                                // Quick Picks Section with dynamic background
                                item {
                                    DynamicSectionBackground(
                                        albumColors = albumColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        useAccent = true
                                    ) {
                                        QuickPicksSection(
                                            songs = uiState.quickPicks,
                                            onSongClick = { song ->
                                                playerViewModel.playSong(song, uiState.quickPicks)
                                            },
                                            onRefreshClick = { homeViewModel.generateQuickPicks() },
                                            onViewAllClick = { /* Navigate to Quick Picks */ },
                                            isGenerating = uiState.isGeneratingRecommendations,
                                            onPlayAll = {
                                                uiState.quickPicks.firstOrNull()?.let { firstSong ->
                                                    playerViewModel.playSong(
                                                        firstSong,
                                                        uiState.quickPicks
                                                    )
                                                }
                                            }
                                        )
                                    }
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
                                        songs = uiState.allSongs,
                                        onSongClick = { song ->
                                            playerViewModel.playSong(song, uiState.allSongs)
                                        },
                                        userInitial = userInitial
                                    )
                                }

                                // All Songs Section
                                item {
                                    SectionHeader(
                                        title = "All Songs",
                                        subtitle = null,
                                        onViewAllClick = null,
                                        showSortButton = true,
                                        currentSortOption = currentSortOption,
                                        onSortOptionChange = { currentSortOption = it }
                                    )
                                }

                                items(sortedSongs, key = { it.id }) { song ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(400)) +
                                                expandVertically(animationSpec = tween(400))
                                    ) {
                                        SongCard(
                                            song = song,
                                            onClick = {
                                                playerViewModel.playSong(song, uiState.allSongs)
                                            }
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                item {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                item {
                                    SpeedDialGrid(
                                        songs = uiState.allSongs,
                                        onSongClick = { song ->
                                            playerViewModel.playSong(song, uiState.allSongs)
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                item {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                // Bottom padding for mini player
                                item {
                                    Spacer(modifier = Modifier.height(96.dp))
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        }
                    )
                }
            }
        }
    }
}