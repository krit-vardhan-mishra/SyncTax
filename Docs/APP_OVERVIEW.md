# SyncTax - App Overview

## Introduction

**SyncTax** is an offline-first Android music player and recommendation app with on-device machine learning. It combines rich local media playback with lightweight personalized recommendations that run entirely on the device — no cloud required.

---

## Key Goals

- **Modern Music UI**: Provide a responsive music experience using Jetpack Compose
- **Personalized Recommendations**: Offer Quick Picks by combining on-device approaches (statistical, collaborative, and Python ML model via Chaquopy)
- **Privacy-First**: Keep user data local with resettable models and user-controlled training
- **Online Integration**: Stream and download songs from YouTube/YouTube Music with full metadata embedding

---

## Technology Stack

### Platform & Language
| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.1.0 |
| Min SDK | Android 10 (API 29) | - |
| Target SDK | Android 15 (API 36) | - |
| UI Framework | Jetpack Compose | BOM 2024.02.00 |
| Build System | Gradle | 8.13.1 |

### Core Libraries
| Library | Purpose | Version |
|---------|---------|---------|
| ExoPlayer (Media3) | Audio playback | 1.2.1 |
| Room Database | Local data persistence | 2.6.1 |
| NewPipe Extractor | YouTube stream URL extraction | 0.24.8 |
| Chaquopy | Python integration for ML | 16.1.0 |
| yt-dlp | Audio downloading | 2025.11.12 |
| Mutagen | Metadata embedding | Latest |
| ytmusicapi | YouTube Music API | Latest |
| Coil | Image loading | 2.5.0 |
| OkHttp | HTTP client | 4.12.0 |

### Python Dependencies
- **yt-dlp**: YouTube audio extraction and downloading
- **Mutagen**: Audio metadata (ID3/MP4) embedding
- **ytmusicapi**: YouTube Music recommendations and search
- **requests/urllib3**: HTTP operations

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI Layer                                │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │
│  │ HomeScreen  │  │ SearchScreen │  │ FullScreenPlayerContent │ │
│  │ LibraryScreen│  │ QuickPicksScreen│ │ OnlineHistorySection  │ │
│  └─────────────┘  └──────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                            │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ PlayerViewModel │  │ HomeViewModel  │  │ DynamicBgViewModel│  │
│  └─────────────────┘  └────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Core Layer                                 │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │  MusicPlayer    │  │  QueueManager  │  │ MusicRepository  │  │
│  │  MusicService   │  │ PlaybackEventBus│ │ RecommendationMgr│  │
│  └─────────────────┘  └────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Data Layer                                 │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │  Room Database  │  │ UserPreferences│  │  Python/Chaquopy │  │
│  │  (Songs, History)│ │ (SharedPrefs)  │  │  (ML Models)     │  │
│  └─────────────────┘  └────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. UI Layer (Jetpack Compose)

| Screen | Description |
|--------|-------------|
| `HomeScreen` | Main screen with Quick Picks, Listen Again, Speed Dial sections |
| `SearchScreen` | Local and online music search with filter chips |
| `LibraryScreen` | Full song library with pagination and sorting |
| `QuickPicksScreen` | ML recommendations with scores and details |
| `SettingsScreen` | App configuration and permissions |
| `FullScreenPlayerContent` | Expanded player with lyrics, queue, controls |

### 2. Playback System

- **MusicService**: Foreground service with persistent notification
- **MusicPlayer**: ExoPlayer wrapper with chunked streaming support
- **QueueManager**: Centralized queue operations (add, remove, reorder, shuffle)
- **PlaybackEventBus**: Global event system for playback synchronization

### 3. State Management

- **PlayerViewModel**: Manages playback state, downloads, online streaming
- **HomeViewModel**: Manages library data, sections, recommendations
- **StateFlow/SharedFlow**: Reactive state updates across all screens

### 4. Data Persistence

- **Room Database** (Version 4):
  - `songs` - Local music library
  - `listening_history` - Play history for ML training
  - `online_listening_history` - Online song history (Quick Picks)
  - Database indices on frequently queried columns

- **UserPreferences**: SharedPreferences wrapper for settings

### 5. ML & Recommendations

