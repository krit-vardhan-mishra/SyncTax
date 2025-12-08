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
import com.just_for_fun.synctax.presentation.components.state.PlayerSheetState
import com.just_for_fun.synctax.presentation.ui.theme.PlayerBackground

private const val TAG = "UnifiedPlayerSheet"

/**
 * Enhanced unified player sheet with smooth animated transitions between collapsed (miniplayer)
 * and expanded (fullscreen) states. Includes drag gesture handling, alpha blending, overshoot
 * scaling, and predictive back support.
 *
 * Adapted from PixelPlay's UnifiedPlayerSheet for SyncTax.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPlayerSheet(
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
    playerSheetState: PlayerSheetState,
    onPlayerSheetStateChange: (PlayerSheetState) -> Unit,
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
    Log.d(TAG, "=== UnifiedPlayerSheet Recomposition === state: $playerSheetState, song: ${song.title}")
    
    val snackBarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // Track drag offset for gesture handling
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 200f // pixels to drag to trigger state change

    // Expansion fraction with overshoot animation for bouncy feedback
    val expansionFraction by animateFloatAsState(
        targetValue = if (playerSheetState == PlayerSheetState.EXPANDED) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expansion_fraction"
    )

    // Alpha for mini player content (fades out when expanding)
    val miniPlayerAlpha by animateFloatAsState(
        targetValue = if (playerSheetState == PlayerSheetState.COLLAPSED) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "mini_alpha"
    )

    // Alpha for expanded content (fades in when expanding)
    val expandedAlpha by animateFloatAsState(
        targetValue = if (playerSheetState == PlayerSheetState.EXPANDED) 1f else 0f,
        animationSpec = tween(durationMillis = 200, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "expanded_alpha"
    )

    // Album art scale with overshoot for bouncy feedback
    val albumArtScale by animateFloatAsState(
        targetValue = if (playerSheetState == PlayerSheetState.EXPANDED) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "album_art_scale"
    )

    // Predictive back support: collapse on back press when expanded
    BackHandler(enabled = playerSheetState == PlayerSheetState.EXPANDED) {
        Log.d(TAG, ">>> BackHandler triggered - collapsing player sheet")
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onPlayerSheetStateChange(PlayerSheetState.COLLAPSED)
    }
    
    // Log when sheet state changes
    LaunchedEffect(playerSheetState) {
        Log.d(TAG, ">>> LaunchedEffect: playerSheetState changed to: $playerSheetState")
    }

    // Main Container with drag gesture handling
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Dynamic height based on expansion fraction with smooth interpolation
            .height(
                (80 + (expansionFraction * 920)).dp
            )
            .pointerInput(playerSheetState) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        val absOffset = kotlin.math.abs(dragOffset)
                        if (absOffset > dragThreshold) {
                            // Trigger state change based on drag direction
                            if (dragOffset < 0 && playerSheetState == PlayerSheetState.COLLAPSED) {
                                // Swipe up to expand
                                Log.d(TAG, ">>> Drag gesture: expanding player sheet")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayerSheetStateChange(PlayerSheetState.EXPANDED)
                            } else if (dragOffset > 0 && playerSheetState == PlayerSheetState.EXPANDED) {
                                // Swipe down to collapse
                                Log.d(TAG, ">>> Drag gesture: collapsing player sheet")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayerSheetStateChange(PlayerSheetState.COLLAPSED)
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
        // --- 1. ANIMATED BACKGROUND LAYER ---
        if (!song.albumArtUri.isNullOrEmpty()) {
            // Interpolate blur radius based on expansion fraction
            val blurRadius = (20 + (expansionFraction * 30)).dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(0.5f + (expansionFraction * 0.2f)) // Gradually increase opacity
                    .scale(1f + (expansionFraction * 0.05f)) // Subtle zoom effect
            )

            // Gradient Overlay with animated opacity for smooth contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f + (expansionFraction * 0.1f)),
                                Color.Black.copy(alpha = 0.6f + (expansionFraction * 0.2f))
                            )
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER WITH ALPHA BLENDING ---
        Surface(
            color = Color.Transparent, // Transparent for background visibility
            tonalElevation = 0.dp,
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
                        MiniPlayerContent(
                            song = song,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            albumArtScale = albumArtScale,
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
                                Log.d(TAG, ">>> MiniPlayerContent onClick - expanding")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayerSheetStateChange(PlayerSheetState.EXPANDED)
                            },
                            onSwipeUp = { 
                                Log.d(TAG, ">>> MiniPlayerContent onSwipeUp - expanding")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayerSheetStateChange(PlayerSheetState.EXPANDED)
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
                            showUpNext = false,
                            snackbarHostState = snackBarHostState,
                            onShowUpNextChange = { },
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
                                onPlayerSheetStateChange(PlayerSheetState.COLLAPSED)
                            },
                            expansionProgress = expansionFraction,
                            downloadPercent = downloadPercent
                        )
                    }
                }
            }
        }
    }
}
