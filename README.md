# SyncTax

<p align="left">
  <img src="assets/app_icon.jpg" alt="App icon" height="200" />
</p>

SyncTax is an offline-first Android music player and recommender app that demonstrates privacy-preserving, on-device personalization and compact ML models. It combines a modern Android UI with background playback, local-first data storage, and a lightweight hybrid recommendation engine that uses Kotlin-based agents and an optional Python model via Chaquopy.

---

## Highlights

- Offline-first playback and recommendations
- Background audio with low battery impact
- Lightweight on-device personalization powered by Kotlin agents and a Python-based model (Chaquopy)
- Local media support (MP3/OGG/FLAC) alongside searchable YouTube frontends
- Clean Material 3 UI and modular architecture suitable for experimentation

---

### App Launchpad Screenshots (Latest)

This gallery uses exported launchpad section images from `assets/new_screenshots/launchpad-screenshot` so the README matches the final presentation layout.

#### 1) Brand & Hero

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/brand-hero.png" alt="Brand and hero section" width="100%" />
</div>

#### 2) Onboarding Flow

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/onboarding-flow.png" alt="Onboarding flow section" width="100%" />
</div>

#### 3) Search, Streaming & Lyrics

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/search-online-lyrics.png" alt="Search streaming and lyrics section" width="100%" />
</div>

#### 4) Discovery & Quick Picks

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/discovery.png" alt="Discovery and quick picks section" width="100%" />
</div>

#### 5) Library Experience

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/library-experience.png" alt="Library experience section" width="100%" />
</div>

#### 6) Controls + Intelligence

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/controls-intelligence.png" alt="Controls and intelligence section" width="100%" />
</div>

#### 7) Playlists + Sharing

<div align="center">
  <img src="assets/new_screenshots/launchpad-screenshot/playlists-sharing.png" alt="Playlists and sharing section" width="100%" />
</div>

[View full launchpad screenshot set](./assets/new_screenshots/)

---

## Developer notes and project structure

- `app/` — Android module with UI, background playback, and Kotlin-based agents
- `Docs/` — Architecture, algorithms, and developer guides
- `scripts/` — Helper scripts for testing and data collection

If you're exploring the recommendation engine, start in `app/src/main/java` where Kotlin `agents` and `controllers` are arranged.

---
