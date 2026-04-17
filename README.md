<p align="left">
  <img src="assets/app_icon.jpg" alt="App icon" height="200" />
</p>

# SyncTax - Offline-First AI Music Player for Android

**Privacy-first music player with on-device personalization and smart recommendations.**

Built with modern Jetpack Compose, it plays local music, streams from YouTube, downloads songs, shows synchronized lyrics, and learns your taste completely offline using lightweight ML models.

→ **136 MB APK** | **Android 10+** | **Material 3 Design**
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

| Welcome Screen | Home (Getting Started) | Select Directory |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_01_landing_screen.png" alt="Welcome to the app" height="300" /> | <img src="assets/screenshots/screenshot_02_home_screen_starting_with_guide.png" alt="Home with guide" height="300" /> | <img src="assets/screenshots/screenshot_03_select_directory_option.png" alt="Select directory" height="300" /> |
| App landing & onboarding | First-time home guidance | Directory selection (SAF)

</div>

#### Core Playback & Player

<div align="center">

| Offline Player | Offline Lyrics | Online Player |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_13_offline_player.png" alt="Offline player" height="300" /> | <img src="assets/screenshots/screenshot_14_offline_lyrics.png" alt="Offline lyrics view" height="300" /> | <img src="assets/screenshots/screenshot_09_online_song_player.png" alt="Online player" height="300" /> |
| Now playing (local) | Synchronized LRC display for local files | Online stream player

</div>

#### Online Player — Lyrics Flow

<div align="center">

| Lyrics Overlay | Lyrics Fetching | Lyrics Fetched |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_10_online_player_lyrics_overlay.png" alt="Lyrics overlay" height="300" /> | <img src="assets/screenshots/screenshot_11_online_lyrics_fetching.png" alt="Fetching lyrics" height="300" /> | <img src="assets/screenshots/screenshot_12_online_lyrics_fetched.png" alt="Fetched lyrics" height="300" /> |
| Live overlay while streaming | LRCLIB / online lookup in progress | Online lyrics displayed

</div>

#### Library & Search

<div align="center">

| Library (Songs) | Library (Artists) | Albums |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_17_library_screen_songs.png" alt="Songs library" height="300" /> | <img src="assets/screenshots/screenshot_18_library_screen_artists.png" alt="Artists library" height="300" /> | <img src="assets/screenshots/screenshot_21_library_screen_albums.png" alt="Albums view" height="300" /> |
| Browse songs and play offline | Artist list & quick actions | Album view and track list

</div>

#### Search & Online Discovery

<div align="center">

| Local Search | Online Search Results |
|:---:|:---:|
| <img src="assets/screenshots/screenshot_07_search_screen.png" alt="Search interface" height="300" /> | <img src="assets/screenshots/screenshot_08_searching_song_online.png" alt="Online search results" height="300" /> |
| Search local songs, albums, artists | Search results from online frontends |

</div>

#### Recommendations & Training

<div align="center">

| Quick Picks (Before) | Quick Picks (After) | Training Screen |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_15_quick_pick_before_training.png" alt="Quick picks before training" height="300" /> | <img src="assets/screenshots/screenshot_16_quick_pick_after_training.png" alt="Quick picks after training" height="300" /> | <img src="assets/screenshots/screenshot_24_train_model_screen.png" alt="Model training screen" height="300" /> |
| Initial suggestions during learning | Personalized picks after training | Model training / status

</div>

#### Collections, Albums & Settings

<div align="center">

| Artist Detail | Album Detail | Settings |
|:---:|:---:|:---:|
| <img src="assets/screenshots/screenshot_20_artist_screen.png" alt="Artist screen" height="300" /> | <img src="assets/screenshots/screenshot_22_album_screen.png" alt="Album screen" height="300" /> | <img src="assets/screenshots/screenshot_23_settings_screen.png" alt="Settings" height="300" /> |
| Artist details & tracks | Album track listing & artwork | App settings & directory selection

</div>

[View full image gallery](./assets/screenshots/)

---

## Developer notes and project structure

- `app/` — Android module with UI, background playback, and Kotlin-based agents
- `Docs/` — Architecture, algorithms, and developer guides
- `scripts/` — Helper scripts for testing and data collection

---
Thanks for checking out SyncTax — contributions and feedback are welcome!
