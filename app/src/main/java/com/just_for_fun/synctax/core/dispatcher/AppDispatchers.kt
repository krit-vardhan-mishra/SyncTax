package com.just_for_fun.synctax.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object AppDispatchers {
    
    /**
     * For database operations (queries, inserts, updates)
     * Limited parallelism to avoid database lock contention
     */
    val Database: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)
    
    /**
     * For image loading and decoding operations
     * Higher parallelism since images are independent
     */
    val ImageLoading: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(8)
    
    /**
     * For music scanning and file I/O operations
     * Moderate parallelism to balance speed and resource usage
     */
    val MusicScanning: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(6)
    
    /**
     * For network operations (YouTube downloads, API calls)
     * Limited to avoid overwhelming network stack
     */
    val Network: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(5)
    
    /**
     * For ML model operations (recommendations, training)
     * Single thread to avoid CPU contention
     */
    val MachineLearning: CoroutineDispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()
    
    /**
     * For audio playback and processing
     * Dedicated thread pool for real-time operations
     */
    val AudioProcessing: CoroutineDispatcher = Executors.newFixedThreadPool(2)
        .asCoroutineDispatcher()
}