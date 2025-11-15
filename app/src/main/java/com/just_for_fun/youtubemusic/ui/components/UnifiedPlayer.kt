package com.just_for_fun.youtubemusic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.youtubemusic.R
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onSeek: (Long) -> Unit
    , downloadPercent: Int = 0
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    // Animation progress (0f = mini, 1f = expanded)
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "expansion"
    )
    
    var showUpNext by remember { mutableStateOf(false) }

    // Handle back button when player is expanded
    BackHandler(enabled = isExpanded) {
        onExpandedChange(false)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isExpanded) (expansionProgress * 1000).dp else 80.dp)
    ) {
        // Background (only visible when expanded)
        if (expansionProgress > 0f) {
            if (!song.albumArtUri.isNullOrEmpty()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur((80 * expansionProgress).dp)
                        .alpha(0.6f * expansionProgress)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f * expansionProgress))
            )
        }

        Surface(
            color = if (isExpanded) Color.Transparent 
                   else MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = if (isExpanded) 0.dp else 3.dp,
            shape = if (isExpanded) RectangleShape else RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isExpanded) {
                // MINI PLAYER VIEW - no state hoisting needed
                MiniPlayerContent(
                    song = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNextClick()
                    },
                    onPreviousClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreviousClick()
                    },
                    onClick = { onExpandedChange(true) },
                    onSwipeUp = { onExpandedChange(true) }
                )
            } else {
                // FULL SCREEN PLAYER VIEW - no state hoisting needed
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
                    snackbarHostState = snackbarHostState,
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
                    onClose = { onExpandedChange(false) },
                    downloadPercent = downloadPercent
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerContent(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit
) {
    // Local state - no callbacks needed
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    
    Column {
        // Progress bar
        if (duration > 0) {
            LinearProgressIndicator(
                progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swipeable container with album art, song name, and artist
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
                                
                                // Determine if horizontal or vertical
                                if (absX > absY && absX > 50f) {
                                    // Horizontal swipe - change song
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
                                    // Vertical swipe up - expand
                                    if (!hasTriggeredAction) {
                                        hasTriggeredAction = true
                                        onSwipeUp()
                                    }
                                }
                                
                                // Reset visual offset
                                albumArtOffsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                
                                // Update visual offset for horizontal movement
                                val absX = kotlin.math.abs(totalDragX)
                                val absY = kotlin.math.abs(totalDragY)
                                if (absX > absY) {
                                    albumArtOffsetX += dragAmount.x
                                }
                            }
                        )
                    }
            ) {
                // ... rest of the Surface with album art and song info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
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

                        Spacer(modifier = Modifier.width(12.dp))

                        // Song Info
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Controls
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onPlayPauseClick) {
                    Crossfade(targetState = isPlaying, label = "play_pause") { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenPlayerContent(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float,
    upNext: List<Song>,
    playHistory: List<Song>,
    showUpNext: Boolean,
    snackbarHostState: SnackbarHostState,
    onShowUpNextChange: (Boolean) -> Unit,
    onSelectSong: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onSetVolume: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit
    , downloadPercent: Int = 0
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Local overlay states
    var seekDirection by remember { mutableStateOf<String?>(null) }
    var showPlayPause by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var overlayVolume by remember { mutableFloatStateOf(volume) }
    
    // Swipe gesture state - LOCAL to this composable
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    
    // Sync overlay volume with actual volume
    LaunchedEffect(volume) {
        overlayVolume = volume
    }

    // Hide overlays after delay
    LaunchedEffect(seekDirection) {
        if (seekDirection != null) {
            delay(1500)
            seekDirection = null
        }
    }

    LaunchedEffect(showPlayPause) {
        if (showPlayPause) {
            delay(3000)
            showPlayPause = false
        }
    }

    LaunchedEffect(showVolume) {
        if (showVolume) {
            delay(3000)
            showVolume = false
        }
    }

    // Detect play/pause changes
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (isPlaying != previousIsPlaying) {
            showPlayPause = true
            previousIsPlaying = isPlaying
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Player",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "OFFLINE MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* More Options */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.music_logo),
                        contentDescription = "Music Logo",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // FIXED: Album art container with proper gesture hierarchy
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .offset(x = albumArtOffsetX.dp)
                    // HORIZONTAL SWIPE - outermost layer
                    .pointerInput(song.id) {
                        var hasTriggered = false
                        detectHorizontalDragGestures(
                            onDragStart = {
                                hasTriggered = false
                            },
                            onDragEnd = {
                                if (!hasTriggered && kotlin.math.abs(albumArtOffsetX) > swipeThreshold) {
                                    hasTriggered = true
                                    when {
                                        albumArtOffsetX > 0 -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPreviousClick()
                                        }
                                        albumArtOffsetX < 0 -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNextClick()
                                        }
                                    }
                                }
                                albumArtOffsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                // Don't consume here - let inner gestures handle their events
                                albumArtOffsetX += dragAmount
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art Surface with SEPARATE gesture zones
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Album Art Image
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
                                        .size(120.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // SEPARATE LAYER: Volume control (vertical drag)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(volume) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume() // Consume to prevent horizontal swipe
                                            val volumeChange = -dragAmount * 0.002f
                                            val newVolume = (overlayVolume + volumeChange).coerceIn(0.0f, 1.0f)
                                            overlayVolume = newVolume
                                            showVolume = true
                                            onSetVolume(newVolume)
                                        }
                                    }
                            )

                            // SEPARATE LAYER: Tap zones for seek and play/pause
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Left: Rewind (double-tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(position, duration) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    val target = (position - 10_000L).coerceAtLeast(0L)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    seekDirection = "backward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )

                                // Middle: Play/Pause (double-tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(isPlaying) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onPlayPauseClick()
                                                }
                                            )
                                        }
                                )

                                // Right: Forward (single tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(position, duration) {
                                            detectTapGestures(
                                                onTap = {
                                                    val target = (position + 10_000L).coerceAtMost(duration)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    seekDirection = "forward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )
                            }

                            // Overlays (same as before)
                            if (seekDirection != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (seekDirection == "forward") Icons.Default.Forward10 else Icons.Default.Replay10,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            if (showPlayPause) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Paused" else "Playing",
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            if (showVolume) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Volume",
                                            modifier = Modifier.size(60.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${(overlayVolume * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Song Info Container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            PlayerSlider(
                position = position,
                duration = duration,
                onSeek = onSeek
            )

            // Download progress for streaming online songs
            if (downloadPercent > 0 && downloadPercent < 100) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(text = "Downloading: $downloadPercent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls (same as before)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(28.dp),
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                FilledIconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = onRepeatClick) {
                    Icon(
                        imageVector = if (repeatEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        modifier = Modifier.size(28.dp),
                        tint = if (repeatEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Up Next Button
            TextButton(onClick = { onShowUpNextChange(true) }) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = "Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UP NEXT",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Up Next Bottom Sheet
            if (showUpNext) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                ModalBottomSheet(
                    onDismissRequest = { onShowUpNextChange(false) },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    UpNextSheet(
                        upcomingItems = upNext,
                        historyItems = playHistory,
                        onSelect = onSelectSong,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onReorderQueue = onReorderQueue,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val safeDuration = maxOf(1L, duration)
    val safePosition = position.coerceIn(0, safeDuration)

    var sliderPosition by remember(safePosition, safeDuration) { 
        mutableFloatStateOf(safePosition.toFloat()) 
    }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(position, duration) {
        if (!isSeeking) {
            sliderPosition = position.coerceIn(0, safeDuration).toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                isSeeking = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                onSeek(sliderPosition.toLong().coerceIn(0L, safeDuration))
                isSeeking = false
            },
            valueRange = 0f..safeDuration.toFloat(),
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = MaterialTheme.colorScheme.onBackground,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(sliderPosition.toLong().coerceIn(0L, safeDuration)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    if (milliseconds < 0) return "0:00"
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

@Composable
private fun UpNextSheet(
    upcomingItems: List<Song>,
    historyItems: List<Song>,
    onSelect: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (upcomingItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Up next (${upcomingItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(upcomingItems) { song ->
                    UpNextItem(
                        song = song,
                        onSelect = onSelect,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        snackbarHostState = snackbarHostState
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                }
            }
            if (historyItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History (${historyItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(historyItems.reversed()) { song ->
                    UpNextItem(
                        song = song,
                        onSelect = onSelect,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        snackbarHostState = snackbarHostState,
                        isHistory = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                }
            }
            if (upcomingItems.isEmpty() && historyItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs in queue",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpNextItem(
    song: Song,
    onSelect: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    snackbarHostState: SnackbarHostState,
    isHistory: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRemoveFromQueue(song)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { snackbarHostState.showSnackbar("Removed: ${song.title}") }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onPlaceNext(song)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { snackbarHostState.showSnackbar("Placed next: ${song.title}") }
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isHistory,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.PlaylistAdd
                else -> Icons.Default.Delete
            }
            val tint = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) 
                    Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable { onSelect(song) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                if (song.albumArtUri.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
