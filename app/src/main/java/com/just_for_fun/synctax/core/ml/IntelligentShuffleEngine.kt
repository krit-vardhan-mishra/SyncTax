package com.just_for_fun.synctax.core.ml

import com.just_for_fun.synctax.core.ml.agents.CollaborativeFilteringAgent
import com.just_for_fun.synctax.core.ml.agents.SequenceRecommenderAgent
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.ml.models.SongFeatures
import com.just_for_fun.synctax.core.utils.MathUtils
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.local.entities.UserPreference
import kotlinx.coroutines.flow.first
import kotlin.math.exp
import kotlin.random.Random

/**
 * Intelligent Shuffle Engine that solves the "bad shuffle" problem.
 * 
 * Instead of random permutation, this engine uses probability-weighted shuffle:
 * 
 * Score formula for each song:
 * score(song) = similarity + transition_weight - skip_penalty - recent_penalty + exploration_bonus
 * 
 * Features:
 * 1. Not same order every time (exploration factor)
 * 2. Avoids disliked/frequently skipped songs
 * 3. Learns user taste through transitions
 * 4. Works fully offline
 * 5. Maintains diversity (artist/genre spread)
 * 
 * Algorithm:
 * 1. Get all candidate songs
 * 2. Calculate weighted score for each
 * 3. Use weighted random selection (roulette wheel)
 * 4. Apply diversity constraints (no same artist in a row)
 * 5. Build queue iteratively
 */
