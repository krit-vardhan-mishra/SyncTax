package com.just_for_fun.youtubemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.youtubemusic.data.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit
) {
    val themeMode by userPreferences.themeMode.collectAsState(initial = UserPreferences.KEY_THEME_MODE_SYSTEM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
