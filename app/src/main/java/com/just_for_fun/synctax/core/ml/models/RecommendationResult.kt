package com.just_for_fun.synctax.core.ml.models

data class RecommendationResult(
    val songId: String,
    val score: Double,
    val confidence: Float,
    val reason: String
)

data class QuickPicksResult(
    val recommendations: List<RecommendationResult>,
    val modelVersion: String,
    val generatedAt: Long = System.currentTimeMillis()
)