package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
}

/**
 * Repeat mode options for playback.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}


/**
 * Enum representing player anchor states for dragging.
 */
enum class PlayerState {
    Collapsed,
    Expanded
}

private const val MINI_PLAYER_HEIGHT_DP = 72f

// Mini state dimensions
private val MINI_ALBUM_ART_SIZE = 48.dp
private val MINI_ALBUM_ART_X = 12.dp
private val MINI_ALBUM_ART_Y = 15.dp
private val MINI_CORNER_RADIUS = 10.dp

// Full state dimensions
private val FULL_ALBUM_ART_SIZE = 300.dp
private val FULL_ALBUM_ART_Y = 100.dp
private val FULL_CORNER_RADIUS = 16.dp

/**
 * YouTube Music-style sliding player with smooth morphing animation.
 * Uses manual layout interpolation (lerp) for continuous transition between
 * mini and full player states.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidingPlayer(
    songs: List<Song>,
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onSongSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playbackState.currentSong ?: return
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val miniPlayerHeightPx = with(density) { MINI_PLAYER_HEIGHT_DP.dp.toPx() }

        // Collapsed = at bottom, Expanded = at top
        val collapsedOffset = screenHeightPx - miniPlayerHeightPx
        val expandedOffset = 0f

        val anchors = remember(screenHeightPx) {
            DraggableAnchors {
                PlayerState.Collapsed at collapsedOffset
                PlayerState.Expanded at expandedOffset
            }
        }

        val anchoredDraggableState = remember {
            AnchoredDraggableState(
                initialValue = PlayerState.Collapsed,
                positionalThreshold = { distance -> distance * 0.3f },
                velocityThreshold = { with(density) { 200.dp.toPx() } },
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

        // Alpha phasing for YouTube Music feel
        val miniControlsAlpha = (1f - (progress / 0.15f)).coerceIn(0f, 1f)
        val fullControlsAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val scrimAlpha = progress * 0.6f

        // Scrim overlay on background content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(scrimAlpha)
                .background(Color.Black)
        )

        // Player container
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
            UnifiedPlayerLayout(
                progress = progress,
                miniControlsAlpha = miniControlsAlpha,
                fullControlsAlpha = fullControlsAlpha,
                songs = songs,
                currentSong = currentSong,
                playbackState = playbackState,
                screenWidth = maxWidth,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onSeek = onSeek,
                onSongSelected = onSongSelected,
                onCollapseClick = {
                    coroutineScope.launch {
                        anchoredDraggableState.settle(1000f)
                    }
                }
            )
        }
    }
}

/**
 * Unified player layout that morphs based on progress value.
 * All elements are positioned using lerp interpolation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedPlayerLayout(
    progress: Float,
    miniControlsAlpha: Float,
    fullControlsAlpha: Float,
    songs: List<Song>,
    currentSong: Song,
    playbackState: PlaybackState,
    screenWidth: Dp,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onSongSelected: (Int) -> Unit,
    onCollapseClick: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(playbackState.progress) }
    var isDragging by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }
    val displayProgress = if (isDragging > 0f) sliderPosition else playbackState.progress

    // Dynamic Background Color
    var dominantColor by remember { mutableStateOf(Color.DarkGray) }
    val context = LocalContext.current

    LaunchedEffect(currentSong.albumArtUri) {
        if (currentSong.albumArtUri != null) {
            val request = ImageRequest.Builder(context)
                .data(currentSong.albumArtUri)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable).toBitmap()
                Palette.from(bitmap).generate { palette ->
                    palette?.dominantSwatch?.rgb?.let { colorValue ->
                        dominantColor = Color(colorValue)
                    }
                }
            }
        } else {
            dominantColor = Color.DarkGray
        }
    }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "BackgroundColor"
    )

    // Pager State for song swiping (works in both states)
    val initialPage = remember(songs, currentSong) {
        songs.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { songs.size })

    // Sync Pager with Current Song (when song changes externally)
    LaunchedEffect(currentSong.id) {
        val index = songs.indexOfFirst { it.id == currentSong.id }
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    // Sync Song with Pager (when user swipes)
    val currentSongState = androidx.compose.runtime.rememberUpdatedState(currentSong)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page in songs.indices && songs[page].id != currentSongState.value.id) {
                onSongSelected(page)
            }
        }
    }

    // Calculate interpolated values
    val albumArtSize = lerp(MINI_ALBUM_ART_SIZE.value, FULL_ALBUM_ART_SIZE.value, progress).dp
    val albumArtCornerRadius = lerp(MINI_CORNER_RADIUS.value, FULL_CORNER_RADIUS.value, progress).dp

    // For horizontal pager padding in full state
    val fullPagerPadding = lerp(0f, 40f, progress).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBackgroundColor.copy(alpha = 0.7f * progress),
                        animatedBackgroundColor.copy(alpha = 0.3f * progress),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
    ) {
        // ============ MINI PLAYER ELEMENTS ============
        // These fade out quickly (0% -> 15%)

        // Mini progress bar at top
        if (miniControlsAlpha > 0f) {
            LinearProgressIndicator(
                progress = { playbackState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .alpha(miniControlsAlpha),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        // Mini play button (right side)
        if (miniControlsAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .alpha(miniControlsAlpha)
            ) {
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ============ MORPHING ALBUM ART (HorizontalPager) ============

        // Calculate album art position
        val miniAlbumX = MINI_ALBUM_ART_X.value
        val fullAlbumX = (screenWidth.value - albumArtSize.value) / 2f
        val albumArtX = lerp(miniAlbumX, fullAlbumX, progress)

        val miniAlbumY = MINI_ALBUM_ART_Y.value
        val fullAlbumY = FULL_ALBUM_ART_Y.value
        val albumArtY = lerp(miniAlbumY, fullAlbumY, progress)

        Box(
            modifier = Modifier
                .offset(x = albumArtX.dp, y = albumArtY.dp)
                .size(albumArtSize)
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = fullPagerPadding),
                pageSpacing = lerp(0f, 16f, progress).dp,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val pageSong = songs.getOrNull(page)

                if (pageSong != null) {
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).let {
                        if (it < 0) -it else it
                    }

                    val scale = lerp(1f, lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f)), progress)
                    val alpha = lerp(1f, lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f)), progress)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .shadow(
                                elevation = lerp(4f, 16f, progress).dp,
                                shape = RoundedCornerShape(albumArtCornerRadius),
                                spotColor = Color.Black.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(albumArtCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageSong.albumArtUri != null) {
                            AsyncImage(
                                model = pageSong.albumArtUri,
                                contentDescription = "Album art for ${pageSong.album}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (pageSong.albumArtUri == null) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(lerp(20f, 80f, progress).dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // ============ MORPHING TEXT (Title/Artist) ============

        val miniTextX = MINI_ALBUM_ART_X.value + MINI_ALBUM_ART_SIZE.value + 12f
        val miniTextY = MINI_ALBUM_ART_Y.value
        val fullTextY = FULL_ALBUM_ART_Y.value + FULL_ALBUM_ART_SIZE.value + 32f

        val textX = lerp(miniTextX, 24f, progress)
        val textY = lerp(miniTextY, fullTextY, progress)
        val textWidth = lerp(screenWidth.value - miniTextX - 60f, screenWidth.value - 48f, progress)

        Column(
            modifier = Modifier
                .offset(x = textX.dp, y = textY.dp)
                .width(textWidth.dp),
            horizontalAlignment = if (progress > 0.5f) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = lerp(14f, 22f, progress).sp,
                    fontWeight = if (progress > 0.5f) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (progress > 0.5f) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(lerp(2f, 8f, progress).dp))

            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = lerp(12f, 16f, progress).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (progress > 0.5f) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ============ FULL PLAYER ELEMENTS ============
        // These fade in late (85% -> 100%)

        if (fullControlsAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .alpha(fullControlsAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ===== TOP BAR =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Down arrow to collapse
                    IconButton(onClick = onCollapseClick) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Current time display
                    Text(
                        text = formatDuration(playbackState.currentPosition),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    // More options menu
                    IconButton(onClick = { /* TODO: Show menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Spacer for album art and song info area
                Spacer(modifier = Modifier.weight(1f))

                // ===== SEEKBAR =====
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Slider(
                        value = displayProgress,
                        onValueChange = {
                            sliderPosition = it
                            isDragging = 1f
                        },
                        onValueChangeFinished = {
                            onSeek(sliderPosition)
                            isDragging = 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(playbackState.currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(playbackState.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ===== MAIN CONTROLS (Previous, Play/Pause, Next) =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Play/Pause - Large circular button
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        onClick = onPlayPauseClick
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ===== BOTTOM ACTION ROW =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Timer/Sleep
                    IconButton(onClick = { /* TODO: Timer */ }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Shuffle
                    IconButton(onClick = onShuffleClick) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(24.dp),
                            tint = if (playbackState.shuffleEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Repeat
                    IconButton(onClick = onRepeatClick) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                RepeatMode.ALL -> Icons.Default.Repeat
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            modifier = Modifier.size(24.dp),
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Queue/Lyrics
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Queue Bottom Sheet
        if (showQueue) {
            QueueBottomSheet(
                songs = songs,
                currentSong = currentSong,
                onDismiss = { showQueue = false },
                onSongClick = {
                    onSongSelected(songs.indexOf(it))
                    showQueue = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    songs: List<Song>,
    currentSong: Song,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Playing Queue",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                items(songs.size) { index ->
                    val song = songs[index]
                    val isPlaying = song.id == currentSong.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Playing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
