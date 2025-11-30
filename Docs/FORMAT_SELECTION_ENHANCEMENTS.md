# Format Selection Enhancement - Implementation Summary

## Overview
This document describes the ytdlnis-inspired format selection enhancements implemented in SyncTax. The improvements provide users with sophisticated format filtering, preference-based sorting, and fallback options similar to ytdlnis.

---

## 🎯 Key Features Implemented

### 1. **Rich Format Data Model** ✅
The `Format.kt` data class now captures comprehensive metadata:
```kotlin
- format_id: Unique format identifier (e.g., "140", "251")
- container: File container/extension (m4a, webm, mp4)
- vcodec: Video codec (none for audio-only)
- acodec: Audio codec (opus, mp4a, aac)
- filesize: File size in bytes
- format_note: Quality description (e.g., "medium 128k")
- fps: Frame rate (for video formats)
- asr: Audio sample rate
- tbr: Total bitrate
- lang: Audio language/locale
```

### 2. **FormatUtil - Intelligent Format Management** ✅
Created `FormatUtil.kt` with advanced sorting and filtering:

#### **Multi-Criteria Sorting**
Sorts formats based on user preferences with priority order:
1. **Exact Format ID match** (if specified)
2. **Codec preference** (opus, mp4a, aac, vorbis, mp3)
3. **Container preference** (webm, m4a, mp3, ogg)
4. **Higher bitrate** (descending order)
5. **Smaller file size** (ascending order)

#### **Generic Fallback Formats**
Hardcoded common YouTube formats when extraction fails:
- **140**: M4A 128k (medium quality AAC)
- **251**: WebM 160k (medium quality Opus)
- **250**: WebM 70k (medium quality Opus)
- **249**: WebM 50k (low quality Opus)
- **139**: M4A 48k (low quality AAC)
- **141**: M4A 256k (high quality AAC)

### 3. **Filter Categories** ✅
Four filtering modes for format display:

| Category | Description | Use Case |
|----------|-------------|----------|
| **SUGGESTED** | Preference-based intelligent sorting | Best for most users |
| **ALL** | All available formats unsorted | Advanced users |
| **SMALLEST** | Groups by quality, shows smallest in each group | Data-conscious users |
| **GENERIC** | Hardcoded fallback formats | When extraction fails |

### 4. **Enhanced UI** ✅

#### **Format Selection Dialog**
- **Filter Button**: Cycles through ALL → SUGGESTED → SMALLEST → GENERIC
- **Refresh Button**: Re-fetches formats from server
- **Category Indicator**: Shows current filter in dialog title
- **VIDEO/AUDIO Grouping**: Separate sections for video and audio formats

#### **Format Cards Display**
Each format card now shows:
- **Quality**: Format note (e.g., "MEDIUM 128K")
- **Container**: File extension (M4A, WEBM, MP4)
- **Codec**: Audio/video codec (OPUS, AAC, AVC1)
- **Format ID**: Unique identifier
- **Bitrate**: Audio bitrate (for audio formats)
- **File Size**: Human-readable size (MB/GB)

### 5. **User Preferences** ✅
Created `format_preferences.xml` with customizable settings:

#### **Available Preferences**
```xml
1. Preferred Audio Codec
   - Opus (Best for streaming)
   - AAC/MP4A (Universal compatibility)
   - Vorbis (Open format)
   - MP3 (Maximum compatibility)

2. Preferred Container Format
   - WebM (Opus codec)
   - M4A (AAC codec)
   - MP3 (Universal)
   - OGG (Vorbis codec)

3. Preferred Format ID
   - Specific format ID (e.g., 140, 251)
   - Leave empty for auto selection

4. Format Selection Priority
   - Codec → Container → File Size (default)
   - Container → Codec → File Size
   - Bitrate → Codec → Container
   - File Size → Codec → Container
   - Format ID Only

5. Prefer Container Over Codec
   - Toggle to prioritize container over codec

6. Default Format Filter
   - SUGGESTED (default)
   - ALL
   - SMALLEST
   - GENERIC
```

