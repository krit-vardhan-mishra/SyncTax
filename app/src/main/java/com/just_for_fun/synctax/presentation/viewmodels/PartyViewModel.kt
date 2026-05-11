package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.party.PartyConnectionManager
import com.just_for_fun.synctax.core.party.PartyMessage
import com.just_for_fun.synctax.core.party.PartySyncController
import com.just_for_fun.synctax.core.service.PartyService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PartyViewModel"

/**
 * ViewModel that drives the entire Party Mode feature.
 *
 * Responsibilities:
 * - Manages hosting & discovery lifecycle.
 * - Forwards player commands (play/pause/seek) to guests.
 * - Enforces **host-only queue authority** — clients cannot skip or add songs.
 * - Sends metadata with every PlayCommand/NowPlaying so clients can show placeholders.
 * - Starts/stops the PartyService (foreground service) to survive Doze mode.
 * - Pauses non-essential background threads (ML training, etc.) during a party.
 * - Reports MediaIssue to host when a client is missing a song.
 */
class PartyViewModel(application: Application) : AndroidViewModel(application) {
    val connectionManager = PartyConnectionManager(application)
    val syncController = PartySyncController(connectionManager, viewModelScope)

    private val _isHosting = connectionManager.isHosting
    val isHosting = _isHosting

    private val _isConnected = connectionManager.isConnected
    val isConnected = _isConnected

    private val _members = connectionManager.members
    val members = _members

    private val _discoveredParties = connectionManager.discoveredParties
    val discoveredParties = _discoveredParties

    // Client issue state — shown on host UI
    val clientIssues = connectionManager.clientIssues
    
    // Use this flow to trigger player actions in the PlayerViewModel
    private val _playerCommands = MutableStateFlow<PartyMessage?>(null)
    val playerCommands: StateFlow<PartyMessage?> = _playerCommands.asStateFlow()

    // Events for the UI (snackbars, navigation)
    private val _uiEvents = MutableSharedFlow<PartyUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // Tracks whether a client is currently showing a placeholder (no local song)
    private val _isShowingPlaceholder = MutableStateFlow(false)
    val isShowingPlaceholder: StateFlow<Boolean> = _isShowingPlaceholder.asStateFlow()

    // Callback to pause/resume ML and background tasks
    var onPartyModeChanged: ((active: Boolean) -> Unit)? = null

