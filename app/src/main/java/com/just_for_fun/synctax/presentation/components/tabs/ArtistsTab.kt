package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.GridCard
import com.just_for_fun.synctax.presentation.components.card.RowCard
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsTab(
    songs: List<Song>,
    onArtistClick: (String, List<Song>) -> Unit = { _, _ -> }
) {
    val artistsMap = songs.groupBy { it.artist }
    val artistList = remember(artistsMap) { artistsMap.entries.toList() }

    // State to toggle between List (RowCard) and Grid (GridCard) views
    var isGridView by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row: Count + View Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${artistList.size} artists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // View Toggle Button
            IconButton(
                onClick = { isGridView = !isGridView }
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.ViewModule,
                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        )

        if (isGridView) {
            // Grid View: Uses LazyVerticalGrid and GridCard
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(artistList, key = { (artist, _) -> artist }) { (artist, artistSongs) ->
                    val imageUri = artistSongs.firstOrNull()?.albumArtUri.orEmpty()
                    val songCount = artistSongs.size

                    GridCard(
                        imageUri = imageUri,
                        title = artist,
                        subtitle = "${songCount} songs",
                        onClick = { onArtistClick(artist, artistSongs) }
                    )
                }
            }
        } else {
            // List View: Uses OptimizedLazyColumn and RowCard
            OptimizedLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artistList, key = { (artist, _) -> artist }) { (artist, artistSongs) ->
                    val imageUri = artistSongs.firstOrNull()?.albumArtUri.orEmpty()

                    RowCard(
                        imageUri = imageUri,
                        title = artist,
                        subtitle = "",
                        detail = "${artistSongs.size} songs",
                        onClick = { onArtistClick(artist, artistSongs) },
                        icon = Icons.Default.ChevronRight
                    )
                }
            }
        }
    }
}