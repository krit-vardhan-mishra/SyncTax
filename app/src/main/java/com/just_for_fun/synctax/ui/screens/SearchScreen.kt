package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.EmptySearchState
import com.just_for_fun.synctax.ui.components.card.OnlineResultCard
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.ui.components.chips.SearchFilterChips
import com.just_for_fun.synctax.ui.components.section.SimpleDynamicMusicTopAppBar
import com.just_for_fun.synctax.ui.dynamic.DynamicAlbumBackground
import com.just_for_fun.synctax.ui.model.SearchFilterType
import com.just_for_fun.synctax.ui.viewmodels.DynamicBackgroundViewModel
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.just_for_fun.synctax.ui.guide.GuideContent
import com.just_for_fun.synctax.ui.guide.GuideOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    resetTrigger: Int = 0,
    onNavigateToAlbum: (String, String, List<Song>) -> Unit = { _, _, _ -> },
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    dynamicBgViewModel: DynamicBackgroundViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val albumColors by dynamicBgViewModel.albumColors.collectAsState()
    val searchQuery = uiState.searchQuery
    val selectedFilter = uiState.selectedFilter

    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Debounced online search - only search after user stops typing for 800ms
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()

        if (searchQuery.isNotEmpty()) {
            // Check if there are local results
            val hasLocal = uiState.allSongs.any { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        (song.album?.contains(searchQuery, ignoreCase = true) == true) ||
                        (song.genre?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Only search online if no local results and after debounce delay
            if (!hasLocal) {
                searchJob = launch {
                    delay(800) // Wait 800ms after user stops typing
                    homeViewModel.searchOnline(searchQuery, selectedFilter)
                }
            }
        }
    }

    LaunchedEffect(playerState.currentSong?.albumArtUri) {
        dynamicBgViewModel.updateAlbumArt(playerState.currentSong?.albumArtUri)
    }

    // Filter songs based on search query and filter type
    val filteredSongs = remember(searchQuery, selectedFilter, uiState.allSongs) {
        if (searchQuery.isEmpty()) {
            uiState.allSongs
        } else {
            val matchedSongs = uiState.allSongs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album?.contains(searchQuery, ignoreCase = true) == true ||
                        song.genre?.contains(searchQuery, ignoreCase = true) == true
            }

            // Apply filter type (for local songs, we only have songs, not albums)
            // Albums filter shows no local results since we don't have album objects
            when (selectedFilter) {
                SearchFilterType.ALL -> matchedSongs
                SearchFilterType.SONGS -> matchedSongs
                SearchFilterType.ALBUMS -> emptyList() // No local album objects
                SearchFilterType.ARTISTS -> emptyList() // No artist objects in local storage
                SearchFilterType.VIDEOS -> emptyList() // No videos in local storage
            }
        }
    }

    // Guide Overlay
    val userPreferences = remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_SEARCH)) }

    Scaffold(
        topBar = {
            SimpleDynamicMusicTopAppBar(
                title = "Search",
                albumColors = albumColors
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
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { homeViewModel.updateSearchQuery(it.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .onFocusChanged { state -> isFocused = state.isFocused },
                    placeholder = { Text("Search songs, artists, albums...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { homeViewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                // Filter chips - shown when search query is not empty
                if (searchQuery.isNotEmpty()) {
                    SearchFilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            homeViewModel.updateSelectedFilter(filter)
                            // Trigger new online search with filter if no local results
                            if (filteredSongs.isEmpty()) {
                                homeViewModel.searchOnline(searchQuery, filter)
                            }
                        },
                        showVideos = false // Only show Songs/Albums filters
                    )
                }

                // Show Listen Again (recently played / most played merged) when search field focused and empty
                if (searchQuery.isEmpty() && isFocused && uiState.listenAgain.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Recently Played",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // show up to 6 recent
                        val recent = uiState.listenAgain.take(6)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recent.forEach { song ->
                                SongCard(song = song, onClick = { playerViewModel.playSong(song) })
                            }
                        }
                    }
                    Divider()
                }

                Divider()

                // Results
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    searchQuery.isEmpty() -> {
                        // Show suggestions or recent searches
                        EmptySearchState()
                    }

                    filteredSongs.isEmpty() -> {
                        // No local results - show scrollable container with no results message and online results
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // No local results message
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "No Local",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Results",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "No songs found on your device for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Show online search status
                                    if (uiState.isSearchingOnline) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Searching online...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Online results section
                            if (!uiState.isSearchingOnline) {
                                if (uiState.onlineSearchResults.isNotEmpty()) {
                                    item {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                    item {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudQueue,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Online Results",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    items(uiState.onlineSearchResults) { result ->
                                        val coroutineScope = rememberCoroutineScope()
                                        OnlineResultCard(result) { res ->
                                            // Check result type and navigate accordingly
                                            when (res.type) {
                                                com.just_for_fun.synctax.core.network.OnlineResultType.ALBUM -> {
                                                    // Fetch album details and show detail screen
                                                    coroutineScope.launch {
                                                        homeViewModel.fetchAlbumDetails(
                                                            browseId = res.browseId ?: res.id,
                                                            onResult = { albumDetails ->
                                                                if (albumDetails != null && albumDetails.songs.isNotEmpty()) {
                                                                    // Convert online songs to Song objects for playback
                                                                    // Use album's thumbnail for all songs to ensure consistent album art display
                                                                    val songs =
                                                                        albumDetails.songs.map { track ->
                                                                            com.just_for_fun.synctax.core.data.local.entities.Song(
                                                                                id = "youtube:${track.videoId}",
                                                                                title = track.title,
                                                                                artist = track.artist,
                                                                                album = albumDetails.title,
                                                                                duration = 0L,
                                                                                filePath = track.watchUrl,
                                                                                genre = null,
                                                                                releaseYear = albumDetails.year.toIntOrNull(),
                                                                                albumArtUri = albumDetails.thumbnail
                                                                            )
                                                                        }
                                                                    // Navigate to AlbumDetailScreen with online songs
                                                                    android.util.Log.d(
                                                                        "SearchScreen",
                                                                        "Album loaded: ${albumDetails.title} with ${songs.size} songs"
                                                                    )
                                                                    homeViewModel.setSelectedOnlineAlbum(
                                                                        albumDetails
                                                                    )
                                                                    onNavigateToAlbum(
                                                                        albumDetails.title,
                                                                        albumDetails.artist,
                                                                        songs
                                                                    )
                                                                } else {
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Album has no songs available",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            },
                                                            onError = { error ->
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Failed to load album: $error",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        )
                                                    }
                                                }

                                                com.just_for_fun.synctax.core.network.OnlineResultType.ARTIST -> {
                                                    // Fetch artist details and show detail screen
                                                    coroutineScope.launch {

                                                        homeViewModel.fetchArtistDetails(
                                                            browseId = res.browseId ?: res.id,
                                                            onResult = { artistDetails ->
                                                                if (artistDetails != null && artistDetails.songs.isNotEmpty()) {
                                                                    homeViewModel.setSelectedOnlineArtist(
                                                                        artistDetails
                                                                    )
                                                                    navController.navigate("online_artist")
                                                                } else {
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Artist has no songs available",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            },
                                                            onError = { error ->
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Failed to load artist: $error",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    // Play online song - construct YouTube URL from videoId
                                                    val youtubeUrl =
                                                        "https://www.youtube.com/watch?v=${res.id}"
                                                    playerViewModel.playUrl(
                                                        url = youtubeUrl,
                                                        title = res.title,
                                                        artist = res.author ?: "Unknown",
                                                        durationMs = res.duration ?: 0L,
                                                        thumbnailUrl = res.thumbnailUrl
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(90.dp))
                                    }
                                } else {
                                    // No online results either
                                    item {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "No Online Results",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "Try different keywords or check your internet connection",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(onClick = {
                                                homeViewModel.searchOnline(
                                                    searchQuery,
                                                    selectedFilter
                                                )
                                            }) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Retry"
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = "Retry")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Show results
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "${filteredSongs.size} results found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(filteredSongs) { song ->
                                SongCard(
                                    song = song,
                                    onClick = { playerViewModel.playSong(song, filteredSongs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGuide) {
        GuideOverlay(
            steps = GuideContent.searchScreenGuide,
            onDismiss = {
                showGuide = false
                userPreferences.setGuideShown(UserPreferences.GUIDE_SEARCH)
            }
        )
    }
}