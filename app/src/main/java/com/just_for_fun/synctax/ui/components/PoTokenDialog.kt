package com.just_for_fun.synctax.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PoTokenDialog(
    currentPoToken: String,
    onPoTokenChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(currentPoToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter YouTube PO Token") },
        text = {
            Column {
                Text(
                    "YouTube now requires a PO Token for downloading. Follow these steps:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Visit: github.com/yt-dlp/yt-dlp/wiki/PO-Token-Guide\n" +
                    "2. Use the web+android method (recommended)\n" +
                    "3. Copy the token (format: web+XXXXX or android+XXXXX)\n" +
                    "4. Paste it below",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("PO Token") },
                    placeholder = { Text("web+XXXXX or android+XXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Tokens expire after ~6 hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPoTokenChanged(token)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
