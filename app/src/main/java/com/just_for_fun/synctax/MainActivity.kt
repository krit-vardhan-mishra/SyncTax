package com.just_for_fun.synctax

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.just_for_fun.synctax.core.service.MusicService
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.ui.theme.SynctaxTheme

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    // Registering the permission request launcher for multiple permissions
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Store that we've asked for permissions
        getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("permissions_requested", true)
            .apply()
        
        // Log results (optional)
        permissions.entries.forEach { entry ->
            android.util.Log.d("MainActivity", "Permission ${entry.key} = ${entry.value}")
        }
    }
    
    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // MANAGE_EXTERNAL_STORAGE permission result handled
        getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("permissions_requested", true)
            .apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permission on app launch
        checkAndRequestPermissions()

        // Start and bind to music service
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            val userPreferences = remember { UserPreferences(this) }
            val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
            val darkTheme = when (themeMode) {
                UserPreferences.KEY_THEME_MODE_DARK -> true
                UserPreferences.KEY_THEME_MODE_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            SynctaxTheme(darkTheme = darkTheme) {
                MusicApp(userPreferences = userPreferences)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh library when app comes back to foreground
        // This ensures the song list is always up to date
        // Note: We access the ViewModel through the composition, but for onResume
        // we need a different approach. The ViewModel init already handles initial scanning.
    }

    private fun checkAndRequestPermissions() {
        // Check if we've already requested permissions this session
        val prefs = getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
        val permissionsRequested = prefs.getBoolean("permissions_requested", false)
        
        if (permissionsRequested) {
            return // Don't ask again
        }
        
        // Build list of permissions to request based on Android version
        val permissionsToRequest = mutableListOf<String>()
        
        // Audio permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO for audio files
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            // Note: READ_MEDIA_IMAGES is requested only in SettingsScreen when user enables album art scanning
            // Notification permission for Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // Request WRITE_EXTERNAL_STORAGE for Android 10 and below
            @Suppress("DEPRECATION")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        // Handle MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStoragePermissionLauncher.launch(intent)
                    return // Handle other permissions after this returns
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting MANAGE_EXTERNAL_STORAGE", e)
                }
            }
        }
        
        // Request all regular permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted, mark as requested
            prefs.edit().putBoolean("permissions_requested", true).apply()
        }
    }
}


