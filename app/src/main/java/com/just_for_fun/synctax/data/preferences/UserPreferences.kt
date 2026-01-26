package com.just_for_fun.synctax.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_preferences",
        Context.MODE_PRIVATE
    )

    private val _userName = MutableStateFlow(getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(isFirstLaunch())
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _showQuickPicksGuide = MutableStateFlow(isQuickPicksGuideEnabled())
    val showQuickPicksGuide: StateFlow<Boolean> = _showQuickPicksGuide.asStateFlow()

    // --- Album art scanning preference ---
    private val _scanLocalAlbumArt = MutableStateFlow(isScanLocalAlbumArtEnabled())
    val scanLocalAlbumArt: StateFlow<Boolean> = _scanLocalAlbumArt.asStateFlow()

    // --- Online history count preference ---
    private val _onlineHistoryCount = MutableStateFlow(getOnlineHistoryCount())
    val onlineHistoryCount: StateFlow<Int> = _onlineHistoryCount.asStateFlow()

    // --- Recommendations count preference ---
    private val _recommendationsCount = MutableStateFlow(getRecommendationsCount())
    val recommendationsCount: StateFlow<Int> = _recommendationsCount.asStateFlow()

    fun isScanLocalAlbumArtEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCAN_LOCAL_ALBUM_ART, false)
    }

    fun setScanLocalAlbumArt(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCAN_LOCAL_ALBUM_ART, enabled).apply()
        _scanLocalAlbumArt.value = enabled
    }

    fun getOnlineHistoryCount(): Int {
        return prefs.getInt(KEY_ONLINE_HISTORY_COUNT, 10)
    }

    fun setOnlineHistoryCount(count: Int) {
        val clamped = count.coerceIn(1, 100)
        prefs.edit().putInt(KEY_ONLINE_HISTORY_COUNT, clamped).apply()
        _onlineHistoryCount.value = clamped
    }

    fun getRecommendationsCount(): Int {
        return prefs.getInt(KEY_RECOMMENDATIONS_COUNT, 20)
    }

    fun setRecommendationsCount(count: Int) {
        val clamped = count.coerceIn(1, 100)
        prefs.edit().putInt(KEY_RECOMMENDATIONS_COUNT, clamped).apply()
        _recommendationsCount.value = clamped
    }

    // --- new: persisted scan paths (list of tree URIs as strings) ---
    private val _scanPaths = MutableStateFlow(getScanPathsPref())
    val scanPaths: StateFlow<List<String>> = _scanPaths.asStateFlow()

    private fun getScanPathsPref(): List<String> {
        val raw = prefs.getString(KEY_SCAN_PATHS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it }.filter { it.isNotBlank() }
    }

    fun addScanPath(uriString: String) {
        val current = getScanPathsPref().toMutableList()
        if (!current.contains(uriString)) {
            current.add(uriString)
            prefs.edit().putString(KEY_SCAN_PATHS, current.joinToString("|")).apply()
            _scanPaths.value = current
        }
    }

    fun removeScanPath(uriString: String) {
        val current = getScanPathsPref().toMutableList()
        if (current.remove(uriString)) {
            prefs.edit().putString(KEY_SCAN_PATHS, if (current.isEmpty()) "" else current.joinToString("|")).apply()
            _scanPaths.value = current
        }
    }
    // --- end new ---

    fun saveUserName(name: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }
        _userName.value = name
        _isFirstLaunch.value = false
    }

    fun isQuickPicksGuideEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_QUICK_PICKS_GUIDE, true)
    }

    fun setQuickPicksGuide(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_QUICK_PICKS_GUIDE, show).apply()
        _showQuickPicksGuide.value = show
    }

    fun getYouTubeApiKey(): String? {
        return prefs.getString(KEY_YOUTUBE_API_KEY, null)
    }

    fun setYouTubeApiKey(key: String?) {
        prefs.edit().putString(KEY_YOUTUBE_API_KEY, key).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun getUserInitial(): String {
        val name = getUserName()
        return if (name.isNotEmpty()) name.first().uppercase() else "M"
    }

    // Theme mode: "system" | "light" | "dark"
    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, KEY_THEME_MODE_SYSTEM) ?: KEY_THEME_MODE_SYSTEM
    }

    // Guide preferences
    fun isGuideShown(screen: String): Boolean {
        return prefs.getBoolean("guide_shown_$screen", false)
    }

    fun setGuideShown(screen: String, shown: Boolean = true) {
        prefs.edit().putBoolean("guide_shown_$screen", shown).apply()
    }

    fun shouldShowGuide(screen: String): Boolean {
        return !isGuideShown(screen)
    }

    // Directory selection dialog preference
    fun isDirectorySelectionShown(): Boolean {
        return prefs.getBoolean(KEY_DIRECTORY_SELECTION_SHOWN, false)
    }

    fun setDirectorySelectionShown(shown: Boolean = true) {
        prefs.edit().putBoolean(KEY_DIRECTORY_SELECTION_SHOWN, shown).apply()
    }

    // --- Sleep Timer preference ---
    private val _sleepTimerMinutes = MutableStateFlow(getSleepTimerMinutes())
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    fun getSleepTimerMinutes(): Int {
        return prefs.getInt(KEY_SLEEP_TIMER_MINUTES, 0) // 0 = disabled
    }

    fun setSleepTimerMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER_MINUTES, minutes).apply()
        _sleepTimerMinutes.value = minutes
    }

    // --- Crossfade preference ---
    private val _crossfadeEnabled = MutableStateFlow(isCrossfadeEnabled())
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    fun isCrossfadeEnabled(): Boolean {
        return prefs.getBoolean(KEY_CROSSFADE_ENABLED, false)
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CROSSFADE_ENABLED, enabled).apply()
        _crossfadeEnabled.value = enabled
    }

    // --- Offline Songs Cache preference ---
    private val _offlineCacheEnabled = MutableStateFlow(isOfflineCacheEnabled())
    val offlineCacheEnabled: StateFlow<Boolean> = _offlineCacheEnabled.asStateFlow()

    private val _offlineCacheCount = MutableStateFlow(getOfflineCacheCount())
    val offlineCacheCount: StateFlow<Int> = _offlineCacheCount.asStateFlow()

    fun isOfflineCacheEnabled(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_CACHE_ENABLED, false)
    }

    fun setOfflineCacheEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_CACHE_ENABLED, enabled).apply()
        _offlineCacheEnabled.value = enabled
    }

    fun getOfflineCacheCount(): Int {
        return prefs.getInt(KEY_OFFLINE_CACHE_COUNT, 50)
    }

    fun setOfflineCacheCount(count: Int) {
        val clamped = count.coerceIn(0, 500)
        prefs.edit().putInt(KEY_OFFLINE_CACHE_COUNT, clamped).apply()
        _offlineCacheCount.value = clamped
    }

    // --- Last Played Song for preloading on splash screen ---
    
    /**
     * Data class representing the last played song for restoration
     */
    data class LastPlayedSong(
        val songId: String?,
        val isOnline: Boolean,
        val videoId: String?,
        val title: String?,
        val artist: String?,
        val thumbnailUrl: String?,
        val watchUrl: String?
    )
    
    /**
     * Saves the last played song info for restoration on next app launch
     */
    fun saveLastPlayedSong(
        songId: String?,
        isOnline: Boolean,
        videoId: String? = null,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        watchUrl: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_LAST_SONG_ID, songId)
            putBoolean(KEY_LAST_SONG_IS_ONLINE, isOnline)
            putString(KEY_LAST_SONG_VIDEO_ID, videoId)
            putString(KEY_LAST_SONG_TITLE, title)
            putString(KEY_LAST_SONG_ARTIST, artist)
            putString(KEY_LAST_SONG_THUMBNAIL, thumbnailUrl)
            putString(KEY_LAST_SONG_WATCH_URL, watchUrl)
            apply()
        }
    }
    
    /**
     * Gets the last played song info for preloading during splash
     */
    fun getLastPlayedSong(): LastPlayedSong? {
        val songId = prefs.getString(KEY_LAST_SONG_ID, null)
        val isOnline = prefs.getBoolean(KEY_LAST_SONG_IS_ONLINE, false)
        
        // Return null if no song was saved
        if (songId == null && !isOnline) return null
        
        return LastPlayedSong(
            songId = songId,
            isOnline = isOnline,
            videoId = prefs.getString(KEY_LAST_SONG_VIDEO_ID, null),
            title = prefs.getString(KEY_LAST_SONG_TITLE, null),
            artist = prefs.getString(KEY_LAST_SONG_ARTIST, null),
            thumbnailUrl = prefs.getString(KEY_LAST_SONG_THUMBNAIL, null),
            watchUrl = prefs.getString(KEY_LAST_SONG_WATCH_URL, null)
        )
    }
    
    /**
     * Clears the last played song (e.g., when user explicitly stops music)
     */
    fun clearLastPlayedSong() {
        prefs.edit().apply {
            remove(KEY_LAST_SONG_ID)
            remove(KEY_LAST_SONG_IS_ONLINE)
            remove(KEY_LAST_SONG_VIDEO_ID)
            remove(KEY_LAST_SONG_TITLE)
            remove(KEY_LAST_SONG_ARTIST)
            remove(KEY_LAST_SONG_THUMBNAIL)
            remove(KEY_LAST_SONG_WATCH_URL)
            apply()
        }
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SHOW_QUICK_PICKS_GUIDE = "show_quick_picks_guide"
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DIRECTORY_SELECTION_SHOWN = "directory_selection_shown"
        const val KEY_THEME_MODE_SYSTEM = "system"
        const val KEY_THEME_MODE_LIGHT = "light"
        const val KEY_THEME_MODE_DARK = "dark"

        // new: key for scan paths
        private const val KEY_SCAN_PATHS = "scan_paths"

        // Album art scanning from local library
        private const val KEY_SCAN_LOCAL_ALBUM_ART = "scan_local_album_art"

        // Online history count
        private const val KEY_ONLINE_HISTORY_COUNT = "online_history_count"

        // Recommendations count
        private const val KEY_RECOMMENDATIONS_COUNT = "recommendations_count"

        // Guide screen identifiers
        const val GUIDE_HOME = "home"
        const val GUIDE_PLAYER = "player"
        const val GUIDE_SEARCH = "search"
        const val GUIDE_LIBRARY = "library"
        const val GUIDE_QUICK_PICKS = "quick_picks"

        // Sleep timer
        private const val KEY_SLEEP_TIMER_MINUTES = "sleep_timer_minutes"

        // Crossfade/gapless playback
        private const val KEY_CROSSFADE_ENABLED = "crossfade_enabled"

        // Offline songs cache
        private const val KEY_OFFLINE_CACHE_ENABLED = "offline_cache_enabled"
        private const val KEY_OFFLINE_CACHE_COUNT = "offline_cache_count"
        
        // Last played song for splash screen preloading
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_SONG_IS_ONLINE = "last_song_is_online"
        private const val KEY_LAST_SONG_VIDEO_ID = "last_song_video_id"
        private const val KEY_LAST_SONG_TITLE = "last_song_title"
        private const val KEY_LAST_SONG_ARTIST = "last_song_artist"
        private const val KEY_LAST_SONG_THUMBNAIL = "last_song_thumbnail"
        private const val KEY_LAST_SONG_WATCH_URL = "last_song_watch_url"
    }
}
