package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Data class for user recommendation inputs
 */
data class UserRecommendationInputs(
    val artists: List<String> = emptyList(),
    val songs: List<String> = emptyList(),
    val albums: List<String> = emptyList(),
    val genres: List<String> = emptyList()
)

/**
 * Enum to define the type of input section for filtering search results
 */
enum class InputSectionType {
    ARTIST, SONG, ALBUM, GENRE
}

/**
 * Screen for users to input their preferences for personalized online recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRecommendationInputScreen(
    recommendationViewModel: RecommendationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onGenerateRecommendations: (UserRecommendationInputs) -> Unit
) {
    // State for input fields
    var currentArtist by remember { mutableStateOf("") }
    var currentSong by remember { mutableStateOf("") }
    var currentAlbum by remember { mutableStateOf("") }
    var currentGenre by remember { mutableStateOf("") }

    // State for lists of inputs
    val artists = remember { mutableStateListOf<String>() }
    val songs = remember { mutableStateListOf<String>() }
    val albums = remember { mutableStateListOf<String>() }
    val genres = remember { mutableStateListOf<String>() }

    // State for search results
    val artistSearchResults = remember { mutableStateOf<List<OnlineSearchResult>>(emptyList()) }
    val songSearchResults = remember { mutableStateOf<List<OnlineSearchResult>>(emptyList()) }
    val albumSearchResults = remember { mutableStateOf<List<OnlineSearchResult>>(emptyList()) }
    val genreSearchResults = remember { mutableStateOf<List<String>>(emptyList()) }

    // State for loading indicators per section
    var isArtistSearching by remember { mutableStateOf(false) }
    var isSongSearching by remember { mutableStateOf(false) }
    var isAlbumSearching by remember { mutableStateOf(false) }
    var isGenreSearching by remember { mutableStateOf(false) }

    // State for loading
    val isLoading by recommendationViewModel.isLoading.collectAsState()

    // Debounced search for artists
    LaunchedEffect(Unit) {
        snapshotFlow { currentArtist }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .collect { query ->
                isArtistSearching = true
                recommendationViewModel.searchArtists(query) { results ->
                    artistSearchResults.value = results
                    isArtistSearching = false
                }
            }
    }

    // Clear artist results when input is cleared
    LaunchedEffect(currentArtist) {
        if (currentArtist.length < 2) {
            artistSearchResults.value = emptyList()
            isArtistSearching = false
        }
    }

    // Debounced search for songs
    LaunchedEffect(Unit) {
        snapshotFlow { currentSong }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .collect { query ->
                isSongSearching = true
                recommendationViewModel.searchSongs(query) { results ->
                    songSearchResults.value = results
                    isSongSearching = false
                }
            }
    }

    // Clear song results when input is cleared
    LaunchedEffect(currentSong) {
        if (currentSong.length < 2) {
            songSearchResults.value = emptyList()
            isSongSearching = false
        }
    }

    // Debounced search for albums
    LaunchedEffect(Unit) {
        snapshotFlow { currentAlbum }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .collect { query ->
                isAlbumSearching = true
                recommendationViewModel.searchAlbums(query) { results ->
                    albumSearchResults.value = results
                    isAlbumSearching = false
                }
            }
    }

    // Clear album results when input is cleared
    LaunchedEffect(currentAlbum) {
        if (currentAlbum.length < 2) {
            albumSearchResults.value = emptyList()
            isAlbumSearching = false
        }
    }

    // For genres, use predefined list or simple filtering
    LaunchedEffect(Unit) {
        snapshotFlow { currentGenre }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 1 }
            .collect { query ->
                isGenreSearching = true
                val commonGenres = listOf(
                    "Rock", "Pop", "Hip Hop", "Jazz", "Classical", "Electronic",
                    "Country", "R&B", "Blues", "Reggae", "Folk", "Indie",
                    "Metal", "Punk", "Alternative", "Dance", "Soul", "Funk",
                    "Disco", "House", "Techno", "Ambient", "Acoustic", "Latin",
                    "Gospel", "World", "New Age", "Ska", "Grunge", "Trap"
                )
                genreSearchResults.value = commonGenres.filter {
                    it.contains(query, ignoreCase = true)
                }
                isGenreSearching = false
            }
    }

    // Clear genre results when input is cleared
    LaunchedEffect(currentGenre) {
        if (currentGenre.isEmpty()) {
            genreSearchResults.value = emptyList()
            isGenreSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personalized Recommendations",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header text
            Text(
                "Tell us what you like and we'll create personalized recommendations just for you!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Artists Section
            InputSection(
                title = "Favorite Artists",
                subtitle = "Add artists you enjoy listening to",
                currentValue = currentArtist,
                onValueChange = { currentArtist = it },
                items = artists,
                placeholder = "Search for artists...",
                sectionType = InputSectionType.ARTIST,
                isSearching = isArtistSearching,
                onAdd = {
                    if (currentArtist.isNotBlank() && !artists.contains(currentArtist.trim())) {
                        artists.add(currentArtist.trim())
                        currentArtist = ""
                        artistSearchResults.value = emptyList()
                    }
                },
                onRemove = { artists.remove(it) },
                searchResults = artistSearchResults.value,
                onResultSelect = { result ->
                    if (result is OnlineSearchResult) {
                        val artistName = result.title
                        if (artistName.isNotBlank() && !artists.contains(artistName)) {
                            artists.add(artistName)
                        }
                        currentArtist = ""
                        artistSearchResults.value = emptyList()
                    }
                }
            )

            // Songs Section
            InputSection(
                title = "Favorite Songs",
                subtitle = "Add specific songs you love",
                currentValue = currentSong,
                onValueChange = { currentSong = it },
                items = songs,
                placeholder = "Search for songs...",
                sectionType = InputSectionType.SONG,
                isSearching = isSongSearching,
                onAdd = {
                    if (currentSong.isNotBlank() && !songs.contains(currentSong.trim())) {
                        songs.add(currentSong.trim())
                        currentSong = ""
                        songSearchResults.value = emptyList()
                    }
                },
                onRemove = { songs.remove(it) },
                searchResults = songSearchResults.value,
                onResultSelect = { result ->
                    if (result is OnlineSearchResult) {
                        val songName = "${result.title} - ${result.author ?: "Unknown"}"
                        if (!songs.contains(songName)) {
                            songs.add(songName)
                        }
                        currentSong = ""
                        songSearchResults.value = emptyList()
                    }
                }
            )

            // Albums Section
            InputSection(
                title = "Favorite Albums",
                subtitle = "Add albums you enjoy",
                currentValue = currentAlbum,
                onValueChange = { currentAlbum = it },
                items = albums,
                placeholder = "Search for albums...",
                sectionType = InputSectionType.ALBUM,
                isSearching = isAlbumSearching,
                onAdd = {
                    if (currentAlbum.isNotBlank() && !albums.contains(currentAlbum.trim())) {
                        albums.add(currentAlbum.trim())
                        currentAlbum = ""
                        albumSearchResults.value = emptyList()
                    }
                },
                onRemove = { albums.remove(it) },
                searchResults = albumSearchResults.value,
                onResultSelect = { result ->
                    if (result is OnlineSearchResult) {
                        val albumName = "${result.title} - ${result.author ?: "Unknown"}"
                        if (!albums.contains(albumName)) {
                            albums.add(albumName)
                        }
                        currentAlbum = ""
                        albumSearchResults.value = emptyList()
                    }
                }
            )

            // Genres Section
            InputSection(
                title = "Preferred Genres",
                subtitle = "Add music genres you like",
                currentValue = currentGenre,
                onValueChange = { currentGenre = it },
                items = genres,
                placeholder = "Search for genres...",
                sectionType = InputSectionType.GENRE,
                isSearching = isGenreSearching,
                onAdd = {
                    if (currentGenre.isNotBlank() && !genres.contains(currentGenre.trim())) {
                        genres.add(currentGenre.trim())
                        currentGenre = ""
                        genreSearchResults.value = emptyList()
                    }
                },
                onRemove = { genres.remove(it) },
                searchResults = genreSearchResults.value,
                onResultSelect = { result ->
                    if (result is String) {
                        if (!genres.contains(result)) {
                            genres.add(result)
                        }
                        currentGenre = ""
                        genreSearchResults.value = emptyList()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button
            Button(
                onClick = {
                    val inputs = UserRecommendationInputs(
                        artists = artists.toList(),
                        songs = songs.toList(),
                        albums = albums.toList(),
                        genres = genres.toList()
                    )
                    onGenerateRecommendations(inputs)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (artists.isNotEmpty() || songs.isNotEmpty() ||
                        albums.isNotEmpty() || genres.isNotEmpty()) && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    Text("Generating Recommendations...")
                } else {
                    Text("Generate Personalized Recommendations")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Reusable composable for input sections with add/remove functionality and search suggestions
 */