class IntelligentShuffleEngine(
    private val database: MusicDatabase,
    private val collaborativeAgent: CollaborativeFilteringAgent,
    private val sequenceAgent: SequenceRecommenderAgent
) {
    companion object {
        // Scoring weights
        const val SIMILARITY_WEIGHT = 0.25
        const val TRANSITION_WEIGHT = 0.35
        const val PREFERENCE_WEIGHT = 0.20
        const val RECENCY_WEIGHT = 0.10
        const val DIVERSITY_WEIGHT = 0.10
        
        // Penalties
        const val SKIP_PENALTY_FACTOR = 0.5
        const val RECENT_PLAY_PENALTY = 0.3
        const val SAME_ARTIST_PENALTY = 0.4
        
        // Exploration
        const val EXPLORATION_RATE = 0.15  // 15% random selection
        const val MAX_RECENT_ARTISTS = 3   // Don't repeat artists within this window
        
        // Time decay (half-life in days)
        const val RECENCY_HALF_LIFE_DAYS = 7.0
    }

    /**
     * Generate an intelligently shuffled queue
     * 
     * @param songs All candidate songs
     * @param currentSongId The starting song (if any)
     * @param recentlyPlayed Songs played recently (to avoid)
     * @param count Target queue size
     * @return Shuffled song list optimized for user experience
     */
    suspend fun generateIntelligentQueue(
        songs: List<Song>,
        currentSongId: String? = null,
        recentlyPlayed: List<String> = emptyList(),
        count: Int = songs.size
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()
        if (songs.size == 1) return songs

        // Get user preferences for scoring
        val preferences = database.userPreferenceDao()
            .getTopPreferences(1000)
            .first()
            .associateBy { it.songId }

        // Get transition data for sequence-aware shuffling
        val transitionScores = if (currentSongId != null) {
            getTransitionScores(currentSongId)
        } else emptyMap()

        // Build queue iteratively
        val queue = mutableListOf<Song>()
        val remaining = songs.toMutableList()
        val recentArtists = mutableListOf<String>()
        val recentlyPlayedSet = recentlyPlayed.toSet()

        // If we have a current song, it should be first
        currentSongId?.let { currentId ->
            remaining.find { it.id == currentId }?.let { song ->
                queue.add(song)
                remaining.remove(song)
                recentArtists.add(song.artist)
            }
        }

        // Build rest of queue
        while (remaining.isNotEmpty() && queue.size < count) {
            val nextSong = selectNextSong(
                remaining = remaining,
                preferences = preferences,
                transitionScores = transitionScores,
                recentArtists = recentArtists,
                recentlyPlayedSet = recentlyPlayedSet,
                lastSongId = queue.lastOrNull()?.id
            )

            if (nextSong != null) {
                queue.add(nextSong)
                remaining.remove(nextSong)
                
                // Update recent artists (sliding window)
                recentArtists.add(nextSong.artist)
                if (recentArtists.size > MAX_RECENT_ARTISTS) {
                    recentArtists.removeAt(0)
                }
            } else {
                // Fallback: add random song
                val randomSong = remaining.randomOrNull() ?: break
                queue.add(randomSong)
                remaining.remove(randomSong)
            }
        }

        return queue
    }

    /**
     * Select next song using weighted scoring with exploration
     */
    private suspend fun selectNextSong(
        remaining: List<Song>,
        preferences: Map<String, UserPreference>,
        transitionScores: Map<String, Float>,
        recentArtists: List<String>,
        recentlyPlayedSet: Set<String>,
        lastSongId: String?
    ): Song? {
        if (remaining.isEmpty()) return null

        // Exploration: sometimes pick random
        if (Random.nextDouble() < EXPLORATION_RATE) {
            return remaining.filter { it.artist !in recentArtists }.randomOrNull()
                ?: remaining.random()
        }

        // Calculate scores for all remaining songs
        val scoredSongs = remaining.map { song ->
            val score = calculateSongScore(
                song = song,
                preference = preferences[song.id],
                transitionScore = transitionScores[song.id] ?: 0f,
                recentArtists = recentArtists,
                recentlyPlayedSet = recentlyPlayedSet,
                lastSongId = lastSongId
            )
            song to score
        }

        // Weighted random selection (roulette wheel)
        return selectByWeightedRandom(scoredSongs)
    }

    /**
     * Calculate composite score for a song
     */
    private suspend fun calculateSongScore(
        song: Song,
        preference: UserPreference?,
        transitionScore: Float,
        recentArtists: List<String>,
        recentlyPlayedSet: Set<String>,
        lastSongId: String?
    ): Double {
        var score = 0.5 // Base score

        // 1. Preference-based score (like score, completion rate)
        if (preference != null) {
            val preferenceScore = (preference.likeScore + 1) / 2 // Normalize to 0-1
            val completionScore = preference.avgCompletionRate
            
            score += (preferenceScore * 0.5 + completionScore * 0.5) * PREFERENCE_WEIGHT
            
            // Skip penalty
            val skipRate = if (preference.playCount + preference.skipCount > 0) {
                preference.skipCount.toFloat() / (preference.playCount + preference.skipCount)
            } else 0f
            score -= skipRate * SKIP_PENALTY_FACTOR
        }

        // 2. Transition score (Markov chain)
        score += transitionScore * TRANSITION_WEIGHT

        // 3. Recency penalty (avoid recently played)
        if (song.id in recentlyPlayedSet) {
            score -= RECENT_PLAY_PENALTY
        }
        
        // Time decay for recency
        if (preference != null && preference.lastPlayed > 0) {
            val daysSince = (System.currentTimeMillis() - preference.lastPlayed) / 
                (24.0 * 60 * 60 * 1000)
            val recencyDecay = 1.0 - exp(-daysSince / RECENCY_HALF_LIFE_DAYS)
            score += recencyDecay * RECENCY_WEIGHT
        }

        // 4. Diversity penalty (avoid same artist in sequence)
        if (song.artist in recentArtists) {
            score -= SAME_ARTIST_PENALTY
        }

        // 5. Similarity to last song (content-based)
        // This would use vector similarity if implemented
        
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Get transition scores from sequence agent
     */
    private suspend fun getTransitionScores(fromSongId: String): Map<String, Float> {
        val transitions = sequenceAgent.getNextSongCandidates(fromSongId, limit = 100)
        return transitions.associate { it.songId to (it.score / 100.0).toFloat() }
    }

    /**
     * Weighted random selection (roulette wheel algorithm)
     */
    private fun selectByWeightedRandom(scoredSongs: List<Pair<Song, Double>>): Song? {
        if (scoredSongs.isEmpty()) return null

        // Shift scores to positive (minimum 0.01 to give everyone a chance)
        val minScore = scoredSongs.minOf { it.second }
        val shiftedScores = scoredSongs.map { (song, score) ->
            song to (score - minScore + 0.01)
        }

        val totalWeight = shiftedScores.sumOf { it.second }
        var random = Random.nextDouble() * totalWeight

        for ((song, score) in shiftedScores) {
            random -= score
            if (random <= 0) {
                return song
            }
        }

        return shiftedScores.lastOrNull()?.first
    }

    /**
     * Reshuffle queue while maintaining context
     * Useful when user triggers shuffle mid-playback
     */
    suspend fun reshuffleFromCurrent(
        currentSong: Song,
        remainingQueue: List<Song>,
        playHistory: List<Song>
    ): List<Song> {
        val recentlyPlayed = playHistory.takeLast(20).map { it.id }
        
        return generateIntelligentQueue(
            songs = listOf(currentSong) + remainingQueue,
            currentSongId = currentSong.id,
            recentlyPlayed = recentlyPlayed,
            count = remainingQueue.size + 1
        )
    }

    /**
     * Adapt queue based on skip pattern
     * Called when SkipHandler detects frustration/searching pattern
     */
    suspend fun adaptQueueForPattern(
        pattern: SkipPattern,
        currentQueue: List<Song>,
        currentIndex: Int,
        allSongs: List<Song>
    ): List<Song> {
        val currentSong = currentQueue.getOrNull(currentIndex) ?: return currentQueue
        val recentlyPlayed = currentQueue.take(currentIndex).map { it.id }

        return when (pattern) {
            SkipPattern.FRUSTRATED -> {
                // User is frustrated - try completely different songs
                // Boost exploration, lower similarity weight
                generateDiverseQueue(
                    songs = allSongs,
                    currentSongId = currentSong.id,
                    recentlyPlayed = recentlyPlayed,
                    diversityBoost = 2.0
                )
            }
            SkipPattern.SEARCHING -> {
                // User is searching - give more variety quickly
                // Mix in more random songs
                generateDiverseQueue(
                    songs = allSongs,
                    currentSongId = currentSong.id,
                    recentlyPlayed = recentlyPlayed,
                    diversityBoost = 1.5
                )
            }
            SkipPattern.INTERRUPTED -> {
                // Late skips - not user preference issue, keep current strategy
                currentQueue
            }
            SkipPattern.NONE -> currentQueue
        }
    }

    /**
     * Generate a more diverse queue (used when user seems frustrated)
     */
    private suspend fun generateDiverseQueue(
        songs: List<Song>,
        currentSongId: String,
        recentlyPlayed: List<String>,
        diversityBoost: Double
    ): List<Song> {
        // Group songs by genre and artist
        val byGenre = songs.groupBy { it.genre ?: "Unknown" }
        val byArtist = songs.groupBy { it.artist }

        // Take songs from different groups
        val diverseSongs = mutableListOf<Song>()
        val usedArtists = mutableSetOf<String>()
        val usedGenres = mutableSetOf<String?>()

        // Round-robin through genres
        val genreIterators = byGenre.mapValues { it.value.shuffled().iterator() }
        
        while (diverseSongs.size < songs.size) {
            var added = false
            for ((genre, iterator) in genreIterators) {
                if (iterator.hasNext()) {
                    val song = iterator.next()
                    if (song.artist !in usedArtists || diversityBoost > 1.5) {
                        diverseSongs.add(song)
                        usedArtists.add(song.artist)
                        usedGenres.add(genre)
                        added = true
                    }
                }
            }
            if (!added) break
        }

        // Place current song first
        val currentSong = diverseSongs.find { it.id == currentSongId }
        if (currentSong != null) {
            diverseSongs.remove(currentSong)
            diverseSongs.add(0, currentSong)
        }

        return diverseSongs
    }

    /**
     * Create a "bag shuffle" - ensures all songs play before repeating
     * but with intelligent ordering within the bag
     */
    suspend fun createBagShuffle(
        songs: List<Song>,
        recentlyPlayed: List<String>
    ): List<Song> {
        // All songs must play before any repeats
        // But order is intelligent, not random
        return generateIntelligentQueue(
            songs = songs,
            recentlyPlayed = recentlyPlayed,
            count = songs.size
        )
    }
}
