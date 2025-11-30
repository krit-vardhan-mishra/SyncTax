# Up Next Queue and Centralized Playback Control - Implementation Documentation

## Overview

This document describes the enhanced "Up Next" queue behavior and centralized playback control system implemented for the SyncTax music player application.

**Last Updated**: November 30, 2025  
**Status**: ✅ Production Ready

## Architecture Changes

### 1. **QueueManager** - Centralized Queue Management
**File**: `app/src/main/java/com/just_for_fun/synctax/core/player/QueueManager.kt`

The `QueueManager` is a centralized component that manages all queue operations:

#### Key Features:
- **Queue State Management**: Maintains current playlist, current index, and play history
- **Dynamic Queue Operations**:
  - Add songs to queue
  - Remove songs from queue
  - Reorder queue (drag and drop support)
  - Play song from queue (removes all songs before it)
  - Place song next (Play Next functionality)
- **Smart Queue Refilling**: Automatically refills queue when songs run out
  - Uses recommendation system to generate similar songs
  - Fallback to genre/artist similarity
  - Last resort: random songs

#### Queue Refill Behavior:
```kotlin
// When queue reaches end, automatically refills with:
1. AI-recommended songs based on last played song (primary)
2. Songs from same genre/artist (fallback)
3. Random songs from library (last resort)
```

#### Queue State:
```kotlin
data class QueueState(
    val currentPlaylist: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val playHistory: List<Song> = emptyList()
)
```

### 2. **PlaybackEventBus** - Global Event System
**File**: `app/src/main/java/com/just_for_fun/synctax/core/player/PlaybackEventBus.kt`

A global event bus for broadcasting playback changes across the application.

#### Supported Events:
- `SongChanged`: When a new song starts playing
- `PlaybackStateChanged`: When play/pause state changes
- `QueueUpdated`: When queue is modified
- `SongPlacedNext`: When song is moved to Play Next
- `SongRemovedFromQueue`: When song is removed
- `QueueReordered`: When queue is reordered (drag & drop)
- `QueueShuffled`: When queue is shuffled
- `QueueRefilled`: When queue is auto-refilled
- `PositionChanged`: When playback position changes
- `VolumeChanged`: When volume changes

#### Usage Example:
```kotlin
// Observing events in a component
viewModelScope.launch {
    PlaybackEventBus.events.collect { event ->
        when (event) {
            is PlaybackEvent.SongChanged -> {
                // Update UI with new song
            }
            is PlaybackEvent.QueueUpdated -> {
                // Refresh queue display
            }
            // Handle other events...
        }
    }
}
```

### 3. **Enhanced PlayerViewModel**
**File**: `app/src/main/java/com/just_for_fun/synctax/ui/viewmodels/PlayerViewModel.kt`

#### Integration Changes:
- Uses `QueueManager` for all queue operations
- Emits `PlaybackEvent`s for global synchronization
- Automatic queue refill on song end
- Smart "Play from Queue" functionality

#### New/Updated Methods:

##### `playFromQueue(song: Song)`
Plays a song from the middle of the queue:
- Removes all songs before the selected song
- Adds removed songs to history
- Makes selected song current
- Continues playback from that point

##### `next()` & `previous()`
Enhanced with auto-refill:
```kotlin
fun next() {
    val nextSong = queueManager.moveToNext(autoRefill = true)
    if (nextSong != null) {
        // Play next song
    } else {
        // Queue exhausted and refill failed
    }
}
```

##### `onSongEnded()`
Automatically handles song completion:
- If repeat enabled: replays current song
- Otherwise: moves to next song with auto-refill
- If queue empty after refill: stops playback

## Behavioral Specifications

### 1. **Up Next Queue Behavior**

#### A. Song Completion
```
Current Queue: [Song A (playing), Song B, Song C]

After Song A completes:
→ Queue: [Song B (playing), Song C]
→ History: [Song A]
→ Auto-play: Song B starts
```

