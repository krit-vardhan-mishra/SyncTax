package com.just_for_fun.synctax

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Python runtime
        initializePython()

        Log.d(TAG, "Music Application initialized")
    }

    private fun initializePython() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
            Log.d(TAG, "Python runtime initialized")
        }
    }

    companion object {
        private const val TAG = "MusicApplication"
    }
}