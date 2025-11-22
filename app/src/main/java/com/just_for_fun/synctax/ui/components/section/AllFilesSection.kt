package com.just_for_fun.synctax.ui.components.section

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.ui.components.utils.SortOption
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

fun LazyListScope.AllFilesSection(
    songs: List<Song>,
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    playerViewModel: PlayerViewModel,
    onNavigateToLibrary: () -> Unit
) {
    // All Songs Section
    item {
        SectionHeader(
            title = "All Songs",
            subtitle = null,
            onViewAllClick = null,
            showSortButton = true,
            currentSortOption = currentSortOption,
            onSortOptionChange = onSortOptionChange
        )
    }

    // Show limited songs initially for better performance
    val displaySongs = if (songs.size > 50) songs.take(50) else songs

    items(
        items = displaySongs,
        key = { song -> song.id }
    ) { song ->
        SongCard(
            song = song,
            onClick = {
                playerViewModel.playSong(song, songs)
            }
        )
    }

    if (songs.size > 50) {
        item {
            Button(
                onClick = onNavigateToLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("View All ${songs.size} Songs")
            }
        }
    }

    // Bottom padding for mini player
    item {
        Spacer(modifier = Modifier.height(96.dp))
    }
}