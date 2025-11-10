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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.ui.components.SongCard
import com.just_for_fun.youtubemusic.ui.viewmodels.HomeViewModel
import com.just_for_fun.youtubemusic.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    ARTIST,
    DATE_ADDED_OLDEST,
    DATE_ADDED_NEWEST,
    CUSTOM
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
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    sortOption = SortOption.NAME_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.NAME_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    sortOption = SortOption.NAME_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.NAME_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Artist") },
                                onClick = {
                                    sortOption = SortOption.ARTIST
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.ARTIST) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Oldest)") },
                                onClick = {
                                    sortOption = SortOption.DATE_ADDED_OLDEST
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.DATE_ADDED_OLDEST) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Newest)") },
                                onClick = {
                                    sortOption = SortOption.DATE_ADDED_NEWEST
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.DATE_ADDED_NEWEST) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom") },
                                onClick = {
                                    sortOption = SortOption.CUSTOM
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == SortOption.CUSTOM) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
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
                    text = { Text("Songs") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text("Artists") }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    text = { Text("Albums") }
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
                        SortOption.NAME_ASC -> "Sorted by Name (A-Z)"
                        SortOption.NAME_DESC -> "Sorted by Name (Z-A)"
                        SortOption.ARTIST -> "Sorted by Artist"
                        SortOption.DATE_ADDED_OLDEST -> "Oldest First"
                        SortOption.DATE_ADDED_NEWEST -> "Newest First"
                        SortOption.CUSTOM -> "Custom Order"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
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