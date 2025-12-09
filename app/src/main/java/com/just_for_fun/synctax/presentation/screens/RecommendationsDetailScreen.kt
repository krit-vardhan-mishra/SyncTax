package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.presentation.components.section.EmptyRecommendationsPrompt
import com.just_for_fun.synctax.presentation.components.section.RecommendationCategorySection
import com.just_for_fun.synctax.presentation.components.section.SongListItem
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel

/**
 * Full screen for viewing all recommendation categories and shuffle mix.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsDetailScreen(
    recommendationViewModel: RecommendationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onPlaySong: (OnlineSearchResult) -> Unit
) {
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isLoading by recommendationViewModel.isLoading.collectAsState()
    val currentShuffleBatch by recommendationViewModel.currentShuffleBatch.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recommendations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { recommendationViewModel.shuffleRecommendations() }
                    ) {
                        Icon(Icons.Default.Shuffle, "Shuffle")
                    }
                    IconButton(
                        onClick = { recommendationViewModel.refreshRecommendations() }
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                recommendations == null -> {
                    EmptyRecommendationsPrompt(
                        onExploreClick = onNavigateBack
                    )
                }
                else -> {
                    RecommendationsDetailContent(
                        recommendations = recommendations!!,
                        currentBatch = currentShuffleBatch,
                        onSongClick = { song ->
                            onPlaySong(song)
                            recommendationViewModel.trackInteraction(
                                song.id, 
                                "played", 
                                recommendationViewModel.getRecommendationReason(song)
                            )
                        },
                        onLoadMore = {
                            recommendationViewModel.loadNextShuffleBatch()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Content for the detail screen with category sections and shuffle batch.
 */
@Composable
private fun RecommendationsDetailContent(
    recommendations: com.just_for_fun.synctax.core.service.RecommendationService.RecommendationResult,
    currentBatch: List<OnlineSearchResult>,
    onSongClick: (OnlineSearchResult) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn {
        // Artist-based recommendations
        item {
            RecommendationCategorySection(
                title = "Based on Your Artists",
                songs = recommendations.artistBased.take(10),
                onSongClick = onSongClick
            )
        }
        
        // Similar songs
        item {
            RecommendationCategorySection(
                title = "Similar Songs",
                songs = recommendations.similarSongs.take(10),
                onSongClick = onSongClick
            )
        }
        
        // Discovery
        item {
            RecommendationCategorySection(
                title = "Discover New Music",
                songs = recommendations.discovery.take(10),
                onSongClick = onSongClick
            )
        }
        
        // Trending
        item {
            RecommendationCategorySection(
                title = "Trending Now",
                songs = recommendations.trending.take(10),
                onSongClick = onSongClick
            )
        }
        
        // Shuffle batch section
        if (currentBatch.isNotEmpty()) {
            item {
                Text(
                    text = "Shuffle Mix (${currentBatch.size} songs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            items(currentBatch) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
            
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Load More Shuffle Songs")
                }
            }
        }
    }
}
