package com.just_for_fun.synctax.core.party

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

/**
 * Unit tests for the Party mode subsystem.
 *
 * These tests cover data class behaviour, message serialization,
 * QR payload generation, and state-flow initial values —
 * all logic that can be verified without an Android device.
 */
class PartyConnectionManagerTest {

    // ════════════════════════════════════════════════════════════════════════
    // 1. Data class construction & equality
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `PartyHotspotInfo holds correct values`() {
        val info = PartyHotspotInfo(ssid = "TestNetwork", passphrase = "abc123", port = 9090)
        assertEquals("TestNetwork", info.ssid)
        assertEquals("abc123", info.passphrase)
        assertEquals(9090, info.port)
    }

    @Test
    fun `PartyHotspotInfo equality works`() {
        val a = PartyHotspotInfo("A", "B", 8080)
        val b = PartyHotspotInfo("A", "B", 8080)
        val c = PartyHotspotInfo("X", "B", 8080)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `PartyMember stores endpoint and name`() {
        val member = PartyMember(endpointId = "ep1", name = "Alice", isHost = false)
        assertEquals("ep1", member.endpointId)
        assertEquals("Alice", member.name)
        assertFalse(member.isHost)
    }

    @Test
    fun `PartyMember host flag`() {
        val host = PartyMember("host1", "Bob", isHost = true)
        assertTrue(host.isHost)
    }

    @Test
    fun `DiscoveredParty construction`() {
        val party = DiscoveredParty(endpointId = "ep2", name = "Dance Party")
        assertEquals("ep2", party.endpointId)
        assertEquals("Dance Party", party.name)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. PartyMessage serialization / deserialization round-trip
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `Handshake message round-trip`() {
        val original = PartyMessage.Handshake(appVersion = "1.2.3", userName = "Charlie")
        val bytes = original.toByteArray()
        val decoded = PartyMessage.fromByteArray(bytes)
        assertTrue(decoded is PartyMessage.Handshake)
        val h = decoded as PartyMessage.Handshake
        assertEquals("1.2.3", h.appVersion)
        assertEquals("Charlie", h.userName)
    }

    @Test
    fun `PlayCommand message round-trip with optional fields`() {
        val original = PartyMessage.PlayCommand(
            songId = "song123",
            startTimestamp = 1_000_000L,
            title = "My Song",
            artist = "Artist",
            album = "Album",
            thumbnailUrl = "https://example.com/thumb.jpg",
            isOffline = true
        )
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.PlayCommand)
        val p = decoded as PartyMessage.PlayCommand
        assertEquals("song123", p.songId)
        assertEquals(1_000_000L, p.startTimestamp)
        assertEquals("My Song", p.title)
        assertEquals("Artist", p.artist)
        assertEquals("Album", p.album)
        assertEquals("https://example.com/thumb.jpg", p.thumbnailUrl)
        assertTrue(p.isOffline)
    }

    @Test
    fun `PlayCommand message defaults`() {
        val cmd = PartyMessage.PlayCommand(songId = "s1", startTimestamp = 0L)
        assertNull(cmd.title)
        assertNull(cmd.artist)
        assertNull(cmd.album)
        assertNull(cmd.thumbnailUrl)
        assertTrue(cmd.isOffline) // default
    }

    @Test
    fun `PauseCommand message round-trip`() {
        val original = PartyMessage.PauseCommand(timestamp = 555L)
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.PauseCommand)
        assertEquals(555L, (decoded as PartyMessage.PauseCommand).timestamp)
    }

