# Music Player Sliding Transition & Lifecycle Management Implementation Guide

## Overview

This guide documents the implementation of smooth sliding transitions between mini player and full screen player, plus automatic pause/resume functionality when activities change. Based on the MusicPlayer app's architecture.

## 🎯 Part 1: Sliding Transition Implementation

### Core Mechanism: AnchoredDraggable + Lerp Interpolation

#### 1. **Dependencies Required**

```kotlin
// In build.gradle.kts
dependencies {
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    // ... other compose dependencies
}
```

#### 2. **Player State Enum**

```kotlin
enum class PlayerState {
    Collapsed,  // Mini player at bottom
    Expanded    // Full screen player
}
```

#### 3. **AnchoredDraggable Setup**

```kotlin
@Composable
fun SlidingPlayer(
    modifier: Modifier = Modifier,
    content: @Composable (progress: Float) -> Unit  // Progress: 0.0 = collapsed, 1.0 = expanded
) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val miniPlayerHeightPx = with(density) { MINI_PLAYER_HEIGHT_DP.dp.toPx() }

    // Define snap points
    val collapsedOffset = screenHeightPx - miniPlayerHeightPx  // Bottom position
    val expandedOffset = 0f                                    // Top position

    val anchors = remember(screenHeightPx) {
        DraggableAnchors {
            PlayerState.Collapsed at collapsedOffset
            PlayerState.Expanded at expandedOffset
        }
    }

    // Core draggable state with physics
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = PlayerState.Collapsed,
            positionalThreshold = { distance -> distance * 0.3f },        // 30% drag threshold
            velocityThreshold = { with(density) { 200.dp.toPx() } },       // Velocity sensitivity
            snapAnimationSpec = spring(                                    // Smooth snap animation
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            decayAnimationSpec = exponentialDecay()                       // Physics-based deceleration
        )
    }

    // Update anchors when screen size changes
    LaunchedEffect(anchors) {
        anchoredDraggableState.updateAnchors(anchors)
    }

    // Calculate progress (0 = collapsed, 1 = expanded)
    val progress by remember {
        derivedStateOf {
            val offset = anchoredDraggableState.offset
            if (collapsedOffset == expandedOffset) 0f
            else 1f - (offset / collapsedOffset).coerceIn(0f, 1f)
        }
    }

    // Scrim overlay for background dimming
    val scrimAlpha = progress * 0.6f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(scrimAlpha)
            .background(Color.Black)
    )

    // Draggable player surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset {
                val safeOffset = anchoredDraggableState.offset
                IntOffset(0, safeOffset.toInt())
            }
            .anchoredDraggable(                    // The magic modifier
                state = anchoredDraggableState,
                orientation = Orientation.Vertical
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(
            topStart = lerp(16f, 0f, progress).dp,  // Morph corner radius
            topEnd = lerp(16f, 0f, progress).dp
        ),
        shadowElevation = 16.dp
    ) {
        content(progress)  // Pass progress to content for interpolation
    }
}
```

#### 4. **Lerp Interpolation for Smooth Morphing**

```kotlin
@Composable
fun UnifiedPlayerLayout(
    progress: Float,  // 0.0 to 1.0
    // ... other parameters
) {
    // Album art morphing
    val albumArtSize = lerp(MINI_ALBUM_ART_SIZE.value, FULL_ALBUM_ART_SIZE.value, progress).dp
    val albumArtX = lerp(miniAlbumX, fullAlbumX, progress)
    val albumArtY = lerp(miniAlbumY, fullAlbumY, progress)

    // Text positioning
    val textX = lerp(miniTextX, 24f, progress)
    val textY = lerp(miniTextY, fullTextY, progress)
    val textSize = lerp(14f, 22f, progress).sp

    // Alpha phasing for YouTube Music feel
    val miniControlsAlpha = (1f - (progress / 0.15f)).coerceIn(0f, 1f)    // Fade out early
    val fullControlsAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)  // Fade in late

    // Apply interpolated values to UI elements
    Box(modifier = Modifier.offset(x = albumArtX.dp, y = albumArtY.dp)) {
        // Album art with interpolated size and position
    }

    Text(
        modifier = Modifier.offset(x = textX.dp, y = textY.dp),
        fontSize = textSize,
        // ... other interpolated properties
    )
}
```