@Composable
private fun InputSection(
    title: String,
    subtitle: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    items: List<String>,
    placeholder: String,
    sectionType: InputSectionType,
    isSearching: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    searchResults: List<Any> = emptyList(),
    onResultSelect: ((Any) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Input field with add button
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                IconButton(
                    onClick = onAdd,
                    enabled = currentValue.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = if (currentValue.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Loading indicator
            if (isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search results dropdown - directly below input
            if (searchResults.isNotEmpty() && currentValue.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 250.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn {
                        items(searchResults.take(6)) { result ->
                            SearchResultItem(
                                result = result,
                                sectionType = sectionType,
                                onClick = { onResultSelect?.invoke(result) }
                            )
                        }
                    }
                }
            }
        }

        // Display added items
        if (items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = getIconForSectionType(sectionType),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { onRemove(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for displaying individual search result items
 */
@Composable
private fun SearchResultItem(
    result: Any,
    sectionType: InputSectionType,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (result) {
            is OnlineSearchResult -> {
                // Show thumbnail/image
                if (!result.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                if (sectionType == InputSectionType.ARTIST) CircleShape
                                else RoundedCornerShape(4.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                if (sectionType == InputSectionType.ARTIST) CircleShape
                                else RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForSectionType(sectionType),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (result.author != null) {
                        Text(
                            text = result.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            is String -> {
                // Genre item
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Get appropriate icon for the section type
 */
private fun getIconForSectionType(sectionType: InputSectionType): ImageVector {
    return when (sectionType) {
        InputSectionType.ARTIST -> Icons.Default.Person
        InputSectionType.SONG -> Icons.Default.MusicNote
        InputSectionType.ALBUM -> Icons.Default.Album
        InputSectionType.GENRE -> Icons.Default.MusicNote
    }
}