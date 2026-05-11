package com.just_for_fun.synctax.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel

private const val TAG = "PartyDashboard"

/**
 * Returns the list of permissions required for Party Mode based on API level.
 */
private fun getRequiredPartyPermissions(): List<String> {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    } else {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    return permissions
}

/**
 * Checks if all required Party Mode permissions are granted.
 */
private fun arePartyPermissionsGranted(context: android.content.Context): Boolean {
    return getRequiredPartyPermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Returns a user-friendly name for a permission.
 */
private fun getPermissionDisplayName(permission: String): String {
    return when (permission) {
        Manifest.permission.BLUETOOTH_ADVERTISE -> "Bluetooth Advertise"
        Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connect"
        Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scan"
        Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location"
        Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location"
        Manifest.permission.NEARBY_WIFI_DEVICES -> "Nearby Wi-Fi Devices"
        else -> permission.substringAfterLast(".")
    }
}

/**
 * Returns an icon for a permission.
 */
private fun getPermissionIcon(permission: String): ImageVector {
    return when (permission) {
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN -> Icons.Default.Bluetooth
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION -> Icons.Default.LocationOn
        Manifest.permission.NEARBY_WIFI_DEVICES -> Icons.Default.Wifi
        else -> Icons.Default.Check
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDashboardScreen(
    partyViewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onCreatePartyClick: () -> Unit,
    onPreviousPartyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lavenderColor = AppColors.textTitle
    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground

    val discoveredParties by partyViewModel.discoveredParties.collectAsState()

    // Permission state
    var permissionsGranted by remember { mutableStateOf(arePartyPermissionsGranted(context)) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "host" or "join"

    // Discovery state
    var isDiscovering by remember { mutableStateOf(false) }

    // Permission bottom sheet state
    val permissionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            Log.d(TAG, "✅ All party permissions granted")
            // Proceed with the pending action
            when (pendingAction) {
                "host" -> onCreatePartyClick()
                "join" -> {
                    isDiscovering = true
                    partyViewModel.startDiscovery()
                }
            }
            pendingAction = null
        } else {
            Log.d(TAG, "❌ Some party permissions denied: ${permissions.filter { !it.value }.keys}")
            showPermissionSheet = true
        }
    }

    /**
     * Checks permissions and either proceeds or shows the permission sheet.
     */
    fun checkPermissionsAndProceed(action: String) {
        if (arePartyPermissionsGranted(context)) {
            permissionsGranted = true
            when (action) {
                "host" -> onCreatePartyClick()
                "join" -> {
                    isDiscovering = true
                    partyViewModel.startDiscovery()
                }
            }
        } else {
            pendingAction = action
            permissionLauncher.launch(getRequiredPartyPermissions().toTypedArray())
        }
    }

    // Stop discovery when leaving
    DisposableEffect(Unit) {
        onDispose {
            if (isDiscovering) {
                partyViewModel.stopDiscovery()
            }
        }
    }

    // Pulsing animation for the Create Party card
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    // Permission Bottom Sheet
    if (showPermissionSheet) {
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
                    text = "Permissions Required",
                    color = AppColors.textTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Party Mode uses Bluetooth and Wi-Fi to connect nearby devices. These permissions are required for the feature to work.",
                    color = AppColors.textBody,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Permission list
                getRequiredPartyPermissions().forEach { permission ->
                    val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getPermissionIcon(permission),
                            contentDescription = null,
                            tint = if (isGranted) Color.Green else AppColors.textBody,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getPermissionDisplayName(permission),
                            color = AppColors.textTitle,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (isGranted) "Granted" else "Denied",
                            tint = if (isGranted) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Grant permissions button
                Button(
                    onClick = {
                        showPermissionSheet = false
                        permissionLauncher.launch(getRequiredPartyPermissions().toTypedArray())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                ) {
                    Text("Allow Permissions", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel button
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Party Mode", color = lavenderColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = lavenderColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Start a New Party Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(lavenderColor)
                    .clickable { checkPermissionsAndProceed("host") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Speaker,
                        contentDescription = "Speaker",
                        tint = accentOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a New Party",
                        color = accentOrange,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Join a Party Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.cardBackground)
                    .clickable { checkPermissionsAndProceed("join") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = accentOrange,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = lavenderColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDiscovering) "Searching for Parties..." else "Join a Party",
                        color = lavenderColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isDiscovering) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Looking for nearby hosts",
                            color = AppColors.textBody,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Available Parties Section (only visible when discovering)
            AnimatedVisibility(
                visible = isDiscovering,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Parties",
                            color = lavenderColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Stop",
                            color = Color.Red.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isDiscovering = false
                                    partyViewModel.stopDiscovery()
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(discoveredParties) { party ->
                            val partyName = party.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.cardBackground)
                                    .clickable {
                                        Log.d(TAG, "🎉 Joining party: $partyName (id=${party.endpointId})")
                                        partyViewModel.joinParty(party.endpointId, "Guest")
                                        isDiscovering = false
                                        partyViewModel.stopDiscovery()
                                        onPreviousPartyClick(partyName)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = lavenderColor,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partyName,
                                        color = AppColors.textTitle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to join",
                                        color = AppColors.textBody,
                                        fontSize = 14.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        Log.d(TAG, "🎉 Joining party: $partyName (id=${party.endpointId})")
                                        partyViewModel.joinParty(party.endpointId, "Guest")
                                        isDiscovering = false
                                        partyViewModel.stopDiscovery()
                                        onPreviousPartyClick(partyName)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                                ) {
                                    Text("Join", color = Color.White)
                                }
                            }
                        }

                        if (discoveredParties.isEmpty()) {
                            item {
                                Text(
                                    "No parties found nearby yet. Make sure a host has started a party.",
                                    color = AppColors.textBody,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        item {
                            BottomPaddingSpacer()
                        }
                    }
                }
            }

            // If not discovering, show empty state
            if (!isDiscovering) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Start a party to host, or join an existing one nearby.",
                    color = AppColors.textBody,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                BottomPaddingSpacer()
            }
        }
    }
}