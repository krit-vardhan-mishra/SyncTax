package com.just_for_fun.synctax.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.data.model.Format

/**
 * Utility class for sorting and filtering audio/video formats based on user preferences
 * Similar to ytdlnis FormatUtil implementation
 */
class FormatUtil(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val PREF_AUDIO_FORMAT_ID = "audio_format_id_preference"
        const val PREF_AUDIO_CODEC = "audio_codec"
        const val PREF_AUDIO_CONTAINER = "audio_format"
        const val PREF_FORMAT_IMPORTANCE = "format_importance"
        const val PREF_CONTAINER_OVER_CODEC = "prefer_container_over_codec"

        // Default format importance order
        const val DEFAULT_FORMAT_IMPORTANCE = "codec,container,filesize"

        // Common audio codecs
        val AUDIO_CODECS = listOf("opus", "mp4a", "aac", "vorbis", "mp3")
        val AUDIO_CONTAINERS = listOf("webm", "m4a", "mp3", "ogg")
    }

    /**
     * Get generic audio formats as fallback when no formats are available
     */
    fun getGenericAudioFormats(): List<Format> {
        return listOf(
            Format(
                format_id = "140",
                container = "m4a",
                vcodec = "none",
                acodec = "mp4a.40.2",
                filesize = 0,
                format_note = "medium (128k)",
                tbr = "128"
            ),
            Format(
                format_id = "251",
                container = "webm",
                vcodec = "none",
                acodec = "opus",
                filesize = 0,
                format_note = "medium (160k)",
                tbr = "160"
            ),
            Format(
                format_id = "250",
                container = "webm",
                vcodec = "none",
                acodec = "opus",
                filesize = 0,
                format_note = "medium (70k)",
                tbr = "70"
            ),
            Format(
                format_id = "249",
                container = "webm",
                vcodec = "none",
                acodec = "opus",
                filesize = 0,
                format_note = "low (50k)",
                tbr = "50"
            ),
            Format(
                format_id = "139",
                container = "m4a",
                vcodec = "none",
                acodec = "mp4a.40.5",
                filesize = 0,
                format_note = "low (48k)",
                tbr = "48"
            ),
            Format(
                format_id = "141",
                container = "m4a",
                vcodec = "none",
                acodec = "mp4a.40.2",
                filesize = 0,
                format_note = "high (256k)",
                tbr = "256"
            )
        )
    }

    /**
     * Get generic video formats as fallback
     */
    fun getGenericVideoFormats(): List<Format> {
        return listOf(
            Format(
                format_id = "18",
                container = "mp4",
                vcodec = "avc1",
                acodec = "mp4a",
                filesize = 0,
                format_note = "360p"
            ),
            Format(
                format_id = "22",
                container = "mp4",
                vcodec = "avc1",
                acodec = "mp4a",
                filesize = 0,
                format_note = "720p"
            )
        )
    }

    /**
     * Get audio format importance order from preferences
     * Returns list like: ["codec", "container", "filesize"]
     */
    fun getAudioFormatImportance(): List<String> {
        val importance = sharedPreferences.getString(
            PREF_FORMAT_IMPORTANCE,
            DEFAULT_FORMAT_IMPORTANCE
        ) ?: DEFAULT_FORMAT_IMPORTANCE

        return importance.split(",").map { it.trim() }
    }

    /**
     * Sort audio formats based on user preferences
     * Uses multi-level comparison: preferred codec → container → bitrate → filesize
     */
    fun sortAudioFormats(formats: List<Format>): List<Format> {
        if (formats.isEmpty()) return formats

        val preferredFormatId = sharedPreferences.getString(PREF_AUDIO_FORMAT_ID, "")
        val preferredCodec = sharedPreferences.getString(PREF_AUDIO_CODEC, "opus") ?: "opus"
        val preferredContainer = sharedPreferences.getString(PREF_AUDIO_CONTAINER, "webm") ?: "webm"
        val containerOverCodec = sharedPreferences.getBoolean(PREF_CONTAINER_OVER_CODEC, false)
        val importance = getAudioFormatImportance()

        return formats.sortedWith(
            compareBy<Format> { format ->
                // Priority 0: Exact format ID match (highest priority)
                if (!preferredFormatId.isNullOrEmpty() && importance.contains("id")) {
                    format.format_id != preferredFormatId
                } else {
                    false
                }
            }.thenBy { format ->
                // Priority 1: Codec or Container (based on preference)
                if (containerOverCodec && importance.contains("container")) {
                    format.container != preferredContainer
                } else if (importance.contains("codec")) {
                    !format.acodec.contains(preferredCodec, ignoreCase = true)
                } else {
                    false
                }
            }.thenBy { format ->
                // Priority 2: Container or Codec (opposite of above)
                if (containerOverCodec && importance.contains("codec")) {
                    !format.acodec.contains(preferredCodec, ignoreCase = true)
                } else if (importance.contains("container")) {
                    format.container != preferredContainer
                } else {
                    false
                }
            }.thenBy { format ->
                // Priority 3: Higher bitrate (descending)
                if (importance.contains("bitrate")) {
                    val tbr = format.tbr?.toDoubleOrNull() ?: 0.0
                    -tbr // Negative for descending order
                } else {
                    0.0
                }
            }.thenBy { format ->
                // Priority 4: Smaller filesize (ascending)
                if (importance.contains("filesize")) {
                    format.filesize
                } else {
                    0L
                }
            }
        )
    }

    /**
     * Sort video formats based on resolution preference
     */
    fun sortVideoFormats(formats: List<Format>, preferBest: Boolean = true): List<Format> {
        if (formats.isEmpty()) return formats

        return if (preferBest) {
            // Sort by resolution descending (best quality first)
            formats.sortedWith(
                compareBy<Format> { format ->
                    // Extract height from format_note (e.g., "1080p" -> 1080)
                    val height = extractHeight(format.format_note)
                    -height // Negative for descending order
                }.thenBy { format ->
                    // Then by bitrate descending
                    val tbr = format.tbr?.toDoubleOrNull() ?: 0.0
                    -tbr
                }
            )
        } else {
            // Sort by filesize ascending (smallest first)
            formats.sortedBy { it.filesize }
        }
    }

    /**
     * Filter formats by category
     */
    fun filterFormatsByCategory(
        formats: List<Format>,
        category: FormatCategory,
        isAudioDownload: Boolean
    ): List<Format> {
        return when (category) {
            FormatCategory.ALL -> formats

            FormatCategory.SUGGESTED -> {
                if (isAudioDownload) {
                    sortAudioFormats(formats)
                } else {
                    val audioFormats = formats.filter { isAudioOnly(it) }
                    val videoFormats = formats.filter { !isAudioOnly(it) }
                    sortVideoFormats(videoFormats) + sortAudioFormats(audioFormats)
                }
            }

            FormatCategory.SMALLEST -> {
                // Group by quality and pick smallest filesize in each group
                formats
                    .filter { it.filesize > 0 }
                    .groupBy { normalizeQuality(it.format_note) }
                    .map { it.value.minByOrNull { format -> format.filesize }!! }
                    .sortedBy { it.filesize }
            }

            FormatCategory.GENERIC -> {
                if (isAudioDownload) {
                    getGenericAudioFormats()
                } else {
                    getGenericVideoFormats()
                }
            }
        }
    }

    /**
     * Check if format is audio-only
     */
    fun isAudioOnly(format: Format): Boolean {
        return format.vcodec.isBlank() ||
                format.vcodec == "none" ||
                format.format_note.contains("audio", ignoreCase = true) ||
                format.format_id in listOf("140", "139", "141", "249", "250", "251", "171", "172")
    }

    /**
     * Extract height from format note (e.g., "1080p60" -> 1080)
     */
    private fun extractHeight(formatNote: String): Int {
        val cleaned = formatNote
            .lowercase()
            .replace(Regex("[^0-9x]"), "") // Remove non-numeric except 'x'

        // Check for "widthxheight" format (e.g., "1920x1080")
        if (cleaned.contains("x")) {
            val parts = cleaned.split("x")
            if (parts.size == 2) {
                return parts[1].toIntOrNull() ?: 0
            }
        }

        // Otherwise extract first number (e.g., "1080")
        return cleaned.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
    }

    /**
     * Normalize quality string for grouping
     * "1080p60" -> "1080", "medium (128k)" -> "128"
     */
    private fun normalizeQuality(formatNote: String): String {
        var tmp = formatNote.lowercase()

        // Remove brackets and content
        tmp = tmp.replace(Regex("\\s*\\(.*?\\)"), "")

        // Remove "p" suffix and "60fps" indicators
        if (tmp.endsWith("060")) {
            tmp = tmp.removeSuffix("60")
        }
        tmp = tmp.removeSuffix("p")

        // Handle "widthxheight" format
        val split = tmp.split("x")
        if (split.size > 1) {
            tmp = split[1]
        }

        // Extract just numbers
        return tmp.filter { it.isDigit() }
    }

    /**
     * Format categories for filtering
     */
    enum class FormatCategory {
        ALL,        // Show all formats
        SUGGESTED,  // Preference-based sorting
        SMALLEST,   // Group by quality, show smallest in each group
        GENERIC     // Hardcoded fallback formats
    }

    /**
     * Get human-readable format description
     */
    fun getFormatDescription(format: Format): String {
        val quality = format.format_note.ifEmpty { "unknown" }
        val codec = format.acodec.takeIf { it.isNotEmpty() && it != "none" }
            ?: format.vcodec.takeIf { it.isNotEmpty() && it != "none" }
            ?: "unknown"
        val container = format.container.ifEmpty { "unknown" }
        val size = if (format.filesize > 0) {
            " • ${formatFileSize(format.filesize)}"
        } else {
            ""
        }

        return "$quality • $codec • $container$size"
    }


    /**
     * Get the best audio format based on preferences
     */
    fun getBestAudioFormat(formats: List<Format>): Format? {
        val sorted = sortAudioFormats(formats)
        return sorted.firstOrNull()
    }

    /**
     * Get quality label based on bitrate/format
     */
    fun getQualityLabel(format: Format): String {
        val bitrate = format.tbr?.replace("k", "")?.trim()?.toIntOrNull() ?: 0

        return when {
            bitrate >= 192 -> "High quality"
            bitrate >= 128 -> "Medium quality"
            bitrate >= 96 -> "Good quality"
            bitrate > 0 -> "Low quality"
            else -> {
                // Fallback to format note
                when {
                    format.format_note.contains("high", ignoreCase = true) -> "High quality"
                    format.format_note.contains("medium", ignoreCase = true) -> "Medium quality"
                    format.format_note.contains("low", ignoreCase = true) -> "Low quality"
                    else -> "Audio"
                }
            }
        }
    }

    /**
     * Get display name for format (codec + bitrate)
     */
    fun getFormatDisplayName(format: Format): String {
        val codec = when {
            format.acodec.contains("opus", ignoreCase = true) -> "Opus"
            format.acodec.contains("aac", ignoreCase = true) ||
                    format.acodec.contains("mp4a", ignoreCase = true) -> "AAC"
            format.acodec.contains("mp3", ignoreCase = true) -> "MP3"
            format.acodec.contains("vorbis", ignoreCase = true) -> "Vorbis"
            format.container.isNotBlank() -> format.container.uppercase()
            else -> "Audio"
        }

        val bitrate = formatBitrate(format.tbr)
        return if (bitrate.isNotBlank()) "$codec • $bitrate" else codec
    }

    /**
     * Format bitrate to human-readable string
     */
    fun formatBitrate(tbr: String?): String {
        if (tbr.isNullOrBlank()) return ""

        val cleaned = tbr.replace("k", "").trim()
        val kbps = cleaned.toIntOrNull() ?: return tbr

        return when {
            kbps >= 1000 -> String.format("%.1f Mbps", kbps / 1000.0)
            else -> "${kbps} kbps"
        }
    }

    /**
     * Format file size to human-readable string
     */

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