#### B. Playing from Middle of Queue
```
Current Queue: [Song A (playing), Song B, Song C, Song D]
User taps Song C:

→ Queue: [Song C (playing), Song D]
→ History: [Song A, Song B]
→ Songs A and B are removed from queue
```

#### C. Play Next Functionality
```
Current Queue: [Song A (playing), Song B, Song C]
User selects "Play Next" on Song D:

→ Queue: [Song A (playing), Song D, Song B, Song C]
→ Song D is inserted right after current song
```

#### D. Queue Refill on Depletion
```
Current Queue: [Song A (playing)]
Song A completes and no next song:

1. Generate recommendations based on Song A
2. If recommendations available:
   → Queue: [Song B, Song C, Song D, ...] (recommended)
3. If no recommendations:
   → Find similar songs (same genre/artist)
4. If no similar songs:
   → Add random songs
5. Start playing first refilled song
```

### 2. **Global Playback Control**

#### Service Architecture
```
MusicService (Foreground Service)
    ↓ Bound to
PlayerViewModel (Playback Logic)
    ↓ Uses
QueueManager (Queue State)
    ↓ Uses
MusicPlayer (ExoPlayer Wrapper)
```

#### Communication Flow
```
User Action (UI)
    ↓
PlayerViewModel method
    ↓
QueueManager operation
    ↓
PlaybackEventBus.emit(event)
    ↓
All subscribed components update
```

### 3. **Edge Case Handling**

#### Empty Queue
- If queue becomes empty, auto-refill is triggered
- If refill fails, playback stops gracefully
- User can manually add songs or shuffle all songs

#### Removing Current Song
```
Current Queue: [Song A (playing), Song B, Song C]
User removes Song A:

→ Playback stops
→ Queue: [Song B, Song C]
→ User must manually select next song
```

#### Shuffle Behavior
```
Current Queue: [Song A (playing), Song B, Song C, Song D]
User enables shuffle:

→ Queue: [Song A (playing), Song D, Song B, Song C] (shuffled)
→ Current song stays at position 0
→ Other songs are randomly ordered
```

## UI Integration

### Current Integration Points

#### 1. **MainActivity.kt**
- Updated `onSelectSong` callback to use `playFromQueue()`
- All queue operations routed through PlayerViewModel

#### 2. **UpNextSheet.kt**
- Displays upcoming queue and history
- Supports drag-and-drop reordering
- Swipe actions for Play Next and Remove

#### 3. **PlayerBottomSheet.kt**
- Shows mini player and full player
- Real-time queue updates

## Testing Scenarios

### Test 1: Song Completion Flow
1. Play a song with multiple songs in queue
2. Let it complete naturally
3. ✓ Next song should start automatically
4. ✓ Completed song should appear in history

### Test 2: Play from Middle
1. Open Up Next sheet
2. Tap on 3rd song in queue
3. ✓ Song should start playing immediately
4. ✓ Songs before it should move to history
5. ✓ Queue should update correctly

### Test 3: Play Next
1. Play a song
2. Select another song and choose "Play Next"
3. ✓ Song should appear right after current
4. ✓ Skip to next: selected song should play

### Test 4: Queue Refill
1. Play last song in queue
2. Let it complete
3. ✓ Queue should refill with recommended songs
4. ✓ Next song should start playing

### Test 5: Remove from Queue
1. Open Up Next sheet
2. Swipe to remove a song
3. ✓ Song should be removed
4. ✓ Queue should update
5. ✓ Notification shows removal

### Test 6: Reorder Queue
1. Open Up Next sheet
2. Long-press and drag a song
3. ✓ Song should move to new position
4. ✓ Queue order should update

### Test 7: Shuffle
1. Play a song with queue
2. Enable shuffle
3. ✓ Queue should shuffle
4. ✓ Current song stays at front
5. ✓ Order updates in Up Next

### Test 8: Empty Queue Handling
1. Play single song
2. Let it complete
3. ✓ Should attempt refill
4. ✓ If refill fails, playback stops gracefully

