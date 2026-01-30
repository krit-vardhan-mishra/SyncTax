package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.data.local.entities.Song

/**
 * Content-Based Filtering Agent
 * 
 * Recommends songs based on metadata similarity (artist, album, genre).
 * This complements the behavior-based agents by finding songs with similar
 * audio characteristics even if the user hasn't listened to them yet.
 * 
 * From RECOMMENDATION_LOGIC_NEW.md:
 * - Enables recommendations based on audio features similarity
 * - Songs with similar sounds will be discovered
 * - Works for new songs that lack listening history
 */
class ContentBasedAgent {
    
    companion object {
        // Weights for different similarity factors
        private const val ARTIST_WEIGHT = 0.35
        private const val ALBUM_WEIGHT = 0.25
        private const val GENRE_WEIGHT = 0.30
        private const val DURATION_WEIGHT = 0.10
        
        // Duration tolerance in milliseconds (5 minutes)
        private const val DURATION_TOLERANCE_MS = 5 * 60 * 1000L
    }
    
    /**
     * Calculate content similarity between two songs
     * Returns a score between 0.0 and 1.0
     */
    fun calculateSimilarity(song1: Song, song2: Song): Double {
        if (song1.id == song2.id) return 1.0
        
        var similarity = 0.0
        
        // Artist similarity (exact match or contains)
        similarity += when {
            song1.artist.equals(song2.artist, ignoreCase = true) -> ARTIST_WEIGHT
            song1.artist.contains(song2.artist, ignoreCase = true) || 
                song2.artist.contains(song1.artist, ignoreCase = true) -> ARTIST_WEIGHT * 0.5
            else -> 0.0
        }
        
        // Album similarity
        if (song1.album != null && song2.album != null) {
            similarity += when {
                song1.album.equals(song2.album, ignoreCase = true) -> ALBUM_WEIGHT
                else -> 0.0
            }
        }
        
        // Genre similarity
        if (song1.genre != null && song2.genre != null) {
            similarity += when {
                song1.genre.equals(song2.genre, ignoreCase = true) -> GENRE_WEIGHT
                areGenresSimilar(song1.genre, song2.genre) -> GENRE_WEIGHT * 0.6
                else -> 0.0
            }
        }
        
        // Duration similarity (closer durations = more similar)
        val durationDiff = kotlin.math.abs(song1.duration - song2.duration)
        val durationSim = maxOf(0.0, 1.0 - (durationDiff.toDouble() / DURATION_TOLERANCE_MS))
        similarity += durationSim * DURATION_WEIGHT
        
        return similarity.coerceIn(0.0, 1.0)
    }
    
    /**
     * Check if two genres are similar (handles variations like "Rock", "Hard Rock", "Alternative Rock")
     */
    private fun areGenresSimilar(genre1: String, genre2: String): Boolean {
        val normalized1 = genre1.lowercase().replace("-", " ").replace("_", " ")
        val normalized2 = genre2.lowercase().replace("-", " ").replace("_", " ")
        
        // Check if one contains the other
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            return true
        }
        
        // Check for common base genres
        val baseGenres = listOf("rock", "pop", "hip hop", "rap", "electronic", "jazz", "blues", 
            "classical", "country", "r&b", "soul", "metal", "punk", "indie", "folk", "reggae")
        
        val base1 = baseGenres.find { normalized1.contains(it) }
        val base2 = baseGenres.find { normalized2.contains(it) }
        
        return base1 != null && base1 == base2
    }
    
    /**
     * Find similar songs based on content
     * 
     * @param targetSong The song to find similar songs for
     * @param candidates All available songs to search through
     * @param limit Maximum number of results
     * @param excludeIds Song IDs to exclude from results
     */
    fun findSimilarSongs(
        targetSong: Song,
        candidates: List<Song>,
        limit: Int = 10,
        excludeIds: Set<String> = emptySet()
    ): List<RecommendationResult> {
        return candidates
            .filter { it.id != targetSong.id && it.id !in excludeIds }
            .map { song ->
                val similarity = calculateSimilarity(targetSong, song)
                RecommendationResult(
                    songId = song.id,
                    score = similarity * 100,
                    confidence = calculateConfidence(targetSong, song),
                    reason = buildSimilarityReason(targetSong, song)
                )
            }
            .filter { it.score > 10.0 } // Only include meaningful similarities
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * Calculate confidence based on how much metadata is available
     */
    private fun calculateConfidence(song1: Song, song2: Song): Float {
        var metadataCount = 0
        var matchCount = 0
        
        // Artist always available
        metadataCount++
        if (song1.artist.equals(song2.artist, ignoreCase = true)) matchCount++
        
        // Album may be null
        if (song1.album != null && song2.album != null) {
            metadataCount++
            if (song1.album.equals(song2.album, ignoreCase = true)) matchCount++
        }
        
        // Genre may be null
        if (song1.genre != null && song2.genre != null) {
            metadataCount++
            if (song1.genre.equals(song2.genre, ignoreCase = true)) matchCount++
        }
        
        // Base confidence on metadata availability and match rate
        val availabilityFactor = metadataCount / 3.0
        val matchFactor = if (metadataCount > 0) matchCount.toDouble() / metadataCount else 0.0
        
        return ((availabilityFactor * 0.5 + matchFactor * 0.5) * 100).toFloat()
    }
    
    /**
     * Build a human-readable reason for the similarity
     */
    private fun buildSimilarityReason(song1: Song, song2: Song): String {
        val reasons = mutableListOf<String>()
        
        if (song1.artist.equals(song2.artist, ignoreCase = true)) {
            reasons.add("Same artist")
        } else if (song1.artist.contains(song2.artist, ignoreCase = true) ||
            song2.artist.contains(song1.artist, ignoreCase = true)) {
            reasons.add("Similar artist")
        }
        
        if (song1.album != null && song2.album != null && 
            song1.album.equals(song2.album, ignoreCase = true)) {
            reasons.add("Same album")
        }
        
        if (song1.genre != null && song2.genre != null) {
            if (song1.genre.equals(song2.genre, ignoreCase = true)) {
                reasons.add("Same genre")
            } else if (areGenresSimilar(song1.genre, song2.genre)) {
                reasons.add("Similar genre")
            }
        }
        
        return if (reasons.isEmpty()) {
            "Content-based similarity"
        } else {
            reasons.joinToString(", ")
        }
    }
    
    /**
     * Analyze a song's content features for recommendations
     * Used by FusionAgent to combine with other recommendation sources
     */
    suspend fun analyze(
        targetSong: Song,
        candidates: List<Song>,
        excludeIds: Set<String> = emptySet()
    ): List<RecommendationResult> {
        return findSimilarSongs(targetSong, candidates, limit = 20, excludeIds = excludeIds)
    }
}
