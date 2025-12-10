package com.just_for_fun.synctax.presentation.components.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.state.PlayerSheetState
import com.just_for_fun.synctax.presentation.ui.theme.PlayerBackground
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "UnifiedPlayerSheet"

/**
 * Enhanced unified player sheet with smooth animated transitions between collapsed (miniplayer)
 * and expanded (fullscreen) states. Features PixelPlay-style staggered alpha crossfade,
 * continuous drag-based expansion tracking, translation animations, and spring physics.
 *
 * Key animation features:
 * - Mini player fades out at 2x speed (completely gone by 50% expansion)
 * - Full player starts fading in at 25% expansion
 * - Full player slides up as it fades in for dynamic feel
 * - Spring animations with overshoot for bouncy feedback
 * - Real-time drag tracking for responsive gesture interaction
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // === CORE ANIMATION STATE ===
    // Animatable for continuous expansion tracking (0 = collapsed, 1 = expanded)
    val expansionFractionAnimatable = remember { Animatable(if (playerSheetState == PlayerSheetState.EXPANDED) 1f else 0f) }
    val expansionFraction by expansionFractionAnimatable.asState()

    // Track if user is currently dragging
    var isDragging by remember { mutableStateOf(false) }
    
    // Velocity tracker for fling detection
    val velocityTracker = remember { VelocityTracker() }
    
    // Accumulated drag for threshold detection
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var initialFractionOnDragStart by remember { mutableFloatStateOf(0f) }

    // === STAGGERED ALPHA CROSSFADE (PixelPlay-style) ===
    // Mini player fades out at 2x speed - completely gone by 50% expansion
    val miniPlayerAlpha by remember {
        derivedStateOf {
            (1f - expansionFraction * PlayerSheetConstants.MINI_ALPHA_FADE_MULTIPLIER)
                .coerceIn(0f, 1f)
        }
    }

    // Full player starts fading in at 25% expansion, fully visible by 100%
    val fullPlayerContentAlpha by remember {
        derivedStateOf {
            val fadeRange = 1f - PlayerSheetConstants.FULL_ALPHA_FADE_START
            ((expansionFraction - PlayerSheetConstants.FULL_ALPHA_FADE_START)
                .coerceIn(0f, fadeRange) / fadeRange)
        }
    }

    // === TRANSLATION ANIMATION FOR FULL PLAYER ===
    // Full player slides up as it fades in for dynamic feel
    val initialFullPlayerOffsetPx = with(density) {
        PlayerSheetConstants.FULL_PLAYER_INITIAL_OFFSET_DP.dp.toPx()
    }
    
    val fullPlayerTranslationY by remember {
        derivedStateOf {
            lerp(initialFullPlayerOffsetPx, 0f, fullPlayerContentAlpha)
        }
    }

    // Album art scale with overshoot for bouncy feedback
    val albumArtScale by animateFloatAsState(
        targetValue = if (playerSheetState == PlayerSheetState.EXPANDED) {
            PlayerSheetConstants.ALBUM_ART_EXPANDED_SCALE
        } else {
            PlayerSheetConstants.ALBUM_ART_MINI_SCALE
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "album_art_scale"
    )

    // Sync animatable with external state changes (e.g., from back button)
    LaunchedEffect(playerSheetState) {
        if (!isDragging) {
            val targetFraction = if (playerSheetState == PlayerSheetState.EXPANDED) 1f else 0f
            if (abs(expansionFractionAnimatable.value - targetFraction) > 0.01f) {
                expansionFractionAnimatable.animateTo(
                    targetValue = targetFraction,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
        Log.d(TAG, ">>> LaunchedEffect: playerSheetState changed to: $playerSheetState")
    }

    // Predictive back support: collapse on back press when expanded
    BackHandler(enabled = playerSheetState == PlayerSheetState.EXPANDED) {
        Log.d(TAG, ">>> BackHandler triggered - collapsing player sheet")
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onPlayerSheetStateChange(PlayerSheetState.COLLAPSED)
    }

    // Calculate minimum drag threshold in pixels
    val minDragDistancePx = with(density) { PlayerSheetConstants.MIN_DRAG_DISTANCE_DP.dp.toPx() }

    // Main Container with enhanced drag gesture handling
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Dynamic height based on expansion fraction with smooth interpolation
            .height(
                (PlayerSheetConstants.MINI_PLAYER_HEIGHT_DP +
                        (expansionFraction * PlayerSheetConstants.MAX_EXPANSION_HEIGHT_DP)).dp
            )
            .pointerInput(Unit) {
                val screenHeightPx = size.height.toFloat()
                val maxExpansionPx = PlayerSheetConstants.MAX_EXPANSION_HEIGHT_DP * density.density
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Stop any ongoing animation
                        scope.launch {
                            expansionFractionAnimatable.stop()
                        }
                        isDragging = true
                        velocityTracker.resetTracking()
                        initialFractionOnDragStart = expansionFractionAnimatable.value
                        accumulatedDragY = 0f
                        Log.d(TAG, ">>> Drag started at fraction: $initialFractionOnDragStart")
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount
                        
                        // Convert drag distance to expansion fraction change
                        // Negative drag (up) = expand, positive drag (down) = collapse
                        val dragFraction = -accumulatedDragY / (maxExpansionPx * PlayerSheetConstants.DRAG_TO_EXPANSION_MULTIPLIER)
                        val newFraction = (initialFractionOnDragStart + dragFraction).coerceIn(0f, 1f)
                        
                        // Update expansion fraction in real-time
                        scope.launch {
                            expansionFractionAnimatable.snapTo(newFraction)
                        }
                        
                        // Track velocity for fling detection
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    },
                    onDragEnd = {
                        isDragging = false
                        val velocity = velocityTracker.calculateVelocity().y
                        val currentFraction = expansionFractionAnimatable.value
                        
                        Log.d(TAG, ">>> Drag ended - velocity: $velocity, fraction: $currentFraction, accumulatedDrag: $accumulatedDragY")
                        
                        // Determine target state based on drag direction, velocity, and position
                        val shouldExpand = when {
                            // Check if drag was significant
                            abs(accumulatedDragY) > minDragDistancePx ->
                                accumulatedDragY < 0 // Dragged up = expand
                            // Check for fast fling
                            abs(velocity) > PlayerSheetConstants.VELOCITY_THRESHOLD ->
                                velocity < 0 // Fast upward = expand
                            // Fallback to position-based decision
                            else ->
                                currentFraction > 0.5f
                        }
                        
                        val targetFraction = if (shouldExpand) 1f else 0f
                        val newState = if (shouldExpand) PlayerSheetState.EXPANDED else PlayerSheetState.COLLAPSED
                        
                        Log.d(TAG, ">>> Animating to target: $targetFraction, newState: $newState")
                        
                        // Animate to target with spring physics
                        scope.launch {
                            // Provide haptic feedback at state change
                            if ((currentFraction > 0.5f) != shouldExpand || 
                                abs(currentFraction - targetFraction) > 0.3f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            
                            expansionFractionAnimatable.animateTo(
                                targetValue = targetFraction,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        
                        // Update the sheet state
                        onPlayerSheetStateChange(newState)
                    }
                )
            }
            .background(PlayerBackground)
    ) {
        // --- 1. ANIMATED BACKGROUND LAYER ---
        if (!song.albumArtUri.isNullOrEmpty()) {
            // Interpolate blur radius based on expansion fraction
            val blurRadius = (PlayerSheetConstants.MIN_BLUR_RADIUS_DP +
                    (expansionFraction * PlayerSheetConstants.BLUR_RADIUS_EXPANSION_DP)).dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(
                        PlayerSheetConstants.MIN_BACKGROUND_OPACITY +
                                (expansionFraction * PlayerSheetConstants.BACKGROUND_OPACITY_EXPANSION)
                    )
                    .scale(
                        PlayerSheetConstants.MIN_BACKGROUND_SCALE +
                                (expansionFraction * PlayerSheetConstants.BACKGROUND_SCALE_EXPANSION)
                    )
            )

            // Gradient Overlay with animated opacity for smooth contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_TOP_ALPHA +
                                            (expansionFraction * PlayerSheetConstants.GRADIENT_TOP_ALPHA_EXPANSION)
                                ),
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_BOTTOM_ALPHA +
                                            (expansionFraction * PlayerSheetConstants.GRADIENT_BOTTOM_ALPHA_EXPANSION)
                                )
                            )
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER WITH STAGGERED ALPHA CROSSFADE ---
        Surface(
            color = Color.Transparent, // Transparent for background visibility
            tonalElevation = 0.dp,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mini player content with staggered fade-out (fades completely by 50% expansion)
                if (miniPlayerAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
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

                // Expanded player content with staggered fade-in and slide-up animation
                if (fullPlayerContentAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(fullPlayerContentAlpha)
                            .graphicsLayer {
                                translationY = fullPlayerTranslationY
                            }
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
