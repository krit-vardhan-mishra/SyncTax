package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.SnackbarUtils
import com.just_for_fun.synctax.ui.components.card.PlaylistCard
import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.ui.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlaylistViewModel

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
    onImportClick: () -> Unit = {}
) {
    val uiState by playlistViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                onRefreshClick = { playlistViewModel.reloadPlaylists() },
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = null,
                currentTab = 0,
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import playlist"
                    )
                },
                text = { Text("Import") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
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
                EmptyPlaylistsState(onImportClick = onImportClick)
            } else {
                LazyColumn(
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

                    items(uiState.playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.playlistId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistsState(
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No playlists yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Import your favorite YouTube playlists to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Import Playlist") }
            )
        }
    }
}
