package com.just_for_fun.synctax.core.ml.models

import com.just_for_fun.synctax.data.local.entities.Song

data class SongFeatures(
    val songId: String,
    val playFrequency: Double,        // How often played
    val avgCompletionRate: Double,    // Average % of song completed
    val skipRate: Double,             // % of times skipped
    val recencyScore: Double,         // How recently played (0-1)
    val timeOfDayMatch: Double,       // Match with current time preferences
    val genreAffinity: Double,        // User's affinity to this genre
    val artistAffinity: Double,       // User's affinity to this artist
    val consecutivePlays: Double,     // How many times played in a row
    val sessionContext: Double,       // Context similarity score
    val durationScore: Double,        // Song duration preference match (0-1)
    val albumAffinity: Double,        // User's affinity to this album
    val releaseYearScore: Double,     // Preference for song release year
    val songPopularity: Double,       // How popular this song is for the user
    val tempoEnergy: Double,          // Estimated tempo/energy level (0-1)
    val embeddingVector: FloatArray? = null  // Rich feature embedding (16-32D)
) {
    fun toVector(): DoubleArray = doubleArrayOf(
        playFrequency,
        avgCompletionRate,
        skipRate,
        recencyScore,
        timeOfDayMatch,
        genreAffinity,
        artistAffinity,
        consecutivePlays,
        sessionContext,
        durationScore,
        albumAffinity,
        releaseYearScore,
        songPopularity,
        tempoEnergy
    )
    
    companion object {
        /**
         * Generate a basic embedding from song metadata.
         * This creates a 16-dimensional vector from available metadata.
         * 
         * From RECOMMENDATION_LOGIC_NEW.md:
         * - Metadata embedding: artist, album, genre encoded as numeric values
         * - Duration normalized to [0,1]
         * - This enables similarity calculations beyond exact metadata matches
         */
        fun generateEmbedding(song: Song): FloatArray {
            val embedding = FloatArray(16)
            
            // Artist hash (positions 0-3): Encode artist name into 4 floats
            val artistHash = song.artist.lowercase().hashCode()
            embedding[0] = ((artistHash and 0xFF) / 255f)
            embedding[1] = (((artistHash shr 8) and 0xFF) / 255f)
            embedding[2] = (((artistHash shr 16) and 0xFF) / 255f)
            embedding[3] = (((artistHash shr 24) and 0xFF) / 255f)
            
            // Album hash (positions 4-7): Encode album name into 4 floats
            val albumHash = (song.album ?: "unknown").lowercase().hashCode()
            embedding[4] = ((albumHash and 0xFF) / 255f)
            embedding[5] = (((albumHash shr 8) and 0xFF) / 255f)
            embedding[6] = (((albumHash shr 16) and 0xFF) / 255f)
            embedding[7] = (((albumHash shr 24) and 0xFF) / 255f)
            
            // Genre encoding (positions 8-11): Map genre to category
            val genreVector = encodeGenre(song.genre)
            embedding[8] = genreVector[0]
            embedding[9] = genreVector[1]
            embedding[10] = genreVector[2]
            embedding[11] = genreVector[3]
            
            // Duration features (positions 12-15)
            val durationMin = song.duration / 60000f // Convert ms to minutes
            embedding[12] = (durationMin / 10f).coerceIn(0f, 1f) // Normalized 0-10 min
            embedding[13] = if (durationMin < 3f) 1f else 0f // Short song indicator
            embedding[14] = if (durationMin in 3f..5f) 1f else 0f // Medium song
            embedding[15] = if (durationMin > 5f) 1f else 0f // Long song indicator
            
            return embedding
        }
        
        private fun encodeGenre(genre: String?): FloatArray {
            if (genre == null) return floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
            
            val normalized = genre.lowercase()
            
            // Energy axis: calm (0) to energetic (1)
            val energy = when {
                normalized.contains("metal") || normalized.contains("punk") || 
                    normalized.contains("rock") -> 0.9f
                normalized.contains("electronic") || normalized.contains("dance") -> 0.8f
                normalized.contains("pop") || normalized.contains("hip hop") -> 0.6f
                normalized.contains("jazz") || normalized.contains("blues") -> 0.4f
                normalized.contains("classical") || normalized.contains("ambient") -> 0.2f
                else -> 0.5f
            }
            
            // Acoustic axis: electronic (0) to acoustic (1)
            val acoustic = when {
                normalized.contains("electronic") || normalized.contains("techno") -> 0.1f
                normalized.contains("pop") -> 0.4f
                normalized.contains("rock") -> 0.5f
                normalized.contains("folk") || normalized.contains("country") -> 0.8f
                normalized.contains("classical") || normalized.contains("acoustic") -> 0.9f
                else -> 0.5f
            }
            
            // Vocals axis: instrumental (0) to vocal-heavy (1)
            val vocals = when {
                normalized.contains("instrumental") -> 0.1f
                normalized.contains("classical") -> 0.3f
                normalized.contains("jazz") -> 0.5f
                normalized.contains("pop") || normalized.contains("hip hop") -> 0.8f
                normalized.contains("vocal") -> 0.9f
                else -> 0.5f
            }
            
            // Genre hash for uniqueness
            val hash = (genre.hashCode() and 0xFF) / 255f
            
            return floatArrayOf(energy, acoustic, vocals, hash)
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SongFeatures
        if (songId != other.songId) return false
        return embeddingVector?.contentEquals(other.embeddingVector) ?: (other.embeddingVector == null)
    }
    
    override fun hashCode(): Int {
        var result = songId.hashCode()
        result = 31 * result + (embeddingVector?.contentHashCode() ?: 0)
        return result
    }
}
