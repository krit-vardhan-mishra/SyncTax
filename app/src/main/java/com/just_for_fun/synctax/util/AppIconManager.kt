package com.just_for_fun.synctax.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility class for managing app icon variants for contributors
 */
object AppIconManager {

    private const val MAIN_ACTIVITY = "com.just_for_fun.synctax.MainActivity"
    private const val NEON_ACTIVITY = "com.just_for_fun.synctax.MainActivityNeon"
    private const val MONO_ACTIVITY = "com.just_for_fun.synctax.MainActivityMono"

    enum class IconType {
        DEFAULT,
        NEON,
        MONOCHROME
    }

    /**
     * Get the currently active icon type
     */
    fun getCurrentIconType(context: Context): IconType {
        val pm = context.packageManager

        return when {
            isActivityEnabled(pm, context, NEON_ACTIVITY) -> IconType.NEON
            isActivityEnabled(pm, context, MONO_ACTIVITY) -> IconType.MONOCHROME
            else -> IconType.DEFAULT
        }
    }

    /**
     * Switch to the specified icon type
     * Note: Icon changes take effect after app restart
     */
    fun setIconType(context: Context, iconType: IconType) {
        val pm = context.packageManager

        // Disable all aliases first
        setActivityEnabled(pm, context, NEON_ACTIVITY, false)
        setActivityEnabled(pm, context, MONO_ACTIVITY, false)

        // Enable the selected one (or none for default)
        when (iconType) {
            IconType.NEON -> setActivityEnabled(pm, context, NEON_ACTIVITY, true)
            IconType.MONOCHROME -> setActivityEnabled(pm, context, MONO_ACTIVITY, true)
            IconType.DEFAULT -> {
                // Default is already enabled in manifest
            }
        }
    }

    private fun isActivityEnabled(pm: PackageManager, context: Context, activityName: String): Boolean {
        return try {
            val componentName = ComponentName(context, activityName)
            pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            false
        }
    }

    private fun setActivityEnabled(pm: PackageManager, context: Context, activityName: String, enabled: Boolean) {
        try {
            val componentName = ComponentName(context, activityName)
            val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                          else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
}