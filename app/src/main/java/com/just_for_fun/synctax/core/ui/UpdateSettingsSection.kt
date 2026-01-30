package com.just_for_fun.synctax.core.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.BuildConfig
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.data.model.GithubRelease
import com.just_for_fun.synctax.presentation.viewmodels.AppUpdateViewModel
import kotlinx.coroutines.launch

/**
 * Comprehensive update settings section for Settings screen
 * Shows app version, update checking, and library update status
 */
@Composable
fun UpdateSettingsSection(
    modifier: Modifier = Modifier,
    viewModel: AppUpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val libraryUpdateState by viewModel.libraryUpdateState.collectAsState()

    var showAppUpdateDialog by remember { mutableStateOf(false) }
    var showLibraryUpdateDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    // Show dialogs
    if (showAppUpdateDialog && appUpdateState is AppUpdateViewModel.AppUpdateState.UpdateAvailable) {
        AppUpdateDialog(
            release = (appUpdateState as AppUpdateViewModel.AppUpdateState.UpdateAvailable).release,
            onDismiss = { 
                showAppUpdateDialog = false 
                viewModel.resetAppUpdateState()
            },
            onUpdate = { release ->
                viewModel.downloadAppUpdate(release)
            },
            onSkip = { version ->
                viewModel.skipAppVersion(version)
                showAppUpdateDialog = false
            }
        )
    }

    if (showLibraryUpdateDialog && libraryUpdateState is AppUpdateViewModel.LibraryUpdateState.UpdateAvailable) {
        val state = libraryUpdateState as AppUpdateViewModel.LibraryUpdateState.UpdateAvailable
        LibraryUpdateInfoDialog(
            currentVersion = state.currentVersion,
            latestVersion = state.latestVersion,
            releaseNotes = state.releaseNotes,
            releaseUrl = state.releaseUrl,
            onDismiss = { 
                showLibraryUpdateDialog = false 
                viewModel.resetLibraryUpdateState()
            }
        )
    }

    Column(modifier = modifier) {
        // Section Header
        Text(
            text = "Updates",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // App Version Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // App version info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Check for update button
                    when (appUpdateState) {
                        is AppUpdateViewModel.AppUpdateState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is AppUpdateViewModel.AppUpdateState.UpToDate -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Up to date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is AppUpdateViewModel.AppUpdateState.UpdateAvailable -> {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text("NEW")
                            }
                        }
                        is AppUpdateViewModel.AppUpdateState.Downloading -> {
                            val progress = (appUpdateState as AppUpdateViewModel.AppUpdateState.Downloading).progress
                            CircularProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        else -> {
                            IconButton(
                                onClick = { viewModel.checkForAppUpdate() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check for updates"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status message
                when (val state = appUpdateState) {
                    is AppUpdateViewModel.AppUpdateState.UpToDate -> {
                        Text(
                            text = "✓ You're using the latest version",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is AppUpdateViewModel.AppUpdateState.UpdateAvailable -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Update available: ${state.release.tagName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { showAppUpdateDialog = true }) {
                                Text("Update")
                            }
                        }
                    }
                    is AppUpdateViewModel.AppUpdateState.Downloading -> {
                        Text(
                            text = "Downloading update... ${state.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is AppUpdateViewModel.AppUpdateState.DownloadComplete -> {
                        Text(
                            text = "✓ Download complete! Install pending...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is AppUpdateViewModel.AppUpdateState.Error -> {
                        Text(
                            text = "⚠ ${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Check for Updates button
                OutlinedButton(
                    onClick = { viewModel.checkForAppUpdate() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = appUpdateState !is AppUpdateViewModel.AppUpdateState.Checking &&
                              appUpdateState !is AppUpdateViewModel.AppUpdateState.Downloading
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for App Updates")
                }

                // View Changelog button
                TextButton(
                    onClick = { 
                        viewModel.loadChangelog()
                        showChangelogDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Changelog")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Library Updates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NewPipe Extractor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "YouTube streaming library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    when (libraryUpdateState) {
                        is AppUpdateViewModel.LibraryUpdateState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is AppUpdateViewModel.LibraryUpdateState.UpToDate -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Up to date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is AppUpdateViewModel.LibraryUpdateState.UpdateAvailable -> {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text("UPDATE")
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = { viewModel.checkForLibraryUpdates() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check library updates"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Library status
                when (val state = libraryUpdateState) {
                    is AppUpdateViewModel.LibraryUpdateState.UpToDate -> {
                        Text(
                            text = "✓ Version ${state.currentVersion} (Latest)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is AppUpdateViewModel.LibraryUpdateState.UpdateAvailable -> {
                        Column {
                            Text(
                                text = "⚠ Update available: ${state.latestVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Current: ${state.currentVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ℹ Library updates require a new app version. Check for app updates to get the latest libraries.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showLibraryUpdateDialog = true }) {
                                Text("View Details")
                            }
                        }
                    }
                    is AppUpdateViewModel.LibraryUpdateState.Error -> {
                        Text(
                            text = "⚠ ${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Text(
                            text = "Version 0.25.1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.checkForLibraryUpdates() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = libraryUpdateState !is AppUpdateViewModel.LibraryUpdateState.Checking
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_update),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Library Updates")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-update settings
        AutoUpdateSettingsCard()
    }

    // Changelog dialog
    if (showChangelogDialog) {
        val releases by viewModel.allReleases.collectAsState()
        ChangelogDialog(
            releases = releases,
            onDismiss = { showChangelogDialog = false }
        )
    }
}

/**
 * Auto-update settings card
 */
@Composable
private fun AutoUpdateSettingsCard() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
    }

    var checkOnStart by remember { mutableStateOf(prefs.getBoolean("check_update_on_start", true)) }
    var includeBeta by remember { mutableStateOf(prefs.getBoolean("include_beta_updates", false)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Update Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check on app start
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        checkOnStart = !checkOnStart
                        prefs.edit().putBoolean("check_update_on_start", checkOnStart).apply()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check on app start",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Automatically check for updates when app opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checkOnStart,
                    onCheckedChange = {
                        checkOnStart = it
                        prefs.edit().putBoolean("check_update_on_start", it).apply()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Include beta versions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        includeBeta = !includeBeta
                        prefs.edit().putBoolean("include_beta_updates", includeBeta).apply()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include beta versions",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Get notified about pre-release versions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeBeta,
                    onCheckedChange = {
                        includeBeta = it
                        prefs.edit().putBoolean("include_beta_updates", it).apply()
                    }
                )
            }
        }
    }
}

/**
 * App update dialog
 */
@Composable
fun AppUpdateDialog(
    release: GithubRelease,
    onDismiss: () -> Unit,
    onUpdate: (GithubRelease) -> Unit,
    onSkip: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Version ${release.tagName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = release.body.ifEmpty { "No release notes available" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 15,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(release) }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSkip(release.tagName) }) {
                    Text("Skip")
                }
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}

/**
 * Library update info dialog
 */
@Composable
fun LibraryUpdateInfoDialog(
    currentVersion: String,
    latestVersion: String,
    releaseNotes: String,
    releaseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text("Library Update Available")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "NewPipe Extractor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: $currentVersion → Latest: $latestVersion",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Library updates are bundled with app updates. " +
                           "When a new app version is released with updated libraries, " +
                           "you'll be notified through the app update system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (releaseNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Release Notes:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri())
                    context.startActivity(intent)
                }
            ) {
                Text("View on GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Changelog dialog showing all releases
 */
@Composable
fun ChangelogDialog(
    releases: List<GithubRelease>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Changelog",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Release list
                if (releases.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(releases.size) { index ->
                            val release = releases[index]
                            ChangelogItem(
                                release = release,
                                isCurrentVersion = release.tagName.contains(BuildConfig.VERSION_NAME),
                                onOpenUrl = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogItem(
    release: GithubRelease,
    isCurrentVersion: Boolean,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentVersion)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = release.tagName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrentVersion) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Current", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (release.prerelease) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                            Text("Beta", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TextButton(onClick = { onOpenUrl(release.htmlUrl) }) {
                    Text("View", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (release.body.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = release.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download buttons for APKs
            if (release.assets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    release.assets.filter { it.name.endsWith(".apk") }.forEach { asset ->
                        AssistChip(
                            onClick = { onOpenUrl(asset.browserDownloadUrl) },
                            label = { 
                                Text(
                                    text = asset.name.take(20) + if (asset.name.length > 20) "..." else "",
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
