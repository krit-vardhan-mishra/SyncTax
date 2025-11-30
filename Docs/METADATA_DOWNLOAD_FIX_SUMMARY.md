# Metadata & Cover Art Download Fix Summary

## Issues Identified

### 1. FFmpeg Initialization Failure
**Problem**: The logs showed `⚠️ Failed to initialize FFmpeg/YoutubeDL: failed to initialize`

**Root Cause**: 
- FFmpeg was being initialized inside the download coroutine
- The initialization was failing silently, causing all subsequent conversion attempts to fail
- Multiple conversion attempts (M4A, MP3, FLAC, OPUS) all failed because FFmpeg wasn't properly initialized

### 2. Metadata Not Embedded
**Problem**: Downloaded files had no metadata (title, artist, album) or cover art despite correct data being available in PlayerViewModel

**Root Cause**:
- FFmpeg initialization failure meant the conversion process never succeeded
- All FFmpeg commands returned non-zero exit codes
- Files remained as WebM without metadata embedding

### 3. No Download Cancellation
**Problem**: Users couldn't stop a download once started

## Solutions Implemented

### 1. FFmpeg Initialization Fix

**Changes in `NewPipeAudioDownloader.kt`**:

```kotlin
// OLD: Silent failure, unclear status
try {
    YoutubeDL.getInstance().init(context)
    FFmpeg.getInstance().init(context)
} catch (e: Exception) {
    Log.w(TAG, "⚠️ Failed to initialize FFmpeg/YoutubeDL: ${e.message}")
}

// NEW: Proper initialization with clear logging and state tracking
var ffmpegInitialized = false
try {
    Log.d(TAG, "🔧 Initializing YoutubeDL...")
    YoutubeDL.getInstance().init(context.applicationContext)
    Log.d(TAG, "✅ YoutubeDL initialized")
    
    Log.d(TAG, "🔧 Initializing FFmpeg...")
    FFmpeg.getInstance().init(context.applicationContext)
    ffmpegInitialized = true
    Log.d(TAG, "✅ FFmpeg initialized successfully")
} catch (e: Exception) {
    Log.e(TAG, "❌ Failed to initialize FFmpeg/YoutubeDL: ${e.message}", e)
    ffmpegInitialized = false
}
```

**Key Improvements**:
- Use `context.applicationContext` instead of `context` for proper lifecycle management
- Track initialization status with `ffmpegInitialized` flag
- Add detailed logging at each step
- Skip conversion if FFmpeg fails to initialize (keeps WebM instead of failing entirely)

### 2. FFmpeg Execution Simplification

**Changes in `runFfmpeg()` method**:

```kotlin
// OLD: Complex reflection-based approach trying multiple methods
var rc = -1
val execMethods = ffmpeg.javaClass.methods.filter { it.name == "execute" }
// ... complex reflection logic

// NEW: Direct, simple execution
val rc = try {
    // FFmpeg.execute() expects Array<String> and returns Int (0 = success)
    ffmpeg.execute(args)
} catch (e: Exception) {
    Log.e(TAG, "❌ FFmpeg execution error for ${t.desc}: ${e.message}", e)
    -1
}
```

**Key Improvements**:
- Removed complex reflection code that was trying multiple method overloads
- Direct call to `ffmpeg.execute(args)` which is the standard API
- Better error handling and logging
- Clearer success/failure detection
- Proper cleanup of failed output files

### 3. Download Cancellation Support

**Changes in `PlayerViewModel.kt`**:

```kotlin
// Track active download jobs
private val activeDownloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

fun downloadWithFormat(format: Format) {
    // Cancel existing download for this song if any
    activeDownloadJobs[currentSong.id]?.cancel()

    val downloadJob = viewModelScope.launch {
        try {
            // ... download logic
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.w("PlayerViewModel", "📥 Format Download: Download cancelled by user")
                _uiState.value = _uiState.value.copy(
                    downloadMessage = "Download cancelled"
                )
            }
        } finally {
            activeDownloadJobs.remove(currentSong.id)
        }
    }
    
    // Store the job for potential cancellation
    activeDownloadJobs[currentSong.id] = downloadJob
}

// New cancellation method
fun cancelDownload(songId: String) {
    activeDownloadJobs[songId]?.let { job ->
        Log.d("PlayerViewModel", "🛑 Cancelling download for song: $songId")
        job.cancel()
        activeDownloadJobs.remove(songId)
        
        // Update UI state
        _uiState.value = _uiState.value.copy(
            downloadingSongs = _uiState.value.downloadingSongs - songId,
            downloadProgress = _uiState.value.downloadProgress - songId,
            downloadMessage = "Download cancelled"
        )
    }
}
```

**Changes in `FullScreenPlayerContent.kt`**:

```kotlin
// Add dialog state
var showCancelDownloadDialog by remember { mutableStateOf(false) }

// Update download button click handler
AnimatedDownloadButton(
    onClick = {
        if (isDownloading) {
            // Show cancellation confirmation dialog
            showCancelDownloadDialog = true
        } else {
            // Start new download
            playerViewModel.startDownloadProcess()
        }
    }
)

// Add cancellation dialog
if (showCancelDownloadDialog) {
    AlertDialog(
        onDismissRequest = { showCancelDownloadDialog = false },
        title = { Text("Cancel Download") },
        text = { Text("Do you want to stop the current download?") },
        confirmButton = {
            TextButton(
                onClick = {
                    playerViewModel.cancelDownload(song.id)
                    showCancelDownloadDialog = false
                    // Show snackbar confirmation
                }
            ) {
                Text("Yes, Cancel")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showCancelDownloadDialog = false }
            ) {
                Text("No, Continue")
            }
        }
    )
}
```