#### 5. **Key Implementation Steps**

1. **Setup AnchoredDraggable with proper anchors**
2. **Calculate progress from drag offset**
3. **Use lerp() for all visual interpolations**
4. **Implement alpha phasing for staggered reveals**
5. **Add physics-based snap animations**

---

## 🎵 Part 2: Audio Lifecycle Management (Pause/Resume)

### Core Mechanism: Media3 + MediaSession + Audio Focus

#### 1. **MediaService Setup**

```kotlin
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // Create ExoPlayer with audio focus handling
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true  // handleAudioFocus = true (AUTO)
            )
            .setHandleAudioBecomingNoisy(true)  // Auto-pause on headphone unplug
            .build()

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
```

#### 2. **AndroidManifest Configuration**

```xml
<manifest>
    <!-- Audio Permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- Media Playback Service -->
        <service
            android:name=".service.MusicService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

#### 3. **ViewModel MediaController Setup**

```kotlin
class MusicViewModel(context: Context) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        initializeController(context)
    }

    private fun initializeMediaController(context: Context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            updatePlaybackState()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Handle different playback states
            when (playbackState) {
                Player.STATE_READY -> { /* Ready to play */ }
                Player.STATE_ENDED -> { /* Playback ended */ }
                Player.STATE_BUFFERING -> { /* Buffering */ }
            }
        }
    }
}
```

#### 4. **Automatic Pause/Resume Triggers**

**Media3 handles these automatically when `handleAudioFocus = true`:**

1. **Phone Calls**: Audio focus lost → Auto-pause
2. **Other Apps**: Audio focus lost → Auto-pause
3. **Headphones Unplugged**: `setHandleAudioBecomingNoisy(true)` → Auto-pause
4. **Notifications**: Audio focus lost → Auto-pause
5. **Screen Off**: No automatic pause (configurable)

**Manual Lifecycle Management:**

```kotlin
// In Activity/Composable
DisposableEffect(Unit) {
    onDispose {
        // Media3 handles cleanup automatically via MediaSession
        // No manual pause needed - ExoPlayer manages audio focus
    }
}
```

---

## 🚀 Part 3: Complete Implementation Process

### Step-by-Step Guide for New App

#### **Phase 1: Project Setup**

1. **Add Dependencies** (build.gradle.kts):

```kotlin
dependencies {
    // Media3
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")

    // Compose Foundation (for AnchoredDraggable)
    implementation("androidx.compose.foundation:foundation:1.6.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
```

2. **Update AndroidManifest.xml**:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.MusicService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

#### **Phase 2: Core Architecture**

3. **Create Data Models**:

```kotlin
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?
)

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val progress: Float get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
}

enum class RepeatMode { OFF, ONE, ALL }
enum class PlayerState { Collapsed, Expanded }
```

4. **Implement MusicService**:

```kotlin
class MusicService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true  // handleAudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
```

5. **Create Repository**:

```kotlin
class MusicRepository(private val contentResolver: ContentResolver) {
    fun getAllSongs(): Flow<List<Song>> = flow {
        // MediaStore query implementation
        // Filter by MIME_TYPE LIKE 'audio/%'
    }.flowOn(Dispatchers.IO)
}
```

#### **Phase 3: Sliding Player Implementation**

6. **Create SlidingPlayer Component**:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidingPlayer(
    playbackState: PlaybackState,
    songs: List<Song>,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSongSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implement AnchoredDraggable setup as shown above
    // Use lerp interpolation for smooth transitions
    // Handle progress calculation and UI morphing
}
```

7. **Implement Unified Layout**:

