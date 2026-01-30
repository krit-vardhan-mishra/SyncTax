package com.just_for_fun.synctax.presentation.components.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.presentation.ui.theme.AppColors


// Composable helper functions for consistent spacing
@Composable
fun SectionSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SmallSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun LargeSpacer() {
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun ExtraLargeSpacer() {
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun BottomPaddingSpacer() {
    Spacer(modifier = Modifier.height(100.dp)) // Increased for bottom quick actions
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = AppColors.divider,
        thickness = 1.dp
    )
}

// Additional spacing functions for better layout control
@Composable
fun TinySpacer() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun MediumSpacer() {
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun HorizontalTinySpacer() {
    Spacer(modifier = Modifier.width(4.dp))
}

@Composable
fun HorizontalSmallSpacer() {
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
fun HorizontalMediumSpacer() {
    Spacer(modifier = Modifier.width(12.dp))
}

@Composable
fun HorizontalLargeSpacer() {
    Spacer(modifier = Modifier.width(16.dp))
}
