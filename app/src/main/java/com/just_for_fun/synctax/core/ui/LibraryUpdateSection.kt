package com.just_for_fun.synctax.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.utils.LibraryUpdateResult
import com.just_for_fun.synctax.presentation.viewmodels.LibraryUpdateViewModel
import kotlinx.coroutines.launch

/**
 * Composable for checking library updates in settings
 */
@Composable
fun LibraryUpdateSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LibraryUpdateViewModel = viewModel()
    val updateState by viewModel.updateState.collectAsState()
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf<LibraryUpdateResult?>(null) }

    // Show dialog when update is found
    showUpdateDialog?.let { updateResult ->
        LibraryUpdateDialog(
            updateResult = updateResult,
            onDismiss = { showUpdateDialog = null }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_update),
                    contentDescription = "Library Updates",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Library Updates",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keep streaming libraries up to date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current version info
            when (val state = updateState) {
                is LibraryUpdateViewModel.UpdateState.Idle -> {
                    Text(
                        text = "Current version: ${getCurrentVersion()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is LibraryUpdateViewModel.UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking for updates...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is LibraryUpdateViewModel.UpdateState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Up to date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All libraries are up to date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is LibraryUpdateViewModel.UpdateState.UpdateAvailable -> {
                    Column {
                        Text(
                            text = "Update available: ${state.result.latestVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Current: ${state.result.currentVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is LibraryUpdateViewModel.UpdateState.Error -> {
                    Text(
                        text = "Check failed: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel.checkForUpdates()
                        }
                    },
                    enabled = updateState !is LibraryUpdateViewModel.UpdateState.Checking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check Now")
                }

                if (updateState is LibraryUpdateViewModel.UpdateState.UpdateAvailable) {
                    Button(
                        onClick = {
                            showUpdateDialog = (updateState as LibraryUpdateViewModel.UpdateState.UpdateAvailable).result
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

/**
 * Get current library version (hardcoded for now, should be from build config)
 */
private fun getCurrentVersion(): String {
    return "0.25.1" // Update this when you update the library
}