package com.just_for_fun.synctax

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.service.MusicService
import com.just_for_fun.synctax.ui.components.app.AppNavigationBar
import com.just_for_fun.synctax.ui.components.player.PlayerBottomSheet
import com.just_for_fun.synctax.ui.screens.*
import com.just_for_fun.synctax.ui.theme.synctaxTheme
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel

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

    // Registering the permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, music scanning can proceed in the ViewModel
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission result
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

            synctaxTheme(darkTheme = darkTheme) {
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
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO for audio files
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }

            else -> {
                // Request the permission
                requestPermissionLauncher.launch(permission)
            }
        }

        // Request READ_MEDIA_IMAGES permission for Android 13+ (for album art)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request audio recording permission (optional - for audio visualization)
        // Note: This is optional and the app will work without it using fallback visualization
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // We don't force request this permission as it's not critical
            // The AudioAnalyzer will gracefully handle missing permission
        }
    }
}


