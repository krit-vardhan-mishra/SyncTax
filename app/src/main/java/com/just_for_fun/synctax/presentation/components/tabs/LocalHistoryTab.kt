package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.presentation.components.utils.EmptyHistoryState

@Composable
fun OnlineHistoryTab(
    history: List<OnlineListeningHistory>,
    onHistoryClick: (OnlineListeningHistory) -> Unit,
    onRemove: (OnlineListeningHistory) -> Unit
) {
    if (history.isEmpty()) {
        EmptyHistoryState(message = "No online listening history yet.\nPlay some songs online to see them here.")
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { item ->
                    // Map OnlineListeningHistory to Song for SimpleSongCard
                    val mappedSong = com.just_for_fun.synctax.data.local.entities.Song(
                        id = item.videoId,
                        title = item.title,
                        artist = item.artist,
                        album = "Online History",
                        duration = item.playDuration * 1000L, // Convert seconds to ms if needed, or use playDuration directly if logic dictates
                        filePath = item.watchUrl,
                        genre = item.genre,
                        releaseYear = 0,
                        albumArtUri = item.thumbnailUrl
                    )

                    com.just_for_fun.synctax.presentation.components.card.SimpleSongCard(
                        song = mappedSong,
                        onClick = { onHistoryClick(item) },
                        onLongClick = { onRemove(item) } // Using long click for remove as an option, or keep it distinct? User only said "card design". SimpleSongCard has limited actions. Will use long press for remove if appropriate or add swipe behavior later.
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(225.dp))
                }
            }
        }
    }
}

