package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.components.utils.EmptyHistoryState

@Composable
fun LocalHistoryTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onLongClick: (Song) -> Unit
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
                    onLongClick = { onLongClick(song) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(225.dp))
            }
        }
    }
}
