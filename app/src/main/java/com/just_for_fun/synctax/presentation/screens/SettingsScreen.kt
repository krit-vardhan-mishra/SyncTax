package com.just_for_fun.synctax.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.material3.SliderDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.os.Process
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.app.TooltipIconButton
import com.just_for_fun.synctax.core.utils.AppConfig
import com.just_for_fun.synctax.core.utils.AppIconManager
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri

/**
 * Data class representing an app icon option
 */
data class AppIconOption(
    val id: String,
    val name: String,
    val assetPath: String,
    val isPremium: Boolean = false
)

/**
 * Load bitmap from assets folder
 */
@Composable
private fun rememberAssetBitmap(context: Context, assetPath: String): ImageBitmap? {
    return remember(assetPath) {
        try {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Helper component for Asset-based Icon Preview Cards (Telegram style)
@Composable
private fun AssetIconPreviewCard(
    context: Context,
    iconOption: AppIconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconBitmap = rememberAssetBitmap(context, iconOption.assetPath)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            iconBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = iconOption.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Icon name with premium indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconOption.isPremium) {
                Text(
                    text = "★ ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD700)
                )
            }
            Text(
                text = iconOption.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Helper component for Theme Radio Button Rows
@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Updated with Custom "Thick Track" Design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericalSetting(
    label: String,
    description: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..100f
) {
    // Custom Colors extracted from the reference image
    val activeColor = Color(0xFFFF0033)
    val inactiveColor = Color(0xFFE8DEF8) // Light Lavender

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp)) // Increased spacing slightly for the taller slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Slider(
                value = currentValue.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange,
                steps = (valueRange.endInclusive - valueRange.start).toInt() - 1,
                modifier = Modifier.weight(1f),

                // 1. Custom Thumb: A vertical bar extending beyond the track
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 32.dp) // Tall, thin vertical bar
                            .clip(RoundedCornerShape(50)) // Fully rounded ends
                            .background(activeColor)
                    )
                },

                // 2. Custom Track: Thick and rounded
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier
                            .height(20.dp) // Much thicker than default
                            .clip(RoundedCornerShape(50)), // Rounded track edges
                        colors = SliderDefaults.colors(
                            activeTrackColor = activeColor,
                            inactiveTrackColor = inactiveColor,
                            thumbColor = activeColor // Matches thumb
                        ),
                        thumbTrackGapSize = 0.dp, // Removes the gap between track and thumb
                        drawStopIndicator = null // Hides the little dots for steps to keep it clean
                    )
                }
            )

            OutlinedTextField(
                value = currentValue.toString(),
                onValueChange = { input ->
                    val value = input.toIntOrNull()?.coerceIn(
                        valueRange.start.toInt(),
                        valueRange.endInclusive.toInt()
                    ) ?: currentValue
                    onValueChange(value)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onScanTrigger: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {}
) {
    val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
    val scanPaths by userPreferences.scanPaths.collectAsState(initial = emptyList())
    val scanLocalAlbumArt by userPreferences.scanLocalAlbumArt.collectAsState(initial = false)
    val onlineHistoryCount by userPreferences.onlineHistoryCount.collectAsState(initial = 10)
    val recommendationsCount by userPreferences.recommendationsCount.collectAsState(initial = 20)
    val userName by userPreferences.userName.collectAsState(initial = "")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if READ_MEDIA_IMAGES permission is granted
    var hasImagePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Not needed on older versions
            }
        )
    }

    // Permission launcher for READ_MEDIA_IMAGES - only requested when user enables this feature
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasImagePermission = granted
        if (granted) {
            userPreferences.setScanLocalAlbumArt(true)
            onScanTrigger()
        }
    }

    // SAF folder picker logic remains the same
    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            scope.launch {
                userPreferences.addScanPath(uri.toString())
                onScanTrigger()
            }
        } catch (_: Exception) {
            // Error handling omitted for brevity, but a Snackbar should be shown
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = onBackClick,
                        tooltipText = "Go back"
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
        OptimizedLazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Thank You Contributor Card ---
            if (AppConfig.isCreator(context, userName)) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFCC02)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💛 Thank you for building SyncTax!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Your contributions make this app amazing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            }

            // --- 1. Theme Settings ---
            item {
                Text("Theme", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeOptionRow(
                        label = "System default",
                        selected = themeMode == UserPreferences.KEY_THEME_MODE_SYSTEM,
                        onSelect = { userPreferences.setThemeMode(UserPreferences.KEY_THEME_MODE_SYSTEM) }
                    )
                    ThemeOptionRow(
                        label = "Light",
                        selected = themeMode == UserPreferences.KEY_THEME_MODE_LIGHT,
                        onSelect = { userPreferences.setThemeMode(UserPreferences.KEY_THEME_MODE_LIGHT) }
                    )
                    ThemeOptionRow(
                        label = "Dark",
                        selected = themeMode == UserPreferences.KEY_THEME_MODE_DARK,
                        onSelect = { userPreferences.setThemeMode(UserPreferences.KEY_THEME_MODE_DARK) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- Contributor Features (Hidden) ---
            if (AppConfig.isCreator(context, userName)) {
                item {
                    Text("Contributor Features", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))

                    // App Icon Variants with Visual Previews - Telegram Style
                    Text("App Icon", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Choose your exclusive app icon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    // Available icon options from assets
                    val iconOptions = listOf(
                        AppIconOption("default", "Default", "app_icon/app_icon_1.jpg"),
                        AppIconOption("vintage", "Vintage", "app_icon/app_icon_2.png"),
                        AppIconOption("aqua", "Aqua", "app_icon/app_icon_3.png"),
                        AppIconOption("premium", "Premium", "app_icon/app_icon_4.png", isPremium = true),
                        AppIconOption("turbo", "Turbo", "app_icon/app_icon_5.png"),
                        AppIconOption("neon", "Neon", "app_icon/app_icon_6.png")
                    )
                    
                    // Get current icon from AppIconManager
                    var selectedIconId by remember { mutableStateOf(AppIconManager.getCurrentIconId(context)) }
                    
                    // Telegram-style horizontal scrolling icon picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        iconOptions.forEach { iconOption ->
                            AssetIconPreviewCard(
                                context = context,
                                iconOption = iconOption,
                                isSelected = selectedIconId == iconOption.id,
                                onClick = { 
                                    selectedIconId = iconOption.id
                                    // Apply icon change using AppIconManager
                                    AppIconManager.setIconById(context, iconOption.id)
                                    
                                    // Show snackbar and close app
                                    scope.launch {
                                        SnackbarUtils.ShowSnackbar(
                                            scope = scope,
                                            snackbarHostState = snackbarHostState,
                                            message = "App icon changed! Restarting app...",
                                            duration = SnackbarDuration.Short
                                        )
                                        // Wait for snackbar to show
                                        delay(1500)
                                        // Close the app
                                        (context as? Activity)?.finishAffinity() ?: Process.killProcess(Process.myPid())
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Note about icon change
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Changes take effect after app restart",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
            }

            // --- 2. Album Art Section ---
            item {
                Text("Album Art", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan local library for album art", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                "Requires media images permission"
                            } else {
                                "Show album art for local songs"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = scanLocalAlbumArt,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasImagePermission) {
                                    imagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    userPreferences.setScanLocalAlbumArt(true)
                                    onScanTrigger()
                                }
                            } else {
                                userPreferences.setScanLocalAlbumArt(false)
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- 3. Display Settings (Numerical Inputs) ---
            item {
                Text("Display Settings", style = MaterialTheme.typography.titleLarge)

                // Online History Count
                NumericalSetting(
                    label = "Online History Songs Count",
                    description = "Number of recently played online songs to display (1-100).",
                    currentValue = onlineHistoryCount,
                    onValueChange = { userPreferences.setOnlineHistoryCount(it) },
                    valueRange = 1f..100f
                )

                // Recommendations Count
                NumericalSetting(
                    label = "Recommendations Count",
                    description = "Number of AI-recommended songs based on listening history (1-100).",
                    currentValue = recommendationsCount,
                    onValueChange = { userPreferences.setRecommendationsCount(it) },
                    valueRange = 1f..100f
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- 4. Scan Directories ---
            item {
                Text("Scan Directories", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                // Info text about app directory
                Text(
                    text = "Note: Download/SyncTax folder is always scanned for downloaded songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = { dirPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Scan Folder")
                }
                Spacer(Modifier.height(16.dp))

                Text("Current Scan Paths:", style = MaterialTheme.typography.titleSmall)
            }

            // App directory item (always scanned, non-removable)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Download/SyncTax (App Managed)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary, // Highlight as important
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // User-added scan paths
            items(scanPaths) { uriString ->
                val uri = runCatching { uriString.toUri() }.getOrNull()
                val displayName = remember(uri) {
                    uri?.let { DocumentFile.fromTreeUri(context, it)?.name ?: "External Folder" } ?: "Invalid Path"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* optional: show path details */ }
                        .padding(vertical = 4.dp, horizontal = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                uri?.let { context.contentResolver.releasePersistableUriPermission(it, releaseFlags) }
                            } catch (_: Exception) {
                            }
                            userPreferences.removeScanPath(uriString)
                            onScanTrigger() // Rescan after removal
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove path $displayName",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
