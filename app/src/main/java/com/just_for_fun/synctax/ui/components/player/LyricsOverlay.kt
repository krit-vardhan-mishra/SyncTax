package com.just_for_fun.synctax.ui.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.model.LyricLine
import com.just_for_fun.synctax.core.network.LrcLibResponse
import kotlin.math.max
import io.github.fletchmckee.liquid.rememberLiquidState
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .liquid(liquidState)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change: PointerInputChange, dragAmount: Float ->
                    if (dragAmount > 100f) { // Swipe down threshold
                        onDismiss()
                    }
                }
            },
        color = Color.Black.copy(alpha = 0.7f).compositeOver(songDominantColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Back Button (Material 3 style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Return to Player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isFetchingLyrics) {
                            // Show loading indicator
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Fetching lyrics...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else if (hasSearchResults && searchResults.isNotEmpty()) {
                            // Show search results for user selection
                            Text(
                                text = "Select lyrics to use:",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(searchResults) { result ->
                                    val index = searchResults.indexOf(result)
                                    Button(
                                        onClick = { onSelectLyrics(index) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = songDominantColor.copy(alpha = 0.8f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                text = result.trackName ?: "Unknown Title",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = result.artistName ?: "Unknown Artist",
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!result.albumName.isNullOrBlank()) {
                                                Text(
                                                    text = result.albumName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Not the right lyrics? Try searching again",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Show fetch button or failure message
                            Text(
                                text = if (hasFailedFetch) "Lyrics not available for this song" else (lyricsError
                                    ?: "No Lyrics Found"),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (!hasFailedFetch) {
                                Button(
                                    onClick = { showCustomSearchDialog.value = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = songDominantColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetch Lyrics from LRCLIB")
                                }

                                if (lyricsError != null) {
                                    Text(
                                        text = "Tap to try again",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()

                // Auto-scroll to current lyric. Scroll to keep the current line near the 3rd line of the visible area.
                LaunchedEffect(currentLyricIndex) {
                    if (currentLyricIndex >= 0) {
                        listState.animateScrollToItem(
                            index = max(0, currentLyricIndex - 2),
                        )
                    }
                }

                // 2. Reduced Viewport: Use Spacers to push the lyrics list into a smaller vertical window.
                Spacer(modifier = Modifier.weight(0.15f)) // Pushes content down

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f) // Occupies only 70% of the remaining vertical space
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // Use padding to ensure the top and bottom lines of the list are visible
                    contentPadding = PaddingValues(vertical = 40.dp)
                ) {
                    items(lyrics) { lyric ->
                        val index = lyrics.indexOf(lyric)
                        val isCurrentLine = index == currentLyricIndex

                        // Animate opacity and scale for highlighting
                        val targetAlpha = if (isCurrentLine) 1.0f else 0.4f
                        val targetScale = if (isCurrentLine) 1.0f else 0.95f

                        val alpha by animateFloatAsState(targetValue = targetAlpha, label = "alpha")
                        val scale by animateFloatAsState(targetValue = targetScale, label = "scale")

                        Text(
                            text = lyric.text,
                            style = if (isCurrentLine)
                                MaterialTheme.typography.headlineMedium
                            else
                                MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = alpha),
                            textAlign = TextAlign.Center,
                            fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .padding(vertical = 12.dp) // Generous padding for mobile tap/read
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.15f)) // Pushes content up
            }
        }
    }

    // Custom Search Input Dialog
    if (showCustomSearchDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomSearchDialog.value = false },
            containerColor = Color.Black.copy(alpha = 0.9f),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Search Lyrics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Enter the song name and artist for more accurate results:",
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
                            focusedBorderColor = songDominantColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = songDominantColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customArtistName.value,
                        onValueChange = { customArtistName.value = it },
                        label = { Text("Artist Name", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = songDominantColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = songDominantColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customSongName.value.isNotBlank() && customArtistName.value.isNotBlank()) {
                            onFetchLyricsWithCustomQuery(
                                customSongName.value.trim(),
                                customArtistName.value.trim()
                            )
                            showCustomSearchDialog.value = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = songDominantColor,
                        contentColor = Color.White
                    ),
                    enabled = customSongName.value.isNotBlank() && customArtistName.value.isNotBlank()
                ) {
                    Text("Search")
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
            }
        )
    }
}