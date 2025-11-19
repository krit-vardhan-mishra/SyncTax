package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import com.just_for_fun.synctax.ui.components.utils.AudioAnalyzer

@Composable
fun MiniPlayerContent(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    albumArtScale: Float = 1f,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f

    // AudioAnalyzer instance with proper error handling
    val audioAnalyzer = remember { 
        try {
            AudioAnalyzer().takeIf { it.isReady() }
        } catch (e: Exception) {
            null
        }
    }

    // Clean up when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            audioAnalyzer?.stop()
        }
    }

    // Check if we should use real audio analysis or fake animation
    val useRealAudioAnalysis = audioAnalyzer != null

    // Create gradient background with the dynamic color
    val gradientColors = listOf(
        backgroundColor.copy(alpha = 0.9f),
        backgroundColor.copy(alpha = 0.7f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                brush = Brush.horizontalGradient(gradientColors)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress bar
            if (duration > 0) {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .offset(x = albumArtOffsetX.dp)
                        .pointerInput(song.id) {
                            detectTapGestures(
                                onTap = { onClick() }
                            )
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

                                    if (absX > absY && absX > 50f) {
                                        if (!hasTriggeredAction) {
                                            when {
                                                totalDragX > swipeThreshold -> {
                                                    hasTriggeredAction = true
                                                    onPreviousClick()
                                                }
                                                totalDragX < -swipeThreshold -> {
                                                    hasTriggeredAction = true
                                                    onNextClick()
                                                }
                                            }
                                        }
                                    } else if (absY > absX && absY > 50f && totalDragY < 0) {
                                        if (!hasTriggeredAction) {
                                            hasTriggeredAction = true
                                            onSwipeUp()
                                        }
                                    }

                                    albumArtOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y

                                    val absX = kotlin.math.abs(totalDragX)
                                    val absY = kotlin.math.abs(totalDragY)
                                    if (absX > absY) {
                                        albumArtOffsetX += dragAmount.x
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art with elevated styling
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(albumArtScale)
                                .clip(MaterialTheme.shapes.medium),
                            color = Color.White.copy(alpha = 0.1f),
                            shadowElevation = 8.dp
                        ) {
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Song Info
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Crossfade(targetState = song.title, label = "song_title") { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Crossfade(targetState = song.artist, label = "song_artist") { artist ->
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Controls on the right side
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Waveform indicator when playing
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (useRealAudioAnalysis) {
                                // Use real audio analysis data
                                val amplitudeData = audioAnalyzer?.getAmplitudeData() ?: floatArrayOf(0.4f, 0.6f, 0.7f, 0.8f)
                                WaveformAnimation(amplitudeData = amplitudeData)
                            } else {
                                // Use fake animated waveform
                                AnimatedWaveform()
                            }
                        }
                    }

                    TooltipIconButton(
                        onClick = onPlayPauseClick,
                        tooltipText = if (isPlaying) "Pause" else "Play"
                    ) {
                        val playScale = animateFloatAsState(
                            targetValue = if (isPlaying) 1.0f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "play_scale"
                        )
                        Crossfade(targetState = isPlaying, label = "play_pause") { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier
                                    .scale(playScale.value)
                                    .size(32.dp)
                            )
                        }
                    }

                    TooltipIconButton(
                        onClick = onNextClick,
                        tooltipText = "Next song"
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
