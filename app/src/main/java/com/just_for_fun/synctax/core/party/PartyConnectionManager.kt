package com.just_for_fun.synctax.core.party

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.just_for_fun.synctax.core.party.DiscoveredParty
import kotlinx.coroutines.launch

data class PartyMember(val endpointId: String, val name: String, val isHost: Boolean)

/**
 * Holds a discovered party endpoint with its real endpointId AND display name.
 * DiscoveredEndpointInfo from Nearby only carries the name — we must carry
 * the endpointId ourselves so the UI can pass it to requestConnection().
 */
data class DiscoveredParty(val endpointId: String, val name: String)

class PartyConnectionManager(private val context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.just_for_fun.synctax.PARTY_MODE"
    private val strategy = Strategy.P2P_STAR

    private val _isHosting = MutableStateFlow(false)
    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _members = MutableStateFlow<List<PartyMember>>(emptyList())
    val members: StateFlow<List<PartyMember>> = _members.asStateFlow()

    private val _discoveredParties = MutableStateFlow<List<DiscoveredParty>>(emptyList())
    val discoveredParties: StateFlow<List<DiscoveredParty>> = _discoveredParties.asStateFlow()

    // Client issue state — tracks which clients have reported media issues
    private val _clientIssues = MutableStateFlow<Map<String, String>>(emptyMap()) // endpointId -> issueType
    val clientIssues: StateFlow<Map<String, String>> = _clientIssues.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hostEndpointId: String? = null

    // Callbacks for receiving messages
    var onMessageReceived: ((PartyMessage) -> Unit)? = null

    // Callback for when connection is lost (used by ViewModel to handle UI)
    var onHostDisconnected: (() -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null // endpointId

    // Callback for media requests from clients (shown to host)
    var onMediaRequestReceived: ((endpointId: String, PartyMessage.MediaRequest) -> Unit)? = null

    // Callback for media issue notifications from clients
    var onClientIssueReceived: ((endpointId: String, PartyMessage.MediaIssue) -> Unit)? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    try {
                        val message = PartyMessage.fromByteArray(bytes)
                        Log.d(TAG, "📩 Received message from $endpointId: ${message::class.simpleName}")
                        
                        when (message) {
                            is PartyMessage.MediaRequest -> {
                                Log.d(TAG, "📥 MediaRequest from $endpointId: type=${message.requestType}")
                                onMediaRequestReceived?.invoke(endpointId, message)
                            }
                            is PartyMessage.MediaIssue -> {
                                Log.d(TAG, "⚠️ MediaIssue from ${message.userName}: ${message.issueType}")
                                // Update client issues map
                                val updated = _clientIssues.value.toMutableMap()
                                updated[endpointId] = "${message.userName}: ${message.issueType}"
                                _clientIssues.value = updated
                                onClientIssueReceived?.invoke(endpointId, message)
                            }
                            is PartyMessage.EndParty -> {
                                Log.d(TAG, "🛑 EndParty received: ${message.reason}")
                                // Host ended the party
                                leaveParty()
                                onHostDisconnected?.invoke()
                            }
                            else -> {
                                onMessageReceived?.invoke(message)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing message from $endpointId", e)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Track file/stream transfer progress here in future
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val endpointName = connectionInfo.endpointName
            if (_isHosting.value) {
                Log.d(TAG, "🎉 HOST: Join request from $endpointName (endpoint=$endpointId)")
            } else {
                Log.d(TAG, "🤝 CLIENT: Connection initiated with host $endpointName (endpoint=$endpointId)")
            }
            // Automatically accept connection for now
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            if (_isHosting.value) {
                Log.d(TAG, "✅ HOST: Accepted join request from $endpointName (endpoint=$endpointId)")
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "✅ Connected successfully to $endpointId")
                if (!_isHosting.value) {
                    _isConnected.value = true
                    hostEndpointId = endpointId
                }
                val currentMembers = _members.value.toMutableList()
                currentMembers.add(PartyMember(endpointId, "Guest $endpointId", false))
                _members.value = currentMembers
                Log.d(TAG, "👥 Members count: ${_members.value.size}")
            } else {
                Log.e(TAG, "❌ Connection failed to $endpointId — status: ${result.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "⚡ Disconnected from $endpointId")

            // Remove from members
            val currentMembers = _members.value.toMutableList()
            currentMembers.removeAll { it.endpointId == endpointId }
            _members.value = currentMembers

            // Clear any issues from this endpoint
            val updatedIssues = _clientIssues.value.toMutableMap()
            updatedIssues.remove(endpointId)
            _clientIssues.value = updatedIssues

            if (_isHosting.value) {
                // A client disconnected from the host
                Log.d(TAG, "👤 Client $endpointId left the party. Remaining: ${_members.value.size}")
                onClientDisconnected?.invoke(endpointId)
            } else if (endpointId == hostEndpointId) {
                // The HOST disconnected — the party is over for this client
                Log.d(TAG, "🛑 Host disconnected! Party is over.")
                _isConnected.value = false
                hostEndpointId = null
                onHostDisconnected?.invoke()
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "🔍 Found party: ${info.endpointName} (endpoint=$endpointId)")
            val current = _discoveredParties.value.toMutableList()
            // Deduplicate by endpointId (the real unique key, not the name)
            if (current.none { it.endpointId == endpointId }) {
                current.add(DiscoveredParty(endpointId = endpointId, name = info.endpointName))
                _discoveredParties.value = current
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "❌ Lost party endpoint: $endpointId")
            // Remove the lost party from the list so the UI stays accurate
            _discoveredParties.value = _discoveredParties.value.filter { it.endpointId != endpointId }
        }
    }

    fun startHosting(partyName: String) {
        Log.d(TAG, "=========================================")
        Log.d(TAG, "🎤 HOST ACTION: Starting to host party: $partyName")
        Log.d(TAG, "=========================================")
        
        // Stop any ongoing discovery or advertising before starting
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            partyName,
            serviceId,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "✅ Started advertising party: $partyName")
            _isHosting.value = true
            _isConnected.value = true
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to start advertising", e)
        }
    }

    fun stopHosting() {
        Log.d(TAG, "🛑 Stopping hosting. Broadcasting EndParty to all members.")
        // Notify all clients that the party is ending
        try {
            sendMessageToAll(PartyMessage.EndParty("Host ended the party"))
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Could not broadcast EndParty", e)
        }
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        _isHosting.value = false
        _isConnected.value = false
        _members.value = emptyList()
        _clientIssues.value = emptyMap()
    }

    fun startDiscovery() {
        Log.d(TAG, "=========================================")
        Log.d(TAG, "🔍 CLIENT ACTION: Starting discovery...")
        Log.d(TAG, "=========================================")
        
        // Stop any ongoing discovery to prevent STATUS_ALREADY_DISCOVERING
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        _discoveredParties.value = emptyList()
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "✅ Started discovery")
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        Log.d(TAG, "🔍 Stopping discovery")
        connectionsClient.stopDiscovery()
    }

    fun joinParty(endpointId: String, userName: String) {
        Log.d(TAG, "🎉 Requesting connection to party: $endpointId as $userName")
        connectionsClient.requestConnection(
            userName,
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d(TAG, "✅ Requested connection to $endpointId")
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to request connection", e)
        }
    }

    fun leaveParty() {
        Log.d(TAG, "👋 Leaving party")
        connectionsClient.stopAllEndpoints()
        _isConnected.value = false
        _members.value = emptyList()
        hostEndpointId = null
        _clientIssues.value = emptyMap()
    }

    fun sendMessageToAll(message: PartyMessage) {
        val bytes = message.toByteArray()
        val payload = Payload.fromBytes(bytes)
        val endpointIds = _members.value.map { it.endpointId }
        if (endpointIds.isNotEmpty()) {
            Log.d(TAG, "📤 Sending ${message::class.simpleName} to ${endpointIds.size} members")
            connectionsClient.sendPayload(endpointIds, payload)
        }
    }

    fun sendMessageToHost(message: PartyMessage) {
        hostEndpointId?.let { hostId ->
            val bytes = message.toByteArray()
            val payload = Payload.fromBytes(bytes)
            Log.d(TAG, "📤 Sending ${message::class.simpleName} to host ($hostId)")
            connectionsClient.sendPayload(hostId, payload)
        }
    }

    fun sendMessageToUser(endpointId: String, message: PartyMessage) {
        val bytes = message.toByteArray()
        val payload = Payload.fromBytes(bytes)
        Log.d(TAG, "📤 Sending ${message::class.simpleName} to $endpointId")
        connectionsClient.sendPayload(endpointId, payload)
    }

    /**
     * Clears a specific client issue (called when the issue is resolved).
     */
    fun clearClientIssue(endpointId: String) {
        val updated = _clientIssues.value.toMutableMap()
        updated.remove(endpointId)
        _clientIssues.value = updated
    }

    companion object {
        private const val TAG = "PartyConnectionManager"
    }
}