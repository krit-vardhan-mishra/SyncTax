package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.init.AppInitializer
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    
    // Collect initialization progress from AppInitializer
    val initProgress by AppInitializer.progress.collectAsState(
        initial = AppInitializer.InitProgress("Initializing...", 0f)
    )
    
    // Animate the progress smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = initProgress.progress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "SplashProgress"
    )
    
    // Current status message
    var statusMessage by remember { mutableStateOf("Initializing...") }
    
    // Update status message when phase changes
    LaunchedEffect(initProgress.phase) {
        statusMessage = initProgress.phase
    }
    
    // Start initialization when splash screen appears
    LaunchedEffect(Unit) {
        // Run the initialization process
        AppInitializer.initialize(context).collect { progress ->
            // When initialization is complete, call onSplashFinished
            if (progress.isComplete) {
                onSplashFinished()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            WavySlider(
                value = animatedProgress,
                onValueChange = {},
                valueRange = 0f..1f,
                enabled = false,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}