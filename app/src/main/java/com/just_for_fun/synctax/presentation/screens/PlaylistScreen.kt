package com.just_for_fun.synctax.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.card.PlaylistCard
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.components.state.EmptyPlaylistsState
import com.just_for_fun.synctax.presentation.components.utils.FABMenu
import com.just_for_fun.synctax.presentation.components.utils.FabAction
import com.just_for_fun.synctax.presentation.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlaylistViewModel

private const val TAG = "PlaylistScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    dynamicBgViewModel: DynamicBackgroundViewModel,
    userPreferences: UserPreferences,
    scaffoldState: BottomSheetScaffoldState,
    onOpenSettings: () -> Unit = {},
    onPlaylistClick: (Int) -> Unit = {},
    onImportClick: () -> Unit = {},
    onCreatePlaylistClick: () -> Unit = {}
) {
    val uiState by playlistViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val fabActions = remember {
        listOf(
            FabAction(
                label = "Import YT Playlist",
                icon = Icons.Default.PlayArrow,
                onClick = onImportClick
            ),
            FabAction(
                label = "Create Playlist",
                icon = Icons.Default.Add,
                onClick = onCreatePlaylistClick
            )
        )
    }

    // Debug: Log when PlaylistScreen is composed
    LaunchedEffect(Unit) {
        Log.d(TAG, "PlaylistScreen composed - screen is now active")
    }

    // Update background when song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Ensure bottom sheet is partially expanded when on playlist screen
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.partialExpand()
    }

    // Collect error messages from player view model
    LaunchedEffect(Unit) {
        playerViewModel.errorMessages.collect { message ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Playlists",
                albumColors = albumColors,
                showSortButton = false,
                showShuffleButton = false,
                showRefreshButton = true,
                showProfileButton = true,
                onRefreshClick = {
                    Log.d(TAG, "Refresh button clicked")
                    playlistViewModel.reloadPlaylists()
                },
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = null,
                currentTab = 0,
                onOpenSettings = {
                    Log.d(TAG, "Settings button clicked")
                    onOpenSettings()
                }
            )
        },
        floatingActionButton = {
            FABMenu(actions = fabActions)
        }
    ) { paddingValues ->
        DynamicAlbumBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.playlists.isEmpty() && !uiState.isLoading) {
                // Empty state
                EmptyPlaylistsState(onImportClick = {
                    Log.d(TAG, "Empty state Import clicked")
                    onImportClick()
                })
            } else {
                OptimizedLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 150.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${uiState.playlists.size} playlists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.playlists, key = { it.playlistId }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = {
                                Log.d(
                                    TAG,
                                    "Playlist card clicked: ${playlist.name} (id: ${playlist.playlistId})"
                                )
                                onPlaylistClick(playlist.playlistId)
                            }
                        )
                    }
                }
            }
        }
    }
}
