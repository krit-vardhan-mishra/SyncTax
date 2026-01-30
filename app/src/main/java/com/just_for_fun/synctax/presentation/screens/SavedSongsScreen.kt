package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.presentation.components.card.OnlineSongCard
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSongsScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Songs",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 150.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.offlineSavedSongs, key = { it.id }) { song ->
                val displayItem = remember(song) {
                    OnlineListeningHistory(
                        videoId = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        watchUrl = "https://music.youtube.com/watch?v=${song.videoId}",
                        timestamp = song.addedAt,
                        playDuration = 0,
                        completionRate = 0f,
                        playCount = 1,
                        skipCount = 0
                    )
                }

                OnlineSongCard(
                    history = displayItem,
                    onClick = {
                        playerViewModel.playUrl(
                            url = "https://music.youtube.com/watch?v=${song.videoId}",
                            title = song.title,
                            artist = song.artist,
                            durationMs = song.duration?.times(1000L) ?: 0L,
                            thumbnailUrl = song.thumbnailUrl
                        )
                    },
                    isPlaying = song.videoId == (if (playerState.currentSong?.id?.startsWith("online:") == true)
                        playerState.currentSong?.id?.removePrefix("online:")
                    else null),
                    onRemoveFromHistory = { homeViewModel.removeSavedSong(song) },
                    onAddToQueue = null, // TODO: implement if needed
                    onSave = null, // Already saved
                    onDownload = null // TODO: implement if needed
                )
            }
        }
    }
}