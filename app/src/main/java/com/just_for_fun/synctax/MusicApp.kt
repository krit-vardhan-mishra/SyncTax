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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.AppNavigationBar
import com.just_for_fun.synctax.ui.components.player.PlayerBottomSheet
import com.just_for_fun.synctax.ui.screens.AlbumDetailScreen
import com.just_for_fun.synctax.ui.screens.ArtistDetailScreen
import com.just_for_fun.synctax.ui.screens.HomeScreen
import com.just_for_fun.synctax.ui.screens.ImportPlaylistScreen
import com.just_for_fun.synctax.ui.screens.LibraryScreen
import com.just_for_fun.synctax.ui.screens.PlaylistDetailScreen
import com.just_for_fun.synctax.ui.screens.PlaylistScreen
import com.just_for_fun.synctax.ui.screens.QuickPicksScreen
import com.just_for_fun.synctax.ui.screens.SearchScreen
import com.just_for_fun.synctax.ui.screens.SettingsScreen
import com.just_for_fun.synctax.ui.screens.TrainingScreen
import com.just_for_fun.synctax.ui.screens.WelcomeScreen
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlaylistViewModel

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
    val dynamicBgViewModel: com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel =
        viewModel()

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
    val homeState by homeViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchResetTrigger by remember { mutableStateOf(0) }

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
                    onNextClick = { playerViewModel.next() },
                    onPreviousClick = { playerViewModel.previous() },
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
                        composable("playlists") {
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            PlaylistScreen(
                                playlistViewModel = playlistViewModel,
                                playerViewModel = playerViewModel,
                                dynamicBgViewModel = dynamicBgViewModel,
                                userPreferences = userPreferences,
                                scaffoldState = scaffoldState,
                                onOpenSettings = { navController.navigate("settings") },
                                onPlaylistClick = { playlistId ->
                                    navController.navigate("playlist_detail/$playlistId")
                                },
                                onImportClick = {
                                    navController.navigate("import_playlist")
                                }
                            )
                        }
                        composable("import_playlist") {
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            ImportPlaylistScreen(
                                playlistViewModel = playlistViewModel,
                                onBackClick = { navController.popBackStack() },
                                onImportSuccess = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "playlist_detail/{playlistId}",
                            arguments = listOf(navArgument("playlistId") {
                                type = NavType.IntType
                            })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
                            val playlistViewModel: PlaylistViewModel = viewModel()
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistViewModel = playlistViewModel,
                                scaffoldState = scaffoldState,
                                onBackClick = { navController.popBackStack() },
                                onSongClick = { song ->
                                    playerViewModel.playUrl(
                                        url = "https://music.youtube.com/watch?v=${song.videoId}",
                                        title = song.title,
                                        artist = song.artist,
                                        durationMs = (song.duration ?: 0) * 1000L,
                                        thumbnailUrl = song.thumbnailUrl
                                    )
                                },
                                onPlayAll = {
                                    val songs = playlistViewModel.detailState.value.songs
                                    if (songs.isNotEmpty()) {
                                        val firstSong = songs.first()
                                        playerViewModel.playUrl(
                                            url = "https://music.youtube.com/watch?v=${firstSong.videoId}",
                                            title = firstSong.title,
                                            artist = firstSong.artist,
                                            durationMs = (firstSong.duration ?: 0) * 1000L,
                                            thumbnailUrl = firstSong.thumbnailUrl
                                        )
                                        // TODO: Add remaining songs to queue
                                    }
                                },
                                onShuffle = {
                                    val songs = playlistViewModel.detailState.value.songs.shuffled()
                                    if (songs.isNotEmpty()) {
                                        val firstSong = songs.first()
                                        playerViewModel.playUrl(
                                            url = "https://music.youtube.com/watch?v=${firstSong.videoId}",
                                            title = firstSong.title,
                                            artist = firstSong.artist,
                                            durationMs = (firstSong.duration ?: 0) * 1000L,
                                            thumbnailUrl = firstSong.thumbnailUrl
                                        )
                                        // TODO: Add remaining songs to queue
                                    }
                                }
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
                            arguments = listOf(navArgument("artistName") {
                                type = NavType.StringType
                            })
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
                        composable("online_artist") {
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
                                        // Play all online artist songs with queue
                                        playerViewModel.playRecommendedSongsPlaylist(artist.songs, 0)
                                    },
                                    onShuffle = {
                                        // Shuffle and play all online artist songs
                                        playerViewModel.playRecommendedSongsShuffled(artist.songs)
                                    },
                                    isOnline = true,
                                    artistDetails = artist,
                                    onOnlineSongClick = { song ->
                                        // Play clicked song with remaining songs as queue
                                        val clickedIndex = artist.songs.indexOf(song)
                                        if (clickedIndex >= 0) {
                                            playerViewModel.playRecommendedSongsPlaylist(artist.songs, clickedIndex)
                                        } else {
                                            playerViewModel.playUrl(
                                                url = song.watchUrl,
                                                title = song.title,
                                                artist = song.artist,
                                                durationMs = 0L,
                                                thumbnailUrl = song.thumbnail
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        composable(
                            route = "album/{albumName}",
                            arguments = listOf(navArgument("albumName") {
                                type = NavType.StringType
                            })
                        ) {
                            val uiState by homeViewModel.uiState.collectAsState()
                            uiState.selectedAlbumSongs?.let { songs ->
                                // Check for both "youtube:" and "online:" prefixes for online albums
                                val isOnlineAlbum = songs.firstOrNull()?.id?.let { id ->
                                    id.startsWith("youtube:") || id.startsWith("online:")
                                } == true
                                AlbumDetailScreen(
                                    albumName = uiState.selectedAlbum ?: "",
                                    artistName = uiState.selectedAlbumArtist ?: "",
                                    songs = songs,
                                    onBackClick = { 
                                        navController.popBackStack() 
                                    },
                                    onSongClick = { song ->
                                        if (isOnlineAlbum) {
                                            playerViewModel.playUrl(
                                                url = song.filePath,
                                                title = song.title,
                                                artist = song.artist,
                                                durationMs = song.duration,
                                                thumbnailUrl = song.albumArtUri
                                            )
                                        } else {
                                            playerViewModel.playSong(song, songs)
                                        }
                                    },
                                    onPlayAll = {
                                        if (isOnlineAlbum) {
                                            // Play all online songs in order with queue
                                            playerViewModel.playOnlinePlaylist(songs, 0)
                                        } else {
                                            songs.firstOrNull()?.let { firstSong ->
                                                playerViewModel.playSong(firstSong, songs)
                                            }
                                        }
                                    },
                                    onShuffle = {
                                        if (isOnlineAlbum) {
                                            // Shuffle and play all online songs with queue
                                            playerViewModel.playOnlinePlaylistShuffled(songs)
                                        } else {
                                            // Shuffle and play all offline songs
                                            playerViewModel.playSongShuffled(songs)
                                        }
                                    },
                                    isOnline = isOnlineAlbum,
                                    albumDetails = uiState.selectedOnlineAlbum
                                )
                            }
                        }
                    }
                }
            }

            // Get current route to determine navigation bar visibility
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

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