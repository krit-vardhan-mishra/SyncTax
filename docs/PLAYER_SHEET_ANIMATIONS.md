# Player Sheet Animations

This document describes the animated player sheet transition system integrated into SyncTax, inspired by PixelPlay's smooth animation system.

## Overview

The player sheet provides smooth, animated transitions between two states:
- **COLLAPSED** (Miniplayer): Compact 80dp bar at the bottom of the screen
- **EXPANDED** (Fullscreen): Full-screen player taking up the entire display

## Features

### 1. Spring Animations
- Natural bouncy feel with medium damping ratio
- Responsive transitions with medium stiffness
- Smooth interpolation between states

### 2. Alpha Blending
- Fade-out animation for collapsing content (200ms)
- Fade-in animation for expanding content (200ms + 100ms delay)
- Prevents content overlap during transitions

### 3. Gesture Handling
- **Vertical Drag**: Swipe up to expand, swipe down to collapse
- **Tap**: Tap mini player to expand
- **Drag Threshold**: 200 pixels to trigger state change
- **Haptic Feedback**: Tactile response on all interactions

### 4. Dynamic Background Effects
- **Blur**: Interpolates from 20dp (mini) to 50dp (expanded)
- **Opacity**: Increases from 0.5 to 0.7 for better readability
- **Scale**: Subtle zoom effect (1.0 → 1.05) for depth
- **Gradient**: Intensifies with expansion for better contrast

### 5. Predictive Back Support
- System back button collapses player when expanded
- Prevents accidental app exits
- Includes haptic feedback

## Architecture

### State Management

#### PlayerSheetState Enum
```kotlin
enum class PlayerSheetState {
    COLLAPSED,  // Miniplayer mode
    EXPANDED    // Fullscreen mode
}
```

#### PlayerViewModel Integration
```kotlin
// Expand to fullscreen
playerViewModel.expandPlayerSheet()

// Collapse to miniplayer
playerViewModel.collapsePlayerSheet()

// Toggle between states
playerViewModel.togglePlayerSheet()

// Access current state
val playerState = playerViewModel.uiState.collectAsState()
val sheetState = playerState.playerSheetState
```

## Usage

### Option 1: Enhanced UnifiedPlayer (Recommended)

The existing `UnifiedPlayer` composable has been enhanced with new animations while maintaining backward compatibility:

```kotlin
UnifiedPlayer(
    song = currentSong,
    isPlaying = isPlaying,
    isBuffering = isBuffering,
    position = position,
    duration = duration,
    shuffleEnabled = shuffleEnabled,
    repeatEnabled = repeatEnabled,
    volume = volume,
    upNext = upcomingQueue,
    playHistory = playHistory,
    isExpanded = isExpanded,  // Boolean state
    onExpandedChange = { expanded ->
        // Handle state change
        isExpanded = expanded
    },
    // ... other callbacks
)
```

### Option 2: UnifiedPlayerSheet (New Standalone)

The new `UnifiedPlayerSheet` composable provides advanced features with direct ViewModel integration:

```kotlin
UnifiedPlayerSheet(
    song = currentSong,
    isPlaying = isPlaying,
    isBuffering = isBuffering,
    position = position,
    duration = duration,
    shuffleEnabled = shuffleEnabled,
    repeatEnabled = repeatEnabled,
    volume = volume,
    upNext = upcomingQueue,
    playHistory = playHistory,
    playerSheetState = playerState.playerSheetState,  // Enum state
    onPlayerSheetStateChange = { newState ->
        when (newState) {
            PlayerSheetState.EXPANDED -> playerViewModel.expandPlayerSheet()
            PlayerSheetState.COLLAPSED -> playerViewModel.collapsePlayerSheet()
        }
    },
    // ... other callbacks
)
```

## Animation Details

### Expansion Fraction
The core animation parameter that drives all transitions:
- **Value**: 0.0 (collapsed) to 1.0 (expanded)
- **Spring Config**: DampingRatioMediumBouncy + StiffnessMedium
- **Duration**: ~300ms with natural overshoot

### Height Interpolation
Dynamic height calculation based on expansion fraction:
```kotlin
height = 80.dp + (expansionFraction * 920.dp)
// Collapsed: 80dp
// Expanded: 1000dp
```

