package com.just_for_fun.synctax.core.party

import android.util.Base64
import com.just_for_fun.synctax.core.party.proto.*
import java.nio.charset.StandardCharsets

sealed class PartyMessage {
    data class Handshake(val appVersion: String, val userName: String) : PartyMessage()

    data class PlayCommand(
        val songId: String, 
        val startTimestamp: Long,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val isOffline: Boolean = true
    ) : PartyMessage()

    data class PauseCommand(val timestamp: Long) : PartyMessage()

    data class SeekCommand(val position: Long, val timestamp: Long) : PartyMessage()

    data class SyncRequest(val clientTxTime: Long) : PartyMessage()

    data class SyncResponse(
        val clientTxTime: Long,
        val hostRxTime: Long,
        val hostTxTime: Long
    ) : PartyMessage()
    
    data class NowPlaying(
        val songId: String,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val isOffline: Boolean = true
    ) : PartyMessage()

    data class MediaIssue(val userName: String, val issueType: String) : PartyMessage()

    data class MediaRequest(val requestType: String, val songId: String) : PartyMessage() // requestType: "STREAM" or "FILE"

    data class MediaResponse(val isAccepted: Boolean, val requestType: String, val songId: String) : PartyMessage()

    data class EndParty(val reason: String = "Host disconnected") : PartyMessage()

    data class HostTransferRequest(val userName: String) : PartyMessage()

    data class HostTransferResponse(val accepted: Boolean, val fromHost: String) : PartyMessage()

    fun toByteArray(): ByteArray {
        val builder = PartyMessageProto.newBuilder()
        when (this) {
            is Handshake -> builder.handshake = com.just_for_fun.synctax.core.party.proto.Handshake.newBuilder()
                .setAppVersion(appVersion).setUserName(userName).build()
            is PlayCommand -> builder.play = com.just_for_fun.synctax.core.party.proto.PlayCommand.newBuilder()
                .setSongId(songId).setStartTimestamp(startTimestamp)
                .setTitle(title ?: "").setArtist(artist ?: "").setAlbum(album ?: "")
                .setThumbnailUrl(thumbnailUrl ?: "").setIsOffline(isOffline).build()
            is PauseCommand -> builder.pause = com.just_for_fun.synctax.core.party.proto.PauseCommand.newBuilder()
                .setTimestamp(timestamp).build()
            is SeekCommand -> builder.seek = com.just_for_fun.synctax.core.party.proto.SeekCommand.newBuilder()
                .setPosition(position).setTimestamp(timestamp).build()
            is SyncRequest -> builder.syncRequest = com.just_for_fun.synctax.core.party.proto.SyncRequest.newBuilder()
                .setClientTxTime(clientTxTime).build()
            is SyncResponse -> builder.syncResponse = com.just_for_fun.synctax.core.party.proto.SyncResponse.newBuilder()
                .setClientTxTime(clientTxTime).setHostRxTime(hostRxTime).setHostTxTime(hostTxTime).build()
            is NowPlaying -> builder.nowPlaying = com.just_for_fun.synctax.core.party.proto.NowPlaying.newBuilder()
                .setSongId(songId).setTitle(title ?: "").setArtist(artist ?: "")
                .setAlbum(album ?: "").setThumbnailUrl(thumbnailUrl ?: "").setIsOffline(isOffline).build()
            is MediaIssue -> builder.mediaIssue = com.just_for_fun.synctax.core.party.proto.MediaIssue.newBuilder()
                .setUserName(userName).setIssueType(issueType).build()
            is MediaRequest -> builder.mediaRequest = com.just_for_fun.synctax.core.party.proto.MediaRequest.newBuilder()
                .setRequestType(requestType).setSongId(songId).build()
            is MediaResponse -> builder.mediaResponse = com.just_for_fun.synctax.core.party.proto.MediaResponse.newBuilder()
                .setIsAccepted(isAccepted).setRequestType(requestType).setSongId(songId).build()
            is EndParty -> builder.endParty = com.just_for_fun.synctax.core.party.proto.EndParty.newBuilder()
                .setReason(reason).build()
            is HostTransferRequest -> builder.hostTransferRequest = com.just_for_fun.synctax.core.party.proto.HostTransferRequest.newBuilder()
                .setUserName(userName).build()
            is HostTransferResponse -> builder.hostTransferResponse = com.just_for_fun.synctax.core.party.proto.HostTransferResponse.newBuilder()
                .setAccepted(accepted).setFromHost(fromHost).build()
        }
        val bytes = builder.build().toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP).toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): PartyMessage {
            val base64Str = String(bytes, StandardCharsets.UTF_8)
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            val proto = PartyMessageProto.parseFrom(decodedBytes)
            return when (proto.payloadCase) {
                PartyMessageProto.PayloadCase.HANDSHAKE -> Handshake(proto.handshake.appVersion, proto.handshake.userName)
                PartyMessageProto.PayloadCase.PLAY -> PlayCommand(
                    songId = proto.play.songId,
                    startTimestamp = proto.play.startTimestamp,
                    title = proto.play.title.takeIf { it.isNotEmpty() },
                    artist = proto.play.artist.takeIf { it.isNotEmpty() },
                    album = proto.play.album.takeIf { it.isNotEmpty() },
                    thumbnailUrl = proto.play.thumbnailUrl.takeIf { it.isNotEmpty() },
                    isOffline = proto.play.isOffline
                )
                PartyMessageProto.PayloadCase.PAUSE -> PauseCommand(proto.pause.timestamp)
                PartyMessageProto.PayloadCase.SEEK -> SeekCommand(proto.seek.position, proto.seek.timestamp)
                PartyMessageProto.PayloadCase.SYNC_REQUEST -> SyncRequest(proto.syncRequest.clientTxTime)
                PartyMessageProto.PayloadCase.SYNC_RESPONSE -> SyncResponse(proto.syncResponse.clientTxTime, proto.syncResponse.hostRxTime, proto.syncResponse.hostTxTime)
                PartyMessageProto.PayloadCase.NOW_PLAYING -> NowPlaying(
                    songId = proto.nowPlaying.songId,
                    title = proto.nowPlaying.title.takeIf { it.isNotEmpty() },
                    artist = proto.nowPlaying.artist.takeIf { it.isNotEmpty() },
                    album = proto.nowPlaying.album.takeIf { it.isNotEmpty() },
                    thumbnailUrl = proto.nowPlaying.thumbnailUrl.takeIf { it.isNotEmpty() },
                    isOffline = proto.nowPlaying.isOffline
                )
                PartyMessageProto.PayloadCase.MEDIA_ISSUE -> MediaIssue(proto.mediaIssue.userName, proto.mediaIssue.issueType)
                PartyMessageProto.PayloadCase.MEDIA_REQUEST -> MediaRequest(proto.mediaRequest.requestType, proto.mediaRequest.songId)
                PartyMessageProto.PayloadCase.MEDIA_RESPONSE -> MediaResponse(proto.mediaResponse.isAccepted, proto.mediaResponse.requestType, proto.mediaResponse.songId)
                PartyMessageProto.PayloadCase.END_PARTY -> EndParty(proto.endParty.reason)
                PartyMessageProto.PayloadCase.HOST_TRANSFER_REQUEST -> HostTransferRequest(proto.hostTransferRequest.userName)
                PartyMessageProto.PayloadCase.HOST_TRANSFER_RESPONSE -> HostTransferResponse(proto.hostTransferResponse.accepted, proto.hostTransferResponse.fromHost)
                else -> throw IllegalArgumentException("Unknown payload case: ${proto.payloadCase}")
            }
        }
    }
}