## API Reference

### QueueManager

```kotlin
// Initialize queue
fun initializeQueue(playlist: List<Song>, startIndex: Int)

// Get current song
fun getCurrentSong(): Song?

// Navigation
suspend fun moveToNext(autoRefill: Boolean = true): Song?
fun moveToPrevious(): Song?

// Queue operations
fun playFromQueue(song: Song): Song?
fun placeNext(song: Song)
fun removeFromQueue(song: Song)
fun reorderQueue(fromIndex: Int, toIndex: Int)
fun shuffle()
fun addToQueue(songs: List<Song>)
fun clearQueue()

// Queue state
fun getUpcomingQueue(): List<Song>
fun getPlayHistory(): List<Song>
fun shouldRefillQueue(threshold: Int = 3): Boolean
```

### PlaybackEventBus

```kotlin
// Observe events
PlaybackEventBus.events.collect { event -> /* handle */ }

// Emit events
suspend fun emit(event: PlaybackEvent)
fun tryEmit(event: PlaybackEvent): Boolean
```

### PlayerViewModel (New/Updated Methods)

```kotlin
// Queue operations
fun playFromQueue(song: Song)
fun placeNext(song: Song)
fun removeFromQueue(song: Song)
fun reorderQueue(fromIndex: Int, toIndex: Int)

// Playback control
fun playSong(song: Song, playlist: List<Song>)
fun next()
fun previous()
fun togglePlayPause()
fun toggleShuffle()
fun toggleRepeat()

// Queue access
fun getUpcomingQueue(): List<Song>
fun getPlayHistory(): List<Song>
fun getQueueState(): StateFlow<QueueState>
```

## Performance Considerations

### 1. **Queue Refill**
- Runs asynchronously using coroutines
- Doesn't block UI or playback
- Falls back to simpler methods if recommendation fails

### 2. **Event Bus**
- Uses SharedFlow with limited buffer
- Non-blocking emission with `tryEmit()`
- Automatic cleanup when collectors are removed

### 3. **State Management**
- Single source of truth (QueueManager)
- Minimal state copies
- Efficient list operations

## Future Enhancements

### Potential Improvements:
1. **Smart Queue Prefetching**: Pre-load next 3 songs for instant playback
2. **Queue Persistence**: Save queue state across app restarts
3. **Multiple Queue Modes**: Different refill strategies (radio, album, etc.)
4. **Queue Analytics**: Track queue interaction patterns
5. **Collaborative Queues**: Share queue with friends
6. **Queue History**: View and restore previous queue states

## Troubleshooting

### Queue not refilling:
- Check recommendation system is trained
- Verify songs exist in database
- Check coroutine scope is active

### Events not received:
- Ensure collector is active (viewModelScope)
- Check event subscription lifecycle
- Verify events are being emitted

### Queue state desync:
- Use QueueManager as single source of truth
- Don't manipulate queue directly in PlayerViewModel
- Always emit events after queue changes

## Migration Guide

### For Developers:

#### Before:
```kotlin
// Old approach
playerViewModel.playSong(song)  // Playing any song
```

#### After:
```kotlin
// New approach
playerViewModel.playSong(song, playlist)  // Playing with playlist
playerViewModel.playFromQueue(song)  // Playing from queue
```

#### Event Subscription:
```kotlin
// In your composable or fragment
LaunchedEffect(Unit) {
    PlaybackEventBus.events.collect { event ->
        when (event) {
            is PlaybackEvent.QueueUpdated -> {
                // Refresh your queue UI
            }
            // Handle other events
        }
    }
}
```

## Conclusion

This implementation provides a robust, centralized playback control system with intelligent queue management. The architecture ensures:
- ✅ Global synchronization across app components
- ✅ Smart queue refilling based on user preferences
- ✅ Flexible queue manipulation (Play Next, Remove, Reorder)
- ✅ Seamless playback experience
- ✅ Real-time UI updates via event bus

For questions or issues, refer to the source files or check the test scenarios above.
