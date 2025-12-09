package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.presentation.viewmodels.RecommendationViewModel

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

    // State for loading
    val isLoading by recommendationViewModel.isLoading.collectAsState()

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
                placeholder = "Enter artist name...",
                onAdd = {
                    if (currentArtist.isNotBlank() && !artists.contains(currentArtist.trim())) {
                        artists.add(currentArtist.trim())
                        currentArtist = ""
                    }
                },
                onRemove = { artists.remove(it) }
            )

            // Songs Section
            InputSection(
                title = "Favorite Songs",
                subtitle = "Add specific songs you love",
                currentValue = currentSong,
                onValueChange = { currentSong = it },
                items = songs,
                placeholder = "Enter song name...",
                onAdd = {
                    if (currentSong.isNotBlank() && !songs.contains(currentSong.trim())) {
                        songs.add(currentSong.trim())
                        currentSong = ""
                    }
                },
                onRemove = { songs.remove(it) }
            )

            // Albums Section
            InputSection(
                title = "Favorite Albums",
                subtitle = "Add albums you enjoy",
                currentValue = currentAlbum,
                onValueChange = { currentAlbum = it },
                items = albums,
                placeholder = "Enter album name...",
                onAdd = {
                    if (currentAlbum.isNotBlank() && !albums.contains(currentAlbum.trim())) {
                        albums.add(currentAlbum.trim())
                        currentAlbum = ""
                    }
                },
                onRemove = { albums.remove(it) }
            )

            // Genres Section
            InputSection(
                title = "Preferred Genres",
                subtitle = "Add music genres you like",
                currentValue = currentGenre,
                onValueChange = { currentGenre = it },
                items = genres,
                placeholder = "Enter genre...",
                onAdd = {
                    if (currentGenre.isNotBlank() && !genres.contains(currentGenre.trim())) {
                        genres.add(currentGenre.trim())
                        currentGenre = ""
                    }
                },
                onRemove = { genres.remove(it) }
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
 * Reusable composable for input sections with add/remove functionality
 */
@Composable
private fun InputSection(
    title: String,
    subtitle: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    items: List<String>,
    placeholder: String,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        // Display added items
        if (items.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "• $item",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemove(item) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}