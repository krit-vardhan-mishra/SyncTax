# SyncTax Documentation

This folder contains comprehensive documentation for the SyncTax music player app, including architecture, algorithms, implementation guides, and feature summaries.

---

## 📖 Documentation Index

### Core Architecture
| Document | Description |
|----------|-------------|
| [APP_OVERVIEW.md](APP_OVERVIEW.md) | High-level app architecture, technology stack, and features |
| [PYTHON_MODEL.md](PYTHON_MODEL.md) | Python-based ML model (k-means + z-score scorer) via Chaquopy |

### ML & Recommendations
| Document | Description |
|----------|-------------|
| [STATISTICAL_AGENT.md](STATISTICAL_AGENT.md) | Statistical scoring algorithm with weighted features |
| [COLLABORATIVE_FILTERING_AGENT.md](COLLABORATIVE_FILTERING_AGENT.md) | Vector similarity and nearest-neighbor recommendations |
| [FUSION_AGENT.md](FUSION_AGENT.md) | Multi-agent fusion and diversity boosting |

### UI & Features
| Document | Description |
|----------|-------------|
| [UI_REORGANIZATION_SUMMARY.md](UI_REORGANIZATION_SUMMARY.md) | Home screen sections and online listening history |
| [UP_NEXT_QUEUE_ENHANCEMENT.md](UP_NEXT_QUEUE_ENHANCEMENT.md) | Queue management and playback controls |
| [YTMUSIC_INTEGRATION_SUMMARY.md](YTMUSIC_INTEGRATION_SUMMARY.md) | YouTube Music API integration via ytmusicapi |

### Download & Format Selection
| Document | Description |
|----------|-------------|
| [YOUTUBE_AUDIO_DOWNLOAD_FIX.md](YOUTUBE_AUDIO_DOWNLOAD_FIX.md) | Audio download implementation with yt-dlp |
| [FORMAT_SELECTION_SUMMARY.md](FORMAT_SELECTION_SUMMARY.md) | ytdlnis-inspired format selection implementation |
| [FORMAT_SELECTION_ENHANCEMENTS.md](FORMAT_SELECTION_ENHANCEMENTS.md) | Advanced format filtering and preferences |
| [FORMAT_SELECTION_TESTING.md](FORMAT_SELECTION_TESTING.md) | Testing guide for format selection |
| [YTDLNIS_AUDIO_DOWNLOAD_ANALYSIS.md](YTDLNIS_AUDIO_DOWNLOAD_ANALYSIS.md) | Analysis of ytdlnis download approach |
| [METADATA_DOWNLOAD_FIX_SUMMARY.md](METADATA_DOWNLOAD_FIX_SUMMARY.md) | Metadata embedding using Mutagen |

### Performance
| Document | Description |
|----------|-------------|
| [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) | Guide to performance optimizations |
| [PERFORMANCE_OPTIMIZATIONS_APPLIED.md](PERFORMANCE_OPTIMIZATIONS_APPLIED.md) | Applied optimizations with code examples |
| [OPTIMIZATION_SUMMARY.md](OPTIMIZATION_SUMMARY.md) | Summary of all optimizations |
| [PAGINATION_GUIDE.md](PAGINATION_GUIDE.md) | Large library pagination strategy |
| [PAGINATION_IMPLEMENTATION_SUMMARY.md](PAGINATION_IMPLEMENTATION_SUMMARY.md) | Pagination implementation details |
| [PAGINATION_COMPLEXITY_ANALYSIS.md](PAGINATION_COMPLEXITY_ANALYSIS.md) | Complexity analysis of pagination |

### Checklists & Status
| Document | Description |
|----------|-------------|
| [IMPLEMENTATION_CHECKLIST.md](IMPLEMENTATION_CHECKLIST.md) | Feature implementation status |

---

## 🎯 Quick Start

1. **Understanding the App**: Start with [APP_OVERVIEW.md](APP_OVERVIEW.md)
2. **ML Recommendations**: Read the agent docs (Statistical → Collaborative → Fusion)
3. **Download Features**: Check [FORMAT_SELECTION_SUMMARY.md](FORMAT_SELECTION_SUMMARY.md)
4. **Performance**: Review [OPTIMIZATION_SUMMARY.md](OPTIMIZATION_SUMMARY.md)

---

## 🔧 Key Technologies

- **Kotlin 2.1.0** with Jetpack Compose
- **ExoPlayer (Media3)** for audio playback
- **Room Database** for local persistence
- **Chaquopy** for Python ML integration
- **yt-dlp + Mutagen** for audio downloading and metadata
- **ytmusicapi** for YouTube Music integration
- **NewPipe Extractor** for stream URL extraction

---

## 📱 App Version

- **Version**: 3.0.0
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Android 15 (API 36)
- **APK Size**: ~136 MB

---

*Last Updated: November 30, 2025*
