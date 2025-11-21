# SyncTax

<img src="assets/screenshots/app_icon.png" alt="App icon" height="96" />

SyncTax is an offline-first Android music player and recommender app that demonstrates privacy-preserving, on-device personalization and compact ML models. It combines a modern Android UI with background playback, local-first data storage, and a lightweight hybrid recommendation engine that uses Kotlin-based agents and an optional Python model via Chaquopy.

This README gives a quick project overview, usage tips, and developer setup instructions. For a deeper dive into architecture, models, and implementation details, see the `Docs/` folder.

---

## Highlights

- Offline-first playback and recommendations
- Background audio with low battery impact
- Lightweight on-device personalization powered by Kotlin agents and a Python-based model (Chaquopy)
- Local media support (MP3/OGG/FLAC) alongside searchable YouTube frontends
- Clean Material 3 UI and modular architecture suitable for experimentation

---

### App screenshots

Below are screenshots showcasing the app's UI and key features, from initial setup through personalized recommendations.

#### Getting Started

<div align="center">

| Welcome Screen | Home (Before Training) |
|:---:|:---:|
| <img src="assets/screenshots/screenshot_01_landing.png" alt="Welcome to the app" height="400" /> | <img src="assets/screenshots/screenshot_02_home_before_training.png" alt="Initial home screen" height="400" /> |
| Set up your profile | Home screen before the AI learns your taste |

</div>

#### Core Playback Experience

<div align="center">

| Music Player | Queue Management (Half) | Queue Management (Full) 
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_03_player.png" alt="Now playing interface" height="400" /> | <img src="assets/screenshots/screenshot_04_queue_semi_opened.png" alt="Up next queue" height="400" /> |  <img src="assets/screenshots/screenshot_04_queue_opened.png" alt="Up next queue" height="400" />
| Full-featured player with offline support | View and manage your play queue | Queue fully opened |

</div>

#### Library & Search

<div align="center">

| Your Library | Local Search | Online Search Results |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_05_library.png" alt="Music library" height="400" /> | <img src="assets/screenshots/screenshot_07_search_screen.png" alt="Search interface" height="400" /> | <img src="assets/screenshots/screenshot_08_online_search.png" alt="YouTube search results" height="400" /> |
| Browse your local collection | Search songs, artists, and albums | Find new music via YouTube frontends |

</div>

#### AI-Powered Recommendations

<div align="center">

| Quick Picks (Learning) | Quick Picks (Trained) | Home (After Training) |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_06_quick_picks_before_training.png" alt="Initial Quick Picks" height="400" /> | <img src="assets/screenshots/screenshot_09_quick_picks_after_training.png" alt="Personalized recommendations" height="400" /> | <img src="assets/screenshots/screenshot_10_home_after_training.png" alt="Smart home with recommendations" height="400" /> |
| App analyzing your preferences | AI-curated picks based on listening habits | Personalized home with mood categories |

</div>

#### Enhanced Home Experience

<div align="center">

| Speed Dial & Collections | All Songs View |
|:---:|:---:|
| <img src="assets/screenshots/screenshot_11_more_home.png" alt="Quick access to favorites" height="400" /> | <img src="assets/screenshots/screenshot_12_more_home.png" alt="Complete song library" height="400" /> |
| Quick access to favorite artists and playlists | Browse your complete music collection |

</div>

[View full image gallery](./assets/screenshots/)

---

## Quick start (development)

1. Clone the repository

```
git clone https://github.com/krit-vardhan-mishra/SyncTax.git
cd SyncTax
```

2. Build and run (command line)

Windows (PowerShell):

```
./gradlew.bat assembleDebug
./gradlew.bat installDebug
```

Or open the project in Android Studio and run the `app` module on an emulator or device.

3. Optional: Provide a YouTube API key for fallback search results

If you want to use the YouTube Data API for search fallback, set `YOUTUBE_API_KEY` in your environment before launching the app. The app defaults to public frontends (Piped/Invidious) but uses the official API if configured.

On Windows PowerShell:

```
$env:YOUTUBE_API_KEY = "YOUR_YOUTUBE_API_KEY"
```

---

## Developer notes and project structure

- `app/` — Android module with UI, background playback, and Kotlin-based agents
- `Docs/` — Architecture, algorithms, and developer guides
- `scripts/` — Helper scripts for testing and data collection

If you're exploring the recommendation engine, start in `app/src/main/java` where Kotlin `agents` and `controllers` are arranged.

---

## Contributing

We welcome contributions! Good first issues, feature requests, translations, and bug reports help this project improve. Please see the `Docs/` for guidance on the repository layout and recommended tasks.

When contributing:

- Open issues for bugs or feature requests
- Submit PRs against the `master` branch with clear testing instructions
- Add or update docs when you add features

---

## Attribution & license

SyncTax is open source — see the repository license for details. This project is an independent app and is not affiliated with YouTube, Google LLC, or any of their subsidiaries.

---

## Help & Support

If you run into issues, please open an issue in the repository with steps to reproduce, logs, and device configuration.

For architecture and models, consult `Docs/` — it contains additional diagrams and how-tos to extend the Python model.

---

Thanks for checking out SyncTax — contributions and feedback are welcome!