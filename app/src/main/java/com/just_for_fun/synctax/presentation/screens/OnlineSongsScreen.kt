package com.just_for_fun.synctax.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.card.OnlineSongCard
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.OnlineSongsViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel

private const val TAG = "OnlineSongsScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlineSongsScreen(
    onlineSongsViewModel: OnlineSongsViewModel,
    playerViewModel: PlayerViewModel,
    dynamicBgViewModel: DynamicBackgroundViewModel,
    userPreferences: UserPreferences,
    scaffoldState: BottomSheetScaffoldState,
    onOpenSettings: () -> Unit = {},
    onShuffleClick: () -> Unit = {}
) {
    val uiState by onlineSongsViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Debug: Log when OnlineSongsScreen is composed
    LaunchedEffect(Unit) {
        Log.d(TAG, "OnlineSongsScreen composed - screen is now active")
    }

    // Update background when song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Ensure bottom sheet is partially expanded
    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.partialExpand()
    }

    // Collect error messages from player view model
    LaunchedEffect(Unit) {
        playerViewModel.errorMessages.collect { message ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, message)
        }
    }

    // Show error from uiState
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, error)
            onlineSongsViewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Online Songs",
                albumColors = albumColors,
                showShuffleButton = true,
                showSortButton = false,
                showRefreshButton = false,
                onShuffleClick = onShuffleClick,
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = null,
                currentTab = 0,
                onOpenSettings = onOpenSettings
            )
        }
    ) { paddingValues ->
        DynamicAlbumBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (uiState.history.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No online songs listened yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Songs you play online will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                            text = "${uiState.history.size} online songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(uiState.history, key = { it.id }) { history ->
                        OnlineSongCard(
                            history = history,
                            onClick = {
                                playerViewModel.playUrl(
                                    url = history.watchUrl,
                                    title = history.title,
                                    artist = history.artist,
                                    durationMs = 0L
                                )
                            },
                            isPlaying = history.videoId == (if (playerState.currentSong?.id?.startsWith("online:") == true)
                                playerState.currentSong?.id?.removePrefix("online:")
                            else null),
                            onRemoveFromHistory = {
                                onlineSongsViewModel.deleteHistory(history.videoId)
                            }
                        )
                    }

                    // Load more button
                    if (uiState.hasMore && !uiState.isLoadingMore) {
                        item {
                            OutlinedButton(
                                onClick = { onlineSongsViewModel.loadMoreHistory() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("Load More")
                            }
                        }
                    }

                    // Loading more indicator
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}