package com.just_for_fun.synctax.ui.screens

import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.dynamic.DynamicHorizontalBackground
import com.just_for_fun.synctax.ui.background.EnhancedEmptyQuickPicksState
import com.just_for_fun.synctax.ui.components.RecommendationCard
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

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
    val showGuide by userPreferences.showQuickPicksGuide.collectAsState()
    var guideStep by remember { mutableStateOf(0) }
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

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
                        playerViewModel.shufflePlay(uiState.quickPicks)
                    }
                }
            )
        }
    ) { paddingValues ->
        DynamicHorizontalBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
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
                        EnhancedEmptyQuickPicksState(albumColors = albumColors)
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
    
        // Quick tour overlay
        if (showGuide) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Close icon in the top-right of the card
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { userPreferences.setQuickPicksGuide(false) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Quick Tour",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Text(
                            text = "Quick Picks Tour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val steps = listOf(
                            "This section shows recommendations we think you'll love.",
                            "Tap a song to start playing or view more details.",
                            "Use the shuffle button to play the picks in random order."
                        )
                        Text(
                            text = steps.getOrNull(guideStep) ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = {
                                if (guideStep < steps.size - 1) {
                                    guideStep++
                                } else {
                                    userPreferences.setQuickPicksGuide(false)
                                }
                            }) {
                                Text("Next")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { userPreferences.setQuickPicksGuide(false) }) {
                                Text("Skip")
                            }
                        }
                    }
                }
            }
        }
    }
}