package com.just_for_fun.synctax.core.utils

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Simple in-memory vector database for song embeddings with persistence
 * Optimized with memory limits and LRU cache to prevent excessive memory usage
 */
class VectorDatabase(private val context: Context) {
    private val vectors = object : LinkedHashMap<String, DoubleArray>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DoubleArray>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val mutex = Mutex()
    private val vectorsFile = File(context.filesDir, "vectors.dat")

    companion object {
        private const val MAX_CACHE_SIZE = 5000 // Limit in-memory vectors to 5000 songs
    }

    init {
        loadVectors()
    }

    suspend fun storeVector(id: String, vector: DoubleArray) {
        mutex.withLock {
            vectors[id] = vector.clone()
            // Save periodically or on significant changes
            if (vectors.size % 100 == 0) {
                saveVectors()
            }
        }
    }

    suspend fun getVector(id: String): DoubleArray? {
        mutex.withLock {
            return vectors[id]?.clone()
        }
    }

    suspend fun findSimilar(queryVector: DoubleArray, topK: Int = 10): List<Pair<String, Double>> {
        mutex.withLock {
            // Use a min-heap to efficiently find top K without sorting all
            val candidates = mutableListOf<Pair<String, Double>>()
            
            for ((id, vector) in vectors) {
                val similarity = MathUtils.cosineSimilarity(queryVector, vector)
                candidates.add(id to similarity)
                
                // Early optimization: only keep top candidates
                if (candidates.size > topK * 2) {
                    candidates.sortByDescending { it.second }
                    candidates.subList(topK * 2, candidates.size).clear()
                }
            }
            
            return candidates.sortedByDescending { it.second }.take(topK)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            vectors.clear()
            vectorsFile.delete()
        }
    }

    suspend fun size(): Int {
        mutex.withLock {
            return vectors.size
        }
    }
    
    suspend fun flush() {
        mutex.withLock {
            saveVectors()
        }
    }

    private fun saveVectors() {
        try {
            FileOutputStream(vectorsFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    val serializableMap = vectors.mapValues { it.value.toList() }
                    oos.writeObject(serializableMap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadVectors() {
        if (!vectorsFile.exists()) return
        try {
            FileInputStream(vectorsFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val serializableMap = ois.readObject() as? Map<String, List<Double>> ?: return
                    // Only load up to MAX_CACHE_SIZE vectors
                    serializableMap.entries.take(MAX_CACHE_SIZE).forEach { (id, list) ->
                        vectors[id] = list.toDoubleArray()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
