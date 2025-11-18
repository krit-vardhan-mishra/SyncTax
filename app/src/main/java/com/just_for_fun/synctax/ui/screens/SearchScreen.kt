package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.ui.background.ProfessionalGradientBackground
import com.just_for_fun.synctax.ui.background.SearchSectionHeader
import com.just_for_fun.synctax.ui.components.SongCard
import com.just_for_fun.synctax.ui.components.OnlineResultCard
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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
                    homeViewModel.searchOnline(searchQuery)
                }
            }
        }
    }

    // Filter songs based on search query
    val filteredSongs = remember(searchQuery, uiState.allSongs) {
        if (searchQuery.isEmpty()) {
            uiState.allSongs
        } else {
            uiState.allSongs.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album?.contains(searchQuery, ignoreCase = true) == true ||
                        song.genre?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        ProfessionalGradientBackground (
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
                        IconButton(onClick = { searchQuery = "" }) {
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

            // Show Listen Again (recently played / most played merged) when search field focused and empty
            if (searchQuery.isEmpty() && isFocused && uiState.listenAgain.isNotEmpty()) {
                SearchSectionHeader(text = "Recently Played")

                // show up to 6 recent
                val recent = uiState.listenAgain.take(6)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recent.forEach { song ->
                        SongCard(song = song, onClick = { playerViewModel.playSong(song) })
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
                                    text = "No Local Results",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "No songs found on your device for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
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
                                    SearchSectionHeader(
                                        text = "Online Results",
                                        icon = Icons.Default.CloudQueue
                                    )
                                }
                                items(uiState.onlineSearchResults) { result ->
                                    val coroutineScope = rememberCoroutineScope()
                                    OnlineResultCard(result) { res ->
                                        // Play online song using chunked progressive streaming
                                        val url = res.streamUrl
                                        if (!url.isNullOrEmpty()) {
                                            playerViewModel.playChunkedStream(res.id, url, res.title, res.author ?: "Unknown", res.duration ?: 0L, res.thumbnailUrl)
                                        } else {
                                            // Try to fetch stream URL then play
                                            coroutineScope.launch {
                                                val fetched = homeViewModel.fetchStreamUrl(res.id)
                                                if (!fetched.isNullOrEmpty()) {
                                                    playerViewModel.playChunkedStream(res.id, fetched, res.title, res.author ?: "Unknown", res.duration ?: 0L, res.thumbnailUrl)
                                                } else {
                                                    android.util.Log.w("SearchScreen", "No stream url available for ${res.title}")
                                                }
                                            }
                                        }
                                    }
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
                                        Button(onClick = { homeViewModel.searchOnline(searchQuery) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
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
                                onClick = {
                                    playerViewModel.playSong(song, filteredSongs)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search Your Music",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Find songs, artists, albums, and more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NoResultsState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Results Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No songs match \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try different keywords or check your spelling",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}