package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.background.EnhancedEmptyQuickPicksState
import com.just_for_fun.synctax.presentation.components.card.RecommendationCard
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.presentation.guide.GuideContent
import com.just_for_fun.synctax.presentation.guide.GuideOverlay
import com.just_for_fun.synctax.presentation.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPicksScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel()

) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_QUICK_PICKS)) }
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

    // Bottom sheet state
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Full screen Motion Player
    Box(modifier = Modifier.fillMaxSize()) {
        MotionPlayerScreen(
            songs = uiState.quickPicks,
            currentSong = playerState.currentSong ?: uiState.quickPicks.firstOrNull(),
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
                    playerViewModel.playSong(song, uiState.quickPicks)
            }
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
                            playerViewModel.playSong(song, uiState.quickPicks)
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
                
                // Add to Favorites option
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
