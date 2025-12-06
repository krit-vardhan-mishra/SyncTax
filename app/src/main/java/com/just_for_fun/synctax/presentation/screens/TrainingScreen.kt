package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.presentation.viewmodels.HomeUiState
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.ModelTrainingStatus
import com.just_for_fun.synctax.presentation.viewmodels.SongPlayCount
import com.just_for_fun.synctax.presentation.viewmodels.TrainingSession
import com.just_for_fun.synctax.presentation.viewmodels.TrainingStatistics
import java.text.SimpleDateFormat
import java.util.Date

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
                title = { Text("ML Training Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        OptimizedLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Training Control Section
            item {
                TrainingControlCard(uiState, homeViewModel)
            }

            // Model Status Section
            item {
                ModelStatusCard(uiState.modelStatus)
            }

            // Training Statistics Section
            item {
                TrainingStatisticsCard(uiState.trainingStatistics)
            }

            // Training Logs Section
            if (uiState.trainingLogs.isNotEmpty()) {
                item {
                    TrainingLogsCard(uiState.trainingLogs)
                }
            }

            // Training History Section
            if (uiState.trainingHistory.isNotEmpty()) {
                item {
                    TrainingHistoryCard(uiState.trainingHistory)
                }
            }

            // Top Songs Visualization
            if (uiState.trainingStatistics.topSongs.isNotEmpty()) {
                item {
                    TopSongsCard(uiState.trainingStatistics.topSongs)
                }
            }

            // Listening Patterns Heatmap
            if (uiState.trainingStatistics.listeningPatterns.isNotEmpty()) {
                item {
                    ListeningPatternsCard(uiState.trainingStatistics.listeningPatterns)
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TrainingControlCard(uiState: HomeUiState, viewModel: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Model Training",
                style = MaterialTheme.typography.headlineSmall
            )

            if (uiState.isTraining) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiState.currentTrainingPhase,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = uiState.trainingProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(uiState.trainingProgress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.trainModels() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.allSongs.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Train Models")
                    }

                    if (uiState.trainingComplete) {
                        Button(
                            onClick = { viewModel.dismissError() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Complete")
                        }
                    }
                }

                Text(
                    text = "Available songs: ${uiState.allSongs.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            uiState.error?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModelStatusCard(modelStatus: ModelTrainingStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Model Status",
                style = MaterialTheme.typography.headlineSmall
            )

            StatusRow("Statistical Agent", modelStatus.statisticalAgentTrained)
            StatusRow("Collaborative Filtering", modelStatus.collaborativeAgentTrained)
            StatusRow("Python ML Model", modelStatus.pythonModelTrained)
            StatusRow("Fusion Agent", modelStatus.fusionAgentReady)

            if (modelStatus.lastTrainingTime > 0) {
                Text(
                    text = "Last trained: ${SimpleDateFormat("MMM dd, HH:mm").format(Date(modelStatus.lastTrainingTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Version: ${modelStatus.modelVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, isReady: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isReady) Color.Green else Color.Red
        )
    }
}

@Composable
private fun TrainingStatisticsCard(statistics: TrainingStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Training Statistics",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatisticItem("Total Plays", statistics.totalPlays.toString())
                StatisticItem("Unique Songs", statistics.uniqueSongsPlayed.toString())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatisticItem("Avg Completion", "${statistics.averageCompletionRate.toInt()}%")
                StatisticItem("Most Active Hour", "${statistics.mostActiveHour}:00")
            }

            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            StatisticItem("Most Active Day", dayNames.getOrNull(statistics.mostActiveDay) ?: "Unknown")
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
        // modifier = Modifier.weight(1f)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrainingLogsCard(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Training Logs",
                style = MaterialTheme.typography.headlineSmall
            )

            OptimizedLazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingHistoryCard(history: List<TrainingSession>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Training History",
                style = MaterialTheme.typography.headlineSmall
            )

            history.takeLast(5).forEach { session ->
                TrainingSessionRow(session)
            }
        }
    }
}

@Composable
private fun TrainingSessionRow(session: TrainingSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm").format(Date(session.timestamp)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Duration: ${session.duration / 1000}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (session.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (session.success) Color.Green else Color.Red
        )
    }
    Divider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun TopSongsCard(topSongs: List<SongPlayCount>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Top Songs",
                style = MaterialTheme.typography.headlineSmall
            )

            topSongs.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${song.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${song.playCount} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (index < topSongs.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ListeningPatternsCard(patterns: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Listening Patterns",
                style = MaterialTheme.typography.headlineSmall
            )

            // Simple heatmap visualization
            val maxCount = patterns.values.maxOrNull() ?: 1
            val hours = 0..23
            val days = 0..6

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Day labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(40.dp))
                    days.forEach { day ->
                        val dayName = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[day]
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                hours.forEach { hour ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d", hour),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(40.dp)
                        )
                        days.forEach { day ->
                            val count = patterns["$hour:$day"] ?: 0
                            val intensity = count.toFloat() / maxCount
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .padding(1.dp)
                                    .background(
                                        color = Color(
                                            red = (intensity * 255).toInt(),
                                            green = ((1 - intensity) * 255).toInt(),
                                            blue = 100
                                        ).copy(alpha = 0.7f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
