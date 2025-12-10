package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.presentation.viewmodels.PlaylistViewModel
import kotlinx.coroutines.delay

// 1. Define custom colors for the platforms
private val YtMusicRed = Color(0xFFFF0000)
private val SpotifyGreen = Color(0xFF1DB954)
private val SuccessGreen = Color(0xFF4CAF50)

/**
 * A data class to hold platform-specific colors.
 */
private data class PlatformColors(
    val primary: Color,
    val onPrimary: Color,
    val surfaceVariant: Color
)

@Composable
private fun getPlatformColors(selectedPlatform: String): PlatformColors {
    return when (selectedPlatform) {
        "youtube" -> PlatformColors(
            primary = YtMusicRed,
            onPrimary = Color.White,
            surfaceVariant = YtMusicRed.copy(alpha = 0.2f)
        )

        "spotify" -> PlatformColors(
            primary = SpotifyGreen,
            onPrimary = Color.Black, // Spotify typically uses black text on green
            surfaceVariant = SpotifyGreen.copy(alpha = 0.2f)
        )

        else -> PlatformColors( // Fallback to MaterialTheme colors
            primary = MaterialTheme.colorScheme.primary,
            onPrimary = MaterialTheme.colorScheme.onPrimary,
            surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Screen for importing playlists from YouTube/YouTube Music
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    onBackClick: () -> Unit,
    onImportSuccess: () -> Unit,
    initialPlatform: String = "youtube"
) {
    val importState by playlistViewModel.importState.collectAsState()
    var playlistUrl by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(initialPlatform) }
    val focusManager = LocalFocusManager.current

    // Get the dynamic colors based on the selected platform
    val platformColors = getPlatformColors(selectedPlatform)

    // Handle import success
    LaunchedEffect(importState.importSuccess) {
        if (importState.importSuccess == true) {
            // Delay briefly to show success message, then navigate back
            delay(1500)
            playlistViewModel.resetImportState()
            onImportSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Playlist") },
                navigationIcon = {
                    IconButton(onClick = {
                        playlistViewModel.resetImportState()
                        onBackClick()
                    }) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header icon (tinted by platform color)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = platformColors.primary // Use dynamic primary color
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedPlatform == "youtube") "Import from YouTube" else "Import from Spotify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (selectedPlatform == "youtube") "Paste a YouTube or YouTube Music playlist link to import all songs" else "Paste a Spotify playlist link to import all songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Platform selection buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { selectedPlatform = "youtube" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlatform == "youtube") platformColors.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedPlatform == "youtube") platformColors.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("YouTube")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { selectedPlatform = "spotify" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlatform == "spotify") platformColors.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedPlatform == "spotify") platformColors.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Spotify")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // URL input field
            OutlinedTextField(
                value = playlistUrl,
                onValueChange = {
                    playlistUrl = it
                    if (it.length > 10) {
                        playlistViewModel.validateUrl(it)
                    }
                },
                label = { Text("Playlist URL") },
                placeholder = { Text(if (selectedPlatform == "youtube") "https://youtube.com/playlist?list=..." else "https://open.spotify.com/playlist/...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    when {
                        importState.isValidating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = platformColors.primary // Tint loading with platform color
                            )
                        }

                        importState.isUrlValid == true -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid URL",
                                tint = SuccessGreen
                            )
                        }

                        importState.isUrlValid == false && playlistUrl.isNotEmpty() -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Invalid URL",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        playlistUrl.isNotEmpty() -> {
                            IconButton(onClick = { playlistUrl = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                },
                isError = importState.isUrlValid == false && playlistUrl.isNotEmpty(),
                supportingText = {
                    when {
                        importState.isUrlValid == true && importState.platform != null -> {
                            Text(
                                text = "✓ Valid ${importState.platform} playlist",
                                color = SuccessGreen
                            )
                        }

                        importState.isUrlValid == false && playlistUrl.isNotEmpty() -> {
                            Text(
                                text = "Invalid playlist URL",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (importState.isUrlValid == true && !importState.isImporting) {
                            playlistViewModel.importPlaylist(playlistUrl)
                        }
                    }
                ),
                enabled = !importState.isImporting
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Import button (tinted by platform color)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    playlistViewModel.importPlaylist(playlistUrl)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = importState.isUrlValid == true && !importState.isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = platformColors.primary,
                    contentColor = platformColors.onPrimary
                )
            ) {
                if (importState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = platformColors.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Importing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Playlist")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Success message
            AnimatedVisibility(
                visible = importState.importSuccess == true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SuccessGreen.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Import Successful!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            importState.importedPlaylist?.let { playlist ->
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${importState.importedSongCount} songs imported",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            AnimatedVisibility(
                visible = importState.importSuccess == false && importState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = importState.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Help section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = platformColors.surfaceVariant // Tint help section background with platform color
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = platformColors.primary // Tint info icon with platform color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Supported Links",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HelpItem(
                        icon = if (selectedPlatform == "youtube") Icons.Default.PlayCircle else Icons.Default.MusicNote,
                        text = if (selectedPlatform == "youtube") "YouTube playlists (youtube.com)" else "Spotify playlists (open.spotify.com)"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Note: Only public playlists can be imported. Private playlists require login which is not supported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}