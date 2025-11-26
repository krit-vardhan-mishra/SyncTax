package com.just_for_fun.synctax.ui.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.model.LyricLine
import com.just_for_fun.synctax.core.network.LrcLibResponse
import kotlin.math.max
import io.github.fletchmckee.liquid.liquid

@Composable
fun LyricsOverlay(
    song: Song,
    lyrics: List<LyricLine>?,
    currentLyricIndex: Int,
    songDominantColor: Color,
    onDismiss: () -> Unit,
    onFetchLyrics: () -> Unit = {},
    onFetchLyricsWithCustomQuery: (String, String) -> Unit = { _, _ -> },
    isFetchingLyrics: Boolean = false,
    lyricsError: String? = null,
    hasFailedFetch: Boolean = false,
    searchResults: List<LrcLibResponse> = emptyList(),
    onSelectLyrics: (Int) -> Unit = {},
    hasSearchResults: Boolean = false,
    liquidState: io.github.fletchmckee.liquid.LiquidState
) {

    BackHandler {
        onDismiss()
    }

    // State for custom search input dialog
    val showCustomSearchDialog = remember { mutableStateOf(false) }
    val customSongName = remember { mutableStateOf(song.title) }
    val customArtistName = remember { mutableStateOf(song.artist) }

    // The overlay now uses the same background as FullScreenPlayer
    // which is transparent to show the blurred album art behind it
    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquid(liquidState)
            .background(Color.Transparent) // Let the blurred background show through
            .pointerInput(Unit) {
                detectVerticalDragGestures { change: PointerInputChange, dragAmount: Float ->
                    if (dragAmount > 100f) {
                        onDismiss()
                    }
                }
            }
    ) {
        // Additional dark scrim for better text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 8.dp, end = 24.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glass-morphic back button
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(25.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Return to Player",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Main Lyrics Content Area
            if (lyrics.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        if (isFetchingLyrics) {
                            // Modern loading state
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Fetching lyrics...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        } else if (hasSearchResults && searchResults.isNotEmpty()) {
                            // Search results section
                            Text(
                                text = "Select Lyrics",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(searchResults) { result ->
                                    val index = searchResults.indexOf(result)
                                    Surface(
                                        onClick = { onSelectLyrics(index) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp)
                                        ) {
                                            Text(
                                                text = result.trackName ?: "Unknown Title",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = result.artistName ?: "Unknown Artist",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!result.albumName.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = result.albumName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Not finding the right lyrics?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Text(
                                text = if (hasFailedFetch) "Lyrics Unavailable" else "No Lyrics Found",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            if (!hasFailedFetch) {
                                Text(
                                    text = "Search for synced lyrics from LRCLIB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { showCustomSearchDialog.value = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Fetch Lyrics",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (lyricsError != null) {
                                    Text(
                                        text = lyricsError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF6B6B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()

                // Auto-scroll to current lyric
                LaunchedEffect(currentLyricIndex) {
                    if (currentLyricIndex >= 0) {
                        listState.animateScrollToItem(
                            index = max(0, currentLyricIndex - 2),
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Top fade gradient for smooth edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .align(Alignment.TopCenter)
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 120.dp)
                    ) {
                        items(lyrics) { lyric ->
                            val index = lyrics.indexOf(lyric)
                            val isCurrentLine = index == currentLyricIndex

                            // Smooth animations
                            val targetAlpha = if (isCurrentLine) 1.0f else 0.4f
                            val targetScale = if (isCurrentLine) 1.0f else 0.94f

                            val alpha by animateFloatAsState(
                                targetValue = targetAlpha,
                                animationSpec = tween(300),
                                label = "alpha"
                            )
                            val scale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = tween(300),
                                label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    text = lyric.text,
                                    style = if (isCurrentLine)
                                        MaterialTheme.typography.headlineMedium
                                    else
                                        MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(scale)
                                        .alpha(alpha)
                                )
                            }
                        }
                    }

                    // Bottom fade gradient for smooth edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // Modern Search Dialog
    if (showCustomSearchDialog.value) {
        AlertDialog(
            onDismissRequest = { showCustomSearchDialog.value = false },
            containerColor = Color.Black.copy(alpha = 0.92f),
            title = {
                Text(
                    text = "Search Lyrics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Enter song details for accurate results",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = customSongName.value,
                        onValueChange = { customSongName.value = it },
                        label = { Text("Song Name", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customArtistName.value,
                        onValueChange = { customArtistName.value = it },
                        label = { Text("Artist Name (optional)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("Leave empty to search by title only", color = Color.White.copy(alpha = 0.45f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customSongName.value.isNotBlank()) {
                            onFetchLyricsWithCustomQuery(
                                customSongName.value.trim(),
                                customArtistName.value.trim()
                            )
                            showCustomSearchDialog.value = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = customSongName.value.isNotBlank()
                ) {
                    Text("Search", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCustomSearchDialog.value = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}