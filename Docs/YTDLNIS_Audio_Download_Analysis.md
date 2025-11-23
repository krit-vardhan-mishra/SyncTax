# YTDLNIS Audio Download Analysis

## Overview
This document analyzes how the ytdlnis application downloads YouTube and YouTube Music files as audio files, shows format options to users, and handles quality/format selection.

---

## Core Components

### 1. **Format Data Model** ([Format.kt](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/database/models/Format.kt))

The `Format` data class represents audio/video format information:

```kotlin
data class Format(
    var format_id: String = "",           // Format identifier (e.g., "140", "251")
    var container: String = "",            // File extension (mp3, m4a, webm, etc.)
    var vcodec: String = "",              // Video codec (empty for audio-only)
    var acodec: String = "",              // Audio codec (opus, aac, mp3, etc.)
    var encoding: String = "",
    var filesize: Long = 0,               // File size in bytes
    var format_note: String = "",         // Quality description (e.g., "medium AUDIO")
    var fps: String? = "",
    var asr: String? = "",                // Audio sample rate
    var url: String? = "",                // Direct download URL
    var lang: String? = "",               // Language/locale
    var tbr: String? = ""                 // Total bitrate
)
```

---

## 2. Format Fetching & Parsing

### **YTDLPUtil.kt** - Core Download Logic

