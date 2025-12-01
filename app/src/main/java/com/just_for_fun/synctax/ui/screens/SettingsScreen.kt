package com.just_for_fun.synctax.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import kotlinx.coroutines.launch

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

// Helper component for numerical settings (Slider + Text Field)
@Composable
private fun NumericalSetting(
    label: String,
    description: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..100f
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
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
                modifier = Modifier.weight(1f)
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
                // Make it look simple and small
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
    onScanTrigger: () -> Unit = {}
) {
    val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
    val scanPaths by userPreferences.scanPaths.collectAsState(initial = emptyList())
    val scanLocalAlbumArt by userPreferences.scanLocalAlbumArt.collectAsState(initial = false)
    val onlineHistoryCount by userPreferences.onlineHistoryCount.collectAsState(initial = 10)
    val recommendationsCount by userPreferences.recommendationsCount.collectAsState(initial = 20)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permission state and launcher logic remains the same for correctness
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                val uri = runCatching { Uri.parse(uriString) }.getOrNull()
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
        }
    }
}