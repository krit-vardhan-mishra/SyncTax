package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.ui.theme.*

// Define a default dark, reddish background to match the image if no dynamic color is provided
private val DefaultPlayerBackground = Color(0xFF280A0A)

// Define semi-transparent colors to mimic the blur/frosted glass effect
private val LittleBlurColor = Color.White.copy(alpha = 0.05f) // Subtle overlay for "little blur"
private val MoreBlurColor = Color.White.copy(alpha = 0.15f) // Stronger overlay for "more blur"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerContent(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    albumArtScale: Float = 1f,
    backgroundColor: Color = DefaultPlayerBackground,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    val isTransparentMode = backgroundColor == Color.Transparent

    // Main Box: The large rounded container with the dynamic background color
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp)) // Large rounded border for the Main Box
            .background(backgroundColor) // Dynamic color based on album art
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Progress Indicator at the top
        if (duration > 0) {
            LinearProgressIndicator(
                progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = PlayerAccent, // Assuming PlayerAccent is the red color from the image
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .padding(vertical = 6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            color = if (isTransparentMode) Color.Transparent else backgroundColor,
            tonalElevation = if (isTransparentMode) 0.dp else 3.dp,
            shadowElevation = if (isTransparentMode) 0.dp else 4.dp

        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Content Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = albumArtOffsetX.dp)
                            .pointerInput(song.id) {
                                detectTapGestures(onTap = { onClick() })
                            }
                            .pointerInput(song.id) {
                                var totalDragX = 0f
                                var totalDragY = 0f
                                var hasTriggeredAction = false

                                detectDragGestures(
                                    onDragStart = {
                                        totalDragX = 0f
                                        totalDragY = 0f
                                        hasTriggeredAction = false
                                    },
                                    onDragEnd = {
                                        val absX = kotlin.math.abs(totalDragX)
                                        val absY = kotlin.math.abs(totalDragY)
                                        if (absX > absY && absX > 50f && !hasTriggeredAction) {
                                            if (totalDragX > swipeThreshold) onPreviousClick()
                                            else if (totalDragX < -swipeThreshold) onNextClick()
                                            hasTriggeredAction = true
                                        } else if (absY > absX && absY > 50f && totalDragY < 0 && !hasTriggeredAction) {
                                            onSwipeUp()
                                            hasTriggeredAction = true
                                        }
                                        albumArtOffsetX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragX += dragAmount.x
                                        totalDragY += dragAmount.y
                                        if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
                                            albumArtOffsetX += dragAmount.x
                                        }
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(12.dp)) // Rounded border for the Inner Box
                            .background(MoreBlurColor) // Stronger semi-transparent overlay for "more blur"
                            .padding(8.dp) // Padding inside the inner box
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small Album Art Card
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(albumArtScale)
                                .clip(RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),  // Corner radius for album art
                            color = PlayerSurface, // Use a solid color for the album art background
                            tonalElevation = 2.dp
                        ) {
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp),
                                    tint = PlayerTextSecondary
                                )
                            } else {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Song Info Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PlayerTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlayerTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Visualizer
                        if (isPlaying) {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                AnimatedWaveform()
                            }
                        }
                    }

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp) // Separate controls from inner box
                    ) {
                        // Play/Pause
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Crossfade(
                                targetState = isPlaying,
                                label = "mini_play_pause"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = PlayerTextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Next
                        IconButton(
                            onClick = onNextClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = PlayerTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}