```kotlin
@Composable
fun UnifiedPlayerLayout(
    progress: Float,
    miniControlsAlpha: Float,
    fullControlsAlpha: Float,
    // ... parameters
) {
    // Use lerp() for all size, position, and alpha interpolations
    // Implement mini player elements (fade out early)
    // Implement full screen elements (fade in late)
    // Handle album art morphing and text positioning
}
```

#### **Phase 4: ViewModel & UI Integration**

8. **Create MusicViewModel**:

```kotlin
class MusicViewModel(
    private val repository: MusicRepository,
    context: Context
) : ViewModel() {

    private var mediaController: MediaController? = null

    init {
        loadSongs()
        initializeMediaController(context)
    }

    private fun initializeMediaController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    fun playSong(song: Song) {
        // Load playlist and play selected song
    }

    fun togglePlayPause() {
        mediaController?.playWhenReady = !(mediaController?.playWhenReady ?: false)
    }
}
```

9. **Main Activity Setup**:

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels {
        MusicViewModel.Factory(repository = MusicRepository(contentResolver), context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Request permissions and setup Compose UI
    }
}
```

#### **Phase 5: Navigation & UI**

10. **Create Screen Navigation**:

```kotlin
enum class Screen { HOME, FULL_SCREEN, MOTION_PLAYER }

@Composable
fun MusicApp(viewModel: MusicViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(/* ... */)
        Screen.FULL_SCREEN -> FullScreen(/* ... */)
        Screen.MOTION_PLAYER -> MotionPlayerScreen(/* ... */)
    }

    // Navigation bar and sliding player logic
}
```

---

## 🎯 Key Implementation Notes

### **Sliding Transition Tips:**

- **Progress Calculation**: `progress = 1f - (offset / collapsedOffset)`
- **Lerp Everything**: Size, position, alpha, font size, corner radius
- **Alpha Phasing**: Mini controls fade at 0-15%, full controls at 85-100%
- **Physics**: Use spring animations with `Spring.DampingRatioNoBouncy`

### **Audio Lifecycle Tips:**

- **Media3 Handles Most**: Audio focus, noisy audio, phone calls
- **MediaSession Required**: For system integration and notifications
- **Foreground Service**: Required for background playback
- **No Manual Pause**: Let Media3 handle audio focus automatically

### **Performance Considerations:**

- Use `derivedStateOf` for progress calculations
- Implement proper state hoisting
- Use `remember` for expensive objects
- Handle screen rotation and configuration changes

---

## 📋 Checklist for Implementation

- [ ] Add Media3 dependencies
- [ ] Configure AndroidManifest permissions and service
- [ ] Create data models (Song, PlaybackState, enums)
- [ ] Implement MusicService with MediaSession
- [ ] Create MusicRepository with MediaStore queries
- [ ] Build SlidingPlayer with AnchoredDraggable
- [ ] Implement lerp interpolation for all UI elements
- [ ] Create MusicViewModel with MediaController
- [ ] Setup navigation between screens
- [ ] Add permission requests in MainActivity
- [ ] Test audio focus scenarios (calls, notifications, headphones)
- [ ] Test sliding transitions on different screen sizes

---

## 🔧 Troubleshooting

**Sliding not smooth?**

- Check lerp calculations
- Verify progress range (0.0 to 1.0)
- Ensure proper state hoisting

**Audio not pausing on calls?**

- Verify `handleAudioFocus = true` in ExoPlayer
- Check MediaSession setup
- Test with actual phone calls

**Service stops unexpectedly?**

- Check foreground service configuration
- Verify MediaSession lifecycle
- Add proper error handling

---

## 🎉 Success Metrics

- Smooth 60fps transitions between mini/full player
- Automatic pause on phone calls, notifications, headphone unplug
- Seamless resume when interruptions end
- Proper background playback with notifications
- Consistent behavior across different Android versions

---

**Use this guide to instruct AI assistants for implementing the same smooth sliding transitions and audio lifecycle management in any music player app.**</content>
<parameter name="filePath">e:\Git-Hub\MusicPlayer\MUSIC_PLAYER_IMPLEMENTATION_GUIDE.md
