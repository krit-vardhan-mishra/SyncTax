package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurfaceVariant
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

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
    if (currentSong == null || queue.isEmpty()) return

    val haptic = LocalHapticFeedback.current

    // Gesture Feedback States
    var seekDirection by remember { mutableStateOf<String?>(null) }
    var showPlayPause by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var overlayVolume by remember { mutableFloatStateOf(volume) }

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

    // ========== HorizontalPager Implementation ==========
    // Initialize pager to the current song's index (using ID for lookup)
    val initialPage = remember(queue, currentSong) {
        queue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { queue.size }
    )

    // 1. Sync Pager with Current Song (External Changes - e.g., Next button, song ends)
    LaunchedEffect(currentSong.id) {
        val index = queue.indexOfFirst { it.id == currentSong.id }
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    // 2. Sync Song with Pager (User Swipes)
    // Use rememberUpdatedState to avoid stale captures in the swipe listener
    val currentSongState = rememberUpdatedState(currentSong)
    LaunchedEffect(pagerState) {
        // snapshotFlow detects when the swipe "settles" on a new page
        snapshotFlow { pagerState.settledPage }.collect { page ->
            // Check against currentSongState.value to avoid stale reference bugs
            if (page in queue.indices && queue[page].id != currentSongState.value.id) {
                onSongSelected(queue[page])
            }
        }
    }

    // Album Art Box with HorizontalPager
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp), // Full width
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val pageSong = queue.getOrNull(page) ?: return@HorizontalPager

            // Calculate offset from center (0.0 = centered, 1.0 = adjacent)
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            // Interpolate scale and alpha for "stack" visual effect
            // Center: 1.0 scale, 1.0 alpha
            // Adjacent: 0.85 scale, 0.5 alpha
            val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
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
                        if (pageSong.albumArtUri.isNullOrEmpty()) {
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
                                model = pageSong.albumArtUri,
                                contentDescription = "Album Art for ${pageSong.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(expandedImageScale)
                            )
                        }

                        // Only show gesture layers on current page
                        if (page == pagerState.currentPage) {
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
                                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
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
    }
}
