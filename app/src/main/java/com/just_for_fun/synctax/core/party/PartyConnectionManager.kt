package com.just_for_fun.synctax.core.party

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.just_for_fun.synctax.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

class PartyConnectionManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

        // Pre-check: Wi-Fi must be enabled
        if (!wifiManager.isWifiEnabled) {
            val msg = "Wi-Fi is turned off. Please enable Wi-Fi to host a party."
            Log.e(TAG, msg)
            _hotspotError.value = msg
            return
        }

        // Pre-check: Location Services must be enabled (required for hotspot on most devices)
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!locationEnabled) {
            val msg = "Location Services are turned off. Please enable Location to host a party."
            Log.e(TAG, msg)
            _hotspotError.value = msg
            return
        }

        Log.d(TAG, "Pre-checks passed. Wi-Fi=ON, Location=ON. Calling startLocalOnlyHotspot...")

        val handler = Handler(Looper.getMainLooper())
        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    Log.d(TAG, "✅ onStarted() callback fired")
                    hotspotReservation = reservation
                    val ssid: String
                    val passphrase: String
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = reservation.softApConfiguration
                        ssid = config?.ssid ?: "PartyHotspot"
                        passphrase = config?.passphrase ?: ""
                        Log.d(TAG, "softApConfiguration: ssid=$ssid, passphrase length=${passphrase.length}")
                    } else {
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        ssid = config?.SSID?.trim('"') ?: "PartyHotspot"
                        passphrase = config?.preSharedKey?.trim('"') ?: ""
                        Log.d(TAG, "wifiConfiguration: ssid=$ssid, passphrase length=${passphrase.length}")
                    }
                    _hostHotspotInfo.value = PartyHotspotInfo(ssid, passphrase, PARTY_PORT)
                    _isHosting.value = true
                    _isConnected.value = true
                    _hotspotError.value = null
                    startServer()
                    Log.d(TAG, "✅ Hotspot started with SSID=$ssid, port=$PARTY_PORT")
                }

                override fun onStopped() {
                    Log.d(TAG, "⛔ Hotspot stopped callback")
                    if (_isHosting.value) {
                        stopHosting()
                    }
                }

                override fun onFailed(reason: Int) {
                    val reasonStr = when (reason) {
                        ERROR_NO_CHANNEL -> "ERROR_NO_CHANNEL"
                        ERROR_GENERIC -> "ERROR_GENERIC"
                        ERROR_INCOMPATIBLE_MODE -> "ERROR_INCOMPATIBLE_MODE"
                        ERROR_TETHERING_DISALLOWED -> "ERROR_TETHERING_DISALLOWED"
                        else -> "UNKNOWN($reason)"
                    }
                    Log.e(TAG, "❌ Hotspot onFailed: $reasonStr (code=$reason)")
                    _hotspotError.value = "Hotspot failed: $reasonStr. Make sure Wi-Fi and Location are ON, and no other hotspot is active."
                    _hostHotspotInfo.value = null
                    _isHosting.value = false
                    _isConnected.value = false
                }
            }, handler)
            Log.d(TAG, "startLocalOnlyHotspot() called — waiting for callback...")
        } catch (e: SecurityException) {
            val msg = "Permission denied: ${e.message}"
            Log.e(TAG, msg, e)
            _hotspotError.value = msg
        } catch (e: Exception) {
            val msg = "Failed to start hotspot: ${e.message}"
            Log.e(TAG, msg, e)
            _hotspotError.value = msg
        }
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
        hotspotReservation?.close()
        hotspotReservation = null

        _hostHotspotInfo.value = null
        _members.value = emptyList()
        _clientIssues.value = emptyMap()
    }

    fun startDiscovery() {
        Log.d(TAG, "Manual join flow started")
        _discoveredParties.value = emptyList()
    }

    fun stopDiscovery() {
        // Manual join flow uses text inputs, no active discovery
    }

    fun joinParty(ssid: String, passphrase: String, userName: String) {
        Log.d(TAG, "Joining hotspot ssid=$ssid as $userName")
        leaveParty()

        if (ssid.isBlank() || passphrase.length < 8) {
            Log.e(TAG, "Invalid hotspot credentials")
            return
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passphrase)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetwork = network
                connectivityManager.bindProcessToNetwork(network)
                connectToHost(network, userName)
            }

            override fun onUnavailable() {
                Log.e(TAG, "Hotspot connection unavailable")
                handleHostDisconnected()
            }

            override fun onLost(network: Network) {
                if (activeNetwork == network) {
                    Log.d(TAG, "Hotspot network lost")
                    handleHostDisconnected()
                }
            }
        }

        networkCallback = callback
        connectivityManager.requestNetwork(request, callback)
    }

    fun leaveParty() {
        Log.d(TAG, "Leaving party")
        hostConnection?.let { closeConnection(it) }
        hostConnection = null
        activeNetwork = null
        _isConnected.value = false
        _members.value = emptyList()
        _clientIssues.value = emptyMap()
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

    companion object {
        private const val TAG = "PartyConnectionManager"
        private const val PARTY_PORT = 45123
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val HOST_ENDPOINT_ID = "host"
    }
}