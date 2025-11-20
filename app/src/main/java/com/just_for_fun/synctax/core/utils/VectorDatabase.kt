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
 */
class VectorDatabase(private val context: Context) {
    private val vectors = mutableMapOf<String, DoubleArray>()
    private val mutex = Mutex()
    private val vectorsFile = File(context.filesDir, "vectors.dat")

    init {
        loadVectors()
    }

    suspend fun storeVector(id: String, vector: DoubleArray) {
        mutex.withLock {
            vectors[id] = vector.clone()
            saveVectors()
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
            vectorsFile.delete()
        }
    }

    suspend fun size(): Int {
        mutex.withLock {
            return vectors.size
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
                    serializableMap.forEach { (id, list) ->
                        vectors[id] = list.toDoubleArray()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}