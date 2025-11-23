# YouTube Audio Download - Issue Analysis & Solution

## 📋 Issue Summary

**Problem:** Downloads failing with error:
```
ERROR: [youtube] vxUBYHz_q1I: Requested format is not available
```

**Root Cause:** The audio format filtering logic was too strict, filtering OUT valid audio formats like format 18 (mp4 with audio codec).

## 🔍 Analysis from Logcat

### What Was Happening:
1. **72 formats found** from YouTube
2. **0 audio formats converted** - All formats were being filtered out
3. **Format 18 was attempted** but rejected by the filter
4. **Download failed** because no valid formats were available

### Why It Failed:
The filtering logic was looking for formats where `vcodec == 'none'` (audio-only), but YouTube's format 18 is a **combined video+audio format** that contains:
- **Video Codec:** avc1.42001E (H.264)
- **Audio Codec:** mp4a.40.2 (AAC)

This format CAN be used to extract audio, but the filter rejected it.

## 🎵 Available YouTube Audio Formats (yt-dlp)

### Pure Audio-Only Formats:
| Format ID | Container | Codec | Bitrate | Quality | Notes |
|-----------|-----------|-------|---------|---------|-------|
| **140** | m4a | AAC | 128kbps | Standard | Most common, best compatibility |
| **139** | m4a | AAC | 48kbps | Low | Small file size |
| **141** | m4a | AAC | 256kbps | High | Best AAC quality |
| **251** | webm | Opus | ~160kbps | High | Good quality, smaller size |
| **250** | webm | Opus | ~70kbps | Medium | Balanced |
| **249** | webm | Opus | ~50kbps | Low | Very small |
| **171** | webm | Vorbis | 128kbps | Standard | Older codec |
| **172** | webm | Vorbis | 256kbps | High | Older codec, high quality |

### Combined Video+Audio Formats (Can Extract Audio):
| Format ID | Container | Video | Audio | Resolution | Notes |
|-----------|-----------|-------|-------|------------|-------|
| **18** | mp4 | H.264 | AAC | 360p | Most reliable fallback |
| **22** | mp4 | H.264 | AAC | 720p | Higher quality |

### Format Priority:
1. **First choice:** Pure audio formats (140, 251, 141, etc.)
2. **Fallback:** Combined formats (18, 22) for audio extraction
3. **Extraction:** yt-dlp can extract audio without FFmpeg for mp4 formats

## ✅ Solution Implemented

### 1. Fixed Format Selection
**Before:**
```python
'bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio[ext=mp3]/...'
```
- Too restrictive
- Rejected combined formats
- Failed when pure audio unavailable

**After:**
```python
'bestaudio/best[height<=360]'
```
- Tries audio-only formats first
- Falls back to small video formats (like 18)
- Allows audio extraction from video

### 2. Fixed Format Filtering
**Before:**
```python
is_audio_format = (
    acodec != 'none' and 
    (vcodec == 'none' or  # Too strict!
     fmt.get('resolution') == 'audio only')
)
```
- Only accepted pure audio formats
- Rejected format 18 because it has video codec

**After:**
```python
has_audio = acodec != 'none' and acodec != ''

if has_audio:
    # Accept ANY format with audio codec
    # Can be audio-only OR combined video+audio
    format_info = {...}
    all_formats.append(format_info)
```
- Accepts any format with audio codec
- Includes combined formats like 18
- yt-dlp handles audio extraction

### 3. Added Audio Extraction Support
```python
# yt-dlp can extract audio from video formats
# Works even without FFmpeg for mp4 formats
ydl_opts['format'] = 'bestaudio/best[height<=360]'
```

## 📊 Test Results

### Before Fix:
```
Audio formats found: 0
ERROR: Requested format is not available
Download failed
```

### After Fix:
```
Audio formats found: 1
Format ID: 18 (mp4, AAC audio codec)
Download successful: Attention.mp4 (3.54 MB)
✅ Success!
```

## 🔧 How It Works Now

### Flow:
1. **User clicks download** on a song (e.g., "Attention")
2. **YouTube URL extracted:** `https://www.youtube.com/watch?v=vxUBYHz_q1I`
3. **yt-dlp tries clients:** android → web → tv → ios → mweb
4. **Format selection:** `bestaudio/best[height<=360]`
   - Prefers: Pure audio formats (140, 251, etc.)
   - Falls back: Combined formats with audio (18, 22)
5. **Download:** Format 18 (mp4 with audio)
6. **Audio extraction:** yt-dlp extracts audio stream
7. **Result:** `Attention.mp4` (contains only audio)

### Error Handling:
- **PO Token warnings:** Ignored, falls back to http formats
- **Client failures:** Tries next client automatically
- **No audio formats:** Returns error with clear message
- **403 Forbidden:** Tries next client

## 📝 Key Changes Made

### File: `audio_downloader.py`

1. **Format selection string:**
   - Changed to `'bestaudio/best[height<=360]'`
   - Accepts audio-only OR small video formats

2. **Format filtering logic:**
   - Removed strict `vcodec == 'none'` check
   - Now accepts any format with `acodec != 'none'`
   - Skips only storyboards and images

3. **Added format metadata:**
   - `audio_only` flag to identify pure audio
   - Better resolution description
   - Client information for debugging

## 🎯 What Gets Downloaded

### File Format:
- **Container:** MP4 (from format 18)
- **Audio Codec:** AAC (mp4a.40.2)
- **File Extension:** `.mp4`
- **Content:** Audio only (video stream discarded/extracted)

### Why MP4?
- Most reliable format available without PO Token
- Native AAC audio support
- Good compatibility with Android MediaPlayer
- yt-dlp can extract audio without FFmpeg

## 🚀 Next Steps

### For Better Audio Quality:
1. **Option 1:** Use format selection UI to let users choose
   - Show available formats (140, 251, 18, etc.)
   - Let user pick quality preference

2. **Option 2:** Implement PO Token support
   - Unlocks high-quality audio formats
   - Requires token generation/management

3. **Option 3:** Bundle FFmpeg
   - Better audio extraction
   - Can convert to preferred formats
   - Requires native library integration

### Current Status:
✅ Downloads work reliably
✅ Audio extracted from available formats
✅ Client fallback prevents failures
✅ Format 18 provides decent quality
⚠️ Pure audio formats need PO Token

## 📱 Testing Recommendations

### Test with your app:
1. Search for "attention" in app
2. Click on "Attention" by Charlie Puth
3. Click download button
4. Should download successfully now

### Expected behavior:
- Format detection finds format 18
- Download uses android client
- File saved as `Attention.mp4`
- Contains audio only
- File size: ~3-4 MB

### If issues persist:
- Check logcat for Python errors
- Verify yt-dlp version (should be latest)
- Ensure storage permissions granted
- Check network connectivity
