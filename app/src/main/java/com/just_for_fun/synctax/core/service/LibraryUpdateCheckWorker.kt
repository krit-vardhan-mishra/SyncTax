package com.just_for_fun.synctax.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.utils.LibraryUpdateChecker
import com.just_for_fun.synctax.core.utils.LibraryUpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker to periodically check for library updates and notify users
 */
class LibraryUpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "LibraryUpdateCheckWorker"
        private const val CHANNEL_ID = "library_updates"
        private const val NOTIFICATION_ID = 1001

        fun schedulePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<LibraryUpdateCheckWorker>(
                7, TimeUnit.DAYS, // Check weekly
                1, TimeUnit.DAYS  // Flex interval
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "library_update_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking for library updates...")

                val updateResult = LibraryUpdateChecker.checkForNewPipeUpdate()

                if (updateResult.needsUpdate) {
                    Log.i(TAG, "NewPipeExtractor update available: ${updateResult.latestVersion}")
                    showUpdateNotification(updateResult)
                } else if (updateResult.error != null) {
                    Log.w(TAG, "Failed to check for updates: ${updateResult.error}")
                } else {
                    Log.d(TAG, "NewPipeExtractor is up to date")
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error in library update check", e)
                Result.failure()
            }
        }
    }

    private fun showUpdateNotification(updateResult: LibraryUpdateResult) {
        createNotificationChannel()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(updateResult.releaseUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle("Library Update Available")
            .setContentText("NewPipeExtractor ${updateResult.latestVersion} is available")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("NewPipeExtractor ${updateResult.latestVersion} is available. " +
                        "This update fixes streaming issues. Please update the app when possible."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Library Updates"
            val descriptionText = "Notifications about library updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}