package com.just_for_fun.synctax.presentation.components.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.PlayerBackground

private const val TAG = "UnifiedPlayer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float = 1.0f,
    upNext: List<Song> = emptyList(),
    playHistory: List<Song> = emptyList(),
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectSong: (Song) -> Unit = {},
    onPlaceNext: (Song) -> Unit = {},
    onRemoveFromQueue: (Song) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onSetVolume: (Float) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    downloadPercent: Int = 0,
) {
    Log.d(TAG, "=== UnifiedPlayer Recomposition === isExpanded: $isExpanded, song: ${song.title}")
    
    val snackBarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // Track drag offset for enhanced gesture handling
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 200f // pixels to drag to trigger state change

    // Enhanced expansion animation with spring for bouncy feedback
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expansion"
    )

    var showUpNext by remember { mutableStateOf(false) }

    // Album art scale with overshoot for bouncy feedback
    val albumArtMiniScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "albumArtMiniScale"
    )

    // Alpha blending for content transitions
    val miniPlayerAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "mini_alpha"
    )

    val expandedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "expanded_alpha"
    )

    BackHandler(enabled = isExpanded) {
        Log.d(TAG, ">>> BackHandler triggered - calling onExpandedChange(false)")
        onExpandedChange(false)
    }
    
    // Log when isExpanded changes
    LaunchedEffect(isExpanded) {
        Log.d(TAG, ">>> LaunchedEffect: isExpanded changed to: $isExpanded")
    }

    // Main Container with enhanced drag gesture handling
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Dynamic height with smooth interpolation
            .height((80 + (expansionProgress * 920)).dp)
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        val absOffset = kotlin.math.abs(dragOffset)
                        if (absOffset > dragThreshold) {
                            // Trigger state change based on drag direction
                            if (dragOffset < 0 && !isExpanded) {
                                // Swipe up to expand
                                Log.d(TAG, ">>> Drag gesture: expanding player")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpandedChange(true)
                            } else if (dragOffset > 0 && isExpanded) {
                                // Swipe down to collapse
                                Log.d(TAG, ">>> Drag gesture: collapsing player")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpandedChange(false)
                            }
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
            .background(PlayerBackground)
    ) {
        // --- 1. ENHANCED ANIMATED BACKGROUND LAYER ---
        if (!song.albumArtUri.isNullOrEmpty()) {
            // Dynamic blur radius interpolates smoothly based on expansion
            val blurRadius = (20 + (expansionProgress * 30)).dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(0.5f + (expansionProgress * 0.2f)) // Gradually increase opacity
                    .scale(1f + (expansionProgress * 0.05f)) // Subtle zoom effect
            )

            // Animated gradient overlay for smooth contrast transitions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f + (expansionProgress * 0.1f)),
                                Color.Black.copy(alpha = 0.6f + (expansionProgress * 0.2f))
                            )
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER WITH ALPHA BLENDING ---
        Surface(
            color = Color.Transparent, // Must be transparent for background to show
            tonalElevation = 0.dp,     // Must be 0 to prevent M3 surface tint
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mini player content with fade-out animation
                if (miniPlayerAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(miniPlayerAlpha)
                    ) {
                        Log.d(TAG, ">>> Rendering MiniPlayerContent with alpha: $miniPlayerAlpha")
                        MiniPlayerContent(
                    song = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    albumArtScale = albumArtMiniScale,
                    // Pass Transparent here to trigger the fix in MiniPlayerContent
                    backgroundColor = Color.Transparent,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNextClick()
                    },
                    onPreviousClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreviousClick()
                    },
                    onClick = { 
                        Log.d(TAG, ">>> MiniPlayerContent onClick - calling onExpandedChange(true)")
                        onExpandedChange(true) 
                    },
                    onSwipeUp = { 
                        Log.d(TAG, ">>> MiniPlayerContent onSwipeUp - calling onExpandedChange(true)")
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExpandedChange(true) 
                    }
                )
                    }
                }

                // Expanded player content with fade-in animation
                if (expandedAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(expandedAlpha)
                    ) {
                        Log.d(TAG, ">>> Rendering FullScreenPlayerContent with alpha: $expandedAlpha")
                        FullScreenPlayerContent(
                    song = song,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    position = position,
                    duration = duration,
                    shuffleEnabled = shuffleEnabled,
                    repeatEnabled = repeatEnabled,
                    volume = volume,
                    upNext = upNext,
                    playHistory = playHistory,
                    showUpNext = showUpNext,
                    snackbarHostState = snackBarHostState,
                    onShowUpNextChange = { showUpNext = it },
                    onSelectSong = onSelectSong,
                    onPlaceNext = onPlaceNext,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onReorderQueue = onReorderQueue,
                    onSetVolume = onSetVolume,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNextClick()
                    },
                    onPreviousClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreviousClick()
                    },
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSeek = onSeek,
                    onClose = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExpandedChange(false)
                    },
                    expansionProgress = expansionProgress,
                    downloadPercent = downloadPercent
                )
                    }
                }
            }
        }
    }
}
