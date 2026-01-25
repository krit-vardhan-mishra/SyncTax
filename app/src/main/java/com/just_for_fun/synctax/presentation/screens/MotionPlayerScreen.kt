package com.just_for_fun.synctax.presentation.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.utils.TimeUtils
import com.just_for_fun.synctax.data.local.entities.Song

// --- 1. SENSOR LOGIC ---

/**
 * Hook to access the device's rotation vector sensor.
 * Returns an Offset representing pitch (y) and roll (x).
 */
@Composable
fun rememberParallaxSensorState(): State<Offset> {
    val context = LocalContext.current
    val sensorState = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    // orientation[1] = Pitch (tilt up/down)
                    // orientation[2] = Roll (tilt left/right)
                    // We map these to X/Y offsets
                    sensorState.value = Offset(x = orientation[2], y = orientation[1])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* No-op */ }
        }

        // Register listener if sensor exists
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    return sensorState
}

// --- 2. MAIN SCREEN COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MotionPlayerScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean = false,
    currentPosition: Long = 0L,
    totalDuration: Long = 0L,
    onPlayPause: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSongSelected: (Song) -> Unit = {},
    onModeChanged: (String) -> Unit = {},
    onShuffle: () -> Unit = {},
    selectedMode: String = "Offline"
) {
    if (songs.isEmpty() || currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No songs available", color = Color.White)
        }
        return
    }

    // Track the last selected song to prevent duplicate selections
    var lastSelectedSongId by remember { mutableStateOf(currentSong.id) }

    val initialIndex = remember(currentSong) {
        songs.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { songs.size }
    )

    // Sync pager with current song changes (from external sources like next/previous buttons)
    LaunchedEffect(currentSong.id) {
        val index = songs.indexOfFirst { it.id == currentSong.id }
        if (index >= 0 && index != pagerState.currentPage) {
            lastSelectedSongId = currentSong.id
            pagerState.animateScrollToPage(index)
        }
    }

    // Handle page changes - play song INSTANTLY when page settles
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val selectedSong = songs.getOrNull(page)
            if (selectedSong != null && selectedSong.id != lastSelectedSongId) {
                // Instant song change - no debounce for responsive feel
                lastSelectedSongId = selectedSong.id
                onSongSelected(selectedSong)
            }
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { index -> "${index}_${songs.getOrNull(index)?.id ?: index}" }
    ) { pageIndex ->
        val song = songs[pageIndex]
        MotionPlayerPage(
            song = song,
            isPlaying = isPlaying && (song.id == currentSong.id),
            currentPosition = if (song.id == currentSong.id) currentPosition else 0L,
            totalDuration = if (song.id == currentSong.id) totalDuration else 0L,
            onPlayPause = {
                if (song.id == currentSong.id) onPlayPause() else onSongSelected(song)
            },
            onSeek = onSeek,
            onBack = onBack,
            onNext = onNext,
            onPrevious = onPrevious,
            isCurrentSong = song.id == currentSong.id,
            onModeChanged = onModeChanged,
            onShuffle = onShuffle,
            selectedMode = selectedMode
        )
    }
}

@Composable
fun MotionPlayerPage(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    isCurrentSong: Boolean,
    onModeChanged: (String) -> Unit = {},
    onShuffle: () -> Unit = {},
    selectedMode: String = "Offline"
) {
    // --- State Setup ---

    // 1. Toggle for the global wallpaper blur (Top Right)
    var isGlobalBlurEnabled by remember { mutableStateOf(false) }

    // 2. Sensor Data for Parallax
    val sensorOffset by rememberParallaxSensorState()

    // 3. Animations
    // Smooth out the sensor data so the image "floats" rather than jitters
    val animatedParallax by animateOffsetAsState(
        targetValue = Offset(
            x = sensorOffset.x * 50f, // Sensitivity multiplier (Pixels per radian)
            y = sensorOffset.y * 50f
        ),
        animationSpec = spring(stiffness = Spring.StiffnessLow), // Low stiffness = "heavy/fluid" feel
        label = "Parallax"
    )

    // Smooth transition for the blur radius
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (isGlobalBlurEnabled) 30.dp else 0.dp,
        animationSpec = tween(500),
        label = "BlurRadius"
    )

    // Resolve Image Model (Album Art or Placeholder)
    val imageModel = song.albumArtUri // Fallback logic as needed

    // --- UI Composition ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Base color to prevent white flashes
            .clipToBounds()
    ) {
        // LAYER 1: The Main Motion Background
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                // Apply the toggleable blur here
                .blur(radius = animatedBlurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .graphicsLayer {
                    // Scale up (1.2x) to create a buffer zone for movement
                    scaleX = 1.2f
                    scaleY = 1.2f

                    // Apply the sensor translation
                    translationX = animatedParallax.x
                    translationY = animatedParallax.y
                }
        )

        // LAYER 2: The "Frosted Glass" Bottom Control Area
        // This ensures controls are readable regardless of the background status
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(350.dp) // Height of the control area
                .blur(radius = 50.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Color.Black.copy(alpha = 0.2f)) // Subtle tint
        ) {
            // We render the image *again* here but cropped to the bottom
            // This is the standard trick to create a "glass" effect over a dynamic background
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter, // Align bottom to match
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.5f) // Dim it slightly
            )
        }

        // LAYER 3: Dark Gradient Scrim (Bottom)
        // Adds contrast for white text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // LAYER 4: The Actual UI Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Title Area with Capsule Switch
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlassCapsuleSwitch(
                        selectedOption = selectedMode,
                        onOptionSelected = { onModeChanged(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing.times(1.2)
                    )
                }

                // Blur Toggle (Top Right)
                IconButton(
                    onClick = { isGlobalBlurEnabled = !isGlobalBlurEnabled },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isGlobalBlurEnabled) Icons.Default.BlurOn else Icons.Default.BlurOff,
                        contentDescription = "Toggle Blur",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Bottom Controls ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Song Info
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Slider / Seekbar
                Slider(
                    value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtils.formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = TimeUtils.formatTime(totalDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle - regenerates queue with new recommendations
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Default.Shuffle, null, tint = Color.White.copy(alpha = 0.7f))
                    }

                    // Previous
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Play/Pause (Prominent)
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black, // Dark icon on white button
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Repeat (Dummy)
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Repeat, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCapsuleSwitch(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf("Offline", "Online")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.2f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

