package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.utils.EmptyHistoryState

@Composable
fun OfflineSavedTab(
    songs: List<OnlineSong>,
    onSongClick: (OnlineSong) -> Unit,
    onRemove: (OnlineSong) -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val isOfflineCacheEnabled by userPreferences.offlineCacheEnabled.collectAsState()
    
    if (!isOfflineCacheEnabled) {
        EmptyHistoryState(
            message = "Offline Storage is disabled.\nEnable it in Settings to save songs for offline listening."
        )
    } else if (songs.isEmpty()) {
        EmptyHistoryState(message = "No offline saved songs.\nPlay songs fully to save them automatically.")
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
                // Show info message
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Songs played fully (>80%) are cached here for offline use.", // Shortened text
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
                
                items(
                    items = songs,
                    key = { it.id }
                ) { item ->
                    // Map OnlineSong to Song for SimpleSongCard
                    val mappedSong = com.just_for_fun.synctax.data.local.entities.Song(
                        id = item.videoId,
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        duration = (item.duration ?: 0) * 1000L,
                        filePath = "offline", // Placeholder
                        genre = null,
                        releaseYear = 0,
                        albumArtUri = item.thumbnailUrl
                    )

                    com.just_for_fun.synctax.presentation.components.card.SimpleSongCard(
                        song = mappedSong,
                        onClick = { onSongClick(item) },
                        onLongClick = { onRemove(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(225.dp))
                }
            }
        }
    }
}