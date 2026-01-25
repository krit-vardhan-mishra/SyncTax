package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.launch

private const val TAG = "PlayerBottomSheet"
private const val MINI_PLAYER_HEIGHT_DP = 80f

/**
 * Player anchor states for AnchoredDraggable
 */
enum class PlayerState {
    Collapsed,  // Mini player at bottom
    Expanded    // Full screen player
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float = 1.0f,
    upNext: List<Song>,
    playHistory: List<Song> = emptyList(),
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectSong: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onSetVolume: (Float) -> Unit = {},
    downloadPercent: Int = 0,
    onExpandedChange: (Boolean) -> Unit = {},  // Reports expansion state to parent
    showPlayer: Boolean = true, // Whether to show the player UI at all
    content: @Composable (PaddingValues) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val miniPlayerHeightPx = with(density) { MINI_PLAYER_HEIGHT_DP.dp.toPx() }

        // Collapsed = at bottom (showing mini player), Expanded = at top (full screen)
        val collapsedOffset = screenHeightPx - miniPlayerHeightPx
        val expandedOffset = 0f

        // Define snap points
        val anchors = remember(screenHeightPx) {
            DraggableAnchors {
                PlayerState.Collapsed at collapsedOffset
                PlayerState.Expanded at expandedOffset
            }
        }

        // AnchoredDraggable state with physics-based animations
        val anchoredDraggableState = remember {
            AnchoredDraggableState(
                initialValue = PlayerState.Collapsed,
                positionalThreshold = { distance -> distance * 0.3f },  // 30% drag threshold
                velocityThreshold = { with(density) { 200.dp.toPx() } },  // Velocity sensitivity
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                decayAnimationSpec = exponentialDecay()
            )
        }

        // Update anchors when screen size changes
        LaunchedEffect(anchors) {
            anchoredDraggableState.updateAnchors(anchors)
        }

        // Calculate progress (0 = collapsed, 1 = expanded) - Single source of truth
        val progress by remember {
            derivedStateOf {
                val offset = if (anchoredDraggableState.offset.isNaN()) collapsedOffset else anchoredDraggableState.offset
                if (collapsedOffset == expandedOffset) 0f
                else 1f - (offset / collapsedOffset).coerceIn(0f, 1f)
            }
        }

        // Alpha phasing for smooth transitions (YouTube Music style)
        val miniControlsAlpha = (1f - (progress / 0.15f)).coerceIn(0f, 1f)
        val fullControlsAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val scrimAlpha = progress * 0.6f

        // Is player expanded based on current target state
        val isExpanded = anchoredDraggableState.targetValue == PlayerState.Expanded

        // Report expansion state changes to parent
        LaunchedEffect(isExpanded) {
            onExpandedChange(isExpanded)
        }

        // Content padding for mini player
        val bottomPadding = if (song != null) MINI_PLAYER_HEIGHT_DP.dp else 0.dp

        // --- 1. MAIN CONTENT LAYER ---
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            content(PaddingValues(bottom = bottomPadding))
        }

        // Only show player if there's a song AND the player should be shown
        if (song != null && showPlayer) {
            // --- 2. SCRIM OVERLAY ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(scrimAlpha)
                    .background(Color.Black)
            )

            // --- 3. PLAYER SURFACE WITH ANCHORED DRAGGABLE ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        val safeOffset = if (anchoredDraggableState.offset.isNaN()) {
                            collapsedOffset
                        } else {
                            anchoredDraggableState.offset
                        }
                        IntOffset(0, safeOffset.toInt())
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical
                    ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = lerp(16f, 0f, progress).dp,
                    topEnd = lerp(16f, 0f, progress).dp
                ),
                shadowElevation = 16.dp
            ) {
                UnifiedPlayer(
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
                    progress = progress,
                    miniControlsAlpha = miniControlsAlpha,
                    fullControlsAlpha = fullControlsAlpha,
                    isExpanded = isExpanded,
                    onExpandedChange = { expand ->
                        coroutineScope.launch {
                            if (expand) {
                                anchoredDraggableState.animateTo(PlayerState.Expanded)
                            } else {
                                anchoredDraggableState.animateTo(PlayerState.Collapsed)
                            }
                        }
                    },
                    onSelectSong = onSelectSong,
                    onPlaceNext = onPlaceNext,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onReorderQueue = onReorderQueue,
                    onSetVolume = onSetVolume,
                    downloadPercent = downloadPercent,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSeek = onSeek
                )
            }
        }
    }
}
