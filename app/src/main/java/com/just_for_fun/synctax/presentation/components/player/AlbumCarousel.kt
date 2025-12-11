package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurfaceVariant
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AlbumArtCarousel(
    queue: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    volume: Float,
    expansionProgress: Float,
    onSongSelected: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onLyricsClick: () -> Unit
) {
    if (currentSong == null) return

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Gesture Feedback States
    var seekDirection by remember { mutableStateOf<String?>(null) }
    var showPlayPause by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var overlayVolume by remember { mutableFloatStateOf(volume) }

    // Swipe animation state
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = 150f

    // Sync overlay volume
    LaunchedEffect(volume) {
        overlayVolume = volume
    }

    // Auto-hide overlays
    LaunchedEffect(seekDirection) {
        if (seekDirection != null) {
            delay(1500)
            seekDirection = null
        }
    }
    LaunchedEffect(showPlayPause) {
        if (showPlayPause) {
            delay(2000)
            showPlayPause = false
        }
    }
    LaunchedEffect(showVolume) {
        if (showVolume) {
            delay(2000)
            showVolume = false
        }
    }

    // Detect external play/pause changes for overlay feedback
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (isPlaying != previousIsPlaying) {
            showPlayPause = true
            previousIsPlaying = isPlaying
        }
    }

    // Album Art Box with Swipe Gestures
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(currentSong.id) {
                    var totalDragX = 0f
                    var hasTriggered = false

                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            hasTriggered = false
                        },
                        onDragEnd = {
                            scope.launch {
                                if (!hasTriggered && abs(totalDragX) > swipeThreshold) {
                                    hasTriggered = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    if (totalDragX > 0) {
                                        // Swipe right -> Previous song
                                        onPreviousClick()
                                    } else {
                                        // Swipe left -> Next song
                                        onNextClick()
                                    }
                                }
                                // Animate back to center
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount
                            scope.launch {
                                // Apply some resistance to the drag
                                val resistance = 0.6f
                                offsetX.snapTo(totalDragX * resistance)
                            }
                        }
                    )
                }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    ),
                shape = RoundedCornerShape(24.dp),
                color = PlayerSurfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Image Layer
                    if (currentSong.albumArtUri.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = PlayerTextSecondary.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        val expandedImageScale by animateFloatAsState(
                            targetValue = 1f + (expansionProgress * 0.08f),
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            ),
                            label = "expandedImageScale"
                        )

                        AsyncImage(
                            model = currentSong.albumArtUri,
                            contentDescription = "Album Art for ${currentSong.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(expandedImageScale)
                        )
                    }

                    // Vertical Volume Drag Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val volumeChange = -dragAmount * 0.002f
                                    val newVolume =
                                        (overlayVolume + volumeChange).coerceIn(0.0f, 1.0f)
                                    overlayVolume = newVolume
                                    showVolume = true
                                    onSetVolume(newVolume)
                                }
                            }
                    )

                    // Tap Gesture Layer (Seek & Play/Pause & Lyrics Toggle)
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Double Tap Left: Rewind
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            onLyricsClick()
                                        },
                                        onDoubleTap = {
                                            val target = (position - 10_000L).coerceAtLeast(0L)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            seekDirection = "backward"
                                            onSeek(target)
                                        }
                                    )
                                }
                        )
                        // Double Tap Center: Play/Pause
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            onLyricsClick()
                                        },
                                        onDoubleTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPlayPauseClick()
                                        }
                                    )
                                }
                        )
                        // Double Tap Right: Forward
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            onLyricsClick()
                                        },
                                        onDoubleTap = {
                                            val target =
                                                (position + 10_000L).coerceAtMost(duration)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            seekDirection = "forward"
                                            onSeek(target)
                                        }
                                    )
                                }
                        )
                    }

                    // Feedback Overlays (Seek, Play, Volume)
                    if (seekDirection != null || showPlayPause || showVolume) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                seekDirection != null -> {
                                    Icon(
                                        imageVector = if (seekDirection == "forward") Icons.Default.FastForward else Icons.Default.FastRewind,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White
                                    )
                                }

                                showPlayPause -> {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White
                                    )
                                }

                                showVolume -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${(overlayVolume * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}