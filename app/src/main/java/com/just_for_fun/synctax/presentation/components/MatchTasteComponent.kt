package com.just_for_fun.synctax.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define colors used in the design
private val CardBackground = Color(0xFFDEDEFF) // Light lavender/blue
private val AccentOrange = Color(0xFFFF5E2D)   // Vibrant orange
private val ArrowBgColor = Color.White.copy(alpha = 0.3f)

@Composable
fun InteractiveMatchTasteCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Outer container maintains the shape and background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .clickable { onClick() }
    ) {
        InitialScreenContent()
    }
}

@Composable
private fun BoxScope.InitialScreenContent() {
    // 1. Text Content (Top Left)
    Text(
        text = "Match your taste with friends\nand get a playlist",
        color = AccentOrange,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 28.dp, start = 24.dp, end = 100.dp) // Leave space for right graphics
    )

    // 2. Arrow Button (Bottom Left)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 24.dp, bottom = 24.dp)
            .size(44.dp)
            .background(ArrowBgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Proceed",
            tint = AccentOrange,
            modifier = Modifier.size(24.dp)
        )
    }

    // 3. Right Side Custom Graphics (Canvas)
    RightSideGraphics(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(140.dp)
    )
}

@Composable
private fun BoxScope.BleScreenContent() {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            tint = AccentOrange,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Open BLE and connect with your friend to listen together",
            color = AccentOrange,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RightSideGraphics(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Center the graphics slightly inward from the right edge
        val centerX = w - 40.dp.toPx()
        
        // Helper function to draw the stylized semi-circles
        fun drawStylizedRing(centerY: Float) {
            val maxRadius = 110.dp.toPx()
            
            // Outer soft glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentOrange.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = maxRadius
                ),
                center = Offset(centerX, centerY),
                radius = maxRadius
            )
            
            // Inner solid thick ring
            drawCircle(
                color = AccentOrange,
                center = Offset(centerX, centerY),
                radius = 80.dp.toPx(),
                style = Stroke(width = 30.dp.toPx())
            )
        }

        // Draw Top Ring
        drawStylizedRing(centerY = 0f)
        
        // Draw Bottom Ring
        drawStylizedRing(centerY = h)

        // Center Plus Button
        val buttonRadius = 26.dp.toPx()
        drawCircle(
            color = AccentOrange,
            radius = buttonRadius,
            center = Offset(centerX, h / 2)
        )

        // Draw the white '+' sign inside the center button
        val plusLineLength = 12.dp.toPx()
        val strokeW = 1.5.dp.toPx()
        
        // Horizontal line
        drawLine(
            color = Color.White,
            start = Offset(centerX - plusLineLength, h / 2),
            end = Offset(centerX + plusLineLength, h / 2),
            strokeWidth = strokeW
        )
        // Vertical line
        drawLine(
            color = Color.White,
            start = Offset(centerX, h / 2 - plusLineLength),
            end = Offset(centerX, h / 2 + plusLineLength),
            strokeWidth = strokeW
        )
    }
}

// Preview to render it in Android Studio
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewMatchTasteCard() {
    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = Color.Black // Dark background to make the card pop like in the image
        ) {
            InteractiveMatchTasteCard()
        }
    }
}