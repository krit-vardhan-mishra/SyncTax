package com.just_for_fun.synctax

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.AppNavigationBar
import com.just_for_fun.synctax.ui.components.player.PlayerBottomSheet
import com.just_for_fun.synctax.ui.screens.AlbumDetailScreen
import com.just_for_fun.synctax.ui.screens.ArtistDetailScreen
import com.just_for_fun.synctax.ui.screens.HomeScreen
import com.just_for_fun.synctax.ui.screens.LibraryScreen
import com.just_for_fun.synctax.ui.screens.QuickPicksScreen
import com.just_for_fun.synctax.ui.screens.SearchScreen
import com.just_for_fun.synctax.ui.screens.SettingsScreen
import com.just_for_fun.synctax.ui.screens.TrainingScreen
import com.just_for_fun.synctax.ui.screens.WelcomeScreen
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(userPreferences: UserPreferences) {
    val context = LocalContext.current
    val isFirstLaunch by userPreferences.isFirstLaunch.collectAsState()
    val userName by userPreferences.userName.collectAsState()

    // Show welcome screen on first launch
    if (isFirstLaunch) {
        WelcomeScreen(
            onNameSubmit = { name ->
                userPreferences.saveUserName(name)
            }
        )
        return
    }

    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val dynamicBgViewModel: com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel = viewModel()

    // --- HOISTED STATE ---
    // Hoist the scaffold state here to control nav bar visibility
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val isPlayerExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
    // --- END HOISTED STATE ---

    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

    // Update album colors when current song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            PlayerBottomSheet(
                scaffoldState = scaffoldState, // Pass the hoisted state down
                song = playerState.currentSong,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                position = playerState.position,
                duration = playerState.duration,
                shuffleEnabled = playerState.shuffleEnabled,
                repeatEnabled = playerState.repeatEnabled,
                volume = playerState.volume,
                upNext = playerViewModel.getUpcomingQueue(),
                playHistory = playerViewModel.getPlayHistory(),
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onNextClick = {
                    if (playerState.currentSong?.id?.startsWith("online:") == true) {
                        // For online songs, restart the current song instead of going to next
                        playerViewModel.seekTo(0L)
                    } else {
                        playerViewModel.next()
                    }
                },
                onPreviousClick = {
                    if (playerState.currentSong?.id?.startsWith("online:") == true) {
                        // For online songs, restart the current song instead of going to previous
                        playerViewModel.seekTo(0L)
                    } else {
                        playerViewModel.previous()
                    }
                },
                onShuffleClick = { playerViewModel.toggleShuffle() },
                onRepeatClick = { playerViewModel.toggleRepeat() },
                onSeek = { playerViewModel.seekTo(it) },
                onSelectSong = { song -> playerViewModel.playFromQueue(song) },
                onPlaceNext = { song -> playerViewModel.placeNext(song) },
                onRemoveFromQueue = { song -> playerViewModel.removeFromQueue(song) },
                downloadPercent = playerState.downloadPercent
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = if (playerState.currentSong != null) 0.dp else innerPadding.calculateBottomPadding()
                    )
                ) {
                    composable("home") {
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            userPreferences = userPreferences,
                            onTrainClick = { navController.navigate("train") },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            onBackClick = { navController.popBackStack() },
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel
                        )
                    }
                    composable("library") {
                        val uiState by homeViewModel.uiState.collectAsState()
                        LibraryScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            userPreferences = userPreferences,
                            onNavigateToArtist = { artist, songs ->
                                // Store in ViewModel temporarily
                                homeViewModel.setSelectedArtist(artist, songs)
                                navController.navigate("artist/$artist")
                            },
                            onNavigateToAlbum = { album, artist, songs ->
                                // Store in ViewModel temporarily
                                homeViewModel.setSelectedAlbum(album, artist, songs)
                                navController.navigate("album/$album")
                            },
                            onOpenSettings = { navController.navigate("settings") },
                            onTrainClick = { navController.navigate("train") }
                        )
                    }
                    composable("quick_picks") {
                        QuickPicksScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel
                        )
                    }
                    composable("train") {
                        TrainingScreen(
                            homeViewModel = homeViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            userPreferences = userPreferences,
                            onBackClick = { navController.popBackStack() },
                            onScanTrigger = { homeViewModel.forceRefreshLibrary() }
                        )
                    }
                    composable(
                        route = "artist/{artistName}",
                        arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                    ) {
                        val uiState by homeViewModel.uiState.collectAsState()
                        uiState.selectedArtistSongs?.let { songs ->
                            ArtistDetailScreen(
                                artistName = uiState.selectedArtist ?: "",
                                songs = songs,
                                onBackClick = { navController.popBackStack() },
                                onSongClick = { song ->
                                    playerViewModel.playSong(song, songs)
                                },
                                onPlayAll = {
                                    songs.firstOrNull()?.let { firstSong ->
                                        playerViewModel.playSong(firstSong, songs)
                                    }
                                },
                                onShuffle = {
                                    playerViewModel.toggleShuffle()
                                    songs.firstOrNull()?.let { firstSong ->
                                        playerViewModel.playSong(firstSong, songs)
                                    }
                                }
                            )
                        }
                    }
                    composable(
                        route = "album/{albumName}",
                        arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                    ) {
                        val uiState by homeViewModel.uiState.collectAsState()
                        uiState.selectedAlbumSongs?.let { songs ->
                            AlbumDetailScreen(
                                albumName = uiState.selectedAlbum ?: "",
                                artistName = uiState.selectedAlbumArtist ?: "",
                                songs = songs,
                                onBackClick = { navController.popBackStack() },
                                onSongClick = { song ->
                                    playerViewModel.playSong(song, songs)
                                },
                                onPlayAll = {
                                    songs.firstOrNull()?.let { firstSong ->
                                        playerViewModel.playSong(firstSong, songs)
                                    }
                                },
                                onShuffle = {
                                    playerViewModel.toggleShuffle()
                                    songs.firstOrNull()?.let { firstSong ->
                                        playerViewModel.playSong(firstSong, songs)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            AppNavigationBar(
                navController = navController,
                albumColors = albumColors
            )
        }
    }
}