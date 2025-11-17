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

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SHOW_QUICK_PICKS_GUIDE = "show_quick_picks_guide"
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_MODE_SYSTEM = "system"
        const val KEY_THEME_MODE_LIGHT = "light"
        const val KEY_THEME_MODE_DARK = "dark"
    }
}
