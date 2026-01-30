package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.just_for_fun.synctax.presentation.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.presentation.components.section.SongListItem
import com.just_for_fun.synctax.presentation.utils.AlbumColors
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel

/**
 * Full screen for viewing all recommendation categories and shuffle mix.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsDetailScreen(
    recommendationViewModel: RecommendationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onPlaySong: (OnlineSearchResult) -> Unit,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null,
    onNavigateToUserInput: (() -> Unit)? = null
) {
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isLoading by recommendationViewModel.isLoading.collectAsState()
    val currentShuffleBatch by recommendationViewModel.currentShuffleBatch.collectAsState()
    
    Scaffold(
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Recommendations",
                albumColors = AlbumColors.default(),
                showShuffleButton = true,
                showRefreshButton = true,
                showPersonalizeButton = onNavigateToUserInput != null,
                onShuffleClick = { recommendationViewModel.shuffleRecommendations() },
                onRefreshClick = { recommendationViewModel.refreshRecommendations() },
                onPersonalizeClick = onNavigateToUserInput,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { recommendationViewModel.refreshRecommendations() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = androidx.compose.ui.graphics.Color(0xFFFF0033)
                )
            }
        ) {
            when {
                isLoading && recommendations == null -> {
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
                        },
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist
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
    onLoadMore: () -> Unit,
    onAddToQueue: ((OnlineSearchResult) -> Unit)? = null,
    onAddToPlaylist: ((OnlineSearchResult) -> Unit)? = null
) {
    LazyColumn {
        // Artist-based recommendations
        item {
            RecommendationCategorySection(
                title = "Based on Your Artists",
                songs = recommendations.artistBased.take(10),
                onSongClick = onSongClick,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist
            )
        }
        
        // Similar songs
        item {
            RecommendationCategorySection(
                title = "Similar Songs",
                songs = recommendations.similarSongs.take(10),
                onSongClick = onSongClick,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist
            )
        }
        
        // Discovery
        item {
            RecommendationCategorySection(
                title = "Discover New Music",
                songs = recommendations.discovery.take(10),
                onSongClick = onSongClick,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist
            )
        }
        
        // Trending
        item {
            RecommendationCategorySection(
                title = "Trending Now",
                songs = recommendations.trending.take(10),
                onSongClick = onSongClick,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist
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

        // Bottom padding for mini player
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
