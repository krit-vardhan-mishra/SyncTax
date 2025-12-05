package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.SongCard
import com.just_for_fun.synctax.presentation.components.utils.SortOption
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Composable
fun SongsTab(
    songs: List<Song>,
    sortOption: SortOption,
    onSongClick: (Song, List<Song>) -> Unit,
    homeViewModel: HomeViewModel,
    cardBackgroundColor: Color,
    sectionTitleColor: Color,
    sectionSubtitleColor: Color
) {
    val lazyListState = rememberLazyListState()

    // Memoized sorting - O(n log n) only when songs or sortOption changes
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortOption.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ARTIST_DESC -> songs.sortedByDescending { it.artist.lowercase() }
            SortOption.RELEASE_YEAR_DESC -> songs.sortedByDescending { it.releaseYear ?: 0 }
            SortOption.RELEASE_YEAR_ASC -> songs.sortedBy { it.releaseYear ?: 0 }
            SortOption.ADDED_TIMESTAMP_DESC -> songs.sortedByDescending { it.addedTimestamp }
            SortOption.ADDED_TIMESTAMP_ASC -> songs.sortedBy { it.addedTimestamp }
            SortOption.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SortOption.DURATION_ASC -> songs.sortedBy { it.duration }
            SortOption.NAME_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOption.DATE_ADDED_OLDEST -> songs.sortedBy { it.addedTimestamp }
            SortOption.DATE_ADDED_NEWEST -> songs.sortedByDescending { it.addedTimestamp }
            SortOption.CUSTOM -> songs // Keep original order
        }
    }

    // Fast scroll detection - O(1) computation
    var lastScrollTime by remember { mutableStateOf(0L) }
    var lastScrollPosition by remember { mutableStateOf(0) }
    var isScrollingFast by remember { mutableStateOf(false) }

    // Monitor scroll for pagination trigger - O(1) per frame
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .debounce(50L) // Reduced from 100ms for more responsive loading
            .collectLatest { lastIndex ->
                if (lastIndex != null) {
                    val currentTime = System.currentTimeMillis()
                    val scrollDelta = kotlin.math.abs(lastIndex - lastScrollPosition)
                    val timeDelta = currentTime - lastScrollTime

                    // Detect fast scrolling: more than 10 items per 100ms
                    isScrollingFast = timeDelta > 0 && (scrollDelta.toFloat() / timeDelta * 100) > 10f

                    lastScrollPosition = lastIndex
                    lastScrollTime = currentTime

                    // Trigger preload when approaching end (5 items before end)
                    // This is handled by HomeViewModel now
                }
            }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${sortedSongs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isScrollingFast) {
                        Text(
                            text = "Fast scrolling detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = when (sortOption) {
                        SortOption.TITLE_ASC -> "Sorted by Title (A-Z)"
                        SortOption.TITLE_DESC -> "Sorted by Title (Z-A)"
                        SortOption.ARTIST_ASC -> "Sorted by Artist (A-Z)"
                        SortOption.ARTIST_DESC -> "Sorted by Artist (Z-A)"
                        SortOption.RELEASE_YEAR_DESC -> "Sorted by Newest First"
                        SortOption.RELEASE_YEAR_ASC -> "Sorted by Oldest First"
                        SortOption.ADDED_TIMESTAMP_DESC -> "Sorted by Recently Added"
                        SortOption.ADDED_TIMESTAMP_ASC -> "Sorted by Added First"
                        SortOption.DURATION_DESC -> "Sorted by Longest First"
                        SortOption.DURATION_ASC -> "Sorted by Shortest First"
                        SortOption.NAME_ASC -> "Sorted by Name (A-Z)"
                        SortOption.NAME_DESC -> "Sorted by Name (Z-A)"
                        SortOption.ARTIST -> "Sorted by Artist"
                        SortOption.DATE_ADDED_OLDEST -> "Sorted by Oldest First"
                        SortOption.DATE_ADDED_NEWEST -> "Sorted by Newest First"
                        SortOption.CUSTOM -> "Custom Order"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD80000)
                )
            }
        }

        // Render songs - O(k) where k = visible items only (LazyColumn optimization)
        items(sortedSongs, key = { it.id }) { song ->
            SongCard(
                song = song,
                onClick = {
                    // O(n) worst case for indexOf, but typically O(1) as songs are near top
                    val index = sortedSongs.indexOf(song)
                    val queue = sortedSongs.drop(index) // O(n-index) creates sublist
                    onSongClick(song, queue)
                },
                onDelete = { deletedSong ->
                    // Remove from local database and refresh UI
                    homeViewModel.deleteSong(deletedSong)
                },
                backgroundColor = cardBackgroundColor,
                titleColor = sectionTitleColor,
                artistColor = sectionSubtitleColor
            )
        }
    }
}
