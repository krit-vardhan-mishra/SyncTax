package com.just_for_fun.synctax.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onScanTrigger: () -> Unit = {}
) {
    val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)
    val scanPaths by userPreferences.scanPaths.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF folder picker
    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            scope.launch {
                userPreferences.addScanPath(uri.toString())
                onScanTrigger() // Trigger scan after adding the path
            }
        } catch (e: Exception) {
            // optionally: show a toast or snackbar about failure
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)

            // Radio group for theme selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // --- Scan directories UI ---
            Text("Scan directories", style = MaterialTheme.typography.titleMedium)
            
            // Info text about app directory
            Text(
                text = "Note: Download/SyncTax folder is always scanned for downloaded songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(onClick = { dirPickerLauncher.launch(null) }) {
                Text("Add folder")
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // App directory item (always scanned, non-removable)
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Download/SyncTax (Always scanned)",
                            modifier = Modifier.weight(1f)
                        )
                        // No remove button for app directory
                    }
                }

                items(scanPaths) { uriString ->
                    val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                    val displayName = remember(uri) {
                        uri?.let { DocumentFile.fromTreeUri(context, it)?.name ?: it.toString() } ?: uriString
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = displayName,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    // optional: navigate to a folder preview screen or show details
                                }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    uri?.let { context.contentResolver.releasePersistableUriPermission(it, releaseFlags) }
                                } catch (_: Exception) {
                                }
                                userPreferences.removeScanPath(uriString)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
            // --- end scan directories UI ---
        }
    }
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}