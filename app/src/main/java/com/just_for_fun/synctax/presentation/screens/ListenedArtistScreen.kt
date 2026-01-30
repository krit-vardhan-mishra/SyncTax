package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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

    // Handle refresh completion
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            homeViewModel.forceRefreshLibrary()
            isRefreshing = false
        }
    }

    // Processing data
    val offlineArtists = remember(uiState.artists) {
        uiState.artists.map { ArtistUiModel(it.name, it.songCount, isOnline = false, imageUrl = it.imageUrl) }
    }

    val onlineArtists = remember(uiState.onlineHistory) {
         uiState.onlineHistory
             .groupBy { it.artist }
             .map { (name, history) ->
                 ArtistUiModel(
                     name = name, 
                     songCount = history.size, 
                     isOnline = true,
                     imageUrl = history.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }?.thumbnailUrl
                 )
             }
             .sortedByDescending { it.songCount }
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
                    ArtistListItem(artist) { onArtistClick(artist.name) }
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
fun ArtistListItem(artist: ArtistUiModel, onClick: () -> Unit) {
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
                 if (!artist.imageUrl.isNullOrEmpty()) {
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
