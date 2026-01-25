package com.just_for_fun.synctax

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.just_for_fun.synctax.core.utils.UpdateChecker
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.app.AppNavigationBar
import com.just_for_fun.synctax.presentation.components.player.PlayerBottomSheet
import com.just_for_fun.synctax.presentation.screens.AlbumDetailScreen
import com.just_for_fun.synctax.presentation.screens.ArtistDetailScreen
import com.just_for_fun.synctax.presentation.screens.CreatePlaylistScreen
import com.just_for_fun.synctax.presentation.screens.HistoryScreen
import com.just_for_fun.synctax.presentation.screens.HomeScreen
import com.just_for_fun.synctax.presentation.screens.ImportPlaylistScreen
import com.just_for_fun.synctax.presentation.screens.LibraryScreen
import com.just_for_fun.synctax.presentation.screens.OnlineSongsScreen
import com.just_for_fun.synctax.presentation.screens.PlaylistDetailScreen
import com.just_for_fun.synctax.presentation.screens.PlaylistScreen
import com.just_for_fun.synctax.presentation.screens.QuickPicksScreen
import com.just_for_fun.synctax.presentation.screens.RecommendationsDetailScreen
import com.just_for_fun.synctax.presentation.screens.SearchScreen
import com.just_for_fun.synctax.presentation.screens.SettingsScreen
import com.just_for_fun.synctax.presentation.screens.StatsScreen
import com.just_for_fun.synctax.presentation.screens.TrainingScreen
import com.just_for_fun.synctax.presentation.screens.UserRecommendationInputScreen
import com.just_for_fun.synctax.presentation.screens.WelcomeScreen
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.OnlineSongsViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlaylistViewModel
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(userPreferences: UserPreferences, initialMediaUri: Uri? = null) {
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
    val dynamicBgViewModel: DynamicBackgroundViewModel =
        viewModel()
    val recommendationViewModel: RecommendationViewModel = viewModel()

    // Get current route to determine visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by playerViewModel.uiState.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchResetTrigger by remember { mutableIntStateOf(0) }

    // Handle initial media URI (Open With intent)
    LaunchedEffect(initialMediaUri) {
        initialMediaUri?.let { uri ->
            // Extract a displayable name if possible
            val fileName = uri.lastPathSegment ?: "External Audio"
            
            // Play using the URI
            playerViewModel.playUrl(
                url = uri.toString(),
                title = fileName,
                artist = "External File",
                durationMs = 0L // Duration will be determined by player
            )
        }
    }

    // --- END HOISTED STATE ---

    // Player expansion state - tracked by PlayerBottomSheet, used for nav bar visibility
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Update check state
    val scope = rememberCoroutineScope()
    var checkForUpdate by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Handle update check
    LaunchedEffect(checkForUpdate) {
        if (checkForUpdate && !isCheckingUpdate) {
            checkForUpdate = false
            isCheckingUpdate = true
            val result = UpdateChecker(context).checkForUpdate()
            isCheckingUpdate = false
            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    scope.launch {
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Update available: v${info.latestVersion}",
                            actionLabel = "Download",
                            duration = SnackbarDuration.Long
                        )
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            info.downloadUrl?.let { url ->
                                UpdateChecker(context).downloadAndInstallApk(url)
                            }
                        }
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "App is up to date",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }.onFailure { error ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Failed to check for updates: ${error.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Update album colors when current song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Show loading snackbar when fetching online song
    LaunchedEffect(playerState.isLoadingSong) {
        if (playerState.isLoadingSong) {
            snackbarHostState.showSnackbar(
                message = "Getting song...",
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Show loading snackbar when fetching artist details
    LaunchedEffect(homeState.isLoadingArtistDetails) {
        if (homeState.isLoadingArtistDetails) {
            snackbarHostState.showSnackbar(
                message = "Loading artist details...",
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Show loading snackbar when fetching album details
    LaunchedEffect(homeState.isLoadingAlbumDetails) {
        if (homeState.isLoadingAlbumDetails) {
            snackbarHostState.showSnackbar(
                message = "Loading album details...",
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                PlayerBottomSheet(
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
                    onNextClick = { playerViewModel.next() },
                    onPreviousClick = { playerViewModel.previous() },
                    onShuffleClick = { playerViewModel.toggleShuffle() },
                    onRepeatClick = { playerViewModel.toggleRepeat() },
                    onSeek = { playerViewModel.seekTo(it) },
                    onSelectSong = { song -> playerViewModel.playFromQueue(song) },
                    onPlaceNext = { song -> playerViewModel.placeNext(song) },
                    onRemoveFromQueue = { song -> playerViewModel.removeFromQueue(song) },

                    downloadPercent = playerState.downloadPercent,
                    onExpandedChange = { isPlayerExpanded = it },
                    showPlayer = currentRoute != "quick_picks"
                ) { innerPadding ->
                    // Provide bottom padding to all screens via CompositionLocal
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                            bottom = 0.dp // Screens handle their own bottom padding via LocalBottomPadding
                        )
                    ) {
                        composable(
                            "home",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            HomeScreen(
                                homeViewModel = homeViewModel,
                                playerViewModel = playerViewModel,
                                recommendationViewModel = recommendationViewModel,
                                userPreferences = userPreferences,
                                onTrainClick = { navController.navigate("train") },
                                onOpenSettings = { navController.navigate("settings") },
                                onNavigateToPlaylist = { playlistId ->
                                    navController.navigate("playlist_detail/$playlistId")
                                },
                                onNavigateToPlaylists = { navController.navigate("playlists") },
                                onNavigateToOnlineSongs = { navController.navigate("online_songs") },
                                onNavigateToRecommendations = { navController.navigate("recommendations_detail") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToStats = { navController.navigate("stats") }
                            )
                        }
                        composable(
                            "search",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            SearchScreen(
                                navController = navController,
                                onBackClick = { navController.popBackStack() },
                                resetTrigger = searchResetTrigger,
                                onNavigateToAlbum = { album, artist, songs ->
                                    homeViewModel.setSelectedAlbum(album, artist, songs)
                                    navController.navigate("album/$album")
                                },
                                homeViewModel = homeViewModel,
                                playerViewModel = playerViewModel
                            )
                        }
                        composable(
                            "library",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
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
                        composable(
                            "playlists",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            PlaylistScreen(
                                playlistViewModel = playlistViewModel,
                                playerViewModel = playerViewModel,
                                dynamicBgViewModel = dynamicBgViewModel,
                                userPreferences = userPreferences,
                                onOpenSettings = { navController.navigate("settings") },
                                onPlaylistClick = { playlistId ->
                                    navController.navigate("playlist_detail/$playlistId")
                                },
                                onImportClick = {
                                    navController.navigate("import_playlist")
                                },
                                onCreatePlaylistClick = {
                                    navController.navigate("create_playlist")
                                }
                            )
                        }
                        composable(
                            "import_playlist",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            ImportPlaylistScreen(
                                playlistViewModel = playlistViewModel,
                                onBackClick = { navController.popBackStack() },
                                onImportSuccess = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "create_playlist",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            CreatePlaylistScreen(
                                playlistViewModel = playlistViewModel,
                                homeViewModel = homeViewModel,
                                onBackClick = { navController.popBackStack() },
                                onSaveSuccess = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "playlist_detail/{playlistId}",
                            arguments = listOf(navArgument("playlistId") {
                                type = NavType.IntType
                            }),
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistViewModel = playlistViewModel,
                                onBackClick = { navController.popBackStack() },
                                onSongClick = { song ->
                                    // Play individual song WITH recommendations for continuous playback
                                    playerViewModel.playOnlineSongEntityWithRecommendations(song)
                                },
                                onPlayAll = {
                                    val songs = playlistViewModel.detailState.value.songs
                                    if (songs.isNotEmpty()) {
                                        // Play all playlist songs in order - NO recommendations
                                        playerViewModel.playOnlineSongEntitiesPlaylist(songs, 0)
                                    }
                                },
                                onShuffle = {
                                    val songs = playlistViewModel.detailState.value.songs
                                    if (songs.isNotEmpty()) {
                                        // Shuffle and play all playlist songs - NO recommendations
                                        playerViewModel.playOnlineSongEntitiesShuffled(songs)
                                    }
                                }
                            )
                        }
                        composable(
                            "quick_picks",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            QuickPicksScreen(
                                homeViewModel = homeViewModel,
                                playerViewModel = playerViewModel
                            )
                        }
                        composable(
                            "train",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            TrainingScreen(
                                homeViewModel = homeViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "settings",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            SettingsScreen(
                                userPreferences = userPreferences,
                                onBackClick = { navController.popBackStack() },
                                onScanTrigger = { homeViewModel.forceRefreshLibrary() },
                                onCheckForUpdate = { checkForUpdate = true }
                            )
                        }
                        composable(
                            "history",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            HistoryScreen(
                                homeViewModel = homeViewModel,
                                playerViewModel = playerViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "stats",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            StatsScreen(
                                homeViewModel = homeViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "recommendations_detail",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            RecommendationsDetailScreen(
                                recommendationViewModel = recommendationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onPlaySong = { song ->
                                    playerViewModel.playOnlineSongWithRecommendations(
                                        videoId = song.id,
                                        title = song.title,
                                        artist = song.author ?: "Unknown Artist",
                                        thumbnailUrl = song.thumbnailUrl
                                    )
                                },
                                onNavigateToUserInput = { navController.navigate("user_recommendations_input") }
                            )
                        }
                        composable(
                            "user_recommendations_input",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            UserRecommendationInputScreen(
                                recommendationViewModel = recommendationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onGenerateRecommendations = { userInputs: com.just_for_fun.synctax.presentation.screens.UserRecommendationInputs ->
                                    recommendationViewModel.generateUserInputRecommendations(userInputs)
                                    // Navigate back to home screen to show the new recommendations
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(
                            route = "artist/{artistName}",
                            arguments = listOf(navArgument("artistName") {
                                type = NavType.StringType
                            }),
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
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
                                        // Shuffle and play all offline artist songs
                                        playerViewModel.playSongShuffled(songs)
                                    }
                                )
                            }
                        }
                        composable(
                            "online_artist",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val uiState by homeViewModel.uiState.collectAsState()
                            uiState.selectedOnlineArtist?.let { artist ->
                                ArtistDetailScreen(
                                    artistName = "", // not used
                                    songs = emptyList(), // not used
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onSongClick = {}, // not used
                                    onPlayAll = {
                                        // Play all online artist songs with queue - NO recommendations
                                        playerViewModel.playRecommendedSongsPlaylist(
                                            artist.songs,
                                            0
                                        )
                                    },
                                    onShuffle = {
                                        // Shuffle and play all online artist songs - NO recommendations
                                        playerViewModel.playRecommendedSongsShuffled(artist.songs)
                                    },
                                    isOnline = true,
                                    artistDetails = artist,
                                    onOnlineSongClick = { song ->
                                        // Play individual song WITH recommendations for continuous playback
                                        playerViewModel.playRecommendedSongWithRecommendations(song)
                                    }
                                )
                            }
                        }
                        composable(
                            "online_songs",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val onlineSongsViewModel: OnlineSongsViewModel = viewModel()
                            OnlineSongsScreen(
                                onlineSongsViewModel = onlineSongsViewModel,
                                playerViewModel = playerViewModel,
                                dynamicBgViewModel = dynamicBgViewModel,
                                userPreferences = userPreferences,
                                onOpenSettings = { navController.navigate("settings") },
                                onShuffleClick = {
                                    val history = onlineSongsViewModel.uiState.value.history
                                    if (history.isNotEmpty()) {
                                        playerViewModel.shufflePlayOnlineHistory(history)
                                    }
                                }
                            )
                        }
                        composable(
                            route = "album/{albumName}",
                            arguments = listOf(navArgument("albumName") {
                                type = NavType.StringType
                            }),
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            val uiState by homeViewModel.uiState.collectAsState()
                            val playlistViewModel: PlaylistViewModel = viewModel()

                            // Track if album is saved
                            var isAlbumSaved by remember { mutableStateOf(false) }

                            uiState.selectedAlbumSongs?.let { songs ->
                                // Check for both "youtube:" and "online:" prefixes for online albums
                                val isOnlineAlbum = songs.firstOrNull()?.id?.let { id ->
                                    id.startsWith("youtube:") || id.startsWith("online:")
                                } == true

                                val currentAlbumName = uiState.selectedAlbum ?: ""
                                val currentArtistName = uiState.selectedAlbumArtist ?: ""
                                val albumThumbnail =
                                    if (isOnlineAlbum) uiState.selectedOnlineAlbum?.thumbnail else songs.firstOrNull()?.albumArtUri

                                // Check if album is saved when screen loads or album changes
                                LaunchedEffect(currentAlbumName, currentArtistName) {
                                    playlistViewModel.isAlbumSaved(
                                        currentAlbumName,
                                        currentArtistName
                                    ) { saved ->
                                        isAlbumSaved = saved
                                    }
                                }

                                AlbumDetailScreen(
                                    albumName = currentAlbumName,
                                    artistName = currentArtistName,
                                    songs = songs,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onSongClick = { song ->
                                        if (isOnlineAlbum) {
                                            // Play individual song WITH recommendations for continuous playback
                                            val videoId = song.id.removePrefix("youtube:")
                                                .removePrefix("online:")
                                            playerViewModel.playOnlineSongWithRecommendations(
                                                videoId = videoId,
                                                title = song.title,
                                                artist = song.artist,
                                                thumbnailUrl = song.albumArtUri
                                            )
                                        } else {
                                            playerViewModel.playSong(song, songs)
                                        }
                                    },
                                    onPlayAll = {
                                        if (isOnlineAlbum) {
                                            // Play all online songs in order with queue - NO recommendations
                                            playerViewModel.playOnlinePlaylist(songs, 0)
                                        } else {
                                            songs.firstOrNull()?.let { firstSong ->
                                                playerViewModel.playSong(firstSong, songs)
                                            }
                                        }
                                    },
                                    onShuffle = {
                                        if (isOnlineAlbum) {
                                            // Shuffle and play all online songs with queue - NO recommendations
                                            playerViewModel.playOnlinePlaylistShuffled(songs)
                                        } else {
                                            // Shuffle and play all offline songs
                                            playerViewModel.playSongShuffled(songs)
                                        }
                                    },
                                    isOnline = isOnlineAlbum,
                                    albumDetails = uiState.selectedOnlineAlbum,
                                    isAlbumSaved = isAlbumSaved,
                                    onSaveAlbumClick = {
                                        playlistViewModel.saveAlbumAsPlaylist(
                                            albumName = currentAlbumName,
                                            artistName = currentArtistName,
                                            thumbnailUrl = albumThumbnail,
                                            songs = songs
                                        ) { success ->
                                            if (success) {
                                                isAlbumSaved = true
                                            }
                                        }
                                    },
                                    onUnsaveAlbumClick = {
                                        playlistViewModel.unsaveAlbum(
                                            albumName = currentAlbumName,
                                            artistName = currentArtistName
                                        ) { success ->
                                            if (success) {
                                                isAlbumSaved = false
                                            }
                                        }
                                    }
                                )
                            }

                        }
                    }
                }
            }



            AnimatedVisibility(
                visible = !isPlayerExpanded && currentRoute != "train" && currentRoute != "settings",
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                AppNavigationBar(
                    navController = navController,
                    albumColors = albumColors
                )
            }
        }

        // Snackbar for loading states
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (playerState.currentSong != null) 90.dp else 16.dp)
        )
    }
}
