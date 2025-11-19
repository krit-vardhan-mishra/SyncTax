package com.just_for_fun.synctax

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.service.MusicService
import com.just_for_fun.synctax.ui.components.AppNavigationBar
import com.just_for_fun.synctax.ui.components.PlayerBottomSheet
import com.just_for_fun.synctax.ui.screens.*
import com.just_for_fun.synctax.ui.theme.synctaxTheme
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    // Registering the permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, music scanning can proceed in the ViewModel
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permission on app launch
        checkAndRequestPermissions()

        // Start and bind to music service
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            val userPreferences = remember { UserPreferences(this) }
            val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
            val darkTheme = when (themeMode) {
                UserPreferences.KEY_THEME_MODE_DARK -> true
                UserPreferences.KEY_THEME_MODE_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            synctaxTheme(darkTheme = darkTheme) {
                MusicApp(userPreferences = userPreferences)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO for audio files
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }

            else -> {
                // Request the permission
                requestPermissionLauncher.launch(permission)
            }
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(userPreferences: UserPreferences) {
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
                onSelectSong = { song -> playerViewModel.playSong(song) },
                onPlaceNext = { song -> playerViewModel.placeNext(song) },
                onRemoveFromQueue = { song -> playerViewModel.removeFromQueue(song) },
                onReorderQueue = { from, to -> playerViewModel.reorderQueue(from, to) },
                onSetVolume = { playerViewModel.setVolume(it) }
                , downloadPercent = playerState.downloadPercent
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("home") {
                        HomeScreen(
                            homeViewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            userPreferences = userPreferences,
                            onSearchClick = { navController.navigate("search") },
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
                            onSearchClick = { navController.navigate("search") },
                            onNavigateToArtist = { artist, songs ->
                                // Store in ViewModel temporarily
                                homeViewModel.setSelectedArtist(artist, songs)
                                navController.navigate("artist/$artist")
                            },
                            onNavigateToAlbum = { album, artist, songs ->
                                // Store in ViewModel temporarily
                                homeViewModel.setSelectedAlbum(album, artist, songs)
                                navController.navigate("album/$album")
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
                                onBackClick = { navController.popBackStack() }
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
        
        // --- NAVBAR VISIBILITY CHANGE ---
        // Conditionally show the Nav Bar based on the player's expanded state
        AnimatedVisibility(
            visible = !isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            AppNavigationBar(navController)
        }
        // --- END NAVBAR VISIBILITY CHANGE ---
    }
}