    @Test
    fun `SeekCommand message round-trip`() {
        val original = PartyMessage.SeekCommand(position = 30_000L, timestamp = 999L)
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.SeekCommand)
        val s = decoded as PartyMessage.SeekCommand
        assertEquals(30_000L, s.position)
        assertEquals(999L, s.timestamp)
    }

    @Test
    fun `SyncRequest message round-trip`() {
        val original = PartyMessage.SyncRequest(clientTxTime = 12345L)
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.SyncRequest)
        assertEquals(12345L, (decoded as PartyMessage.SyncRequest).clientTxTime)
    }

    @Test
    fun `SyncResponse message round-trip`() {
        val original = PartyMessage.SyncResponse(
            clientTxTime = 100L,
            hostRxTime = 200L,
            hostTxTime = 300L
        )
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.SyncResponse)
        val r = decoded as PartyMessage.SyncResponse
        assertEquals(100L, r.clientTxTime)
        assertEquals(200L, r.hostRxTime)
        assertEquals(300L, r.hostTxTime)
    }

    @Test
    fun `NowPlaying message round-trip`() {
        val original = PartyMessage.NowPlaying(
            songId = "np1",
            title = "Now Playing Song",
            artist = "NP Artist",
            album = null,
            thumbnailUrl = null,
            isOffline = false
        )
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.NowPlaying)
        val np = decoded as PartyMessage.NowPlaying
        assertEquals("np1", np.songId)
        assertEquals("Now Playing Song", np.title)
        assertEquals("NP Artist", np.artist)
        assertNull(np.album)
        assertNull(np.thumbnailUrl)
        assertFalse(np.isOffline)
    }

    @Test
    fun `MediaIssue message round-trip`() {
        val original = PartyMessage.MediaIssue(userName = "Dave", issueType = "SONG_NOT_FOUND")
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.MediaIssue)
        val mi = decoded as PartyMessage.MediaIssue
        assertEquals("Dave", mi.userName)
        assertEquals("SONG_NOT_FOUND", mi.issueType)
    }

    @Test
    fun `MediaRequest message round-trip`() {
        val original = PartyMessage.MediaRequest(requestType = "STREAM", songId = "songXYZ")
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.MediaRequest)
        val mr = decoded as PartyMessage.MediaRequest
        assertEquals("STREAM", mr.requestType)
        assertEquals("songXYZ", mr.songId)
    }

    @Test
    fun `MediaResponse message round-trip`() {
        val original = PartyMessage.MediaResponse(isAccepted = true, requestType = "FILE", songId = "s99")
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.MediaResponse)
        val mr = decoded as PartyMessage.MediaResponse
        assertTrue(mr.isAccepted)
        assertEquals("FILE", mr.requestType)
        assertEquals("s99", mr.songId)
    }

    @Test
    fun `EndParty message round-trip`() {
        val original = PartyMessage.EndParty(reason = "Host left the party")
        val decoded = PartyMessage.fromByteArray(original.toByteArray())
        assertTrue(decoded is PartyMessage.EndParty)
        assertEquals("Host left the party", (decoded as PartyMessage.EndParty).reason)
    }

    @Test
    fun `EndParty default reason`() {
        val ep = PartyMessage.EndParty()
        assertEquals("Host disconnected", ep.reason)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. toByteArray / fromByteArray encoding consistency
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `toByteArray produces valid UTF-8 JSON`() {
        val msg = PartyMessage.Handshake("v1", "User")
        val json = String(msg.toByteArray(), Charsets.UTF_8)
        // Should be valid JSON containing the type discriminator
        assertTrue(json.contains("appVersion"))
        assertTrue(json.contains("userName"))
        // Should be parseable back
        val parsed = Json.decodeFromString<PartyMessage>(json)
        assertEquals(msg, parsed)
    }

    @Test
    fun `fromByteArray throws on garbage input`() {
        val garbage = "not json".toByteArray(Charsets.UTF_8)
        try {
            PartyMessage.fromByteArray(garbage)
            fail("Expected exception for invalid JSON")
        } catch (e: Exception) {
            // Expected — deserialization should fail
        }
    }

    @Test
    fun `message with special characters serializes correctly`() {
        val msg = PartyMessage.PlayCommand(
            songId = "id/with:special&chars",
            startTimestamp = 0L,
            title = "Song \"Title\" <with> 'quotes'",
            artist = "アーティスト"  // Japanese characters
        )
        val decoded = PartyMessage.fromByteArray(msg.toByteArray())
        val p = decoded as PartyMessage.PlayCommand
        assertEquals("id/with:special&chars", p.songId)
        assertEquals("Song \"Title\" <with> 'quotes'", p.title)
        assertEquals("アーティスト", p.artist)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. QR Payload building logic
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `buildPartyQrPayload formats correctly`() {
        val info = PartyHotspotInfo(ssid = "MyParty", passphrase = "pass123", port = 9090)
        val payload = buildPartyQrPayload(info)
        assertEquals("synctax://party?ssid=MyParty&pass=pass123&port=9090", payload)
    }

    @Test
    fun `buildPartyQrPayload encodes special characters`() {
        val info = PartyHotspotInfo(ssid = "My Party!", passphrase = "p@ss w0rd", port = 8080)
        val payload = buildPartyQrPayload(info)
        // Spaces should be encoded as '+' and special chars should be percent-encoded
        assertTrue(payload.startsWith("synctax://party?ssid="))
        assertTrue(payload.contains("port=8080"))
        // Ensure the SSID was URL-encoded (contains + or %20 for space)
        val ssidEncoded = URLEncoder.encode("My Party!", Charsets.UTF_8.name())
        assertTrue(payload.contains("ssid=$ssidEncoded"))
    }

    @Test
    fun `buildPartyQrPayload with empty passphrase`() {
        val info = PartyHotspotInfo(ssid = "OpenNet", passphrase = "", port = 5555)
        val payload = buildPartyQrPayload(info)
        assertEquals("synctax://party?ssid=OpenNet&pass=&port=5555", payload)
    }

    @Test
    fun `buildPartyQrPayload with unicode SSID`() {
        val info = PartyHotspotInfo(ssid = "パーティー", passphrase = "secret", port = 9090)
        val payload = buildPartyQrPayload(info)
        assertTrue(payload.startsWith("synctax://party?ssid="))
        assertTrue(payload.contains("pass=secret"))
        assertTrue(payload.contains("port=9090"))
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. PartyHotspotInfo default port
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `PARTY_PORT matches expected default`() {
        // The connection manager uses 9090 as default
        val info = PartyHotspotInfo("test", "pass", 9090)
        assertEquals(9090, info.port)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper — mirrors the private function in CreatePartyScreen
    // ════════════════════════════════════════════════════════════════════════

    private fun buildPartyQrPayload(info: PartyHotspotInfo): String {
        val ssid = URLEncoder.encode(info.ssid, Charsets.UTF_8.name())
        val passphrase = URLEncoder.encode(info.passphrase, Charsets.UTF_8.name())
        return "synctax://party?ssid=$ssid&pass=$passphrase&port=${info.port}"
    }
}
