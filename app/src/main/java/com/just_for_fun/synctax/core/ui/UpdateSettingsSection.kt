package com.just_for_fun.synctax.core.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.isSystemInDarkTheme
import coil.compose.AsyncImage
import android.webkit.JavascriptInterface
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex

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
    var selectedRelease by remember { mutableStateOf<GithubRelease?>(null) }

    if (selectedRelease != null) {
        ReleaseDetailDialog(
            release = selectedRelease!!,
            onDismiss = { selectedRelease = null }
        )
    }

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
            onDismiss = { showChangelogDialog = false },
            onReleaseClick = { release -> selectedRelease = release }
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
    onDismiss: () -> Unit,
    onReleaseClick: (GithubRelease) -> Unit
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
                                onClick = { onReleaseClick(release) },
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
    onClick: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isCurrentVersion)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp, 
            if (isCurrentVersion) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.outlineVariant
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
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog showing full details of a release
 */
@Composable
fun ReleaseDetailDialog(
    release: GithubRelease,
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
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = release.name.ifEmpty { release.tagName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = release.publishedAt.take(10), // Show date part
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (release.prerelease) {
                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    text = "Pre-release",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Release Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    MarkdownText(
                        text = release.body.ifEmpty { "No release notes provided." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Assets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    release.assets.forEach { asset ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, asset.browserDownloadUrl.toUri())
                                    context.startActivity(intent)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = asset.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${String.format("%.2f", asset.size / 1024f / 1024f)} MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog showing Read Me content
 */
@Composable
fun ReadmeDialog(
    content: String,
    onDismiss: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (expandedImageUrl != null) {
        ImageViewerDialog(
            imageUrl = expandedImageUrl!!,
            onDismiss = { expandedImageUrl = null }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read Me",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (content.isEmpty()) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             CircularProgressIndicator()
                         }
                    } else {
                        val base64Content = try {
                            Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
                        } catch (e: Exception) { "" }
                        
                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                                <style>
                                    body { 
                                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                                        padding: 16px; 
                                        color: ${if (isDarkTheme) "#E0E0E0" else "#212121"}; 
                                        background-color: ${if (isDarkTheme) "#1E1E1E" else "#FFFFFF"}; 
                                    }
                                    a { color: ${if (isDarkTheme) "#8AB4F8" else "#1A73E8"}; }
                                    img { max-width: 100%; border-radius: 8px; cursor: pointer; } 
                                    pre { background-color: ${if (isDarkTheme) "#2D2D2D" else "#F5F5F5"}; padding: 12px; border-radius: 8px; overflow-x: auto; }
                                    code { font-family: monospace; background-color: ${if (isDarkTheme) "#2D2D2D" else "#F5F5F5"}; padding: 2px 4px; border-radius: 4px; }
                                    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                                    th, td { border: 1px solid ${if (isDarkTheme) "#444" else "#DDD"}; padding: 8px; text-align: left; }
                                    th { background-color: ${if (isDarkTheme) "#333" else "#F9F9F9"}; }
                                    blockquote { border-left: 4px solid ${if (isDarkTheme) "#444" else "#DDD"}; margin: 0; padding-left: 16px; color: ${if (isDarkTheme) "#AAA" else "#666"}; }
                                    h1, h2, h3 { border-bottom: 1px solid ${if (isDarkTheme) "#333" else "#EEE"}; padding-bottom: 8px; }
                                    p { line-height: 1.6; }
                                    .scroll-gallery { overflow-x: auto; white-space: nowrap; -webkit-overflow-scrolling: touch; padding-bottom: 8px; margin: 16px 0; }
                                    .scroll-gallery img { max-width: none; height: 300px; margin-right: 8px; vertical-align: middle; }
                                    .scroll-gallery table { width: auto; min-width: 100%; border-collapse: separate; border-spacing: 8px 0; margin: 0; } 
                                    .scroll-gallery td { border: none !important; padding: 0 !important; vertical-align: top; }
                                    /* Restore normal table style if not in gallery */
                                    table:not(.gallery-table) { width: 100%; border-collapse: collapse; }
                                </style>
                            </head>
                            <body>
                                <div id="content"></div>
                                <script>
                                    try {
                                        const md = decodeURIComponent(escape(window.atob('$base64Content')));
                                        document.getElementById('content').innerHTML = marked.parse(md);
                                        
                                        // Logic to wrap image groups in scrollable container
                                        setTimeout(function() {
                                            // Handle tables with images
                                            var tables = document.querySelectorAll('table');
                                            tables.forEach(function(table) {
                                                if (table.querySelector('img')) {
                                                    var wrapper = document.createElement('div');
                                                    wrapper.className = 'scroll-gallery';
                                                    table.parentNode.insertBefore(wrapper, table);
                                                    wrapper.appendChild(table);
                                                    table.classList.add('gallery-table');
                                                    // Ensure images in table cells don't shrink
                                                    var imgs = table.getElementsByTagName('img');
                                                    for (var i=0; i<imgs.length; i++) {
                                                        imgs[i].style.maxWidth = 'none';
                                                        imgs[i].style.height = '300px'; // Force height
                                                    }
                                                }
                                            });

                                            // Handle paragraphs with multiple images
                                            var ps = document.querySelectorAll('p');
                                            ps.forEach(function(p) {
                                                var imgCount = p.getElementsByTagName('img').length;
                                                if (imgCount >= 2) {
                                                    p.className = 'scroll-gallery';
                                                }
                                            });

                                            // Add click listeners (re-select after potential moves)
                                            var imgs = document.getElementsByTagName('img');
                                            for (var i = 0; i < imgs.length; i++) {
                                                imgs[i].onclick = function() {
                                                    try {
                                                        Android.showImage(this.src);
                                                    } catch(e) { console.error(e); }
                                                }
                                            }
                                        }, 100);
                                    } catch(e) {
                                        document.getElementById('content').innerText = "Error rendering content: " + e.message;
                                    }
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportZoom(true)
                                    
                                    addJavascriptInterface(object : Any() {
                                        @JavascriptInterface
                                        fun showImage(url: String) {
                                            Handler(Looper.getMainLooper()).post {
                                                expandedImageUrl = url
                                            }
                                        }
                                    }, "Android")

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            if (url != null && (url.startsWith("http") || url.startsWith("https"))) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                ctx.startActivity(intent)
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                    setBackgroundColor(0)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(
                                    "https://raw.githubusercontent.com/krit-vardhan-mishra/SyncTax/master/",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            },
                            onRelease = { it.destroy() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to view an image fullscreen
 */
@Composable
fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f) // Dimmed background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .zIndex(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Image
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
