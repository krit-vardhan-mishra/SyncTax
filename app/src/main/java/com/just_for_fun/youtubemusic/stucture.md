app/
├── src/
│   ├── main/
│   │   ├── java/com/just_for_fun/youtubemusic/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MusicApplication.kt
│   │   │   ├── core/
│   │   │   │   ├── ml/
│   │   │   │   │   ├── MusicRecommendationManager.kt
│   │   │   │   │   ├── agents/
│   │   │   │   │   │   ├── StatisticalAgent.kt
│   │   │   │   │   │   ├── CollaborativeFilteringAgent.kt
│   │   │   │   │   │   ├── FusionAgent.kt
│   │   │   │   │   │   └── RecommendationAgent.kt
│   │   │   │   │   └── models/
│   │   │   │   │       ├── UserProfile.kt
│   │   │   │   │       ├── SongFeatures.kt
│   │   │   │   │       └── RecommendationResult.kt
│   │   │   │   ├── chaquopy/
│   │   │   │   │   └── ChaquopyMusicAnalyzer.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── MusicDatabase.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── SongDao.kt
│   │   │   │   │   │   │   ├── ListeningHistoryDao.kt
│   │   │   │   │   │   │   └── UserPreferenceDao.kt
│   │   │   │   │   │   └── entities/
│   │   │   │   │   │       ├── Song.kt
│   │   │   │   │   │       ├── ListeningHistory.kt
│   │   │   │   │   │       └── UserPreference.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── MusicRepository.kt
│   │   │   │   ├── player/
│   │   │   │   │   ├── MusicPlayer.kt
│   │   │   │   │   └── PlaybackCollector.kt
│   │   │   │   └── utils/
│   │   │   │       ├── MathUtils.kt
│   │   │   │       └── VectorDatabase.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── QuickPicksScreen.kt
│   │   │   │   │   └── LibraryScreen.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── SongCard.kt
│   │   │   │   │   ├── RecommendationSection.kt
│   │   │   │   │   └── PlayerControls.kt
│   │   │   │   └── viewmodels/
│   │   │   │       ├── HomeViewModel.kt
│   │   │   │       └── PlayerViewModel.kt
│   │   │   └── di/
│   │   │       └── AppModule.kt
│   │   ├── python/
│   │   │   └── music_ml.py
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── test/
└── build.gradle.kts