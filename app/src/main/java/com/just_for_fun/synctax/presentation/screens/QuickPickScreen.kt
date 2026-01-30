package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.QuickPickSong
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.guide.GuideContent
import com.just_for_fun.synctax.presentation.guide.GuideOverlay
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.viewmodels.QuickPickViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPicksScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    quickPickViewModel: QuickPickViewModel = viewModel(),
    onFullScreenChanged: (Boolean) -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val quickPickState by quickPickViewModel.uiState.collectAsState()
    val selectedMode by quickPickViewModel.selectedMode.collectAsState()
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_QUICK_PICKS)) }
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

    // Bottom sheet state
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    
    // Player Coordination: Pause unified player when entering Picks screen
    // This prevents audio conflicts between unified player and motion player
    var wasPlayingBeforeEntering by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Save current playback state and pause unified player when entering
        wasPlayingBeforeEntering = playerState.isPlaying
        if (playerState.isPlaying) {
            playerViewModel.togglePlayPause()
        }
    }
    
    // Note: We don't auto-resume on exit because the Motion Player handles 
    // its own song playback. User can resume manually if needed.

    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Stable song list - only update when mode changes or initial load
    // This prevents the pager from resetting when queue updates during playback
    var stableSongList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var lastMode by remember { mutableStateOf(selectedMode) }
    
    // Update stable list only on mode change or when list is empty
    LaunchedEffect(selectedMode, quickPickState.offlineQueue, quickPickState.onlineQueueRaw, uiState.quickPicks) {
        val newList = when (selectedMode) {
            "Online" -> quickPickState.onlineQueueRaw.map { it.toSong() }
            else -> {
                if (quickPickState.offlineQueue.isNotEmpty()) {
                    quickPickState.offlineQueue
                } else {
                    uiState.quickPicks
                }
            }
        }
        
        // Check if mode actually changed
        val modeChanged = selectedMode != lastMode
        
        // Update list if:
        // 1. Mode changed
        // 2. List is empty and we have new songs
        // 3. Initial load (stableSongList is empty)
        if (modeChanged || stableSongList.isEmpty() || newList.isNotEmpty() && stableSongList.isEmpty()) {
            stableSongList = newList
            lastMode = selectedMode
            
            // Preload stream URLs for online songs when queue updates
            if (selectedMode == "Online" && newList.isNotEmpty()) {
                quickPickViewModel.preloadOnlineQueue()
            }
            
            // Auto-play first song when mode changes and we have songs
            if (modeChanged && newList.isNotEmpty()) {
                val firstSong = newList.first()
                if (selectedMode == "Online") {
                    // Stop current playback and play first online song
                    val videoId = firstSong.id.removePrefix("online:")
                    playerViewModel.playUrl(
                        url = "https://www.youtube.com/watch?v=$videoId",
                        title = firstSong.title,
                        artist = firstSong.artist,
                        durationMs = firstSong.duration,
                        thumbnailUrl = firstSong.albumArtUri
                    )
                } else {
                    // Play first offline song
                    playerViewModel.playSong(firstSong, newList)
                }
            }
        }
    }
    
    // Use stable list for the player
    val currentSongs = stableSongList

    // Track which songs have been recorded to avoid duplicate tracking
    var lastTrackedSongId by remember { mutableStateOf<String?>(null) }
    
    // Track song plays for Quick Pick seeding - only after minimum play time
    // This prevents rapid tracking when swiping through songs
    val currentSelectedMode by rememberUpdatedState(selectedMode)
    val currentQuickPickState by rememberUpdatedState(quickPickState)
    
    LaunchedEffect(playerState.currentSong?.id, playerState.isPlaying) {
        val song = playerState.currentSong ?: return@LaunchedEffect
        
        // Only track if song is playing and hasn't been tracked yet
        if (playerState.isPlaying && song.id != lastTrackedSongId) {
            // Wait for minimum play time (3 seconds) before tracking
            // This prevents tracking when user is quickly swiping through songs
            delay(3000L)
            
            // Check if same song is still playing after delay
            if (playerState.currentSong?.id == song.id && playerState.isPlaying) {
                lastTrackedSongId = song.id
                
                if (currentSelectedMode == "Online") {
                    // For online mode, check if it's from our queue
                    currentQuickPickState.onlineQueueRaw.find { it.songId == song.id }?.let {
                        quickPickViewModel.onSongPlayedWithoutQueueRefresh(it)
                    }
                } else {
                    // For offline mode, record the play
                    quickPickViewModel.onOfflineSongPlayedWithoutQueueRefresh(song)
                }
            }
        }
    }

    // Full screen Motion Player
    Box(modifier = Modifier.fillMaxSize()) {
        MotionPlayerScreen(
            songs = currentSongs,
            currentSong = playerState.currentSong ?: currentSongs.firstOrNull(),
            isPlaying = playerState.isPlaying,
            currentPosition = playerState.position,
            totalDuration = playerState.duration,
            onPlayPause = { playerViewModel.togglePlayPause() },
            onSeek = { fraction ->
                playerViewModel.seekTo((fraction * playerState.duration).toLong())
            },
            onBack = { /* No navigation controller available here yet */ },
            onNext = { playerViewModel.next() },
            onPrevious = { playerViewModel.previous() },
            onSongSelected = { song ->
                if (selectedMode == "Online") {
                    // For online songs, play via URL
                    // Extract videoId by removing "online:" prefix if present
                    val videoId = song.id.removePrefix("online:")
                    playerViewModel.playUrl(
                        url = "https://www.youtube.com/watch?v=$videoId",
                        title = song.title,
                        artist = song.artist,
                        durationMs = song.duration,
                        thumbnailUrl = song.albumArtUri
                    )
                } else {
                    // For offline songs, play normally
                    playerViewModel.playSong(song, currentSongs)
                }
            },
            onModeChanged = { mode ->
                quickPickViewModel.setMode(mode)
            },
            onShuffle = {
                // Shuffle regenerates the queue with new recommendations
                quickPickViewModel.shuffle()
            },
            selectedMode = selectedMode,
            onFullScreenChanged = onFullScreenChanged
        )

        // Guide overlay
        if (showGuide) {
            GuideOverlay(
                steps = GuideContent.quickPicksScreenGuide,
                onDismiss = {
                    showGuide = false
                    userPreferences.setGuideShown(UserPreferences.GUIDE_QUICK_PICKS)
                }
            )
        }
    }

    // Create options for the dialog
    val dialogOptions = remember(selectedSong, uiState.favoriteSongs) {
        selectedSong?.let { song ->
            val isFavorite = uiState.favoriteSongs.any { it.id == song.id }
            mutableListOf<DialogOption>().apply {
                // Play option
                add(
                    DialogOption(
                        id = "play",
                        title = "Play",
                        subtitle = "Play this song",
                        icon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            if (selectedMode == "Online") {
                                val videoId = song.id.removePrefix("online:")
                                playerViewModel.playUrl(
                                    url = "https://www.youtube.com/watch?v=$videoId",
                                    title = song.title,
                                    artist = song.artist,
                                    durationMs = song.duration,
                                    thumbnailUrl = song.albumArtUri
                                )
                            } else {
                                playerViewModel.playSong(song, currentSongs)
                            }
                        }
                    )
                )
                
                // Add to Queue option
                add(
                    DialogOption(
                        id = "add_to_queue",
                        title = "Add to Queue",
                        subtitle = "Add to end of current queue",
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            playerViewModel.addToQueue(song)
                        }
                    )
                )
                
                // Add to Favorites option (only for offline songs)
                if (selectedMode == "Offline") {
                    add(
                        DialogOption(
                            id = "toggle_favorite",
                            title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            subtitle = if (isFavorite) "Remove from your liked songs" else "Add to your liked songs",
                            icon = {
                                Icon(
                                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                            onClick = {
                                homeViewModel.toggleFavorite(song.id)
                            }
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    // Bottom options dialog
    BottomOptionsDialog(
        song = selectedSong,
        isVisible = showOptionsDialog,
        onDismiss = { showOptionsDialog = false },
        options = dialogOptions,
        title = "Song Options",
        description = "Choose an action for this song"
    )
}

/**
 * Extension function to convert QuickPickSong to Song for playback
 */
private fun QuickPickSong.toSong(): Song {
    // For online songs, use "online:" prefix to match PlayerViewModel's format
    val effectiveId = if (source == "online") "online:$songId" else songId
    return Song(
        id = effectiveId,
        title = title,
        artist = artist,
        album = null,
        duration = duration,
        filePath = filePath ?: "online:$songId",
        genre = null,
        releaseYear = null,
        albumArtUri = thumbnailUrl
    )
}
