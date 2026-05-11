package com.just_for_fun.synctax.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.just_for_fun.synctax.MainActivity
import com.just_for_fun.synctax.R

/**
 * Foreground service to keep Party Mode alive during Doze Mode and App Standby.
 * Holds a partial wake lock and shows a persistent notification so Android
 * does not kill the Nearby Connections sync process in the background.
 */
class PartyService : Service() {

    private val binder = PartyBinder()
    private var wakeLock: PowerManager.WakeLock? = null

    inner class PartyBinder : Binder() {
        fun getService(): PartyService = this@PartyService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎉 PartyService created")
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val partyName = intent?.getStringExtra(EXTRA_PARTY_NAME) ?: "Party Mode"
        val isHost = intent?.getBooleanExtra(EXTRA_IS_HOST, false) ?: false

        val notification = createNotification(partyName, isHost)
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "🎉 PartyService started in foreground — isHost=$isHost, name=$partyName")

        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        Log.d(TAG, "🎉 PartyService destroyed")
        super.onDestroy()
    }

    // ── Notification ────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Party Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Party Mode connections alive"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(partyName: String, isHost: Boolean): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val role = if (isHost) "Hosting" else "Connected to"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Party Mode Active")
            .setContentText("$role: $partyName")
            .setSmallIcon(R.drawable.ic_notification) // reuse existing icon
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    // ── Wake Lock ───────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "synctax:party_mode"
        ).apply {
            acquire(4 * 60 * 60 * 1000L) // 4 hours max
        }
        Log.d(TAG, "🔒 Wake lock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "🔓 Wake lock released")
            }
        }
        wakeLock = null
    }

    companion object {
        private const val TAG = "PartyService"
        const val CHANNEL_ID = "party_mode_channel"
        const val NOTIFICATION_ID = 42
        const val EXTRA_PARTY_NAME = "extra_party_name"
        const val EXTRA_IS_HOST = "extra_is_host"
    }
}
