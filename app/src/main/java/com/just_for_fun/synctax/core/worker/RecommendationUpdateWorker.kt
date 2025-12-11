package com.just_for_fun.synctax.core.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.core.network.YouTubeInnerTubeClient
import com.just_for_fun.synctax.core.service.ListeningAnalyticsService
import com.just_for_fun.synctax.core.service.RecommendationService
import com.just_for_fun.synctax.data.local.MusicDatabase
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically updates recommendation cache in the background.
 * Runs every 12 hours when network is available and battery is not low.
 */
class RecommendationUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RecommendationWorker"
        private const val WORK_NAME = "recommendation_update_work"
        
        /**
         * Schedule periodic recommendation updates.
         * Will update every 12 hours when connected to network.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<RecommendationUpdateWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 2,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Scheduled periodic recommendation updates every 12 hours")
        }
        
        /**
         * Cancel scheduled recommendation updates.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic recommendation updates")
        }
    }

    override suspend fun doWork(): Result = withContext(AppDispatchers.Network) {
        try {
            Log.d(TAG, "Starting recommendation update...")
            
            val database = MusicDatabase.getDatabase(applicationContext)
            val historyDao = database.onlineListeningHistoryDao()
            val cacheDao = database.recommendationCacheDao()
            
            val analyticsService = ListeningAnalyticsService(historyDao)
            val ytClient = YouTubeInnerTubeClient()
            val recommendationService = RecommendationService(
                analyticsService, ytClient, historyDao, cacheDao
            )
            
            // Check if user has enough listening history
            val hasHistory = analyticsService.hasEnoughHistory(3)
            if (!hasHistory) {
                Log.d(TAG, "Not enough listening history for recommendations")
                return@withContext Result.success()
            }
            
            // Clean up expired cache entries
            withContext(AppDispatchers.Database) {
                cacheDao.deleteExpired()
            }
            
            // Generate fresh recommendations (force refresh)
            recommendationService.generateRecommendations(forceRefresh = true)
            
            Log.d(TAG, "Recommendation update completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Recommendation update failed", e)
            
            // Retry on failure (up to 3 times by default)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
