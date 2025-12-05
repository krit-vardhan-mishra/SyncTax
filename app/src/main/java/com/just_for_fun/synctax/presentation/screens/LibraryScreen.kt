package com.just_for_fun.synctax.presentation.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.components.tabs.AlbumsTab
import com.just_for_fun.synctax.presentation.components.tabs.ArtistsTab
import com.just_for_fun.synctax.presentation.components.tabs.SongsTab
import com.just_for_fun.synctax.presentation.components.utils.SortOption
import com.just_for_fun.synctax.presentation.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    userPreferences: UserPreferences,
    onNavigateToArtist: (String, List<Song>) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String, List<Song>) -> Unit = { _, _, _ -> },
    onOpenSettings: () -> Unit = {},
    onTrainClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Theme-aware colors from AppColors
    val cardBackgroundColor = AppColors.homeCardBackground
    val sectionTitleColor = AppColors.homeSectionTitle
    val sectionSubtitleColor = AppColors.homeSectionSubtitle
    
    // Tab colors from AppColors
    val tabContainerColor = AppColors.libraryBackground
    val tabIndicatorColor = AppColors.libraryTabIndicator
    val tabSelectedTextColor = AppColors.libraryTabSelectedText
    val tabUnselectedTextColor = AppColors.libraryTabUnselected
    
    // Storage permission launcher for Android 11+
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Permission result handled
    }
    
    // Regular storage permissions for Android 10 and below
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.w("LibraryScreen", "Write storage permission denied")
        }
    }
    
    // Request storage permissions on first composition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    storagePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e("LibraryScreen", "Error requesting storage permission", e)
                }
            }
        } else {
            // Android 10 and below - Request WRITE_EXTERNAL_STORAGE
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // Update colors when current song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Collect error messages from player view model
    LaunchedEffect(Unit) {
        playerViewModel.errorMessages.collect { message ->
            SnackbarUtils.ShowSnackbar(scope, snackbarHostState, message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Library",
                albumColors = albumColors,
                showSortButton = true,
                showShuffleButton = true,
                showRefreshButton = true,
                showProfileButton = true,
                onShuffleClick = {
                    val songsToShuffle = when (pagerState.currentPage) {
                        0 -> uiState.allSongs // Songs tab
                        1 -> uiState.allSongs // Artists tab - all songs
                        2 -> uiState.allSongs // Albums tab - all songs
                        else -> emptyList()
                    }
                    if (songsToShuffle.isNotEmpty()) {
                        playerViewModel.shufflePlay(songsToShuffle)
                    } else {
                        SnackbarUtils.ShowSnackbar(scope, snackbarHostState, "No songs available on your device, listen songs online")
                    }
                },
                onRefreshClick = { homeViewModel.scanMusic() },
                onSortOptionChange = { sortOption = it },
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = sortOption,
                currentTab = pagerState.currentPage,
                onOpenSettings = onOpenSettings,
                onTrainClick = onTrainClick
            )
        }
    ) { paddingValues ->
        DynamicAlbumBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- CUSTOM SEGMENTED/PILL TAB ROW ---
                val tabs = listOf("Songs", "Artists", "Albums")

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    // Container color: Slightly lighter than background for contrast
                    containerColor = tabContainerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {}, // Remove the default bottom line
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            // The floating pill indicator
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                    .fillMaxSize()
                                    .padding(4.dp) // Inset slightly
                                    .background(
                                        color = tabIndicatorColor,
                                        shape = CircleShape
                                    )
                                    .zIndex(-1f) // Behind the text
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(CircleShape) // Round the entire tab row
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index

                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    // Text color changes based on selection for contrast
                                    color = if (isSelected)
                                        tabSelectedTextColor
                                    else
                                        tabUnselectedTextColor
                                )
                            },
                            modifier = Modifier.zIndex(1f)
                        )
                    }
                }

                // Swipeable Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> SongsTab(
                            songs = uiState.allSongs,
                            sortOption = sortOption,
                            onSongClick = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            homeViewModel = homeViewModel,
                            cardBackgroundColor = cardBackgroundColor,
                            sectionTitleColor = sectionTitleColor,
                            sectionSubtitleColor = sectionSubtitleColor
                        )

                        1 -> ArtistsTab(
                            songs = uiState.allSongs,
                            onArtistClick = { artist, artistSongs ->
                                onNavigateToArtist(artist, artistSongs)
                            }
                        )

                        2 -> AlbumsTab(
                            songs = uiState.allSongs,
                            onAlbumClick = { album, artist, albumSongs ->
                                onNavigateToAlbum(album, artist, albumSongs)
                            }
                        )
                    }
                }
            }
        }
    }
}