### Alpha Blending
Prevents visual overlap during transitions:
```kotlin
miniPlayerAlpha = 1.0 - expansionFraction  // Fade out when expanding
expandedAlpha = expansionFraction          // Fade in when expanding (with delay)
```

### Background Blur
Creates depth and focuses attention:
```kotlin
blurRadius = 20.dp + (expansionFraction * 30.dp)
// Collapsed: 20dp (texture visible)
// Expanded: 50dp (ambient blur)
```

## Performance Considerations

1. **Alpha Cutoff**: Content with alpha < 0.01 is not rendered
2. **Spring Animations**: GPU-accelerated for smooth 60fps
3. **Lazy Composition**: Only active content is composed
4. **Gesture Throttling**: 200px threshold prevents excessive recompositions

## Customization

### Animation Timing
Modify spring parameters in `UnifiedPlayer.kt`:
```kotlin
animationSpec = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,  // Adjust bounce
    stiffness = Spring.StiffnessMedium                // Adjust speed
)
```

### Drag Threshold
Adjust sensitivity in gesture detection:
```kotlin
val dragThreshold = 200f  // Pixels required to trigger
```

### Blur Intensity
Modify dynamic blur calculation:
```kotlin
val blurRadius = (20 + (expansionProgress * 30)).dp  // Min 20dp, Max 50dp
```

## Troubleshooting

### Issue: Choppy animations
**Solution**: Ensure device has sufficient performance and reduce blur radius

### Issue: Gesture conflicts
**Solution**: Increase drag threshold or adjust gesture detection priority

### Issue: Content flickering
**Solution**: Verify alpha blending cutoffs and animation timing

### Issue: Back button not working
**Solution**: Check BackHandler is enabled when expanded

## Migration Guide

### From Boolean State to Enum State

**Before:**
```kotlin
var isExpanded by remember { mutableStateOf(false) }
```

**After:**
```kotlin
val playerState by playerViewModel.uiState.collectAsState()
val isExpanded = playerState.playerSheetState == PlayerSheetState.EXPANDED
```

### From Manual State to ViewModel State

**Before:**
```kotlin
onExpandedChange = { isExpanded = it }
```

**After:**
```kotlin
onPlayerSheetStateChange = { newState ->
    if (newState == PlayerSheetState.EXPANDED) {
        playerViewModel.expandPlayerSheet()
    } else {
        playerViewModel.collapsePlayerSheet()
    }
}
```

## Best Practices

1. **State Management**: Use ViewModel state for persistence across configuration changes
2. **Haptic Feedback**: Always provide haptic feedback for gesture interactions
3. **Alpha Cutoff**: Use 0.01f threshold to avoid rendering invisible content
4. **Performance**: Profile animations on target devices to ensure 60fps
5. **Accessibility**: Ensure keyboard/TalkBack users can expand/collapse player

## Examples

### Example 1: Basic Implementation
```kotlin
@Composable
fun PlayerScreen(playerViewModel: PlayerViewModel) {
    val playerState by playerViewModel.uiState.collectAsState()
    
    if (playerState.currentSong != null) {
        UnifiedPlayer(
            song = playerState.currentSong!!,
            isPlaying = playerState.isPlaying,
            // ... other parameters
            isExpanded = playerState.playerSheetState == PlayerSheetState.EXPANDED,
            onExpandedChange = { expanded ->
                if (expanded) {
                    playerViewModel.expandPlayerSheet()
                } else {
                    playerViewModel.collapsePlayerSheet()
                }
            }
        )
    }
}
```

### Example 2: Advanced with Custom Gestures
```kotlin
@Composable
fun CustomPlayerSheet() {
    var customDragOffset by remember { mutableFloatStateOf(0f) }
    
    UnifiedPlayerSheet(
        // ... parameters
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    customDragOffset += dragAmount.y
                    // Custom drag logic
                }
            }
    )
}
```

## References

- Source: `presentation/components/player/UnifiedPlayer.kt`
- Source: `presentation/components/player/UnifiedPlayerSheet.kt`
- State: `presentation/components/state/PlayerSheetState.kt`
- ViewModel: `presentation/viewmodels/PlayerViewModel.kt`

## Contributing

When modifying animations:
1. Test on various screen sizes (phone, tablet, foldable)
2. Profile performance with GPU rendering profiler
3. Verify haptic feedback on physical devices
4. Test with TalkBack enabled
5. Document any breaking changes
