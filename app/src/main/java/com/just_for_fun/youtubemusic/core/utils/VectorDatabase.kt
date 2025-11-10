package com.just_for_fun.youtubemusic.core.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple in-memory vector database for song embeddings
 */
class VectorDatabase {
    private val vectors = mutableMapOf<String, DoubleArray>()
    private val mutex = Mutex()

    suspend fun storeVector(id: String, vector: DoubleArray) {
        mutex.withLock {
            vectors[id] = vector.clone()
        }
    }

    suspend fun getVector(id: String): DoubleArray? {
        mutex.withLock {
            return vectors[id]?.clone()
        }
    }

    suspend fun findSimilar(queryVector: DoubleArray, topK: Int = 10): List<Pair<String, Double>> {
        mutex.withLock {
            return vectors.mapNotNull { (id, vector) ->
                val similarity = MathUtils.cosineSimilarity(queryVector, vector)
                id to similarity
            }
                .sortedByDescending { it.second }
                .take(topK)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            vectors.clear()
        }
    }

    suspend fun size(): Int {
        mutex.withLock {
            return vectors.size
        }
    }
}