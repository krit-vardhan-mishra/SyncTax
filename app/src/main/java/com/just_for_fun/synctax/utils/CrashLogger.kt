package com.just_for_fun.synctax.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.just_for_fun.synctax.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler that saves crash logs to the app's external data directory.
 * Logs are stored at: Android/data/com.just_for_fun.synctax/files/crash_logs/
 *
 * Each crash creates a timestamped .txt file with:
 * - Device info, OS version, app version
 * - Full stack trace
 * - Thread info
 *
 * Old logs beyond [MAX_LOG_FILES] are automatically cleaned up.
 */
class CrashLogger(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashLogger"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_LOG_FILES = 20

        /**
         * Install the crash logger as the global uncaught exception handler.
         * Call this in Application.onCreate().
         */
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashLogger(context))
            Log.d(TAG, "✅ CrashLogger installed")
        }

        /**
         * Get the crash log directory.
         */
        fun getCrashLogDir(context: Context): File? {
            val externalDir = context.getExternalFilesDir(null) ?: return null
            return File(externalDir, CRASH_LOG_DIR).apply { mkdirs() }
        }

        /**
         * Get all crash log files sorted by most recent first.
         */
        fun getCrashLogs(context: Context): List<File> {
            val dir = getCrashLogDir(context) ?: return emptyList()
            return dir.listFiles()
                ?.filter { it.extension == "txt" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        /**
         * Delete all crash logs.
         */
        fun clearCrashLogs(context: Context) {
            getCrashLogs(context).forEach { it.delete() }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }

        // Forward to the default handler (shows system crash dialog / kills process)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val crashDir = getCrashLogDir(context) ?: return

        // Generate timestamped filename
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val fileName = "crash_$timestamp.txt"
        val file = File(crashDir, fileName)

        // Build the crash report
        val report = buildString {
            appendLine("═══════════════════════════════════════════")
            appendLine("       SYNCTAX CRASH REPORT")
            appendLine("═══════════════════════════════════════════")
            appendLine()
            appendLine("Timestamp    : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())}")
            appendLine("App Version  : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine()
            appendLine("───────────── DEVICE INFO ─────────────")
            appendLine("Device       : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android      : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Brand        : ${Build.BRAND}")
            appendLine("Product      : ${Build.PRODUCT}")
            appendLine("Hardware     : ${Build.HARDWARE}")
            appendLine("ABI          : ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("───────────── THREAD INFO ─────────────")
            appendLine("Thread       : ${thread.name} (id=${thread.id})")
            appendLine("Thread Group : ${thread.threadGroup?.name ?: "none"}")
            appendLine("Priority     : ${thread.priority}")
            appendLine()
            appendLine("──────────── STACK TRACE ──────────────")
            appendLine()

            // Full stack trace
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            appendLine(sw.toString())

            // If there's a cause chain, print all causes
            var cause = throwable.cause
            var depth = 1
            while (cause != null && depth <= 10) {
                appendLine()
                appendLine("──────── CAUSED BY (depth=$depth) ────────")
                val csw = StringWriter()
                cause.printStackTrace(PrintWriter(csw))
                appendLine(csw.toString())
                cause = cause.cause
                depth++
            }

            appendLine()
            appendLine("──────────── MEMORY INFO ──────────────")
            val runtime = Runtime.getRuntime()
            val maxMB = runtime.maxMemory() / (1024 * 1024)
            val totalMB = runtime.totalMemory() / (1024 * 1024)
            val freeMB = runtime.freeMemory() / (1024 * 1024)
            val usedMB = totalMB - freeMB
            appendLine("Max Memory   : ${maxMB}MB")
            appendLine("Total Memory : ${totalMB}MB")
            appendLine("Used Memory  : ${usedMB}MB")
            appendLine("Free Memory  : ${freeMB}MB")
            appendLine()
            appendLine("═══════════════════════════════════════════")
            appendLine("  Share this file with the app developer")
            appendLine("  to help fix this issue. Thank you!")
            appendLine("═══════════════════════════════════════════")
        }

        file.writeText(report)
        Log.e(TAG, "💥 Crash log saved to: ${file.absolutePath}")

        // Clean up old logs
        cleanupOldLogs(crashDir)
    }

    private fun cleanupOldLogs(crashDir: File) {
        val logs = crashDir.listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (logs.size > MAX_LOG_FILES) {
            logs.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }
}
