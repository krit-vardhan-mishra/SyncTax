package com.just_for_fun.synctax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Train ML Models") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "This will train the ML models using the songs available on your device.",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Number of songs: ${uiState.allSongs.size}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Training data size: ${uiState.trainingDataSize} plays",
                style = MaterialTheme.typography.bodyMedium
            )

            val progress = (uiState.trainingDataSize.toFloat() / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Text(
                text = "${(progress * 100).toInt()}% trained",
                style = MaterialTheme.typography.bodySmall
            )

            if (uiState.isTraining) {
                CircularProgressIndicator()
                Text("Training models...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(onClick = { homeViewModel.trainModels() }) {
                    Text("Train models")
                }
            }

            uiState.trainingComplete.takeIf { it }?.let {
                Text("Training complete", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