#### **Getting Available Formats**
[YTDLPUtil.kt:460-480](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/extractors/ytdlp/YTDLPUtil.kt#L460-L480)

```kotlin
fun getFormats(url: String) : List<Format> {
    val request = YoutubeDLRequest(url)
    request.addOption("--print", "%(formats)s")
    request.addOption("--print", "%(duration)s")
    request.applyDefaultOptionsForFetchingData(url)
    
    if (url.isYoutubeURL()) {
        request.setYoutubeExtractorArgs(url)
    }
    
    val res = YoutubeDL.getInstance().execute(request)
    val json = results[0]
    val jsonArray = JSONArray(json)
    
    return parseYTDLFormats(jsonArray)
}
```

#### **Parsing Format Data**
[YTDLPUtil.kt:482-543](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/extractors/ytdlp/YTDLPUtil.kt#L482-L543)

The `parseYTDLFormats()` function:
- Iterates through JSON array from yt-dlp
- Converts each format to a `Format` object using Gson
- Processes format notes (e.g., "medium AUDIO", "1080p")
- Filters out storyboard formats
- Adds resolution info to format notes

---

## 3. Format Selection & Sorting

### **FormatUtil.kt** - Format Prioritization Logic

This class handles how formats are sorted and selected based on user preferences.

#### **Audio Format Sorting Criteria** ([FormatUtil.kt:98-186](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/extractors/ytdlp/FormatUtil.kt#L98-L186))

Formats are sorted based on:
1. **Format ID** - Preferred format IDs from settings
2. **Language** - User's preferred audio language
3. **Codec** - Preferred audio codec (opus, aac, mp3, etc.)
4. **Container** - Preferred file format (m4a, webm, mp3, etc.)
5. **File Size** - Largest or smallest based on preference
6. **No DRC** - Avoids dynamic range compression

```kotlin
fun sortAudioFormats(formats: List<Format>) : List<Format> {
    val orderPreferences = getAudioFormatImportance()
    
    val fieldSorter: Comparator<Format> = object : Comparator<Format> {
        override fun compare(a: Format, b: Format): Int {
            for (order in orderPreferences) {
                val comparison = when (order) {
                    "smallsize" -> a.filesize.compareTo(b.filesize)
                    "file_size" -> b.filesize.compareTo(a.filesize)
                    "id" -> audioFormatIDPreference.contains(b.format_id).compareTo(...)
                    "language" -> compareLanguage(a, b)
                    "codec" -> compareCodec(a, b)
                    "container" -> compareContainer(a, b)
                    "no_drc" -> compareDRC(a, b)
                    else -> 0
                }
                if (comparison != 0) return comparison
            }
            return 0
        }
    }
    return formats.sortedWith(fieldSorter)
}
```

#### **Generic Formats** ([FormatUtil.kt:271-289](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/extractors/ytdlp/FormatUtil.kt#L271-L289))

When specific formats aren't available, generic formats are provided:
- Best Audio
- Worst Audio
- Format ID preferences from settings

---

## 4. User Interface - Format Selection

### **FormatViewModel.kt** - Format State Management

[FormatViewModel.kt:43-268](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/database/viewmodel/FormatViewModel.kt#L43-L268)

Manages format display with:
- **Filter Categories**:
  - `ALL` - Show all available formats
  - `SUGGESTED` - Sorted by user preferences
  - `SMALLEST` - Filter smallest file per quality
  - `GENERIC` - Generic best/worst options

- **Sorting Options**:
  - `filesize` - By file size
  - `container` - Group by file format
  - `codec` - Group by codec
  - `id` - By format ID

```kotlin
// Filter and sort formats
when(filterBy.value) {
    FormatCategory.SUGGESTED -> {
        finalFormats = formatUtil.sortAudioFormats(finalFormats)
    }
    FormatCategory.SMALLEST -> {
        // Filter to show only smallest file per quality level
        finalFormats = finalFormats
            .groupBy { it.format_note }
            .map { it.value.minBy { it.filesize } }
    }
    FormatCategory.ALL -> {
        // Show all formats
    }
    FormatCategory.GENERIC -> {
        // Show generic best/worst options
    }
}
```

### **FormatAdapter.kt** - RecyclerView Adapter

[FormatAdapter.kt:24-145](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/ui/adapter/FormatAdapter.kt#L24-L145)

- Displays format cards in a list
- Shows format details (quality, codec, container, size)
- Handles format selection
- Supports multi-select for video + audio tracks

### **FormatSelectionBottomSheetDialog.kt** - Format Selection UI

[FormatSelectionBottomSheetDialog.kt](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/ui/downloadcard/FormatSelectionBottomSheetDialog.kt)

The bottom sheet dialog provides:

1. **Format List Display**
   - RecyclerView with format cards
   - Shows quality, codec, container, and file size
   - Long-press for detailed format info

2. **Filter Button** - Opens filter options:
   - ALL formats
   - SUGGESTED (sorted by preferences)
   - SMALLEST (best quality-to-size ratio)
   - GENERIC (basic best/worst options)

3. **Refresh Button** - Re-fetches formats from yt-dlp

4. **Format Source Switching** (YouTube only):
   - yt-dlp (default)
   - NewPipe extractor (alternative)

---

## 5. Download Process

### **DownloadWorker.kt** - Background Download Execution

[DownloadWorker.kt:50-438](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/work/DownloadWorker.kt#L50-L438)

The download worker:
1. **Builds yt-dlp command** from selected format
2. **Executes download** in background
3. **Tracks progress** via notifications
4. **Moves file** to selected directory
5. **Adds to history** with metadata

```kotlin
// Build yt-dlp request with selected format
val request = ytdlpUtil.buildYoutubeDLRequest(downloadItem)

// Execute download
YoutubeDL.getInstance().execute(request, downloadItem.id.toString()) { 
    progress, _, line ->
    // Update notification
    notificationUtil.updateDownloadNotification(
        downloadItem.id.toInt(),
        line, progress.toInt(), 0, title,
        NotificationUtil.DOWNLOAD_SERVICE_CHANNEL_ID
    )
}

// Move file to download directory
finalPaths = FileUtil.moveFile(
    tempFileDir.absoluteFile,
    context, 
    downloadLocation, 
    keepCache
)

// Add to history
val historyItem = HistoryItem(
    url = downloadItem.url,
    title = downloadItem.title,
    format = downloadItem.format,
    downloadPaths = finalPaths
)
historyDao.insert(historyItem)
```

### **YTDLPUtil.buildYoutubeDLRequest()** - Command Construction

[YTDLPUtil.kt:747+](file:///e:/Git-Hub/SnycTax/ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/extractors/ytdlp/YTDLPUtil.kt#L747)

Builds the yt-dlp command with:
- Selected format ID (`-f <format_id>`)
- Container/codec preferences
- Audio quality settings (bitrate, sample rate)
- Output template
- Metadata options (embed thumbnail, etc.)

---

## 6. Audio Download Flow

### Step-by-Step Process:

1. **User enters URL** in DownloadAudioFragment
   
2. **Fetch metadata**:
   ```kotlin
   val results = ytdlpUtil.getFromYTDL(url)
   // Returns: title, author, duration, thumbnail, available formats
   ```

3. **Parse available formats**:
   ```kotlin
   val audioFormats = formats.filter { 
       it.format_note.contains("audio", ignoreCase = true) 
   }
   ```

4. **Sort formats** by user preferences:
   ```kotlin
   val sortedFormats = formatUtil.sortAudioFormats(audioFormats)
   val bestFormat = sortedFormats.first()
   ```

5. **Display format card** to user:
   - Quality (e.g., "medium AUDIO")
   - Codec (e.g., "opus")
   - Container (e.g., "webm")
   - File size

6. **User can click** to open format selection dialog:
   - Browse all available formats
   - Filter by category (ALL/SUGGESTED/SMALLEST/GENERIC)
   - Sort by size/codec/container
   - Switch format source (yt-dlp/NewPipe)

7. **User selects format** and initiates download:
   ```kotlin
   downloadViewModel.queueDownload(downloadItem)
   ```

8. **DownloadWorker executes**:
   - Builds yt-dlp command
   - Downloads to cache directory
   - Moves to user's selected folder
   - Adds to download history
   - Shows completion notification

---

## Key Features

### Format Display to User:
- **Format Card** shows selected format with:
  - Quality label (e.g., "medium AUDIO")
  - Codec (e.g., "opus", "aac")
  - Container (e.g., "m4a", "webm", "mp3")
  - File size
  - Bitrate

### Format Selection Options:
- **Filter Categories**:
  - ALL - All available formats
  - SUGGESTED - Auto-sorted by preferences
  - SMALLEST - Best compression
  - GENERIC - Simple best/worst

- **Sorting Methods**:
  - By file size
  - By container type
  - By codec
  - By format ID

### Smart Format Selection:
- Uses user preferences from settings
- Prioritizes preferred codecs/containers
- Considers file size preferences
- Respects language preferences
- Avoids DRC (Dynamic Range Compression) if configured

### Download Capabilities:
- Queue multiple downloads
- Background download with notifications
- Progress tracking
- Automatic metadata embedding
- Thumbnail embedding
- Chapter splitting
- SponsorBlock integration
- Custom filename templates

---

## Configuration Options

Users can configure:
- **Preferred audio codec** (opus, aac, mp3, etc.)
- **Preferred container** (m4a, webm, mp3, etc.)
- **Preferred format IDs** (e.g., "140", "251")
- **Audio language preference**
- **File size preference** (largest/smallest)
- **Format sorting order**
- **Audio bitrate**
- **Sample rate**
- **Embed thumbnail**
- **Split by chapters**
- **SponsorBlock filters**

---

## Technical Stack

- **yt-dlp** - Python-based YouTube downloader (via JNI)
- **NewPipe** - Alternative extractor library
- **YoutubeDL-Android** - Android wrapper for yt-dlp
- **Kotlin Coroutines** - Async operations
- **WorkManager** - Background download queue
- **Room Database** - Local data persistence
- **RecyclerView** - Format list display
- **Material Design Components** - UI components

---

## Comparison with Your Implementation

### Similarities:
- Uses yt-dlp for downloading
- Formats fetched via yt-dlp's JSON output
- Quality selection before download

### Differences:

| Feature | YTDLNIS | Your SyncTax App |
|---------|---------|------------------|
| Format fetching | Full yt-dlp integration | Python script via Chaquopy |
| Format display | RecyclerView with cards | Not currently implemented |
| Format selection | Advanced filtering/sorting | Not implemented |
| Download queue | WorkManager background queue | Direct download |
| Progress tracking | Notifications + EventBus | Not shown |
| History tracking | Room database | Not implemented |
| Multi-format selection | Supported (video + audio) | Single format |

### What You Can Adapt:

1. **Format Fetching**: Use yt-dlp's `--print "%(formats)s"` command
2. **Format Parsing**: Parse JSON array to get all available formats
3. **Format Selection UI**: Show formats in a RecyclerView or Composable list
4. **Smart Sorting**: Implement format prioritization based on preferences
5. **Background Downloads**: Use WorkManager instead of direct execution
6. **Progress Tracking**: Parse yt-dlp output for download progress

---

## Example: Fetching Formats for Audio

```kotlin
// In your Python script (audio_downloader.py)
def get_audio_formats(url):
    ydl_opts = {
        'quiet': True,
        'skip_download': True,
        'format': 'bestaudio',
    }
    
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        
        # Filter audio-only formats
        audio_formats = [
            {
                'format_id': f.get('format_id'),
                'ext': f.get('ext'),
                'acodec': f.get('acodec'),
                'abr': f.get('abr'),  # Audio bitrate
                'filesize': f.get('filesize', 0),
                'format_note': f.get('format_note', ''),
            }
            for f in info['formats']
            if f.get('vcodec') == 'none'  # Audio only
        ]
        
        return audio_formats
```

```kotlin
// In Kotlin (ChaquopyAudioDownloader.kt)
fun getAudioFormats(url: String): List<AudioFormat> {
    val python = Python.getInstance()
    val module = python.getModule("audio_downloader")
    val formatsJson = module.callAttr("get_audio_formats", url).toString()
    
    // Parse JSON and return list of AudioFormat objects
    return Gson().fromJson(formatsJson, Array<AudioFormat>::class.java).toList()
}
```

---

## Summary

YTDLNIS provides a comprehensive audio download solution with:

1. **Smart Format Selection**: Automatic prioritization based on user preferences
2. **Rich Format Information**: Shows quality, codec, container, size
3. **Flexible UI**: Filter, sort, and browse all available formats
4. **Background Downloads**: Queue management with progress tracking
5. **Advanced Features**: Thumbnail embedding, chapter splitting, SponsorBlock

The key innovation is the **FormatUtil** class that intelligently sorts formats based on multiple criteria, and the **FormatViewModel** that manages format display with various filter/sort options for the user.
