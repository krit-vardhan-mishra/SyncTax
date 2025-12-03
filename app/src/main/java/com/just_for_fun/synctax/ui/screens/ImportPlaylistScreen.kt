package com.just_for_fun.synctax.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.viewmodels.PlaylistViewModel

/**
 * Screen for importing playlists from YouTube/YouTube Music
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    onBackClick: () -> Unit,
    onImportSuccess: () -> Unit
) {
    val importState by playlistViewModel.importState.collectAsState()
    var playlistUrl by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    // Handle import success
    LaunchedEffect(importState.importSuccess) {
        if (importState.importSuccess == true) {
            // Delay briefly to show success message, then navigate back
            kotlinx.coroutines.delay(1500)
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
            // Header icon
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Import from YouTube",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Paste a YouTube or YouTube Music playlist link to import all songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
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
                placeholder = { Text("https://youtube.com/playlist?list=...") },
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
                                strokeWidth = 2.dp
                            )
                        }
                        importState.isUrlValid == true -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid URL",
                                tint = Color(0xFF4CAF50)
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
                                color = Color(0xFF4CAF50)
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
            
            // Import button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    playlistViewModel.importPlaylist(playlistUrl)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = importState.isUrlValid == true && !importState.isImporting
            ) {
                if (importState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
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
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
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
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Import Successful!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            tint = MaterialTheme.colorScheme.primary
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
                        icon = Icons.Default.PlayCircle,
                        text = "YouTube playlists (youtube.com)"
                    )
                    HelpItem(
                        icon = Icons.Default.MusicNote,
                        text = "YouTube Music playlists (music.youtube.com)"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Note: Only public playlists can be imported. Private playlists require login which is not supported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
