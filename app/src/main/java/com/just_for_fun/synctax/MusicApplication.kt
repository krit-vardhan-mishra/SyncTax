package com.just_for_fun.synctax

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Python runtime on background thread to avoid blocking main thread
        applicationScope.launch {
            initializePython()
        }

        Log.d(TAG, "Music Application initialized")
    }

    private fun initializePython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
                Log.d(TAG, "Python runtime initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python runtime", e)
        }
    }

    companion object {
        private const val TAG = "MusicApplication"
    }
}