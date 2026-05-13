package com.just_for_fun.synctax.presentation.components

import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerView(
    onNavigateBack: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    val barcodeView = remember {
        BarcodeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Bind the BarcodeView to the Compose Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                barcodeView.resume()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                barcodeView.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            barcodeView.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Preview
        AndroidView(
            factory = {
                barcodeView.apply {
                    decodeContinuous(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult?) {
                            if (!hasScanned && result != null && !result.text.isNullOrBlank()) {
                                hasScanned = true
                                barcodeView.pause()
                                onQrScanned(result.text)
                            }
                        }
                        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Custom Draw Overlay (Cutout window + Animation)
        val infiniteTransition = rememberInfiniteTransition(label = "laser")
        val laserPosition by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser"
        )

        val accentColor = AppColors.accentPrimary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val minDimension = min(canvasWidth, canvasHeight)
            val cutoutSize = minDimension * 0.7f
            val cutoutX = (canvasWidth - cutoutSize) / 2
            val cutoutY = (canvasHeight - cutoutSize) / 2

            val overlayColor = Color.Black.copy(alpha = 0.75f)

            // Draw 4 rectangles around the center to create a foolproof transparent cutout
            // Top
            drawRect(color = overlayColor, topLeft = Offset(0f, 0f), size = Size(canvasWidth, cutoutY))
            // Bottom
            drawRect(color = overlayColor, topLeft = Offset(0f, cutoutY + cutoutSize), size = Size(canvasWidth, canvasHeight - (cutoutY + cutoutSize)))
            // Left
            drawRect(color = overlayColor, topLeft = Offset(0f, cutoutY), size = Size(cutoutX, cutoutSize))
            // Right
            drawRect(color = overlayColor, topLeft = Offset(cutoutX + cutoutSize, cutoutY), size = Size(canvasWidth - (cutoutX + cutoutSize), cutoutSize))

            // Draw glowing bounding frame
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(cutoutX, cutoutY),
                size = Size(cutoutSize, cutoutSize),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw animated laser scanner line
            val laserY = cutoutY + (cutoutSize * laserPosition)
            drawLine(
                color = accentColor.copy(alpha = 0.9f),
                start = Offset(cutoutX + 16.dp.toPx(), laserY),
                end = Offset(cutoutX + cutoutSize - 16.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. UI Controls
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Scan Host QR", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Align the QR code within the frame to connect.",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 32.dp, vertical = 64.dp)
            )
        }
    }
}