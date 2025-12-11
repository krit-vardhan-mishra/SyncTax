package com.just_for_fun.synctax.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.card.PlaylistCardLarger
import com.just_for_fun.synctax.presentation.components.card.PlaylistCardMedium
import com.just_for_fun.synctax.presentation.components.card.PlaylistCardSmall
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

enum class CardType { LARGE, MEDIUM, SMALL }

fun CardType.next() = when (this) {
    CardType.LARGE -> CardType.MEDIUM
    CardType.MEDIUM -> CardType.SMALL
    CardType.SMALL -> CardType.LARGE
}

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
    onImportClick: (String) -> Unit = {},
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
                label = "Create Playlist",
                icon = Icons.Default.Add,
                onClick = onCreatePlaylistClick
            ),
            FabAction(
                label = "Import Playlist",
                icon = Icons.Default.PlayArrow,
                onClick = { onImportClick("youtube") }
            )
        )
    }

    val cardType = remember { mutableStateOf(CardType.LARGE) }

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
                    onImportClick("youtube")
                })
            } else {
                if (cardType.value == CardType.MEDIUM) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 150.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.playlists.size} playlists",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { cardType.value = CardType.LARGE }) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Large view",
                                            tint = if (cardType.value == CardType.LARGE) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { cardType.value = CardType.MEDIUM }) {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = "Medium view",
                                            tint = if (cardType.value == CardType.MEDIUM) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { cardType.value = CardType.SMALL }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = "Small view",
                                            tint = if (cardType.value == CardType.SMALL) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        items(uiState.playlists, key = { it.playlistId }) { playlist ->
                            PlaylistCardMedium(
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
                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.playlists.size} playlists",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { cardType.value = CardType.LARGE }) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Large view",
                                            tint = if (cardType.value == CardType.LARGE) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { cardType.value = CardType.MEDIUM }) {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = "Medium view",
                                            tint = if (cardType.value == CardType.MEDIUM) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { cardType.value = CardType.SMALL }) {
                                        Icon(
                                            Icons.Default.ViewList,
                                            contentDescription = "Small view",
                                            tint = if (cardType.value == CardType.SMALL) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        items(uiState.playlists, key = { it.playlistId }) { playlist ->
                            when (cardType.value) {
                                CardType.LARGE -> PlaylistCardLarger(
                                    playlist = playlist,
                                    onClick = {
                                        Log.d(
                                            TAG,
                                            "Playlist card clicked: ${playlist.name} (id: ${playlist.playlistId})"
                                        )
                                        onPlaylistClick(playlist.playlistId)
                                    }
                                )
                                CardType.SMALL -> PlaylistCardSmall(
                                    playlist = playlist,
                                    onClick = {
                                        Log.d(
                                            TAG,
                                            "Playlist card clicked: ${playlist.name} (id: ${playlist.playlistId})"
                                        )
                                        onPlaylistClick(playlist.playlistId)
                                    }
                                )
                                else -> {} // MEDIUM handled above
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}
