package com.just_for_fun.synctax.core.chaquopy

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.ml.models.SongFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wrapper over the Python ML engine (Chaquopy)
 * The analyzer forwards training and inference requests to the Python module (music_ml.py)
 */
class ChaquopyMusicAnalyzer private constructor(context: Context) {

    private val pythonModule: com.chaquo.python.PyObject

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val python = Python.getInstance()
        pythonModule = python.getModule("music_ml")
        Log.d(TAG, "Chaquopy Music Analyzer initialized")
    }

    suspend fun trainModel(userHistory: List<SongFeatures>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val historyJson = JSONArray().apply {
                    userHistory.forEach { features ->
                        put(JSONObject().apply {
                            put("songId", features.songId)
                            put("features", JSONArray(features.toVector().toList()))
                        })
                    }
                }

                val resultJson = pythonModule.callAttr("train_model", historyJson.toString())
                val result = JSONObject(resultJson.toString())

                val success = result.optBoolean("success", false)
                Log.d(TAG, "Training result: ${result.optString("message")}")

                success
            } catch (e: Exception) {
                Log.e(TAG, "Training failed", e)
                false
            }
        }
    }

    suspend fun getRecommendation(songFeatures: SongFeatures): RecommendationResult {
        return withContext(Dispatchers.IO) {
            try {
                val featuresJson = JSONArray(songFeatures.toVector().toList()).toString()
                val resultJson = pythonModule.callAttr("get_recommendation", featuresJson)
                val result = JSONObject(resultJson.toString())

                RecommendationResult(
                    songId = songFeatures.songId,
                    score = result.optDouble("score", 50.0),
                    confidence = result.optDouble("confidence", 0.5).toFloat(),
                    reason = "Python ML: ${result.optString("message")}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Recommendation failed", e)
                RecommendationResult(
                    songId = songFeatures.songId,
                    score = 50.0,
                    confidence = 0.3f,
                    reason = "ML Error: ${e.message}"
                )
            }
        }
    }

    suspend fun getModelStatus(): ModelStatus {
        return withContext(Dispatchers.IO) {
            try {
                val resultJson = pythonModule.callAttr("get_model_status")
                val result = JSONObject(resultJson.toString())

                ModelStatus(
                    isTrained = result.optBoolean("is_trained", false),
                    hasScorer = result.optBoolean("has_scorer", false),
                    nClusters = result.optInt("n_clusters", 0)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Status check failed", e)
                ModelStatus(isTrained = false, hasScorer = false, nClusters = 0)
            }
        }
    }

    suspend fun resetModel(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val resultJson = pythonModule.callAttr("reset_model")
                val result = JSONObject(resultJson.toString())
                result.optBoolean("success", false)
            } catch (e: Exception) {
                Log.e(TAG, "Model reset failed", e)
                false
            }
        }
    }

    companion object {
        private const val TAG = "ChaquopyMusicAnalyzer"

        @Volatile
        private var INSTANCE: ChaquopyMusicAnalyzer? = null

        fun getInstance(context: Context): ChaquopyMusicAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChaquopyMusicAnalyzer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

data class ModelStatus(
    val isTrained: Boolean,
    val hasScorer: Boolean,
    val nClusters: Int
)