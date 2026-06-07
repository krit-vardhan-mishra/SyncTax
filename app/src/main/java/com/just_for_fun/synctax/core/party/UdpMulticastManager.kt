package com.just_for_fun.synctax.core.party

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

class UdpMulticastManager(private val isHost: Boolean) {

    companion object {
        private const val TAG = "UdpMulticastManager"
        private const val MULTICAST_IP = "224.0.0.1" // Local subnet multicast
        private const val UDP_PORT = 45124
        private const val PACKET_SIZE = 1024
    }

    private var multicastSocket: MulticastSocket? = null
    private var listenJob: Job? = null
    var onMessageReceived: ((PartyMessage) -> Unit)? = null

    fun start() {
        try {
            multicastSocket = MulticastSocket(UDP_PORT)
            val group = InetAddress.getByName(MULTICAST_IP)
            multicastSocket?.joinGroup(group)
            
            Log.d(TAG, "📡 UDP Multicast started. Listening on $MULTICAST_IP:$UDP_PORT")
            
            listenJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(PACKET_SIZE)
                while (isActive && multicastSocket != null) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        multicastSocket?.receive(packet)
                        
                        // Parse actual data
                        val data = packet.data.copyOfRange(0, packet.length)
                        val message = PartyMessage.fromByteArray(data)
                        onMessageReceived?.invoke(message)
                    } catch (e: Exception) {
                        if (isActive) Log.e(TAG, "UDP Receive Error", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UDP Multicast", e)
        }
    }

    fun broadcast(message: PartyMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket()
                val bytes = message.toByteArray()
                val group = InetAddress.getByName(MULTICAST_IP)
                val packet = DatagramPacket(bytes, bytes.size, group, UDP_PORT)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "UDP Broadcast Error", e)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "🛑 Stopping UDP Multicast")
        listenJob?.cancel()
        try {
            val group = InetAddress.getByName(MULTICAST_IP)
            multicastSocket?.leaveGroup(group)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving group", e)
        }
        multicastSocket?.close()
        multicastSocket = null
    }
}
