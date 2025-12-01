package com.just_for_fun.synctax.util

import android.content.Context
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * Utility class for reading application configuration from app_config.properties in assets
 */
object AppConfig {

    // --- CHANGE 1: Use the new asset file name ---
    private const val PROPERTIES_FILE = "app_config.properties"
    private const val CREATOR_NAMES_KEY = "APP_CREATOR_NAMES"

    /**
     * Get the list of app creator names from app_config.properties
     * @param context Android context
     * @return Set of creator names (lowercase for case-insensitive comparison)
     */
    fun getCreatorNames(context: Context): Set<String> {
        return try {
            val properties = Properties()

            // This reads from the assets folder.
            val inputStream = context.assets.open(PROPERTIES_FILE)

            inputStream.use { stream ->
                properties.load(stream)
            }

            val creatorNamesString = properties.getProperty(CREATOR_NAMES_KEY, "")
            if (creatorNamesString.isNotEmpty()) {
                creatorNamesString
                    .split(",")
                    .map { it.trim().lowercase() }
                    .toSet()
            } else {
                emptySet()
            }
        } catch (e: IOException) {
            // If the asset file can't be read, this message helps in debugging
            e.printStackTrace()
            emptySet()
        }
    }

    /**
     * Check if a given name is an app creator
     * @param context Android context
     * @param name Name to check (will be trimmed and lowercased)
     * @return true if the name matches a creator name
     */
    fun isCreator(context: Context, name: String): Boolean {
        val trimmedName = name.trim().lowercase()
        val creatorNames = getCreatorNames(context)
        return creatorNames.contains(trimmedName)
    }
}