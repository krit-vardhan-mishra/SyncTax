package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlaylistViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlaylistType {
    ONLINE, OFFLINE
}

/**
 * Screen for manually creating a new playlist.
 * Supports creating online-only or offline-only playlists with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // Playlist creation state
    var playlistName by remember { mutableStateOf("") }
    var playlistType by remember { mutableStateOf<PlaylistType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    // Selected songs lists
    val selectedOfflineSongs = remember { mutableStateListOf<Song>() }
    val selectedOnlineSongs = remember { mutableStateListOf<OnlineSong>() }
    
    // Search with debounce
    LaunchedEffect(searchQuery) {
        showSearchResults = searchQuery.isNotEmpty()
        searchJob?.cancel()
        
        if (searchQuery.isNotEmpty() && playlistType == PlaylistType.ONLINE) {
            searchJob = coroutineScope.launch {
                delay(500)
                homeViewModel.searchOnline(searchQuery)
            }
        }
    }
    
    // Filter search results based on playlist type
    val searchResults = remember(searchQuery, homeState.allSongs, homeState.onlineSearchResults, playlistType) {
        if (searchQuery.isEmpty()) {
            emptyList()
        } else when (playlistType) {
            PlaylistType.OFFLINE -> {
                homeState.allSongs.filter { song ->
                    song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true) ||
                    song.album?.contains(searchQuery, ignoreCase = true) == true
                }
            }
            PlaylistType.ONLINE -> {
                homeState.onlineSearchResults
            }
            null -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val isEnabled = playlistName.isNotBlank() && 
                            (selectedOfflineSongs.isNotEmpty() || selectedOnlineSongs.isNotEmpty())
            ExtendedFloatingActionButton(
                text = { Text("Create") },
                icon = { Icon(Icons.Filled.Save, contentDescription = "Create Playlist") },
                onClick = {
                    if (isEnabled) {
                        // TODO: Implement save to database
                        // playlistViewModel.createPlaylist(playlistName, selectedSongs, playlistType)
                        onSaveSuccess()
                    }
                },
                containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Playlist Name
                item {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("My Awesome Playlist") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
                
                // Playlist Type Selection
                item {
                    Column {
                        Text(
                            text = "Playlist Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = playlistType == PlaylistType.OFFLINE,
                                onClick = { 
                                    playlistType = PlaylistType.OFFLINE
                                    selectedOnlineSongs.clear()
                                    searchQuery = ""
                                },
                                label = { Text("Offline Songs") },
                                leadingIcon = if (playlistType == PlaylistType.OFFLINE) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = playlistType == PlaylistType.ONLINE,
                                onClick = { 
                                    playlistType = PlaylistType.ONLINE
                                    selectedOfflineSongs.clear()
                                    searchQuery = ""
                                },
                                label = { Text("Online Songs") },
                                leadingIcon = if (playlistType == PlaylistType.ONLINE) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (playlistType == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select playlist type to continue",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Search Section (only show if type is selected)
                if (playlistType != null) {
                    item {
                        Column {
                            Text(
                                text = "Add Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    focusManager.clearFocus()
                                },
                                label = { Text(if (playlistType == PlaylistType.OFFLINE) "Search offline songs" else "Search online") },
                                placeholder = { Text("Type to search...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                            )
                        }
                    }
                    
                    // Search Results
                    if (showSearchResults && searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Search Results",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        items(searchResults.take(10)) { result ->
                            when (playlistType) {
                                PlaylistType.OFFLINE -> {
                                    val song = result as Song
                                    val isSelected = selectedOfflineSongs.any { it.id == song.id }
                                    OfflineSongItem(
                                        song = song,
                                        isSelected = isSelected,
                                        onToggle = {
                                            if (isSelected) {
                                                selectedOfflineSongs.removeAll { it.id == song.id }
                                            } else {
                                                selectedOfflineSongs.add(song)
                                                searchQuery = ""
                                            }
                                        }
                                    )
                                }
                                PlaylistType.ONLINE -> {
                                    val searchResult = result as OnlineSearchResult
                                    OnlineSearchItem(
                                        result = searchResult,
                                        onSelect = {
                                            // Convert to OnlineSong
                                            val onlineSong = OnlineSong(
                                                videoId = searchResult.id,
                                                title = searchResult.title,
                                                artist = searchResult.author ?: "Unknown Artist",
                                                album = null,
                                                thumbnailUrl = searchResult.thumbnailUrl,
                                                duration = searchResult.duration?.toInt(),
                                                sourcePlatform = "YouTube"
                                            )
                                            selectedOnlineSongs.add(onlineSong)
                                            searchQuery = ""
                                        }
                                    )
                                }
                                null -> {}
                            }
                        }
                    }
                    
                    // Selected Songs Section
                    if (selectedOfflineSongs.isNotEmpty() || selectedOnlineSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Selected Songs (${selectedOfflineSongs.size + selectedOnlineSongs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(selectedOfflineSongs) { song ->
                            SelectedSongItem(
                                title = song.title,
                                artist = song.artist,
                                onRemove = { selectedOfflineSongs.remove(song) }
                            )
                        }
                        
                        items(selectedOnlineSongs) { song ->
                            SelectedSongItem(
                                title = song.title,
                                artist = song.artist,
                                onRemove = { selectedOnlineSongs.remove(song) }
                            )
                        }
                    }
                }
                
                // Empty state
                if (playlistType != null && selectedOfflineSongs.isEmpty() && selectedOnlineSongs.isEmpty() && !showSearchResults) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search and add songs to your playlist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun OfflineSongItem(
    song: Song,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OnlineSearchItem(
    result: OnlineSearchResult,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.author ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SelectedSongItem(
    title: String,
    artist: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}