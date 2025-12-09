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
import com.just_for_fun.synctax.presentation.components.player.PlayerSheetConstants
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
        targetValue = if (isExpanded) {
            PlayerSheetConstants.ALBUM_ART_EXPANDED_SCALE
        } else {
            PlayerSheetConstants.ALBUM_ART_MINI_SCALE
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "albumArtMiniScale"
    )

    // Alpha blending for content transitions
    val miniPlayerAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        animationSpec = tween(
            durationMillis = PlayerSheetConstants.FADE_DURATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "mini_alpha"
    )

    val expandedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = PlayerSheetConstants.FADE_DURATION_MS,
            delayMillis = PlayerSheetConstants.FADE_IN_DELAY_MS,
            easing = FastOutSlowInEasing
        ),
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
            .height(
                (PlayerSheetConstants.MINI_PLAYER_HEIGHT_DP +
                        (expansionProgress * PlayerSheetConstants.MAX_EXPANSION_HEIGHT_DP)).dp
            )
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        val absOffset = kotlin.math.abs(dragOffset)
                        if (absOffset > PlayerSheetConstants.DRAG_THRESHOLD_PX) {
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
            val blurRadius = (PlayerSheetConstants.MIN_BLUR_RADIUS_DP +
                    (expansionProgress * PlayerSheetConstants.BLUR_RADIUS_EXPANSION_DP)).dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(
                        PlayerSheetConstants.MIN_BACKGROUND_OPACITY +
                                (expansionProgress * PlayerSheetConstants.BACKGROUND_OPACITY_EXPANSION)
                    )
                    .scale(
                        PlayerSheetConstants.MIN_BACKGROUND_SCALE +
                                (expansionProgress * PlayerSheetConstants.BACKGROUND_SCALE_EXPANSION)
                    )
            )

            // Animated gradient overlay for smooth contrast transitions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_TOP_ALPHA +
                                            (expansionProgress * PlayerSheetConstants.GRADIENT_TOP_ALPHA_EXPANSION)
                                ),
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_BOTTOM_ALPHA +
                                            (expansionProgress * PlayerSheetConstants.GRADIENT_BOTTOM_ALPHA_EXPANSION)
                                )
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
                if (miniPlayerAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
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
                if (expandedAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
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
