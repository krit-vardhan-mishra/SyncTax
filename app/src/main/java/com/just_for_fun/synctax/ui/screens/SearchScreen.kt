package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.just_for_fun.synctax.core.network.OnlineResultType
import com.just_for_fun.synctax.ui.components.SnackbarUtils
import com.just_for_fun.synctax.ui.guide.GuideContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.just_for_fun.synctax.ui.guide.GuideOverlay
import com.just_for_fun.synctax.ui.theme.LightHomeCardBackground
import com.just_for_fun.synctax.ui.theme.LightHomeSectionTitle
import com.just_for_fun.synctax.ui.theme.LightHomeSectionSubtitle

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
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var suggestionsJob by remember { mutableStateOf<Job?>(null) }
    var isSuggestionsExpanded by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Theme-aware colors
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1A1A1D) else LightHomeCardBackground
    val sectionTitleColor = if (isDarkTheme) Color.White else LightHomeSectionTitle
    val sectionSubtitleColor = if (isDarkTheme) Color(0xFFB3B3B3) else LightHomeSectionSubtitle

    // Function to perform search
    fun performSearch() {
        if (searchQuery.isNotEmpty()) {
            keyboardController?.hide()
            homeViewModel.clearSearchSuggestions()
            homeViewModel.searchOnline(searchQuery, selectedFilter)
        }
    }

    // Debounced search suggestions - fetch after 300ms of typing
    LaunchedEffect(searchQuery) {
        suggestionsJob?.cancel()

        if (searchQuery.length >= 2) {
            suggestionsJob = launch {
                delay(300) // Wait 300ms after user stops typing
                homeViewModel.fetchSearchSuggestions(searchQuery)
            }
        } else {
            homeViewModel.clearSearchSuggestions()
        }
    }

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

    // Collect error messages from player view model
    LaunchedEffect(Unit) {
        playerViewModel.errorMessages.collect { message ->
            SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, message)
        }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    onValueChange = { homeViewModel.updateSearchQuery(it) },
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
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch() }
                    )
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

                // Search suggestions - shown while typing
                if (searchQuery.isNotEmpty() && uiState.searchSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSuggestionsExpanded = !isSuggestionsExpanded }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Suggestions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isSuggestionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isSuggestionsExpanded) "Collapse suggestions" else "Expand suggestions"
                            )
                        }
                        if (isSuggestionsExpanded) {
                            uiState.searchSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Set search query and trigger search
                                            homeViewModel.updateSearchQuery(suggestion)
                                            homeViewModel.clearSearchSuggestions()
                                            homeViewModel.searchOnline(suggestion, selectedFilter)
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Arrow to fill search input
                                    IconButton(
                                        onClick = {
                                            homeViewModel.updateSearchQuery(suggestion)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = "Use this suggestion",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Divider()
                        }
                    }
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
                                SongCard(
                                    song = song,
                                    onClick = { playerViewModel.playSong(song) },
                                    backgroundColor = cardBackgroundColor,
                                    titleColor = sectionTitleColor,
                                    artistColor = sectionSubtitleColor
                                )
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
                        // Show search history or empty state
                        if (uiState.searchHistory.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Searches",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { homeViewModel.clearSearchHistory() }) {
                                            Text(
                                                text = "Clear all",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                items(uiState.searchHistory) { historyItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    // Set the search query and trigger search
                                                    homeViewModel.updateSearchQuery(historyItem.query)
                                                    homeViewModel.searchOnline(historyItem.query, selectedFilter)
                                                },
                                                onLongClick = {
                                                    homeViewModel.deleteSearchHistoryItem(historyItem.id)
                                                    SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, "Search history item deleted")
                                                }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = historyItem.query,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Arrow button to fill search input
                                        IconButton(
                                            onClick = {
                                                // Just fill the search box without triggering search
                                                homeViewModel.updateSearchQuery(historyItem.query)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NorthWest,
                                                contentDescription = "Use this search",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                }

                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        } else {
                            // No search history - show default empty state
                            EmptySearchState()
                        }
                    }

                    filteredSongs.isEmpty() -> {
                        // No local results - show scrollable container with no results message and online results
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Online results section
                            if (uiState.isSearchingOnline) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator()
                                            Text(
                                                text = "Searching online...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
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
                                                OnlineResultType.ALBUM -> {
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
                                                                            Song(
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
                                                                    SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, "Album has no songs available")
                                                                }
                                                            },
                                                            onError = { error ->
                                                                SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, "Failed to load album: $error")
                                                            }
                                                        )
                                                    }
                                                }

                                                OnlineResultType.ARTIST -> {
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
                                                                    SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, "Artist has no songs available")
                                                                }
                                                            },
                                                            onError = { error ->
                                                                SnackbarUtils.ShowSnackbar(coroutineScope, snackbarHostState, "Failed to load artist: $error")
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
                                    onClick = { playerViewModel.playSong(song, filteredSongs) },
                                    backgroundColor = cardBackgroundColor,
                                    titleColor = sectionTitleColor,
                                    artistColor = sectionSubtitleColor
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