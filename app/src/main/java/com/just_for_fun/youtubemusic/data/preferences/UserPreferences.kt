package com.just_for_fun.youtubemusic.data.preferences

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

    fun saveUserName(name: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }
        _userName.value = name
        _isFirstLaunch.value = false
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

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
