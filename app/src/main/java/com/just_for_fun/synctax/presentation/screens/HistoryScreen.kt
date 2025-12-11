package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.OnlineHistoryCard
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel

/**
 * History Screen - Shows both offline and online listening history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Online", "Offline")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Listening History",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> OnlineHistoryTab(
                    history = uiState.onlineHistory,
                    onHistoryClick = { history ->
                        playerViewModel.playUrl(
                            url = history.watchUrl,
                            title = history.title,
                            artist = history.artist,
                            durationMs = 0L
                        )
                    },
                    onRemove = { history ->
                        homeViewModel.deleteOnlineHistory(history.videoId)
                    }
                )
                1 -> OfflineHistoryTab(
                    songs = uiState.listenAgain.take(50),
                    onSongClick = { song ->
                        playerViewModel.playSong(song, uiState.allSongs)
                    }
                )
            }
        }
    }
}

@Composable
private fun OnlineHistoryTab(
    history: List<com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory>,
    onHistoryClick: (com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory) -> Unit,
    onRemove: (com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory) -> Unit
) {
    if (history.isEmpty()) {
        EmptyHistoryState(message = "No online listening history yet.\nPlay some songs online to see them here.")
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.width(400.dp), // Adjust width to fit 2 cards comfortably
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { item ->
                    OnlineHistoryCard(
                        history = item,
                        onClick = { onHistoryClick(item) },
                        onRemoveFromHistory = { onRemove(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(225.dp))
                }
            }
        }
    }
}

@Composable
private fun OfflineHistoryTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyHistoryState(message = "No offline listening history yet.\nPlay some local songs to see them here.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = songs,
                key = { it.id }
            ) { song ->
                SimpleSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { /* Options could be added here */ }
                )
            }
            item {
                Spacer(modifier = Modifier.height(225.dp))
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
