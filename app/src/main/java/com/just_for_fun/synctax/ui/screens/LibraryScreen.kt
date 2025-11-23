package com.just_for_fun.synctax.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    onNavigateToArtist: (String, List<Song>) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String, List<Song>) -> Unit = { _, _, _ -> },
    onOpenSettings: () -> Unit = {},
    onTrainClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    
    // Storage permission launcher for Android 11+
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Permission result handled
    }
    
    // Regular storage permissions for Android 10 and below
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.w("LibraryScreen", "Write storage permission denied")
        }
    }
    
    // Request storage permissions on first composition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    storagePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("LibraryScreen", "Error requesting storage permission", e)
                }
            }
        } else {
            // Android 10 and below - Request WRITE_EXTERNAL_STORAGE
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

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
                showRefreshButton = true,
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
                onRefreshClick = { homeViewModel.scanMusic() },
                onSortOptionChange = { sortOption = it },
                userPreferences = userPreferences,
                userName = userName,
                userInitial = userInitial,
                sortOption = sortOption,
                currentTab = pagerState.currentPage,
                onOpenSettings = onOpenSettings,
                onTrainClick = onTrainClick
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
                // --- CUSTOM SEGMENTED/PILL TAB ROW ---
                val tabs = listOf("Songs", "Artists", "Albums")

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    // Container color: Slightly lighter than background for contrast
                    containerColor = Color(0xFF252525),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {}, // Remove the default bottom line
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            // The floating pill indicator
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                    .fillMaxSize()
                                    .padding(4.dp) // Inset slightly
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .zIndex(-1f) // Behind the text
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(CircleShape) // Round the entire tab row
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index

                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    // Text color changes based on selection for contrast
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.zIndex(1f)
                        )
                    }
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
                            onSongClick = { song, queue ->
                                playerViewModel.playSong(song, queue)
                            },
                            homeViewModel = homeViewModel
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

/**
 * SongsTab displays a paginated list of songs with efficient scrolling
 * 
 * Complexity Analysis:
 * - Sorting: O(n log n) where n = total songs, but memoized with remember{}
 * - Rendering: O(k) where k = visible items (LazyColumn only renders visible)
 * - Memory: O(n) for sorted list, but songs loaded progressively via pagination
 */
@Composable
fun SongsTab(
    songs: List<Song>,
    sortOption: SortOption,
    onSongClick: (Song, List<Song>) -> Unit,
    homeViewModel: HomeViewModel
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
                }
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
            top = 8.dp,
            bottom = 100.dp
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
                onClick = { onArtistClick(artist, artistSongs) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF151515)
                )
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
                            overflow = TextOverflow.Ellipsis
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
            top = 8.dp,
            bottom = 100.dp
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
                },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF151515)
                )
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