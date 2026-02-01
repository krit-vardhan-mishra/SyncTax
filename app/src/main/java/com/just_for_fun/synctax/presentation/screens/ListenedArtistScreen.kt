package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.presentation.model.ArtistUiModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel

/**
 * Parses an artist string that may contain multiple artists separated by various delimiters.
 * Returns a list of individual artist names.
 */
private fun parseArtistString(artistString: String): List<String> {
    // Filter out invalid/category-like artist names
    val invalidNames = listOf("Unknown", "Unknown Artist", "Song", "Video", "Album", "Artist", "Podcast", "Episode")
    if (artistString.isBlank() || invalidNames.any { it.equals(artistString, ignoreCase = true) }) {
        return emptyList()
    }
    
    // Split by common delimiters
    val delimiters = listOf(", ", " & ", " and ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " x ", " vs ", " / ", "; ")
    var artists = listOf(artistString)
    
    for (delimiter in delimiters) {
        artists = artists.flatMap { it.split(delimiter, ignoreCase = true) }
    }
    
    // Leading conjunctions that might remain after splitting (e.g., "and Arohi Mhatre" -> "Arohi Mhatre")
    val leadingConjunctions = listOf("and ", "feat ", "feat. ", "ft ", "ft. ", "featuring ", "with ")
    
    return artists
        .map { name ->
            var cleaned = name.trim()
            // Remove leading conjunctions
            for (conjunction in leadingConjunctions) {
                if (cleaned.startsWith(conjunction, ignoreCase = true)) {
                    cleaned = cleaned.removeRange(0, conjunction.length).trim()
                }
            }
            cleaned
        }
        .filter { name -> 
            name.isNotBlank() && 
            !invalidNames.any { it.equals(name, ignoreCase = true) } &&
            name.length > 1 // Filter single-character names
        }
        .distinct()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenedArtistScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onArtistClick: (String) -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Handle refresh completion - also fetch artist details
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            homeViewModel.forceRefreshLibrary()
            // Also refresh online artist photos
            val allOnlineArtistNames = uiState.onlineHistory
                .flatMap { parseArtistString(it.artist) }
                .distinct()
            if (allOnlineArtistNames.isNotEmpty()) {
                homeViewModel.fetchAllArtistPhotos(allOnlineArtistNames)
            }
            isRefreshing = false
        }
    }

    // Processing data
    val offlineArtists = remember(uiState.artists, uiState.artistPhotos) {
        uiState.artists.map { 
            ArtistUiModel(
                name = it.name, 
                songCount = it.songCount, 
                isOnline = false, 
                imageUrl = it.imageUrl ?: uiState.artistPhotos[it.name] // Use cached photo if local image is null
            )
        }
    }

    // Fetch artist photos for offline artists that don't have images
    val artistsWithoutImages = remember(offlineArtists) {
        offlineArtists.filter { it.imageUrl.isNullOrEmpty() }.map { it.name }
    }
    
    LaunchedEffect(artistsWithoutImages) {
        if (artistsWithoutImages.isNotEmpty()) {
            homeViewModel.fetchAllArtistPhotos(artistsWithoutImages)
        }
    }

    // Process online artists - split multi-artist entries into individual artists
    val onlineArtists = remember(uiState.onlineHistory, uiState.artistPhotos) {
        // Create a map of individual artist -> list of history items containing that artist
        val artistToHistoryMap = mutableMapOf<String, MutableList<com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory>>()
        
        uiState.onlineHistory.forEach { history ->
            val individualArtists = parseArtistString(history.artist)
            if (individualArtists.isEmpty()) {
                // Fallback to original artist name if parsing returns empty
                val name = history.artist.takeIf { it.isNotBlank() } ?: "Unknown"
                artistToHistoryMap.getOrPut(name) { mutableListOf() }.add(history)
            } else {
                individualArtists.forEach { artistName ->
                    artistToHistoryMap.getOrPut(artistName) { mutableListOf() }.add(history)
                }
            }
        }
        
        artistToHistoryMap.map { (name, historyList) ->
            ArtistUiModel(
                name = name,
                songCount = historyList.size,
                isOnline = true,
                // Use cached artist photo first, then fallback to thumbnail
                imageUrl = uiState.artistPhotos[name] 
                    ?: historyList.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }?.thumbnailUrl
            )
        }
        .filter { it.name != "Unknown" && it.name != "Song" && it.name.isNotBlank() }
        .sortedByDescending { it.songCount }
    }
    
    // Fetch photos for online artists that don't have cached photos
    val onlineArtistsWithoutPhotos = remember(onlineArtists, uiState.artistPhotos) {
        onlineArtists.filter { artist ->
            uiState.artistPhotos[artist.name].isNullOrEmpty()
        }.map { it.name }
    }
    
    LaunchedEffect(onlineArtistsWithoutPhotos) {
        if (onlineArtistsWithoutPhotos.isNotEmpty()) {
            homeViewModel.fetchAllArtistPhotos(onlineArtistsWithoutPhotos)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listened Artists", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Text("Offline Artists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (offlineArtists.isNotEmpty()) {
                items(offlineArtists) { artist ->
                    val isLoadingImage = uiState.isLoadingArtistPhotos && artist.imageUrl.isNullOrEmpty()
                    ArtistListItem(artist, isLoadingImage) { onArtistClick(artist.name) }
                }
            } else {
                item { Text("No offline artists found", style = MaterialTheme.typography.bodyMedium) }
            }
            
            item {
                Spacer(Modifier.height(16.dp))
                Text("Online Artists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (onlineArtists.isNotEmpty()) {
                items(onlineArtists) { artist ->
                     ArtistListItem(artist) { onArtistClick(artist.name) }
                }
             } else {
                item { Text("No online artists found", style = MaterialTheme.typography.bodyMedium) }
            }
            item {
                Spacer(Modifier.height(80.dp)) // Bottom padding
            }
        } // End LazyColumn
        } // End PullToRefreshBox
    }
}

@Composable
fun ArtistListItem(
    artist: ArtistUiModel, 
    isLoadingImage: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         // Avatar
         Surface(
             shape = androidx.compose.foundation.shape.CircleShape,
             color = MaterialTheme.colorScheme.surfaceVariant,
             modifier = Modifier.size(50.dp)
         ) {
             Box(contentAlignment = Alignment.Center) {
                 if (isLoadingImage) {
                     CircularProgressIndicator(
                         modifier = Modifier.size(24.dp),
                         color = MaterialTheme.colorScheme.primary,
                         strokeWidth = 2.dp
                     )
                 } else if (!artist.imageUrl.isNullOrEmpty()) {
                      coil.compose.AsyncImage(
                          model = artist.imageUrl,
                          contentDescription = null,
                          modifier = Modifier.fillMaxSize(),
                          contentScale = androidx.compose.ui.layout.ContentScale.Crop
                      )
                 } else {
                     Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                 }
             }
         }
         Spacer(Modifier.width(16.dp))
         Column {
             Text(artist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
             Text(
                 text = if (artist.isOnline) "Online • ${artist.songCount} songs" else "Local • ${artist.songCount} songs", 
                 style = MaterialTheme.typography.bodyMedium, 
                 color = MaterialTheme.colorScheme.onSurfaceVariant
             )
         }
    }
}