| Agent | Description |
|-------|-------------|
| `StatisticalAgent` | Feature-weighted scoring with sigmoid normalization |
| `CollaborativeFilteringAgent` | Vector similarity using in-memory VectorDatabase |
| `FusionAgent` | Combines agent outputs with diversity boosting |
| `music_ml.py` (Python) | K-means clustering + z-score similarity scoring |

---

## Key Features

### Local Music
- ✅ Device music library scanning with metadata extraction
- ✅ Album art extraction (embedded + folder-based)
- ✅ Paginated library view (handles 10,000+ songs)
- ✅ Smart shuffle with ML recommendations

### Online Music
- ✅ YouTube/YouTube Music search and streaming
- ✅ Audio downloading with format selection
- ✅ Metadata embedding (title, artist, album, artwork)
- ✅ Online listening history (Quick Picks section)
- ✅ Long-press to remove from Quick Picks history

### Playback
- ✅ Background playback with notification controls
- ✅ Chunked streaming for large files
- ✅ Up Next queue with drag-and-drop reordering
- ✅ Play Next functionality
- ✅ Shuffle and repeat modes

### Recommendations
- ✅ On-device ML-powered Quick Picks
- ✅ Speed Dial section with personalized songs
- ✅ Listen Again section from play history
- ✅ Auto-generated recommendations when queue ends

### Lyrics
- ✅ LRCLIB API integration for synced lyrics
- ✅ Manual lyrics search
- ✅ Lyrics overlay on full-screen player

---

## Permissions Model

| Permission | Status | Purpose |
|------------|--------|---------|
| `READ_MEDIA_AUDIO` | Required | Access local music files |
| `READ_MEDIA_IMAGES` | Optional | Scan folder-based album art (user-controlled in Settings) |
| `FOREGROUND_SERVICE` | Required | Background playback |
| `POST_NOTIFICATIONS` | Required | Playback notification |
| `INTERNET` | Required | Online streaming and downloads |

---

## Download System

### Audio Downloading
1. **Format Selection Dialog**: User selects preferred audio format
2. **Download via yt-dlp**: Extracts audio stream from YouTube
3. **Metadata Embedding**: Uses Mutagen to embed:
   - Title, Artist, Album
   - Thumbnail as album art (embedded in audio file)
   - Duration, year metadata
4. **Thumbnail Cleanup**: Automatically removes leftover thumbnail files

### Format Options
- **SUGGESTED**: Preference-based intelligent sorting
- **ALL**: All available formats
- **SMALLEST**: Smallest file size per quality tier
- **GENERIC**: Fallback formats (140, 251, 250, 249, 139, 141)

---

## Performance Optimizations

- **Pagination**: 30 songs per page, fast initial load
- **LRU Cache**: VectorDatabase limited to 5,000 entries
- **Chunked Processing**: ML generation in 500-song batches
- **Quick Picks Caching**: 5-minute TTL cache
- **Database Indices**: On timestamp, title, artist, album, genre

---

## Security & Privacy

- **Local-First**: All ML models trained on-device
- **No Cloud Dependencies**: Recommendations work offline
- **Resettable Models**: User can clear ML data via settings
- **API Key Protection**: Keys stored in `local.properties` (git-ignored)
- **No Tracking**: No analytics or telemetry

---

## Build Configuration

### APK Size: ~136 MB
- ARM architectures only (armeabi-v7a, arm64-v8a)
- x86/x86_64 excluded (saves ~117 MB)
- FFmpeg excluded (Mutagen used instead)
- ProGuard enabled for release builds

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## App Lifecycle

- When the user removes the app from the recent apps list (swipes it away), the service detects task removal and stops playback to avoid background battery drain
- Playback continues when the app is backgrounded normally
- The Quick Picks guide overlay on the Quick Picks screen can be toggled via User Preferences

---

## Future Enhancements

1. **Offline Album Caching**: Save online albums for offline playback
2. **Playlist Management**: Create and manage playlists
3. **Equalizer**: Audio equalization settings
4. **Sleep Timer**: Auto-stop playback
5. **Model Persistence**: Save trained ML weights across app restarts

---

## References

- Inspired by [OD-MAS (Team COD3INE)](https://github.com/ECVarsha/OD-MAS_Team-COD3INE)
- UI patterns inspired by YouTube Music and Spotify
- Format selection inspired by [ytdlnis](https://github.com/deniscerri/ytdlnis)

---

**Version**: 3.0.0  
**Last Updated**: November 30, 2025