## Expected Behavior After Fixes

### 1. FFmpeg Initialization
- ✅ Clear logging showing initialization status
- ✅ Proper error handling if initialization fails
- ✅ Graceful fallback (keep WebM) if conversion unavailable

### 2. Metadata Embedding
- ✅ Downloaded files include title, artist, and album metadata from PlayerViewModel
- ✅ Cover art embedded in M4A/MP3/FLAC formats
- ✅ Multiple format attempts (M4A → MP3 → FLAC → OPUS → WebM fallback)
- ✅ Detailed logging showing which format succeeded

### 3. Download Cancellation
- ✅ Clicking download button during active download shows confirmation dialog
- ✅ User can choose to cancel or continue
- ✅ Cancelled downloads clean up UI state and show snackbar
- ✅ Download progress and status properly reset

## Testing Checklist

### FFmpeg & Metadata
- [ ] Start a download and check logs for "✅ FFmpeg initialized successfully"
- [ ] Verify FFmpeg commands execute without errors
- [ ] Check downloaded file has correct metadata (title, artist, album)
- [ ] Verify cover art is embedded in the audio file
- [ ] Test with multiple songs to ensure consistency

### Download Cancellation
- [ ] Start a download
- [ ] Click download button again during download
- [ ] Verify confirmation dialog appears
- [ ] Click "Yes, Cancel" and verify download stops
- [ ] Check that UI state resets (progress bar disappears)
- [ ] Verify snackbar shows "Download cancelled"
- [ ] Start new download after cancellation to ensure it works

## Files Modified

1. **NewPipeAudioDownloader.kt**
   - Improved FFmpeg initialization with better error handling
   - Simplified FFmpeg execution logic
   - Added ffmpegInitialized flag to track initialization status
   - Better logging throughout the conversion process

2. **PlayerViewModel.kt**
   - Added `activeDownloadJobs` map to track download coroutines
   - Modified `downloadWithFormat()` to store and manage download jobs
   - Added `cancelDownload()` method for cancellation
   - Added `isDownloading()` check method
   - Proper cleanup in finally block

3. **FullScreenPlayerContent.kt**
   - Added `showCancelDownloadDialog` state
   - Updated download button click handler to check if already downloading
   - Added cancellation confirmation AlertDialog
   - Imported TextButton for dialog buttons

## Log Output to Watch For

### Success Case:
```
🔧 Initializing YoutubeDL...
✅ YoutubeDL initialized
🔧 Initializing FFmpeg...
✅ FFmpeg initialized successfully
🔧 FFmpeg try .m4a (AAC + cover): [command]
🔧 FFmpeg .m4a (AAC + cover) return code: 0
✅ FFmpeg .m4a (AAC + cover) succeeded! Output: 3456KB
✅ Converted and embedded successfully: /path/to/file.m4a
```

### Failure Case (Graceful):
```
❌ Failed to initialize FFmpeg/YoutubeDL: [error details]
⚠️ FFmpeg not initialized - will keep WebM format without conversion
⚠️ Skipping conversion - FFmpeg not available
⚠️ Conversion/embedding failed, keeping original WebM: /path/to/file.webm
```

### Cancellation Case:
```
🛑 Cancelling download for song: online:videoId
📥 Format Download: Download cancelled by user
```

## Notes

- The fix maintains backward compatibility - if FFmpeg fails to initialize, files are still downloaded as WebM
- Metadata from yt-dlp is prioritized (as shown in PlayerViewModel logs), which should match what user sees in UI
- Download cancellation is cooperative - it cancels the coroutine but doesn't interrupt native FFmpeg processes that are already running
- The cancellation dialog prevents accidental cancellation while still allowing users to stop unwanted downloads

---

## Updates (November 2025)

### Current Implementation

#### Metadata Embedding (Mutagen)
- **FFmpeg removed from project** (saves ~136 MB APK size)
- **Mutagen (Python)** used for all metadata embedding
- Works natively with M4A and MP3 formats
- No external binary dependencies

#### Thumbnail Handling
```python
# yt-dlp downloads thumbnail
writethumbnail = True

# Mutagen embeds thumbnail into audio file
# Cleanup removes leftover files:
for ext in ['.jpg', '.webp', '.png', '.jpeg', '.jpg.webp']:
    thumbnail_path = base_path + ext
    if os.path.exists(thumbnail_path):
        os.remove(thumbnail_path)
```

#### Removed Components
- `ffmpeg-kit` library (not needed)
- `youtubedl-android:ffmpeg` (not needed)
- FFmpeg initialization code
- FFmpeg conversion attempts

#### Current Dependencies
```kotlin
// Chaquopy pip packages
pip.install("yt-dlp==2025.11.12")
pip.install("mutagen")  // For metadata embedding
pip.install("requests")
pip.install("urllib3")
pip.install("ytmusicapi")
```

### Benefits
- ✅ Smaller APK size (~136 MB vs ~272 MB with FFmpeg)
- ✅ Simpler download process
- ✅ No native library management
- ✅ Works on all ARM devices
- ✅ Reliable metadata embedding

---

*Last Updated: November 30, 2025*
