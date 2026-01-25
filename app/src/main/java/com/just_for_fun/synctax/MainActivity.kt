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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.just_for_fun.synctax.presentation.components.onboarding.StoragePermissionOnboardingScreen
import com.just_for_fun.synctax.presentation.components.onboarding.NotificationPermissionOnboardingScreen
import com.just_for_fun.synctax.presentation.screens.SplashScreen
import com.just_for_fun.synctax.core.service.MusicService
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.ui.theme.SynctaxTheme

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var serviceBound = false

    // Onboarding state - what to show (using Compose State for reactivity)
    private var currentOnboarding by mutableStateOf<OnboardingType?>(OnboardingType.SPLASH)

    enum class OnboardingType {
        SPLASH, STORAGE, NOTIFICATION
    }

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
        // Log results (optional)
        permissions.entries.forEach { entry ->
            android.util.Log.d("MainActivity", "Permission ${entry.key} = ${entry.value}")
        }

        // Store that we've asked for permissions and continue checking
        getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("permissions_requested", true)
            .apply()

        // Continue checking for remaining permissions
        currentOnboarding = checkAndRequestPermissions()
    }

    // Launcher for MANAGE_EXTERNAL_STORAGE permission (Android 11+)
    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // MANAGE_EXTERNAL_STORAGE permission result handled
        getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("permissions_requested", true)
            .apply()

        // Continue to next permission check
        currentOnboarding = checkAndRequestPermissions()
    }

    // State for external media intent
    private var intentMediaUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start and bind to music service
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Handle initial intent
        handleIntent(intent)

        setContent {
            MusicAppContent(
                onboardingType = currentOnboarding,
                initialMediaUri = intentMediaUri,
                onSplashFinished = { currentOnboarding = checkAndRequestPermissions() },
                onRequestStoragePermissions = { this@MainActivity.requestStoragePermissions() },
                onSkipStorageOnboarding = { this@MainActivity.skipStorageOnboarding() },
                onRequestNotificationPermissions = { this@MainActivity.requestNotificationPermissions() },
                onSkipNotificationOnboarding = { this@MainActivity.skipNotificationOnboarding() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val type = intent.type
            if (type != null && type.startsWith("audio/")) {
                intentMediaUri = intent.data
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

    private fun checkAndRequestPermissions(): OnboardingType? {
        // Check if we've already requested permissions this session
        val prefs = getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
        val permissionsRequested = prefs.getBoolean("permissions_requested", false)

        if (permissionsRequested) {
            return null // Don't ask again
        }

        // Check what permissions we need and show onboarding accordingly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO for audio files
            val needsStoragePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED

            val needsNotificationPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

            if (needsStoragePermission) {
                return OnboardingType.STORAGE
            }

            if (needsNotificationPermission) {
                return OnboardingType.NOTIFICATION
            }
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            val needsStoragePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

            if (needsStoragePermission) {
                return OnboardingType.STORAGE
            }

            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val needsNotificationPermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

                if (needsNotificationPermission) {
                    return OnboardingType.NOTIFICATION
                }
            }
        }

        // Handle MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return OnboardingType.STORAGE
            }
        }

        // All permissions granted, mark as requested
        prefs.edit().putBoolean("permissions_requested", true).apply()
        return null
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO for audio files
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // Request WRITE_EXTERNAL_STORAGE for Android 10 and below
            @Suppress("DEPRECATION")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                    return
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting MANAGE_EXTERNAL_STORAGE", e)
                }
            }
        }

        // Request regular permissions
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Permissions already granted, continue to next onboarding or finish
            currentOnboarding = checkAndRequestPermissions() // Check if we need notification permission next
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestMultiplePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            } else {
                // Permission already granted
                currentOnboarding = checkAndRequestPermissions() // Mark as completed
            }
        } else {
            // No notification permission needed for older Android
            currentOnboarding = checkAndRequestPermissions()
        }
    }

    private fun skipStorageOnboarding() {
        currentOnboarding = checkAndRequestPermissions() // Continue to check other permissions
    }

    private fun skipNotificationOnboarding() {
        currentOnboarding = checkAndRequestPermissions() // Mark as completed
    }
}

@Composable
fun MusicAppContent(
    onboardingType: MainActivity.OnboardingType?,
    initialMediaUri: Uri? = null,
    onSplashFinished: () -> Unit,
    onRequestStoragePermissions: () -> Unit,
    onSkipStorageOnboarding: () -> Unit,
    onRequestNotificationPermissions: () -> Unit,
    onSkipNotificationOnboarding: () -> Unit
) {
    val context = LocalContext.current
    
    val userPreferences = remember { UserPreferences(context) }
    val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
    val darkTheme = when (themeMode) {
        UserPreferences.KEY_THEME_MODE_DARK -> true
        UserPreferences.KEY_THEME_MODE_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    SynctaxTheme(darkTheme = darkTheme) {
        when (onboardingType) {
            MainActivity.OnboardingType.SPLASH -> {
                SplashScreen(onSplashFinished = onSplashFinished)
            }
            MainActivity.OnboardingType.STORAGE -> {
                StoragePermissionOnboardingScreen(
                    onGrantPermission = onRequestStoragePermissions,
                    onSkip = onSkipStorageOnboarding
                )
            }
            MainActivity.OnboardingType.NOTIFICATION -> {
                NotificationPermissionOnboardingScreen(
                    onGrantPermission = onRequestNotificationPermissions,
                    onSkip = onSkipNotificationOnboarding
                )
            }
            null -> {
                MusicApp(
                    userPreferences = userPreferences,
                    initialMediaUri = initialMediaUri
                )
            }
        }
    }
}


