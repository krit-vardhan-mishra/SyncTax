package com.just_for_fun.synctax.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.data.local.entities.Format
import kotlin.math.min

class FormatUtil(private var context: Context) {
    private val sharedPreferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @SuppressLint("RestrictedApi")
    fun getAudioFormatImportance() : List<String> {
        val preferredFormatSize = sharedPreferences.getString("preferred_format_size", "")

        if (sharedPreferences.getBoolean("use_format_sorting", false)) {
            val itemValues = context.getStringArray(R.array.format_importance_audio_values).toMutableList()
            val orderPreferences = sharedPreferences.getString("format_importance_audio", itemValues.joinToString(","))!!.split(",").toMutableList()

            if (preferredFormatSize == "smallest") {
                orderPreferences.remove("file_size")
                orderPreferences.add(0,"smallsize")
            }

            val preferContainerOverCodec = sharedPreferences.getBoolean("prefer_container_over_codec_audio", false)
            if(preferContainerOverCodec) {
                orderPreferences.remove("codec")
            }

            return orderPreferences
        }else {
            val formatImportance = mutableListOf("id", "language", "codec", "container")
            if (preferredFormatSize == "smallest") {
                formatImportance.add("smallsize")
            }else if (preferredFormatSize == "largest") {
                formatImportance.add("file_size")
            }

            val preferContainerOverCodec = sharedPreferences.getBoolean("prefer_container_over_codec_audio", false)
            if(preferContainerOverCodec) {
                formatImportance.remove("codec")
            }

            val noDRC = sharedPreferences.getBoolean("no_prefer_drc_audio", false)
            if (noDRC) {
                formatImportance.add(0,"no_drc")
            }

            return formatImportance
        }
    }

    fun getGenericAudioFormats(resources: Resources) : MutableList<Format>{
        val audioFormatIDPreference = sharedPreferences.getString("format_id_audio", "").toString().split(",").filter { it.isNotEmpty() }
        val audioFormats = resources.getStringArray(R.array.audio_formats)
        val audioFormatsValues = resources.getStringArray(R.array.audio_formats_values)
        val formats = mutableListOf<Format>()
        val containerPreference = sharedPreferences.getString("audio_format", "")
        val acodecPreference = sharedPreferences.getString("audio_codec", "")!!.run {
            if (this.isEmpty()){
                "Deault"
            }else{
                val audioCodecs = resources.getStringArray(R.array.audio_codec)
                val audioCodecsValues = resources.getStringArray(R.array.audio_codec_values)
                audioCodecs[audioCodecsValues.indexOf(this)]
            }
        }
        audioFormats.forEachIndexed { idx, it -> formats.add(Format(audioFormatsValues[idx], containerPreference!!,"",acodecPreference!!, "",0, it)) }
        audioFormatIDPreference.forEach { formats.add(Format(it, containerPreference!!,"","Preferred format ID"), "",1, it)) }
        return formats
    }
}
