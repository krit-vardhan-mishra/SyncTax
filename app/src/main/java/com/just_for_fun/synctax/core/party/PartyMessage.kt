package com.just_for_fun.synctax.core.party

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

@Serializable
sealed class PartyMessage {
    @Serializable
    data class Handshake(val appVersion: String, val userName: String) : PartyMessage()

    @Serializable
    data class PlayCommand(
        val songId: String, 
        val startTimestamp: Long,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val isOffline: Boolean = true
    ) : PartyMessage()

    @Serializable
    data class PauseCommand(val timestamp: Long) : PartyMessage()

    @Serializable
    data class SeekCommand(val position: Long, val timestamp: Long) : PartyMessage()

    @Serializable
    data class SyncRequest(val clientTxTime: Long) : PartyMessage()

    @Serializable
    data class SyncResponse(
        val clientTxTime: Long,
        val hostRxTime: Long,
        val hostTxTime: Long
    ) : PartyMessage()
    
    @Serializable
    data class NowPlaying(
        val songId: String,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val isOffline: Boolean = true
    ) : PartyMessage()

    @Serializable
    data class MediaIssue(val userName: String, val issueType: String) : PartyMessage()

    @Serializable
    data class MediaRequest(val requestType: String, val songId: String) : PartyMessage() // requestType: "STREAM" or "FILE"

    @Serializable
    data class MediaResponse(val isAccepted: Boolean, val requestType: String, val songId: String) : PartyMessage()

    @Serializable
    data class EndParty(val reason: String = "Host disconnected") : PartyMessage()

    fun toByteArray(): ByteArray {
        val jsonString = Json.encodeToString(this)
        return jsonString.toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): PartyMessage {
            val jsonString = String(bytes, StandardCharsets.UTF_8)
            return Json.decodeFromString(jsonString)
        }
    }
}
