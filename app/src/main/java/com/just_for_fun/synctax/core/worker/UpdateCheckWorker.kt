package com.just_for_fun.synctax.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.utils.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically checks for app updates in the background
 * Similar to ytdlnis UpdateYTDLWorker functionality
 */
class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "update_check_worker"
        private const val CHANNEL_ID = "update_notifications"
        private const val NOTIFICATION_ID = 1001

        // Preferences
        private const val PREF_NAME = "update_worker_prefs"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val KEY_SKIPPED_VERSION = "skipped_version"

        /**
         * Schedule periodic update checks
         * Default: every 12 hours when connected to network
         */
        fun schedule(context: Context, repeatInterval: Long = 12, timeUnit: TimeUnit = TimeUnit.HOURS) {
            val prefs = context.getSharedPreferences(
                "${context.packageName}_preferences",
                Context.MODE_PRIVATE
            )
            
            // Don't schedule if user disabled auto-check
            if (!prefs.getBoolean("check_update_on_start", true)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(repeatInterval, timeUnit)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS) // Delay first check by 1 hour
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    30,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateCheckRequest
            )
        }

        /**
         * Cancel scheduled update checks
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Run an immediate one-time update check
         */
        fun runOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateCheckRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(updateCheckRequest)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(
                "${context.packageName}_preferences",
                Context.MODE_PRIVATE
            )
            
            val includeBeta = prefs.getBoolean("include_beta_updates", false)
            val skippedVersion = prefs.getString(KEY_SKIPPED_VERSION, null)

            // Check for app update
            val updateUtil = UpdateUtil(context)
            val result = updateUtil.checkForAppUpdate()
            
            result.onSuccess { release ->
                // Skip if this version was skipped by user
                if (release.tagName == skippedVersion) {
                    return@withContext Result.success()
                }

                // Check if we already notified about this version
                val workerPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val lastNotified = workerPrefs.getString(KEY_LAST_NOTIFIED_VERSION, null)
                
                if (release.tagName != lastNotified) {
                    // New update available - show notification
                    showUpdateNotification(release.tagName, release.body)
                    
                    // Save this version as notified
                    workerPrefs.edit()
                        .putString(KEY_LAST_NOTIFIED_VERSION, release.tagName)
                        .apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun showUpdateNotification(version: String, releaseNotes: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about app updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open settings screen (update section)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_update_settings", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_update)
            .setContentTitle("Update Available")
            .setContentText("SyncTax $version is available")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "SyncTax $version is available.\n\n" +
                        releaseNotes.take(200) + if (releaseNotes.length > 200) "..." else ""
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_update,
                "Update",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * Extension function to check and schedule update worker at app start
 */
fun Context.scheduleUpdateCheckIfEnabled() {
    val prefs = getSharedPreferences(
        "${packageName}_preferences",
        Context.MODE_PRIVATE
    )
    
    if (prefs.getBoolean("check_update_on_start", true)) {
        UpdateCheckWorker.schedule(this)
    }
}