### 6. **Enhanced Python Backend** ✅
Updated `audio_downloader.py` to return comprehensive format data:
```python
format_info = {
    'format_id': format_id,
    'container': ext,
    'vcodec': vcodec,
    'acodec': acodec,
    'encoding': encoding,
    'filesize': filesize or filesize_approx,
    'format_note': format_note or quality,
    'fps': fps,
    'asr': audio_sample_rate,
    'url': format_url,
    'lang': language or audio_lang,
    'tbr': total_bitrate or audio_bitrate,
}
```

---

## 📊 Comparison with ytdlnis

| Feature | ytdlnis | SyncTax (New) | Notes |
|---------|---------|---------------|-------|
| **Format Data Model** | ✅ Complete | ✅ Complete | Identical fields |
| **Preference-based Sorting** | ✅ Advanced | ✅ Implemented | Multi-criteria sorting |
| **Filter Categories** | ✅ 4 categories | ✅ 4 categories | ALL/SUGGESTED/SMALLEST/GENERIC |
| **Generic Fallbacks** | ✅ Yes | ✅ Yes | Common YouTube formats |
| **VIDEO/AUDIO Grouping** | ✅ Yes | ✅ Yes | Separate sections |
| **Format Source Switching** | ✅ Yes | ❌ Not yet | Could add aria2c support |
| **Multi-select Audio** | ✅ Yes | ❌ Not yet | For video downloads |
| **Batch Format Updates** | ✅ Yes | ❌ Not yet | Multiple items |
| **DRC Filtering** | ✅ Yes | ❌ Not yet | Dynamic Range Compression |

---

## 🔧 Technical Implementation

### **Class Structure**
```
FormatUtil.kt
├── getGenericAudioFormats() → Fallback formats
├── getGenericVideoFormats() → Fallback video formats
├── getAudioFormatImportance() → Read preference order
├── sortAudioFormats() → Multi-criteria audio sorting
├── sortVideoFormats() → Resolution-based video sorting
├── filterFormatsByCategory() → Apply filter category
├── isAudioOnly() → Check if format is audio-only
├── getFormatDescription() → Human-readable description
└── formatFileSize() → Convert bytes to MB/GB

FormatViewModel.kt
├── loadFormats() → Fetch and prepare formats
├── prepareFormatItems() → Group and filter formats
├── setCategory() → Change filter category
├── cycleCategory() → Cycle through categories
└── getCategoryName() → Get current category name

FormatSelectionBottomSheetDialog.kt
├── setupClickListeners() → Filter/Refresh buttons
├── updateFilterButtonText() → Update category in title
└── observeViewModel() → React to state changes
```

### **Data Flow**
```
User requests download
    ↓
getVideoInfo(url) → Python yt-dlp extracts formats
    ↓
Parse JSON → List<Format> with rich metadata
    ↓
FormatUtil.filterFormatsByCategory()
    ↓
FormatUtil.sortAudioFormats() (if SUGGESTED)
    ↓
Separate VIDEO/AUDIO sections
    ↓
Display in RecyclerView with FormatAdapter
    ↓
User selects format or cycles filter
    ↓
Download with selected format_id
```

---

## 🚀 Usage Examples

### **Programmatic Format Selection**
```kotlin
val formatUtil = FormatUtil(context)

// Get formats from yt-dlp
val formats = audioProcessor.getFormats(url)

// Apply SUGGESTED filter (preference-based)
val suggested = formatUtil.filterFormatsByCategory(
    formats,
    FormatUtil.FormatCategory.SUGGESTED,
    isAudioDownload = true
)

// Get smallest formats by quality group
val smallest = formatUtil.filterFormatsByCategory(
    formats,
    FormatUtil.FormatCategory.SMALLEST,
    isAudioDownload = true
)

// Use generic fallback if no formats available
val fallback = if (formats.isEmpty()) {
    formatUtil.getGenericAudioFormats()
} else {
    formats
}
```

