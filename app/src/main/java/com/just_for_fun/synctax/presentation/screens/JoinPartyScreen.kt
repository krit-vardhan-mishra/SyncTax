package com.just_for_fun.synctax.presentation.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel
import java.util.Locale

private const val TAG = "JoinPartyScreen"

private data class PartyHotspotCandidate(
    val ssid: String,
    val level: Int
)

private enum class JoinAction {
    Join,
    ScanQr,
    RefreshWifi
}

private fun getJoinPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinPartyScreen(
    partyViewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onJoinSuccess: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wifiManager = remember {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground
    val titleColor = AppColors.textTitle

    var candidates by remember { mutableStateOf<List<PartyHotspotCandidate>>(emptyList()) }
    var scanInProgress by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    var joinSsid by remember { mutableStateOf("") }
    var joinPassphrase by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    var showPermissionSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<JoinAction?>(null) }
    var pendingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var triggerAction by remember { mutableStateOf<JoinAction?>(null) }

    val permissionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val joinPermissions = remember { getJoinPermissions() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            triggerAction = pendingAction
            pendingAction = null
            pendingPermissions = emptyList()
        } else {
            showPermissionSheet = true
        }
    }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            joinError = "QR scan canceled."
        } else {
            val uri = runCatching { Uri.parse(contents) }.getOrNull()
            val ssid = uri?.getQueryParameter("ssid")
            val passphrase = uri?.getQueryParameter("pass")
            if (ssid.isNullOrBlank() || passphrase.isNullOrBlank()) {
                joinError = "Invalid QR code."
            } else {
                joinSsid = ssid
                joinPassphrase = passphrase
                triggerAction = JoinAction.Join
            }
        }
    }

    fun requestPermissionsFor(action: JoinAction, permissions: List<String>) {
        val missing = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            triggerAction = action
            return
        }
        pendingAction = action
        pendingPermissions = missing
        showPermissionSheet = true
    }

    fun refreshWifi() {
        scanInProgress = true
        scanError = null
        val success = wifiManager.startScan()
        if (!success) {
            scanInProgress = false
            scanError = "Wi-Fi scan failed. Try again."
        }
    }

    fun attemptJoin(ssid: String) {
        val passphrase = joinPassphrase.trim()
        if (ssid.isBlank() || passphrase.isBlank()) {
            joinError = "Enter the host SSID and passphrase."
            return
        }
        if (passphrase.length < 8) {
            joinError = "Passphrase must be at least 8 characters."
            return
        }
        joinError = null
        partyViewModel.joinParty(ssid.trim(), passphrase, "Guest")
        onJoinSuccess(ssid.trim())
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                scanInProgress = false
                val results = wifiManager.scanResults
                val filtered = results
                    .map { result -> PartyHotspotCandidate(result.SSID, result.level) }
                    .filter { it.ssid.isNotBlank() }
                    .distinctBy { it.ssid.lowercase(Locale.getDefault()) }
                    .sortedByDescending { it.level }
                candidates = filtered
            }
        }
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(triggerAction) {
        when (triggerAction) {
            JoinAction.Join -> attemptJoin(joinSsid)
            JoinAction.ScanQr -> {
                val options = ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Scan the host QR code")
                    .setBeepEnabled(true)
                    .setOrientationLocked(false)
                qrScanLauncher.launch(options)
            }
            JoinAction.RefreshWifi -> refreshWifi()
            null -> {}
        }
        triggerAction = null
    }

    LaunchedEffect(Unit) {
        val missing = joinPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            refreshWifi()
        }
    }

    if (showPermissionSheet) {
        val permissionsToShow = if (pendingPermissions.isNotEmpty()) {
            pendingPermissions
        } else {
            joinPermissions
        }
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
                    text = "SyncTax needs these permissions to find and join parties.",
                    color = AppColors.textBody,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                permissionsToShow.forEach { permission ->
                    val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                            color = AppColors.textBody,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showPermissionSheet = false
                        permissionLauncher.launch(permissionsToShow.toTypedArray())
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
                title = { Text("Available Nearby Parties", color = titleColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = titleColor)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        requestPermissionsFor(JoinAction.RefreshWifi, joinPermissions)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = titleColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            if (scanInProgress) {
                item {
                    Text(
                        text = "Scanning for nearby hotspots...",
                        color = AppColors.textBody,
                        fontSize = 13.sp
                    )
                }
            }

            if (scanError != null) {
                item {
                    Text(
                        text = scanError ?: "",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }

            items(candidates) { party ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.cardBackground
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = party.ssid,
                                color = AppColors.textTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Local hotspot detected",
                                color = AppColors.textBody,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                joinSsid = party.ssid
                                requestPermissionsFor(JoinAction.Join, joinPermissions)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                        ) {
                            Text("JOIN PARTY", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (candidates.isEmpty() && !scanInProgress) {
                item {
                    Text(
                        text = "No parties found yet. Ask the host for SSID or scan the QR code below.",
                        color = AppColors.textBody,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = AppColors.cardBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Sync via QR Code",
                            color = AppColors.textTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                requestPermissionsFor(JoinAction.ScanQr, listOf(Manifest.permission.CAMERA))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                        ) {
                            Text("Scan QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Enter Host Details Manually",
                            color = AppColors.textTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = joinSsid,
                            onValueChange = { joinSsid = it },
                            label = { Text("SSID", color = AppColors.textBody) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentOrange,
                                unfocusedBorderColor = AppColors.textBody,
                                focusedTextColor = AppColors.textTitle,
                                unfocusedTextColor = AppColors.textTitle,
                                cursorColor = accentOrange
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = joinPassphrase,
                            onValueChange = { joinPassphrase = it },
                            label = { Text("Passphrase", color = AppColors.textBody) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentOrange,
                                unfocusedBorderColor = AppColors.textBody,
                                focusedTextColor = AppColors.textTitle,
                                unfocusedTextColor = AppColors.textTitle,
                                cursorColor = accentOrange
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        if (joinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = joinError ?: "",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                requestPermissionsFor(JoinAction.Join, joinPermissions)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.textTitle)
                        ) {
                            Text("Join with Details", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                BottomPaddingSpacer()
            }
        }
    }
}
