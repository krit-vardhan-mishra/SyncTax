package com.just_for_fun.synctax.core.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility class for managing app icon variants for contributors
 */
object AppIconManager {

    private const val PACKAGE_NAME = "com.just_for_fun.synctax"
    
    // Activity alias names defined in AndroidManifest.xml
    private const val ALIAS_DEFAULT = "$PACKAGE_NAME.MainActivityDefault"
    private const val ALIAS_VINTAGE = "$PACKAGE_NAME.MainActivityVintage"
    private const val ALIAS_AQUA = "$PACKAGE_NAME.MainActivityAqua"
    private const val ALIAS_PREMIUM = "$PACKAGE_NAME.MainActivityPremium"
    private const val ALIAS_TURBO = "$PACKAGE_NAME.MainActivityTurbo"
    private const val ALIAS_NEON = "$PACKAGE_NAME.MainActivityNeon"

    enum class IconType(val aliasName: String, val iconId: String) {
        DEFAULT(ALIAS_DEFAULT, "default"),
        VINTAGE(ALIAS_VINTAGE, "vintage"),
        AQUA(ALIAS_AQUA, "aqua"),
        PREMIUM(ALIAS_PREMIUM, "premium"),
        TURBO(ALIAS_TURBO, "turbo"),
        NEON(ALIAS_NEON, "neon")
    }

    /**
     * Get IconType from string id
     */
    fun getIconTypeFromId(id: String): IconType {
        return IconType.entries.find { it.iconId == id } ?: IconType.DEFAULT
    }

    /**
     * Get the currently active icon type
     */
    fun getCurrentIconType(context: Context): IconType {
        val pm = context.packageManager

        return IconType.entries.find { iconType ->
            isActivityAliasEnabled(pm, context, iconType.aliasName)
        } ?: IconType.DEFAULT
    }

    /**
     * Get current icon type as string id
     */
    fun getCurrentIconId(context: Context): String {
        return getCurrentIconType(context).iconId
    }

    /**
     * Switch to the specified icon type
     * Note: Icon changes take effect after app restart
     */
    fun setIconType(context: Context, iconType: IconType) {
        val pm = context.packageManager

        // Disable all aliases first
        IconType.entries.forEach { type ->
            setActivityAliasEnabled(pm, context, type.aliasName, false)
        }

        // Enable the selected one
        setActivityAliasEnabled(pm, context, iconType.aliasName, true)
    }

    /**
     * Switch to the specified icon type by string id
     */
    fun setIconById(context: Context, iconId: String) {
        val iconType = getIconTypeFromId(iconId)
        setIconType(context, iconType)
    }

    private fun isActivityAliasEnabled(pm: PackageManager, context: Context, aliasName: String): Boolean {
        return try {
            val componentName = ComponentName(context.packageName, aliasName)
            val state = pm.getComponentEnabledSetting(componentName)
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            false
        }
    }

    private fun setActivityAliasEnabled(pm: PackageManager, context: Context, aliasName: String, enabled: Boolean) {
        try {
            val componentName = ComponentName(context.packageName, aliasName)
            val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                          else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
}
