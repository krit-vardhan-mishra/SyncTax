package com.just_for_fun.synctax.presentation.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel
import com.just_for_fun.synctax.core.party.PartyHotspotInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.net.URLEncoder

private const val TAG = "CreatePartyScreen"

private fun getHostPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePartyScreen(
    partyViewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onStartPartyClick: (String) -> Unit,
    initialPartyName: String? = null,
    autoStart: Boolean = false
) {
    val lavenderColor = AppColors.textTitle
    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground
    val context = LocalContext.current

    val isHosting by partyViewModel.isHosting.collectAsState()
    val members by partyViewModel.members.collectAsState()
    val hotspotInfo by partyViewModel.hostHotspotInfo.collectAsState()
    val hotspotError by partyViewModel.hotspotError.collectAsState()

    var partyName by remember { mutableStateOf(initialPartyName ?: "My Party") }
    var autoStartConsumed by remember { mutableStateOf(false) }
    var startAttempted by remember { mutableStateOf(false) }
    
    val hostPermissions = remember { getHostPermissions() }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var pendingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    val permissionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startAttempted = true
            partyViewModel.startHosting(partyName)
        } else {
            showPermissionSheet = true
        }
    }

    fun attemptStartHosting() {
        val missing = hostPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startAttempted = true
            partyViewModel.startHosting(partyName)
        } else {
            pendingPermissions = missing
            showPermissionSheet = true
        }
    }

    LaunchedEffect(initialPartyName) {
        if (!initialPartyName.isNullOrBlank() && !isHosting) {
            partyName = initialPartyName
        }
    }

    LaunchedEffect(autoStart, isHosting, partyName) {
        if (autoStart && !autoStartConsumed && !isHosting) {
            autoStartConsumed = true
            Log.d(TAG, "⚡ Auto-starting party: $partyName")
            attemptStartHosting()
        }
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "radar_anim"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    if (showPermissionSheet) {
        val wifiManager = remember { context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager }
        val locationManager = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager }
        val isWifiEnabled = wifiManager.isWifiEnabled
        val isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        val permissionsToShow = if (pendingPermissions.isNotEmpty()) pendingPermissions else hostPermissions

        ModalBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            sheetState = permissionSheetState,
            containerColor = AppColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Prerequisites to Host",
                    color = AppColors.textTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enable all requirements below, then tap Start.",
                    color = AppColors.textBody,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Wi-Fi Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isWifiEnabled) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Wi-Fi", color = AppColors.textTitle, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (!isWifiEnabled) {
                        Button(
                            onClick = {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Turn On", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Text("ON", color = Color.Green, fontSize = 12.sp)
                    }
                }

                // Location Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isLocationEnabled) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Location", color = AppColors.textTitle, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (!isLocationEnabled) {
                        Button(
                            onClick = {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Turn On", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Text("ON", color = Color.Green, fontSize = 12.sp)
                    }
                }

                // Runtime Permissions
                permissionsToShow.forEach { permission ->
                    val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isGranted) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = permission.substringAfterLast("."),
                            color = AppColors.textTitle,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isGranted) "Granted" else "Needed",
                            color = if (isGranted) Color.Green else AppColors.textBody,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        showPermissionSheet = false
                        val missing = permissionsToShow.filter { p ->
                            ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        } else {
                            // All permissions granted, try starting
                            startAttempted = true
                            partyViewModel.startHosting(partyName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                ) {
                    Text("Grant & Start", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showPermissionSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.mainBackground)
                ) {
                    Text("Cancel", color = AppColors.textBody, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val isWaitingForHotspot = startAttempted && !isHosting && hotspotError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isHosting) "Hosting: $partyName" else "Create a Party",
                        color = lavenderColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = lavenderColor)
                    }
                },
                actions = {
                    if (isHosting && hotspotInfo != null) {
                        Button(
                            onClick = { onStartPartyClick(partyName) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Go to Session", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (!isHosting && !isWaitingForHotspot) {
                        Button(
                            onClick = {
                                Log.d(TAG, "🎉 Starting party from top bar: $partyName")
                                attemptStartHosting()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Start", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (isHosting && hotspotInfo != null) {
                        onStartPartyClick(partyName)
                    } else if (!isHosting && !isWaitingForHotspot) {
                        Log.d(TAG, "🎉 Starting party: $partyName")
                        attemptStartHosting()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWaitingForHotspot) accentOrange.copy(alpha = 0.5f) else accentOrange
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = !isWaitingForHotspot
            ) {
                Text(
                    when {
                        isWaitingForHotspot -> "Starting Hotspot..."
                        isHosting && hotspotInfo != null -> "Go to Session"
                        isHosting -> "Waiting for Hotspot..."
                        else -> "Start Party"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = darkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Party Name Input (only visible before hosting starts)
            if (!isHosting) {
                item {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text("Party Name", color = AppColors.textBody) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentOrange,
                            unfocusedBorderColor = AppColors.textBody,
                            focusedTextColor = AppColors.textTitle,
                            unfocusedTextColor = AppColors.textTitle,
                            cursorColor = accentOrange
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This name will be visible to guests during the session.",
                        color = AppColors.textBody,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Radar Animation Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isHosting) 200.dp else 180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing rings
                    Box(
                        modifier = Modifier
                            .size((pulseRadius * 2).dp)
                            .clip(CircleShape)
                            .background(accentOrange.copy(alpha = pulseAlpha * 0.3f))
                    )

                    // Central Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                    }
                }
            }

            if (isHosting) {
                item {
                    Text(
                        text = "Waiting for devices to join...",
                        color = AppColors.textBody,
                        fontSize = 14.sp
                    )
                }
            }

            // Show loading state while waiting for hotspot
            if (startAttempted && !isHosting && hotspotError == null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Starting hotspot...",
                        color = AppColors.textBody,
                        fontSize = 14.sp
                    )
                }
            }

            // Show error if hotspot failed
            if (hotspotError != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF442222))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "⚠ Failed to Start Hotspot",
                            color = Color(0xFFFF6B6B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hotspotError ?: "",
                            color = AppColors.textBody,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                startAttempted = false
                                partyViewModel.connectionManager.clearHotspotError()
                                // Open the prerequisites sheet so user can fix Wi-Fi/Location/Permissions
                                showPermissionSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Fix & Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isHosting && hotspotInfo != null) {
                item {
                    val qrPayload = remember(hotspotInfo) {
                        hotspotInfo?.let { buildPartyQrPayload(it) }
                    }
                    val qrSizeDp = 180.dp
                    val qrSizePx = with(LocalDensity.current) { qrSizeDp.roundToPx() }
                    val qrBitmap = remember(qrPayload, qrSizePx) {
                        qrPayload?.let { createQrBitmap(it, qrSizePx) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.cardBackground)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Hotspot Details",
                            color = AppColors.textTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SSID: ${hotspotInfo?.ssid ?: ""}",
                            color = AppColors.textBody,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Passphrase: ${hotspotInfo?.passphrase ?: ""}",
                            color = AppColors.textBody,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Port: ${hotspotInfo?.port ?: 0}",
                            color = AppColors.textBody,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share these details so others can join the party.",
                            color = AppColors.textBody,
                            fontSize = 12.sp
                        )

                        if (qrBitmap != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Party QR code",
                                modifier = Modifier
                                    .size(qrSizeDp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.CenterHorizontally),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scan to join",
                                color = AppColors.textBody,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Connected Members Section (visible when hosting)
            if (isHosting && members.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected Members (${members.size})",
                        color = lavenderColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(members) { member ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.cardBackground)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = member.name,
                                    color = AppColors.textTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Connected",
                                color = Color.Green.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                item {
                    BottomPaddingSpacer()
                }
            } else if (isHosting) {
                // Hosting but no one connected yet
                item {
                    Text(
                        text = "No devices connected yet",
                        color = AppColors.textBody,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Always add bottom padding to avoid mini player overlap
            item {
                BottomPaddingSpacer()
            }
        }
    }
}

private fun buildPartyQrPayload(info: PartyHotspotInfo): String {
    val ssid = URLEncoder.encode(info.ssid, Charsets.UTF_8.name())
    val passphrase = URLEncoder.encode(info.passphrase, Charsets.UTF_8.name())
    return "synctax://party?ssid=$ssid&pass=$passphrase&port=${info.port}"
}

private fun createQrBitmap(payload: String, sizePx: Int): Bitmap {
    val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx)
    return matrixToBitmap(matrix)
}

private fun matrixToBitmap(matrix: BitMatrix): Bitmap {
    val width = matrix.width
    val height = matrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}