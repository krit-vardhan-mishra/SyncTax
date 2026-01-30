package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.presentation.components.card.OnlineHistoryCard
import com.just_for_fun.synctax.presentation.components.utils.EmptyHistoryState

@Composable
fun OfflineSavedTab(
    songs: List<OnlineSong>,
    onSongClick: (OnlineSong) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyHistoryState(message = "No offline saved songs.\nEnable 'Offline Storage' in Settings to cache songs.")
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.width(400.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id }
                ) { item ->
                    // Reuse OnlineHistoryCard layout by mapping OnlineSong to OnlineListeningHistory for display
                    // OR create a dedicated card. Reusing seems faster if compatible.
                    // OnlineListeningHistory needs: id, playTimestamp, listenDuration, completionRate, skipped, timeOfDay, dayOfWeek, videoId, title, artist, thumbnailUrl, watchUrl
                    // creating a fake history object for display
                    val displayItem = remember(item) {
                        OnlineListeningHistory(
                            videoId = item.videoId,
                            title = item.title,
                            artist = item.artist,
                            thumbnailUrl = item.thumbnailUrl,
                            watchUrl = "https://music.youtube.com/watch?v=${item.videoId}",
                            timestamp = item.addedAt,
                            playDuration = 0,
                            completionRate = 0f,
                            playCount = 1,
                            skipCount = 0
                        )
                    }

                    OnlineHistoryCard(
                        history = displayItem,
                        onClick = { onSongClick(item) },
                        onRemoveFromHistory = null, // Disable remove for now or implement remove from saved
                        onAddToQueue = null
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(225.dp))
                }
            }
        }
    }
}

