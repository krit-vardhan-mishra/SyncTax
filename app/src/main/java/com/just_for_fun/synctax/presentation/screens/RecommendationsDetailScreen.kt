package com.just_for_fun.synctax.presentation.screens

import android.util.Log
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    
    LaunchedEffect(recommendations) {
        recommendations?.let { result ->
            val tag = "RecommendationDetailLogger"

            // Helper to determine type from author field
            fun getTypeFromSong(song: com.just_for_fun.synctax.core.network.OnlineSearchResult): String {
                val typeStrings = setOf("Song", "Video", "Album", "Artist", "Podcast", "Episode")
                return if (song.author != null && typeStrings.contains(song.author)) {
                    song.author
                } else {
                    song.type.name
                }
            }

            result.artistBased.forEach { song ->
                Log.d(tag, "Category: Based on Your Artists, Title: ${song.title}, Type: ${getTypeFromSong(song)}, ID: ${song.id}")
            }

            result.similarSongs.forEach { song ->
                Log.d(tag, "Category: Similar Songs, Title: ${song.title}, Type: ${getTypeFromSong(song)}, ID: ${song.id}")
            }

            result.discovery.forEach { song ->
                Log.d(tag, "Category: Discover New Music, Title: ${song.title}, Type: ${getTypeFromSong(song)}, ID: ${song.id}")
            }

            result.trending.forEach { song ->
                Log.d(tag, "Category: Trending Now, Title: ${song.title}, Type: ${getTypeFromSong(song)}, ID: ${song.id}")
            }
        }
    }
    
    Scaffold(
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Recommendations",
                albumColors = AlbumColors.default(),
                showShuffleButton = true,
                showRefreshButton = false, // User requested to remove refresh button and use pull-to-refresh
                showPersonalizeButton = onNavigateToUserInput != null,
                onShuffleClick = { recommendationViewModel.shuffleRecommendations() },
                onRefreshClick = { recommendationViewModel.refreshRecommendations() },
                onPersonalizeClick = onNavigateToUserInput,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { recommendationViewModel.refreshRecommendations() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = Color(0xFFFF0033)
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
