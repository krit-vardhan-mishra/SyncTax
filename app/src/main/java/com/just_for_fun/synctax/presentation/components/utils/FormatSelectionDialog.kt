package com.just_for_fun.synctax.presentation.components.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Format
import com.just_for_fun.synctax.presentation.components.card.FormatCard
import com.just_for_fun.synctax.core.utils.FormatUtil

/**
 * Bottom sheet dialog for selecting audio format/quality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionDialog(
    formats: List<Format>,
    onFormatSelected: (Format) -> Unit,
    onDismiss: () -> Unit,
    onRefreshFormats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatUtil = remember { FormatUtil(context) }
    
    var selectedFormat by remember { mutableStateOf<Format?>(null) }
    var showAllFormats by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Get sorted audio formats
    val audioFormats = remember(formats) {
        formatUtil.sortAudioFormats(formats)
    }
    
    // Show top 5 or all
    val displayedFormats = remember(audioFormats, showAllFormats) {
        if (showAllFormats) audioFormats else audioFormats.take(5)
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Audio Format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refresh button
                    if (onRefreshFormats != null) {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                onRefreshFormats()
                            },
                            enabled = !isRefreshing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh formats"
                            )
                        }
                    }
                    
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }
            
            // Show All toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showAllFormats,
                    onClick = { showAllFormats = false },
                    label = { Text("Top 5") }
                )
                FilterChip(
                    selected = showAllFormats,
                    onClick = { showAllFormats = true },
                    label = { Text("Show All (${audioFormats.size})") }
                )
            }

            
            // Format list
            if (displayedFormats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Loading formats..." else "No formats available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedFormats) { format ->
                        FormatCard(
                            format = format,
                            formatUtil = formatUtil,
                            isSelected = selectedFormat == format,
                            onSelected = { selectedFormat = it }
                        )
                    }
                }
            }
            
            // Download button
            Button(
                onClick = {
                    selectedFormat?.let { format ->
                        onFormatSelected(format)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = selectedFormat != null && !isRefreshing
            ) {
                Text(
                    text = if (selectedFormat != null) {
                        "Download ${formatUtil.getFormatDisplayName(selectedFormat!!)}"
                    } else {
                        "Select a format"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Info text
            if (selectedFormat != null) {
                Text(
                    text = "File size: ${formatUtil.formatFileSize(selectedFormat!!.filesize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
