package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Quick Picks",
                albumColors = albumColors,
                showShuffleButton = true,
                onShuffleClick = {
                    if (uiState.quickPicks.isNotEmpty()) {
                        // QuickPicks screen uses smart shuffle by default
                        playerViewModel.shufflePlayWithRecommendations(uiState.quickPicks)
                    }
                }
            )
        }
    ) { paddingValues ->
        DynamicAlbumBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 96.dp)
        ) {
            OptimizedLazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (uiState.isGeneratingRecommendations) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generating Quick Picks...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "x Listen to songs and we'll find your match.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else if (uiState.quickPicks.isEmpty()) {
                    item {
                        EnhancedEmptyQuickPicksState(albumColors = albumColors, trainingDataSize = uiState.trainingDataSize)
                    }
                } else {
                    // Display the list of recommendations
                    itemsIndexed(uiState.quickPicks) { index, song ->
                        val score = uiState.recommendationScores.find { it.songId == song.id }

                        RecommendationCard(
                            song = song,
                            rank = index + 1,
                            score = score?.score?.toFloat() ?: 0f,
                            confidence = score?.confidence,
                            reason = score?.reason,
                            onClick = {
                                playerViewModel.playSong(song, uiState.quickPicks)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedSong = song
                                showOptionsDialog = true
                            }
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    
        // Create options for the dialog
        val dialogOptions = remember(selectedSong) {
            selectedSong?.let { song ->
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
}
