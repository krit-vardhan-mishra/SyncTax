package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlin.math.max

@Composable
fun LyricsOverlay(
    song: Song,
    lyrics: List<LyricLine>?,
    currentLyricIndex: Int,
    songDominantColor: Color,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        // Blending the dark base with the faded song color for the background
        color = Color.Black.copy(alpha = 0.9f).compositeOver(songDominantColor.copy(alpha = 0.1f))
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
                    Text(
                        text = "No Lyrics Found",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
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
}
