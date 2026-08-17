# SyncTax Party Mode: Technical Context & Implementation

This document outlines the architecture, principles, and implementation details of the Party Mode feature in SyncTax using a local-only hotspot and app-level network routing.

## 1. The Idea
Party Mode allows a Host (DJ) to synchronize music playback across multiple Guest (Client) devices in real-time. The system focuses on synchronized local playback to keep quality high while minimizing battery and data usage.

## 2. Host and Client Flow (User Perspective)
1. Host taps Start Party. The app starts a Local-Only Hotspot programmatically.
2. The Host screen shows the hotspot SSID, passphrase, port, and a QR code for sharing.
3. Guests open Party Mode and join by entering the SSID/passphrase (or scanning the QR code).
4. The app connects to the hotspot programmatically and binds only SyncTax traffic to that Wi-Fi network.
5. The Host controls playback; all Guests play the same song at the same millisecond using the sync protocol.

### Online vs Offline Playback
- Offline songs: the Host does not need internet. Guests receive playback commands and optional transfers over the local hotspot.
- Online songs: the Host must have internet, and streams data to Guests over the hotspot. Guests do not need their own internet.

## 3. Principles and Architecture

### Local-Only Hotspot Star Topology
The Host creates a Local-Only Hotspot using WifiManager. Guests connect to that hotspot using WifiNetworkSpecifier and a TCP socket back to the Host. The Host acts as the hub and can serve multiple Guests.

### Dual-Network Routing (No-Internet Hotspot)
Android treats a Wi-Fi network as the default route unless told otherwise. To prevent a no-internet hotspot from breaking cellular data on Guests, SyncTax requests the Wi-Fi network without NET_CAPABILITY_INTERNET and binds only the app process to it:
- ConnectivityManager.requestNetwork with removeCapability(NET_CAPABILITY_INTERNET)
- bindProcessToNetwork(network) for SyncTax traffic only

Result: SyncTax uses the hotspot for party traffic, while the rest of the phone continues to use cellular data.

### Clock Synchronization
We use a master-slave clock sync:
1. Host sends PlayCommand with a future start timestamp.
2. Guests estimate latency and clock offset using SyncRequest/SyncResponse.
3. Guests schedule ExoPlayer to start at the host time plus offset.

## 4. Key Implementation Components

### Communication Protocol
All messages are serialized as JSON and sent as byte payloads.
- PartyMessage.kt defines the sealed class hierarchy (Handshake, Play, Pause, Seek, MediaIssue, etc.).

### Connection Management
- PartyConnectionManager.kt starts the local-only hotspot, accepts TCP socket clients, and binds guest traffic to Wi-Fi only.

### QR Sharing
- CreatePartyScreen.kt displays a QR code that encodes synctax://party?ssid=...&pass=...&port=...

### Sync Control
- PartySyncController.kt stabilizes latency using a rolling median filter and provides synchronized host time.

### Background Stability
- PartyService.kt is a foreground service with a wake lock to keep party networking alive.

### State Management
- PartyViewModel.kt bridges party logic with Compose UI and the PlayerViewModel.

## 5. File Reference Guide

| Component | File Path |
| :--- | :--- |
| Manifest and permissions | app/src/main/AndroidManifest.xml |
| Hotspot core | app/src/main/java/com/just_for_fun/synctax/core/party/PartyConnectionManager.kt |
| Protocol | app/src/main/java/com/just_for_fun/synctax/core/party/PartyMessage.kt |
| Sync logic | app/src/main/java/com/just_for_fun/synctax/core/party/PartySyncController.kt |
| Service | app/src/main/java/com/just_for_fun/synctax/core/service/PartyService.kt |
| Business logic | app/src/main/java/com/just_for_fun/synctax/presentation/viewmodels/PartyViewModel.kt |
| Dashboard UI | app/src/main/java/com/just_for_fun/synctax/presentation/screens/PartyDashboardScreen.kt |
| Host UI and QR | app/src/main/java/com/just_for_fun/synctax/presentation/screens/CreatePartyScreen.kt |
| Session UI | app/src/main/java/com/just_for_fun/synctax/presentation/screens/PartySessionScreen.kt |
| Playback link | app/src/main/java/com/just_for_fun/synctax/MusicApp.kt |

## 6. Error Handling and Robustness
- Missing media: Guests send MediaIssue to the Host; the Host UI shows a warning and Guests display a placeholder song.
- Connection loss: Host leaving ends the party for all Guests; Guest leaving updates the Host list.
- Permissions: Wi-Fi + location permissions are required; Android 13+ uses NEARBY_WIFI_DEVICES.