    init {
        // ── Message routing ──────────────────────────────────────────────
        connectionManager.onMessageReceived = { message ->
            Log.d(TAG, "📩 Message received: ${message::class.simpleName}")
            when (message) {
                is PartyMessage.PlayCommand,
                is PartyMessage.PauseCommand,
                is PartyMessage.SeekCommand,
                is PartyMessage.NowPlaying -> {
                    _playerCommands.value = message
                    // Reset to null so same command can be triggered again later
                    viewModelScope.launch { _playerCommands.value = null }
                }
                is PartyMessage.Handshake -> {
                    Log.d(TAG, "🤝 Handshake from ${message.userName}, version=${message.appVersion}")
                    // Version check could go here
                }
                else -> {} // Handled by syncController or connectionManager
            }
        }

        // ── Host disconnect → party over for client ──────────────────────
        connectionManager.onHostDisconnected = {
            Log.d(TAG, "🛑 Host disconnected — ending party on client side")
            viewModelScope.launch {
                stopForegroundService()
                onPartyModeChanged?.invoke(false)
                _uiEvents.emit(PartyUiEvent.HostDisconnected)
            }
        }

        // ── Client disconnect → host notified ────────────────────────────
        connectionManager.onClientDisconnected = { endpointId ->
            Log.d(TAG, "👤 Client $endpointId disconnected")
            viewModelScope.launch {
                _uiEvents.emit(PartyUiEvent.ClientDisconnected(endpointId))
            }
        }

        // ── Media request from client (shown as prompt on host) ──────────
        connectionManager.onMediaRequestReceived = { endpointId, request ->
            Log.d(TAG, "📥 MediaRequest from $endpointId: ${request.requestType} for ${request.songId}")
            viewModelScope.launch {
                _uiEvents.emit(PartyUiEvent.MediaRequestReceived(endpointId, request))
            }
        }

        // ── Client issue notification ────────────────────────────────────
        connectionManager.onClientIssueReceived = { endpointId, issue ->
            Log.d(TAG, "⚠️ Client issue from ${issue.userName}: ${issue.issueType}")
            viewModelScope.launch {
                _uiEvents.emit(PartyUiEvent.ClientIssueReported(endpointId, issue.userName, issue.issueType))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Hosting
    // ════════════════════════════════════════════════════════════════════

    fun startHosting(partyName: String) {
        Log.d(TAG, "🎤 Starting party: $partyName")
        connectionManager.startHosting(partyName)
        startForegroundService(partyName, isHost = true)
        onPartyModeChanged?.invoke(true) // Pause ML/background tasks
    }

    fun stopHosting() {
        Log.d(TAG, "🛑 Stopping hosting")
        connectionManager.stopHosting()
        stopForegroundService()
        onPartyModeChanged?.invoke(false) // Resume ML/background tasks
    }

    // ════════════════════════════════════════════════════════════════════
    // Discovery & Joining
    // ════════════════════════════════════════════════════════════════════

    fun startDiscovery() {
        connectionManager.startDiscovery()
    }

    fun stopDiscovery() {
        connectionManager.stopDiscovery()
    }

    fun joinParty(endpointId: String, userName: String) {
        Log.d(TAG, "🎉 Joining party $endpointId as $userName")
        connectionManager.joinParty(endpointId, userName)
        // Start continuous sync when joining
        syncController.startSyncing()
        startForegroundService("Party", isHost = false)
        onPartyModeChanged?.invoke(true) // Pause ML/background tasks
    }

    fun leaveParty() {
        Log.d(TAG, "👋 Leaving party")
        connectionManager.leaveParty()
        stopForegroundService()
        _isShowingPlaceholder.value = false
        onPartyModeChanged?.invoke(false) // Resume ML/background tasks
    }

    // ════════════════════════════════════════════════════════════════════
    // Host-Only Commands (Queue Authority)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Sends a PlayCommand with full metadata so clients can show placeholders
     * if they don't have the song locally.
     */
    fun sendPlayCommand(
        songId: String,
        startTimestamp: Long,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        thumbnailUrl: String? = null,
        isOffline: Boolean = true
    ) {
        if (_isHosting.value) {
            val cmd = PartyMessage.PlayCommand(
                songId = songId,
                startTimestamp = startTimestamp,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbnailUrl,
                isOffline = isOffline
            )
            Log.d(TAG, "📤 Sending PlayCommand: $title by $artist (songId=$songId)")
            connectionManager.sendMessageToAll(cmd)
        } else {
            Log.d(TAG, "⚠️ sendPlayCommand ignored — not the host")
        }
    }

    fun sendPauseCommand(timestamp: Long) {
        if (_isHosting.value) {
            Log.d(TAG, "📤 Sending PauseCommand")
            connectionManager.sendMessageToAll(PartyMessage.PauseCommand(timestamp))
        }
    }
    
    fun sendSeekCommand(position: Long, timestamp: Long) {
        if (_isHosting.value) {
            Log.d(TAG, "📤 Sending SeekCommand: position=$position")
            connectionManager.sendMessageToAll(PartyMessage.SeekCommand(position, timestamp))
        }
    }

    /**
     * Sends NowPlaying with full metadata for placeholder support.
     */
    fun sendNowPlaying(
        songId: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        thumbnailUrl: String? = null,
        isOffline: Boolean = true
    ) {
        if (_isHosting.value) {
            val msg = PartyMessage.NowPlaying(
                songId = songId,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbnailUrl,
                isOffline = isOffline
            )
            Log.d(TAG, "📤 Sending NowPlaying: $title by $artist")
            connectionManager.sendMessageToAll(msg)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Client: Placeholder & Media Issue Reporting
    // ════════════════════════════════════════════════════════════════════

    /**
     * Called by the client when it cannot find the song locally.
     * Sets placeholder mode and notifies the host.
     */
    fun reportMissingSong(userName: String) {
        _isShowingPlaceholder.value = true
        if (!_isHosting.value) {
            Log.d(TAG, "⚠️ Reporting missing song to host")
            connectionManager.sendMessageToHost(
                PartyMessage.MediaIssue(userName, "Song not available locally")
            )
        }
    }

    /**
     * Called when the client resolves the issue (e.g., placeholder dismissed,
     * file received, or streaming started).
     */
    fun clearPlaceholder() {
        _isShowingPlaceholder.value = false
    }

    /**
     * Client sends a request to the host for streaming or file transfer.
     */
    fun requestMediaFromHost(requestType: String, songId: String) {
        if (!_isHosting.value) {
            Log.d(TAG, "📤 Sending MediaRequest: type=$requestType, songId=$songId")
            connectionManager.sendMessageToHost(
                PartyMessage.MediaRequest(requestType, songId)
            )
        }
    }

    /**
     * Host responds to a media request from a client.
     */
    fun respondToMediaRequest(endpointId: String, isAccepted: Boolean, requestType: String, songId: String) {
        if (_isHosting.value) {
            Log.d(TAG, "📤 MediaResponse to $endpointId: accepted=$isAccepted, type=$requestType")
            connectionManager.sendMessageToUser(
                endpointId,
                PartyMessage.MediaResponse(isAccepted, requestType, songId)
            )
            // Clear the client issue if accepted
            if (isAccepted) {
                connectionManager.clearClientIssue(endpointId)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Foreground Service
    // ════════════════════════════════════════════════════════════════════

    private fun startForegroundService(partyName: String, isHost: Boolean) {
        val app = getApplication<Application>()
        val intent = Intent(app, PartyService::class.java).apply {
            putExtra(PartyService.EXTRA_PARTY_NAME, partyName)
            putExtra(PartyService.EXTRA_IS_HOST, isHost)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
        Log.d(TAG, "🔔 PartyService started (isHost=$isHost)")
    }

    private fun stopForegroundService() {
        val app = getApplication<Application>()
        app.stopService(Intent(app, PartyService::class.java))
        Log.d(TAG, "🔔 PartyService stopped")
    }

    // ════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 PartyViewModel cleared — stopping everything")
        if (_isHosting.value) stopHosting()
        if (_isConnected.value) leaveParty()
    }
}

// ════════════════════════════════════════════════════════════════════════
// UI Event Types
// ════════════════════════════════════════════════════════════════════════

sealed class PartyUiEvent {
    /** Host left — party is over for all clients */
    data object HostDisconnected : PartyUiEvent()
    /** A specific client disconnected (kicked) */
    data class ClientDisconnected(val endpointId: String) : PartyUiEvent()
    /** A client is requesting media from the host */
    data class MediaRequestReceived(val endpointId: String, val request: PartyMessage.MediaRequest) : PartyUiEvent()
    /** A client reported a media issue */
    data class ClientIssueReported(val endpointId: String, val userName: String, val issueType: String) : PartyUiEvent()
}
