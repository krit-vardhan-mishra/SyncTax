package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.ui.components.utils.SortOption
import com.just_for_fun.synctax.ui.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel(),
    userPreferences: UserPreferences,
    onSearchClick: () -> Unit = {},
    onNavigateToArtist: (String, List<Song>) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String, List<Song>) -> Unit = { _, _, _ -> }
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()

    // Update colors when current song changes
    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    Scaffold(
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Library",
                albumColors = albumColors,
                showSortButton = true,
                showShuffleButton = true,
                showSearchButton = true,
                showProfileButton = true,
                onShuffleClick = {
                    val songsToShuffle = when (pagerState.currentPage) {
                        0 -> uiState.allSongs // Songs tab
                        1 -> uiState.allSongs // Artists tab - all songs
                        2 -> uiState.allSongs // Albums tab - all songs
                        else -> emptyList()
                    }
                    if (songsToShuffle.isNotEmpty()) {
                        playerViewModel.shufflePlay(songsToShuffle)
                    }
                },
                onSearchClick = onSearchClick,
                onSortOptionChange = { sortOption = it },
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = sortOption,
                currentTab = pagerState.currentPage
            )
        }
    ) { paddingValues ->
        DynamicAlbumBackground(
            albumColors = albumColors,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tabs
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = { Text("Songs", color = Color(0xFFD80000)) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = { Text("Artists", color = Color(0xFFD80000)) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        text = { Text("Albums", color = Color(0xFFD80000)) }
                    )
                }

                // Swipeable Content - removed padding modifier
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> SongsTab(
                            songs = uiState.allSongs,
                            sortOption = sortOption,
                            onSongClick = { song ->
                                playerViewModel.playSong(song, uiState.allSongs)
                            }
                        )

                        1 -> ArtistsTab(
                            songs = uiState.allSongs,
                            onArtistClick = { artist, artistSongs ->
                                onNavigateToArtist(artist, artistSongs)
                            }
                        )

                        2 -> AlbumsTab(
                            songs = uiState.allSongs,
                            onAlbumClick = { album, artist, albumSongs ->
                                onNavigateToAlbum(album, artist, albumSongs)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongsTab(
    songs: List<Song>,
    sortOption: SortOption,
    onSongClick: (Song) -> Unit
) {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,  // Space below tabs
            bottom = 100.dp  // Space for mini player
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
                Text(
                    text = "${sortedSongs.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        items(sortedSongs) { song ->
            SongCard(
                song = song,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
fun ArtistsTab(
    songs: List<Song>,
    onArtistClick: (String, List<Song>) -> Unit = { _, _ -> }
) {
    val artistsMap = songs.groupBy { it.artist }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,  // Space below tabs
            bottom = 100.dp  // Space for mini player
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${artistsMap.size} artists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(artistsMap.entries.toList()) { (artist, artistSongs) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onArtistClick(artist, artistSongs) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${artistSongs.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumsTab(
    songs: List<Song>,
    onAlbumClick: (String, String, List<Song>) -> Unit = { _, _, _ -> }
) {
    val albumsMap = songs.groupBy { it.album ?: "Unknown Album" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,  // Space below tabs
            bottom = 100.dp  // Space for mini player
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${albumsMap.size} albums",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(albumsMap.entries.toList()) { (album, albumSongs) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onAlbumClick(album, albumSongs.first().artist, albumSongs)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = album,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${albumSongs.first().artist} • ${albumSongs.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}