### **Setting User Preferences**
```kotlin
val prefs = PreferenceManager.getDefaultSharedPreferences(context)

// Set preferred codec
prefs.edit()
    .putString("audio_codec", "opus")
    .putString("audio_format", "webm")
    .putString("format_importance", "codec,container,filesize")
    .apply()
```

### **Cycling Through Filters**
```kotlin
// In FormatSelectionBottomSheetDialog
filterButton.setOnClickListener {
    viewModel.cycleCategory()
    // Updates from SUGGESTED → ALL → SMALLEST → GENERIC → SUGGESTED
}
```

---

## 🎨 UI/UX Improvements

### **Before**
- Simple format list with limited info
- No filtering options
- No preference-based sorting
- No fallback formats

### **After**
- Rich format cards with detailed metadata
- 4 filter categories with one-click switching
- Intelligent preference-based sorting
- Generic fallback formats (140, 251, etc.)
- VIDEO/AUDIO grouping
- Filter indicator in dialog title

---

## 📱 User Benefits

1. **Better Format Selection**: Users can choose based on codec, container, bitrate, and file size
2. **Smart Defaults**: SUGGESTED filter automatically picks best formats based on preferences
3. **Data Conscious**: SMALLEST filter helps users with limited data plans
4. **Reliability**: Generic fallback formats ensure downloads work even when extraction fails
5. **Transparency**: Detailed format info helps users understand what they're downloading
6. **Customization**: Extensive preferences for power users

---

## 🔮 Future Enhancements (Not Yet Implemented)

1. **Multi-select Audio**: Select multiple audio tracks for video downloads
2. **Format Source Switching**: Support aria2c or other downloaders
3. **Batch Format Updates**: Update formats for multiple items simultaneously
4. **DRC Filtering**: Filter Dynamic Range Compression audio
5. **Format Comparison**: Side-by-side format comparison view
6. **Download Speed Estimation**: Show estimated download time per format
7. **Format History**: Remember user's most selected formats
8. **Custom Format Presets**: Save and load custom filter/sort combinations

---

## 🧪 Testing

### **Test Scenarios**
1. ✅ Load formats from YouTube video
2. ✅ Cycle through all filter categories
3. ✅ Verify SUGGESTED sorting follows preferences
4. ✅ Test SMALLEST grouping and selection
5. ✅ Verify GENERIC fallback formats display
6. ✅ Test VIDEO/AUDIO grouping
7. ✅ Verify format card displays all metadata
8. ⏳ Test with various preference combinations
9. ⏳ Test with videos that have limited formats
10. ⏳ Test with age-restricted or private videos

---

## 📝 Notes

- All format preferences are stored in SharedPreferences
- Format sorting uses stable sort (preserves original order for equal elements)
- Generic formats have filesize=0 (unknown until download)
- Python backend tries multiple clients (android→web→tv→ios→mweb) for format extraction
- Format cards use MaterialCardView with selection state
- Filter category persists across dialog dismissals within the same session

---

## 🎓 Key Learnings from ytdlnis

1. **Preference-driven UX**: Users want control over format selection
2. **Fallback is critical**: Generic formats ensure reliability
3. **Visual grouping**: VIDEO/AUDIO separation reduces cognitive load
4. **Filter categories**: Different users have different needs (quality vs size vs compatibility)
5. **Rich metadata**: More information = better decisions

---

## 📚 References

- [ytdlnis FormatUtil.kt](../ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/util/FormatUtil.kt)
- [ytdlnis Format.kt](../ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/database/models/Format.kt)
- [ytdlnis FormatSelectionBottomSheetDialog.kt](../ytdlnis-main/app/src/main/java/com/deniscerri/ytdl/ui/downloadcard/FormatSelectionBottomSheetDialog.kt)
- [yt-dlp Documentation](https://github.com/yt-dlp/yt-dlp)

---

**Last Updated**: November 30, 2025  
**Implementation Date**: November 23, 2025  
**Status**: ✅ Production Ready
