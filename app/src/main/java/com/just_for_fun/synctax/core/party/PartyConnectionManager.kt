package com.just_for_fun.synctax.core.party

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.just_for_fun.synctax.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID

data class PartyMember(val endpointId: String, val name: String, val isHost: Boolean)

/**
 * Holds a discovered party endpoint with its SSID AND display name.
 * Hotspot discovery is manual in this implementation.
 */
data class DiscoveredParty(val endpointId: String, val name: String)

data class PartyHotspotInfo(val ssid: String, val passphrase: String, val port: Int)

@SuppressLint("MissingPermission")
class PartyConnectionManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isHosting = MutableStateFlow(false)
    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _members = MutableStateFlow<List<PartyMember>>(emptyList())
    val members: StateFlow<List<PartyMember>> = _members.asStateFlow()

    private val _discoveredParties = MutableStateFlow<List<DiscoveredParty>>(emptyList())
    val discoveredParties: StateFlow<List<DiscoveredParty>> = _discoveredParties.asStateFlow()

    private val _clientIssues = MutableStateFlow<Map<String, String>>(emptyMap())
    val clientIssues: StateFlow<Map<String, String>> = _clientIssues.asStateFlow()

    private val _hostHotspotInfo = MutableStateFlow<PartyHotspotInfo?>(null)
    val hostHotspotInfo: StateFlow<PartyHotspotInfo?> = _hostHotspotInfo.asStateFlow()

    private val _hotspotError = MutableStateFlow<String?>(null)
    val hotspotError: StateFlow<String?> = _hotspotError.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private var serverSocket: ServerSocket? = null
    private val clients = mutableMapOf<String, ClientConnection>()
    private var hostConnection: ClientConnection? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var activeNetwork: Network? = null
    internal var hostDisplayName: String = "Host"

    // ── BLE ──────────────────────────────────────────────────────────
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiseCallback: AdvertiseCallback? = null
    private var bleScanCallback: ScanCallback? = null

    // ── Wi-Fi P2P ───────────────────────────────────────────────────
    private var wifiP2pManager: WifiP2pManager? = null
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var p2pReceiver: BroadcastReceiver? = null

    private data class ClientConnection(
        val endpointId: String,
        val socket: Socket,
        val writer: BufferedWriter
    )

    // Callbacks for receiving messages
    var onMessageReceived: ((PartyMessage) -> Unit)? = null

    // Callback for when connection is lost (used by ViewModel to handle UI)
    var onHostDisconnected: (() -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null // endpointId

    // Callback for media requests from clients (shown to host)
    var onMediaRequestReceived: ((endpointId: String, PartyMessage.MediaRequest) -> Unit)? = null

    // Callback for media issue notifications from clients
    var onClientIssueReceived: ((endpointId: String, PartyMessage.MediaIssue) -> Unit)? = null

    // Callback for host transfer requests from clients
    var onHostTransferRequested: ((endpointId: String, userName: String) -> Unit)? = null

    fun clearHotspotError() {
        _hotspotError.value = null
    }

    fun startHosting(partyName: String) {
        Log.d(TAG, "▶▶▶ startHosting() called for party: $partyName")
        _hotspotError.value = null
        stopDiscovery()
        if (_isHosting.value) {
            Log.d(TAG, "Already hosting — stopping first")
            stopHosting()
        }
        hostDisplayName = partyName

        // Step 1: Create Wi-Fi P2P Group (acts as a soft AP for data)
        initWifiP2p()
        wifiP2pManager?.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Wi-Fi P2P Group created")
                // onSuccess fires before group info is ready; we get the actual
                // group details from the WIFI_P2P_CONNECTION_CHANGED_ACTION broadcast.
            }
            override fun onFailure(reason: Int) {
                val msg = "Wi-Fi Direct group failed (reason=$reason). Make sure Wi-Fi and Location are ON."
                Log.e(TAG, "❌ $msg")
                _hotspotError.value = msg
            }
        })

        // Step 2: Start BLE Advertising so guests can discover us
        startBleAdvertising(partyName)
    }

    fun stopHosting() {
        Log.d(TAG, "Stopping hosting. Broadcasting EndParty to all members.")
        try {
            sendMessageToAll(PartyMessage.EndParty("Host ended the party"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast EndParty", e)
        }

        _isHosting.value = false
        _isConnected.value = false
        stopServer()
        cleanupP2p()
        hotspotReservation?.close()
        hotspotReservation = null

        _hostHotspotInfo.value = null
        _members.value = emptyList()
        _clientIssues.value = emptyMap()
    }

    fun startDiscovery() {
        Log.d(TAG, "🔍 Starting BLE discovery for parties")
        _discoveredParties.value = emptyList()
        startBleScanning()
    }

    fun stopDiscovery() {
        stopBleScanning()
    }

    /**
     * Joins via Wi-Fi Direct. The 'ssid' param is the P2P device address
     * discovered via BLE, and 'passphrase' is unused for P2P but kept
     * for backward-compat with the manual-entry UI.
     */
    fun joinParty(ssid: String, passphrase: String, userName: String) {
        Log.d(TAG, "🔌 joinParty via Wi-Fi Direct: deviceAddr=$ssid as $userName")
        leaveParty()
        initWifiP2p()

        val config = WifiP2pConfig().apply {
            deviceAddress = ssid
        }

        wifiP2pManager?.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Wi-Fi P2P connect initiated to $ssid")
                // Connection info arrives via broadcast; we handle it in the receiver.
                // Store userName so we can send handshake when TCP connects.
                pendingJoinUserName = userName
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Wi-Fi P2P connect failed (reason=$reason)")
                handleHostDisconnected()
            }
        })
    }

    private var pendingJoinUserName: String? = null

    fun leaveParty() {
        Log.d(TAG, "Leaving party")
        hostConnection?.let { closeConnection(it) }
        hostConnection = null
        activeNetwork = null
        _isConnected.value = false
        _members.value = emptyList()
        _clientIssues.value = emptyMap()
        cleanupP2p()
        connectivityManager.bindProcessToNetwork(null)
        networkCallback?.let { callback ->
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
    }

    fun sendMessageToAll(message: PartyMessage) {
        val snapshot = clients.values.toList()
        if (snapshot.isNotEmpty()) {
            snapshot.forEach { connection ->
                sendMessage(connection, message)
            }
        }
    }

    fun sendMessageToHost(message: PartyMessage) {
        val connection = hostConnection
        if (connection == null) {
            Log.d(TAG, "Host connection not available")
            return
        }
        sendMessage(connection, message)
    }

    fun sendMessageToUser(endpointId: String, message: PartyMessage) {
        val connection = clients[endpointId] ?: return
        sendMessage(connection, message)
    }

    /**
     * Clears a specific client issue (called when the issue is resolved).
     */
    fun clearClientIssue(endpointId: String) {
        val updated = _clientIssues.value.toMutableMap()
        updated.remove(endpointId)
        _clientIssues.value = updated
    }

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PARTY_PORT)
                Log.d(TAG, "🖥️ Server started on port $PARTY_PORT")
                while (_isHosting.value) {
                    val socket = serverSocket?.accept() ?: break
                    val endpointId = UUID.randomUUID().toString()
                    Log.d(TAG, "🤝 New client connected: $endpointId from ${socket.inetAddress?.hostAddress}")
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                    val connection = ClientConnection(endpointId, socket, writer)
                    clients[endpointId] = connection
                    addMember(PartyMember(endpointId, "Guest", false))
                    sendMessage(connection, PartyMessage.Handshake(BuildConfig.VERSION_NAME, hostDisplayName))
                    listenToClient(connection)
                }
            } catch (e: Exception) {
                if (_isHosting.value) {
                    Log.e(TAG, "Server socket error", e)
                }
            }
        }
    }

    private fun stopServer() {
        clients.values.forEach { closeConnection(it) }
        clients.clear()
        serverSocket?.close()
        serverSocket = null
    }

    private fun listenToClient(connection: ClientConnection) {
        scope.launch {
            val reader = BufferedReader(InputStreamReader(connection.socket.getInputStream(), StandardCharsets.UTF_8))
            try {
                Log.d(TAG, "👂 Listening to client: ${connection.endpointId}")
                while (true) {
                    val line = reader.readLine() ?: break
                    Log.d(TAG, "📨 [HOST←CLIENT:${connection.endpointId}] raw=$line")
                    val message = PartyMessage.fromByteArray(line.toByteArray(StandardCharsets.UTF_8))
                    Log.d(TAG, "📨 [HOST←CLIENT:${connection.endpointId}] parsed=${message::class.simpleName}")
                    handleIncomingMessage(connection.endpointId, message)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client connection closed: ${connection.endpointId}")
            } finally {
                handleClientDisconnected(connection.endpointId)
            }
        }
    }

    private fun connectToHost(network: Network, userName: String) {
        scope.launch {
            val gateway = resolveGateway(connectivityManager.getLinkProperties(network))
            if (gateway == null) {
                Log.e(TAG, "❌ Gateway not available for hotspot")
                handleHostDisconnected()
                return@launch
            }
            Log.d(TAG, "🔌 Connecting to host at $gateway:$PARTY_PORT as '$userName'")
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(gateway, PARTY_PORT), CONNECT_TIMEOUT_MS)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                val connection = ClientConnection(HOST_ENDPOINT_ID, socket, writer)
                hostConnection = connection
                _isConnected.value = true
                _members.value = listOf(PartyMember(HOST_ENDPOINT_ID, "Host", true))
                Log.d(TAG, "✅ Connected to host! Sending handshake as '$userName'")
                sendMessage(connection, PartyMessage.Handshake(BuildConfig.VERSION_NAME, userName))
                listenToHost(connection)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to connect to host", e)
                handleHostDisconnected()
            }
        }
    }

    private fun listenToHost(connection: ClientConnection) {
        scope.launch {
            val reader = BufferedReader(InputStreamReader(connection.socket.getInputStream(), StandardCharsets.UTF_8))
            try {
                Log.d(TAG, "👂 Listening to host...")
                while (true) {
                    val line = reader.readLine() ?: break
                    Log.d(TAG, "📨 [CLIENT←HOST] raw=$line")
                    val message = PartyMessage.fromByteArray(line.toByteArray(StandardCharsets.UTF_8))
                    Log.d(TAG, "📨 [CLIENT←HOST] parsed=${message::class.simpleName}")
                    handleIncomingMessage(connection.endpointId, message)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Host connection closed")
            } finally {
                handleHostDisconnected()
            }
        }
    }

    private fun resolveGateway(linkProperties: LinkProperties?): InetAddress? {
        return linkProperties?.routes?.firstOrNull { it.hasGateway() }?.gateway
    }

    private fun handleIncomingMessage(endpointId: String, message: PartyMessage) {
        Log.d(TAG, "📬 handleIncomingMessage from=$endpointId type=${message::class.simpleName}")
        when (message) {
            is PartyMessage.Handshake -> {
                Log.d(TAG, "🤝 Handshake from $endpointId: user='${message.userName}', version='${message.appVersion}'")
                updateMemberName(endpointId, message.userName)
                onMessageReceived?.invoke(message)
            }
            is PartyMessage.PlayCommand -> {
                Log.d(TAG, "▶️ PlayCommand received: songId='${message.songId}', title='${message.title}', artist='${message.artist}'")
                onMessageReceived?.invoke(message)
            }
            is PartyMessage.PauseCommand -> {
                Log.d(TAG, "⏸️ PauseCommand received: timestamp=${message.timestamp}")
                onMessageReceived?.invoke(message)
            }
            is PartyMessage.SeekCommand -> {
                Log.d(TAG, "⏩ SeekCommand received: position=${message.position}")
                onMessageReceived?.invoke(message)
            }
            is PartyMessage.NowPlaying -> {
                Log.d(TAG, "🎵 NowPlaying received: songId='${message.songId}', title='${message.title}', artist='${message.artist}'")
                onMessageReceived?.invoke(message)
            }
            is PartyMessage.MediaRequest -> {
                Log.d(TAG, "📥 MediaRequest from $endpointId: type='${message.requestType}', songId='${message.songId}'")
                onMediaRequestReceived?.invoke(endpointId, message)
            }
            is PartyMessage.MediaIssue -> {
                Log.d(TAG, "⚠️ MediaIssue from $endpointId: user='${message.userName}', issue='${message.issueType}'")
                val updated = _clientIssues.value.toMutableMap()
                updated[endpointId] = "${message.userName}: ${message.issueType}"
                _clientIssues.value = updated
                onClientIssueReceived?.invoke(endpointId, message)
            }
            is PartyMessage.EndParty -> {
                Log.d(TAG, "🛑 EndParty received: reason='${message.reason}'")
                leaveParty()
                onHostDisconnected?.invoke()
            }
            is PartyMessage.HostTransferRequest -> {
                Log.d(TAG, "👑 HostTransferRequest from $endpointId: user='${message.userName}'")
                onHostTransferRequested?.invoke(endpointId, message.userName)
            }
            else -> {
                Log.d(TAG, "📩 Other message from $endpointId: ${message::class.simpleName}")
                onMessageReceived?.invoke(message)
            }
        }
    }

    private fun addMember(member: PartyMember) {
        val current = _members.value.toMutableList()
        current.add(member)
        _members.value = current
    }

    private fun updateMemberName(endpointId: String, name: String) {
        val updated = _members.value.map { member ->
            if (member.endpointId == endpointId) member.copy(name = name) else member
        }
        _members.value = updated
    }

    private fun handleClientDisconnected(endpointId: String) {
        val currentMembers = _members.value.toMutableList()
        currentMembers.removeAll { it.endpointId == endpointId }
        _members.value = currentMembers

        val updatedIssues = _clientIssues.value.toMutableMap()
        updatedIssues.remove(endpointId)
        _clientIssues.value = updatedIssues

        clients.remove(endpointId)?.let { closeConnection(it) }
        if (_isHosting.value) {
            onClientDisconnected?.invoke(endpointId)
        }
    }

    private fun handleHostDisconnected() {
        if (!_isConnected.value) return
        leaveParty()
        onHostDisconnected?.invoke()
    }

    private fun closeConnection(connection: ClientConnection) {
        runCatching { connection.writer.close() }
        runCatching { connection.socket.close() }
    }

    private fun sendMessage(connection: ClientConnection, message: PartyMessage) {
        ioScope.launch {
            val json = String(message.toByteArray(), StandardCharsets.UTF_8)
            Log.d(TAG, "📤 sendMessage to=${connection.endpointId} type=${message::class.simpleName}")
            try {
                synchronized(connection) {
                    connection.writer.write(json)
                    connection.writer.newLine()
                    connection.writer.flush()
                }
                Log.d(TAG, "📤 sendMessage SUCCESS to=${connection.endpointId}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send message to=${connection.endpointId}", e)
                if (_isHosting.value) {
                    handleClientDisconnected(connection.endpointId)
                } else {
                    handleHostDisconnected()
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // BLE Advertising (Host)
    // ════════════════════════════════════════════════════════════════════

    private fun startBleAdvertising(partyName: String) {
        try {
            bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            if (bleAdvertiser == null) {
                Log.w(TAG, "⚠️ BLE Advertiser not available on this device")
                return
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()

            val serviceData = partyName.toByteArray(StandardCharsets.UTF_8).take(20).toByteArray()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(SYNCTAX_BLE_UUID))
                .addServiceData(ParcelUuid(SYNCTAX_BLE_UUID), serviceData)
                .build()

            bleAdvertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.d(TAG, "📡 BLE Advertising started for '$partyName'")
                }
                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG, "❌ BLE Advertising failed: errorCode=$errorCode")
                }
            }

            bleAdvertiser?.startAdvertising(settings, data, bleAdvertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE Advertise permission denied", e)
        }
    }

    private fun stopBleAdvertising() {
        try {
            bleAdvertiseCallback?.let { bleAdvertiser?.stopAdvertising(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE advertising", e)
        }
        bleAdvertiseCallback = null
    }

    // ════════════════════════════════════════════════════════════════════
    // BLE Scanning (Guest)
    // ════════════════════════════════════════════════════════════════════

    private fun startBleScanning() {
        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bleScanner == null) {
                Log.w(TAG, "⚠️ BLE Scanner not available")
                return
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SYNCTAX_BLE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bleScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val record = result.scanRecord ?: return
                    val serviceData = record.getServiceData(ParcelUuid(SYNCTAX_BLE_UUID))
                    val partyName = serviceData?.toString(StandardCharsets.UTF_8) ?: "Unknown Party"
                    val deviceAddr = result.device?.address ?: return

                    val current = _discoveredParties.value
                    if (current.none { it.endpointId == deviceAddr }) {
                        _discoveredParties.value = current + DiscoveredParty(deviceAddr, partyName)
                        Log.d(TAG, "🔍 Discovered party: '$partyName' at $deviceAddr")
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "❌ BLE Scan failed: errorCode=$errorCode")
                }
            }

            bleScanner?.startScan(listOf(filter), settings, bleScanCallback)
            Log.d(TAG, "📡 BLE Scanning started")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE Scan permission denied", e)
        }
    }

    private fun stopBleScanning() {
        try {
            bleScanCallback?.let { bleScanner?.stopScan(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan", e)
        }
        bleScanCallback = null
    }

    // ════════════════════════════════════════════════════════════════════
    // Wi-Fi P2P Initialization & Broadcast Receiver
    // ════════════════════════════════════════════════════════════════════

    private fun initWifiP2p() {
        if (wifiP2pManager != null) return

        wifiP2pManager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        wifiP2pChannel = wifiP2pManager?.initialize(appContext, Looper.getMainLooper(), null)

        p2pReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val info = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                        if (info?.groupFormed == true) {
                            Log.d(TAG, "📶 P2P Group formed. isGroupOwner=${info.isGroupOwner}, ownerAddr=${info.groupOwnerAddress}")
                            if (info.isGroupOwner) {
                                // We are the Host — set hotspot info and start TCP server
                                val ownerIp = info.groupOwnerAddress?.hostAddress ?: "unknown"
                                _hostHotspotInfo.value = PartyHotspotInfo(ownerIp, "", PARTY_PORT)
                                _isHosting.value = true
                                _isConnected.value = true
                                _hotspotError.value = null
                                startServer()
                            } else {
                                // We are the Guest — connect TCP to the group owner
                                val ownerAddr = info.groupOwnerAddress
                                if (ownerAddr != null) {
                                    val userName = pendingJoinUserName ?: "Guest"
                                    connectToP2pHost(ownerAddr, userName)
                                }
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                            Log.w(TAG, "⚠️ Wi-Fi P2P is not enabled")
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        appContext.registerReceiver(p2pReceiver, intentFilter)
        Log.d(TAG, "📡 Wi-Fi P2P initialized")
    }

    /**
     * Guest connects TCP to the P2P group owner (Host).
     */
    private fun connectToP2pHost(ownerAddress: InetAddress, userName: String) {
        scope.launch {
            Log.d(TAG, "🔌 Connecting TCP to P2P host at ${ownerAddress.hostAddress}:$PARTY_PORT")
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ownerAddress, PARTY_PORT), CONNECT_TIMEOUT_MS)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                val connection = ClientConnection(HOST_ENDPOINT_ID, socket, writer)
                hostConnection = connection
                _isConnected.value = true
                _members.value = listOf(PartyMember(HOST_ENDPOINT_ID, "Host", true))
                Log.d(TAG, "✅ Connected to P2P host! Sending handshake as '$userName'")
                sendMessage(connection, PartyMessage.Handshake(BuildConfig.VERSION_NAME, userName))
                listenToHost(connection)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to connect TCP to P2P host", e)
                handleHostDisconnected()
            }
        }
    }

    private fun cleanupP2p() {
        stopBleAdvertising()
        stopBleScanning()
        try {
            p2pReceiver?.let { appContext.unregisterReceiver(it) }
        } catch (_: Exception) {}
        p2pReceiver = null
        try {
            wifiP2pManager?.removeGroup(wifiP2pChannel, null)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "PartyConnectionManager"
        private const val PARTY_PORT = 45123
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val HOST_ENDPOINT_ID = "host"
        private val SYNCTAX_BLE_UUID: UUID = UUID.fromString("0000FFAB-0000-1000-8000-00805F9B34FB")
    }
}