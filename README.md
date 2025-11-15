# YouTubeMusic

YouTubeMusic is an offline-first Android music player and recommendation app that demonstrates on-device personalization and lightweight ML models. The app delivers a modern UI, background playback, and a small ensemble-based recommendation system that uses both Kotlin agents and a Python ML module (via Chaquopy).

For a deeper dive into the architecture, models, and implementation, check the Docs folder in this repo.

Inspiration
- This project was inspired by OD-MAS (Team COD3INE): https://github.com/ECVarsha/OD-MAS_Team-COD3INE

See `Docs/` for the full architecture, component descriptions, and algorithm documentation.

Online search
- The app can fetch online search results using public third-party frontends (Piped/Invidious) by default and falls back to using the YouTube Data API if you provide an API key.
- To set a YouTube API key for search, export YOUTUBE_API_KEY in your environment before running the app or set it with the settings UI (coming soon).

