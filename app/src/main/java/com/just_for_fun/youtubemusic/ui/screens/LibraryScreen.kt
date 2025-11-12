package com.just_for_fun.youtubemusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.ui.components.SongCard
import com.just_for_fun.youtubemusic.ui.viewmodels.HomeViewModel
import com.just_for_fun.youtubemusic.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    RELEASE_YEAR_DESC("Newest First"),
    RELEASE_YEAR_ASC("Oldest First"),
    ADDED_TIMESTAMP_DESC("Recently Added"),
    ADDED_TIMESTAMP_ASC("Added First"),
    DURATION_DESC("Longest First"),
    DURATION_ASC("Shortest First"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    ARTIST("Artist"),
    DATE_ADDED_OLDEST("Date Added (Oldest)"),
    DATE_ADDED_NEWEST("Date Added (Newest)"),
    CUSTOM("Custom")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onSearchClick: () -> Unit = {},
    onNavigateToArtist: (String, List<Song>) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String, List<Song>) -> Unit = { _, _, _ -> }
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Sync pager state with selected tab
    LaunchedEffect(pagerState.currentPage) {
        // Already synced via pager state
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    // Sort button (only visible on Songs tab)
                    if (pagerState.currentPage == 0) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (option == sortOption) Icons.Default.Check else Icons.Default.Sort,
                                            contentDescription = null,
                                            tint = if (option == sortOption)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.shuffleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            // Swipeable Content
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
        contentPadding = PaddingValues(16.dp),
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
        contentPadding = PaddingValues(16.dp),
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
                    Column {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
        contentPadding = PaddingValues(16.dp),
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
                    Column {
                        Text(
                            text = album,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${albumSongs.first().artist} â€¢ ${albumSongs.size} songs",
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