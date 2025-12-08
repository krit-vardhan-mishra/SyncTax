package com.just_for_fun.synctax.presentation.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.network.LrcLibResponse
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.model.LyricLine
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import kotlin.math.max

/**
 * Enhanced Lyrics Overlay with proper Liquid Glass effect
 *
 * Key improvements:
 * 1. Proper glass morphism with blur and transparency
 * 2. Layered glass effect for UI elements
 * 3. Better visual hierarchy with frosted glass containers
 * 4. Smooth animations and transitions
 *
 * To achieve the full SimpMusic-style liquid glass effect, consider adding:
 * - AndroidLiquidGlass library (io.github.kyant0:backdrop) for advanced shader effects
 * - Real-time backdrop blur with proper refractive properties
 */
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
    liquidState: LiquidState
) {
    BackHandler {
        onDismiss()
    }

    val showCustomSearchDialog = remember { mutableStateOf(false) }
    val customSongName = remember { mutableStateOf(song.title) }
    val customArtistName = remember { mutableStateOf(song.artist) }

    // Enhanced Glass Background with multiple layers for depth
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _: PointerInputChange, dragAmount: Float ->
                    if (dragAmount > 100f) {
                        onDismiss()
                    }
                }
            }
    ) {
        // Dark scrim with gradient for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Glass Morphic Top Bar with blur effect
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquid(liquidState),
                color = Color.White.copy(alpha = 0.08f),
                tonalElevation = 0.dp
            ) {
                // Additional blur layer for glass effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(top = 48.dp, start = 8.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frosted glass back button
                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(25.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(48.dp)
                                .liquid(liquidState),
                            tonalElevation = 8.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
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

                        // Glass container for song info
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .liquid(liquidState),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                }
            }

            // Main Content Area with glass morphism
            if (lyrics.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Glass container for empty state
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .liquid(liquidState),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        tonalElevation = 16.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp)
                        ) {
                            if (isFetchingLyrics) {
                                // Glass loading indicator
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .liquid(liquidState),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                                Text(
                                    text = "Fetching lyrics...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (hasSearchResults && searchResults.isNotEmpty()) {
                                SearchResultsSection(
                                    searchResults = searchResults,
                                    onSelectLyrics = onSelectLyrics,
                                    liquidState = liquidState
                                )
                            } else {
                                EmptyLyricsState(
                                    hasFailedFetch = hasFailedFetch,
                                    lyricsError = lyricsError,
                                    onFetchClick = { showCustomSearchDialog.value = true },
                                    liquidState = liquidState
                                )
                            }
                        }
                    }
                }
            } else {
                LyricsContentSection(
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    liquidState = liquidState
                )
            }
        }
    }

    // Glass Morphic Search Dialog
    if (showCustomSearchDialog.value) {
        GlassSearchDialog(
            songName = customSongName.value,
            artistName = customArtistName.value,
            onSongNameChange = { customSongName.value = it },
            onArtistNameChange = { customArtistName.value = it },
            onSearch = {
                if (customSongName.value.isNotBlank()) {
                    onFetchLyricsWithCustomQuery(
                        customSongName.value.trim(),
                        customArtistName.value.trim()
                    )
                    showCustomSearchDialog.value = false
                }
            },
            onDismiss = { showCustomSearchDialog.value = false },
            liquidState = liquidState
        )
    }
}

@Composable
private fun SearchResultsSection(
    searchResults: List<LrcLibResponse>,
    onSelectLyrics: (Int) -> Unit,
    liquidState: LiquidState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(searchResults) { result ->
                val index = searchResults.indexOf(result)
                Surface(
                    onClick = { onSelectLyrics(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquid(liquidState),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = result.trackName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.artistName,
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
        }

        Text(
            text = "Not finding the right lyrics?",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyLyricsState(
    hasFailedFetch: Boolean,
    lyricsError: String?,
    onFetchClick: () -> Unit,
    liquidState: LiquidState
) {
    // Glass icon container
    Box(
        modifier = Modifier
            .size(80.dp)
            .liquid(liquidState)
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

        // Glass button with gradient overlay
        Surface(
            onClick = onFetchClick,
            modifier = Modifier
                .height(56.dp)
                .liquid(liquidState),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.22f),
            tonalElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Fetch Lyrics",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
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

@Composable
private fun LyricsContentSection(
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    liquidState: LiquidState
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0) {
            listState.animateScrollToItem(
                index = max(0, currentLyricIndex - 2)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        // Top gradient fade with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 140.dp)
        ) {
            items(lyrics) { lyric ->
                val index = lyrics.indexOf(lyric)
                val isCurrentLine = index == currentLyricIndex

                val targetAlpha = if (isCurrentLine) 1.0f else 0.45f
                val targetScale = if (isCurrentLine) 1.0f else 0.95f

                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(350),
                    label = "alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(350),
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

        // Bottom gradient fade with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GlassSearchDialog(
    songName: String,
    artistName: String,
    onSongNameChange: (String) -> Unit,
    onArtistNameChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    liquidState: LiquidState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.liquid(liquidState),
        title = {
            Text(
                text = "Search Lyrics",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "Enter song details for accurate results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Glass text fields
                OutlinedTextField(
                    value = songName,
                    onValueChange = onSongNameChange,
                    label = { Text("Song Name", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artistName,
                    onValueChange = onArtistNameChange,
                    label = { Text("Artist Name (optional)", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("Leave empty to search by title only", color = Color.White.copy(alpha = 0.45f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = songName.isNotBlank()
            ) {
                Text("Search", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
