# Contributor Features Suggestions

Based on my exploration of the project files, here's what I found regarding the contributors/creators and existing features for them:

### Current State
- **Special Welcome Screen**: If a user enters a creator name during onboarding, they get a dedicated `SpecialCreatorWelcomeScreenTwo` (the file you're currently editing). This is triggered via `AppConfig.isCreator()` in `WelcomeScreen.kt`.
- **Credits Mention**: The regular welcome screen shows "made by Krit" at the bottom.
- **No Other Dedicated Features**: No credits screen, developer mode, or additional perks exist yet. The app focuses on music playback, recommendations, and search.

### Suggested Additional Features for Contributors
These are lightweight, fun additions that fit the app's music-focused, offline-first nature. They build on the existing special welcome screen without overcomplicating the codebase. Each could be toggled based on `AppConfig.isCreator()` for easy access.

1. **Developer Debug Mode**:
   - **Details**: Add a hidden toggle in settings (visible only to creators) that enables a debug overlay showing real-time app stats (e.g., DB query times, memory usage, active threads, or recommendation engine logs). Include a "Share Debug Log" button to export logs via email/share intent.
   - **Why for Contributors**: Helps with testing and troubleshooting during development. Could integrate with existing logging in `YTMusicRecommender.kt` or DB operations.
   - **Implementation Hint**: Extend `SettingsScreen.kt` with a conditional section; use `AppConfig.isCreator()` to show it.

2. **Contributor Easter Egg Playlist**:
   - **Details**: Auto-create a hidden "Contributor Playlist" with curated songs (e.g., developer favorites or app-themed tracks like "SyncTax Anthem"). It appears in the library only for creators, with a fun icon (e.g., a developer badge).
   - **Why for Contributors**: A playful nod to their work, encouraging exploration. Songs could be hardcoded or pulled from existing recommendations.
   - **Implementation Hint**: Add to `PlaylistRepository` and library UI; trigger creation on first login if creator.

3. **Custom Theme/Avatar Unlock**:
   - **Details**: Unlock exclusive themes (e.g., "Developer Dark Mode" with code-inspired colors) or custom avatars (e.g., pixel art of the contributors). Let creators select from a small set in settings.
   - **Why for Contributors**: Personalizes the app as a thank-you, tying into the music player UI.
   - **Implementation Hint**: Extend user preferences in `UserPreference.kt`; add theme options in `SettingsScreen.kt`.

4. **Beta Feature Access**:
   - **Details**: Grant early access to experimental features (e.g., advanced recommendation filters or export playlists to JSON). Show a "Beta Lab" section in settings with toggles.
   - **Why for Contributors**: Lets them test new ideas, aligning with the app's experimentation focus (e.g., Python models).
   - **Implementation Hint**: Add flags to `UserPreference.kt`; conditionally enable in relevant screens.

5. **Enhanced Credits/About Screen**:
   - **Details**: Add a full "About" screen (accessible from settings) listing contributors with photos/icons, roles, and fun facts (e.g., "Krit: Lead Developer & Music Lover"). Include app version, changelog, and links to repo.
   - **Why for Contributors**: Proper recognition beyond the welcome screen, fostering community.
   - **Implementation Hint**: Create `AboutScreen.kt`; link from `SettingsScreen.kt`; pull names dynamically from `AppConfig`.

These features are minimal (no major new dependencies) and respect the app's architecture. They could be prioritized based on contributor feedback. If you provide more context, I can refine this list!