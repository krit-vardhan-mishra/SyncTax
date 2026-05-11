package com.just_for_fun.synctax.core.party

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles clock synchronization between host and clients using a simplified SNTP-like protocol.
 * Calculates clock offset to allow synchronized playback.
 *
 * Improvements:
 * - Sync frequency increased to every 1.5 seconds for tighter drift correction.
 * - Rolling median filter over 10 samples for stability.
 * - Manual audio latency offset that the user can configure.
 */
class PartySyncController(
    private val connectionManager: PartyConnectionManager,
    private val scope: CoroutineScope
) {
    private val _clockOffset = MutableStateFlow(0L)
    val clockOffset: StateFlow<Long> = _clockOffset.asStateFlow()

    private val _syncLatency = MutableStateFlow(0L)
    val syncLatency: StateFlow<Long> = _syncLatency.asStateFlow()

    /**
     * User-adjustable audio latency offset (in ms).
     * Positive values delay playback, negative values advance it.
     * This compensates for device-specific speaker/DAC latency.
     */
    private val _localLatencyOffsetMs = MutableStateFlow(0L)
    val localLatencyOffsetMs: StateFlow<Long> = _localLatencyOffsetMs.asStateFlow()

    private val syncOffsets = mutableListOf<Long>()
    private val maxSamples = 10

    init {
        // Listen to messages for sync protocol
        val previousListener = connectionManager.onMessageReceived
        connectionManager.onMessageReceived = { message ->
            previousListener?.invoke(message)
            handleMessage(message)
        }
    }

    private fun handleMessage(message: PartyMessage) {
        when (message) {
            is PartyMessage.SyncRequest -> {
                // We are the host. Respond with our time.
                val hostRxTime = SystemClock.elapsedRealtime()
                val hostTxTime = SystemClock.elapsedRealtime()
                Log.d(TAG, "⏱️ SyncRequest received, responding. clientTxTime=${message.clientTxTime}")
                connectionManager.sendMessageToAll(
                    PartyMessage.SyncResponse(
                        clientTxTime = message.clientTxTime,
                        hostRxTime = hostRxTime,
                        hostTxTime = hostTxTime
                    )
                )
            }
            is PartyMessage.SyncResponse -> {
                // We are the client. Calculate offset.
                val clientRxTime = SystemClock.elapsedRealtime()
                
                // Round trip delay
                val delay = (clientRxTime - message.clientTxTime) - (message.hostTxTime - message.hostRxTime)
                
                // Clock offset: host_time - client_time
                // According to SNTP: offset = ((hostRxTime - clientTxTime) + (hostTxTime - clientRxTime)) / 2
                val offset = ((message.hostRxTime - message.clientTxTime) + (message.hostTxTime - clientRxTime)) / 2

                _syncLatency.value = delay

                // Add to rolling average
                syncOffsets.add(offset)
                if (syncOffsets.size > maxSamples) {
                    syncOffsets.removeAt(0)
                }

                // Use median offset for stability
                _clockOffset.value = syncOffsets.sorted()[syncOffsets.size / 2]
                Log.d(TAG, "⏱️ Sync: offset=${_clockOffset.value}ms, latency=${delay}ms, samples=${syncOffsets.size}")
            }
            else -> {} // Handled elsewhere
        }
    }

    /**
     * Starts continuous sync process if we are a client.
     * Syncs every 1.5 seconds for tighter drift correction.
     */
    fun startSyncing() {
        if (!connectionManager.isHosting.value) {
            Log.d(TAG, "⏱️ Starting continuous sync (interval=1500ms)")
            scope.launch(Dispatchers.IO) {
                while (connectionManager.isConnected.value) {
                    val clientTxTime = SystemClock.elapsedRealtime()
                    connectionManager.sendMessageToHost(PartyMessage.SyncRequest(clientTxTime))
                    // Send sync request every 1.5 seconds (reduced from 3s for tighter sync)
                    delay(1500)
                }
                Log.d(TAG, "⏱️ Sync loop ended — no longer connected")
            }
        }
    }

    /**
     * Get synchronized host time.
     */
    fun getSynchronizedTime(): Long {
        return SystemClock.elapsedRealtime() + _clockOffset.value
    }

    /**
     * Converts a future host time to local device time for playback scheduling.
     * Includes the user-configurable audio latency offset.
     */
    fun hostTimeToLocalTime(hostTime: Long): Long {
        return hostTime - _clockOffset.value + _localLatencyOffsetMs.value
    }

    /**
     * Sets the manual audio latency offset (in milliseconds).
     * Use positive values if this device plays audio *earlier* than others (delay it).
     * Use negative values if this device plays audio *later* than others (advance it).
     */
    fun setLatencyOffset(offsetMs: Long) {
        _localLatencyOffsetMs.value = offsetMs
        Log.d(TAG, "🔧 Audio latency offset set to ${offsetMs}ms")
    }

    companion object {
        private const val TAG = "PartySyncController"
    }
}
