package com.localstream.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.localstream.app.domain.model.DeviceType
import com.localstream.app.domain.model.RendererDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

class UpnpDiscovery(private val context: Context) {

    private val tag = "UpnpDiscovery"

    suspend fun discoverDevices(timeoutMs: Int = 3500): List<RendererDevice> = withContext(Dispatchers.IO) {
        val discoveredDevices = mutableMapOf<String, RendererDevice>()

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = try {
            wifiManager?.createMulticastLock("LocalStream_SSDP")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            null
        }

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 800

            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: ssdp:all\r\n\r\n"

            val queryBytes = ssdpQuery.toByteArray(Charsets.US_ASCII)
            val multicastGroup = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(queryBytes, queryBytes.size, multicastGroup, 1900)

            // Send discovery packet
            socket.send(packet)

            val receiveBuffer = ByteArray(4096)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length, Charsets.US_ASCII)
                    parseSsdpResponse(response, receivePacket.address.hostAddress ?: "")?.let { dev ->
                        discoveredDevices[dev.id] = dev
                    }
                } catch (e: SocketTimeoutException) {
                    // Packet receive timeout - continue listening until total duration
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "SSDP Discovery exception", e)
        } finally {
            socket?.close()
            try {
                if (multicastLock != null && multicastLock.isHeld) {
                    multicastLock.release()
                }
            } catch (e: Exception) {}
        }

        return@withContext discoveredDevices.values.toList()
    }

    private fun parseSsdpResponse(response: String, senderIp: String): RendererDevice? {
        val headers = mutableMapOf<String, String>()
        val lines = response.split("\r\n")

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().uppercase()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }

        val location = headers["LOCATION"]
        val st = headers["ST"]?.lowercase() ?: ""
        val usn = headers["USN"] ?: "dev_$senderIp"
        val server = headers["SERVER"] ?: ""

        val deviceType = when {
            st.contains("mediarenderer") || server.contains("renderer", ignoreCase = true) -> DeviceType.DLNA_RENDERER
            st.contains("mediaserver") || server.contains("mediaserver", ignoreCase = true) -> DeviceType.UPNP_MEDIA_SERVER
            st.contains("dial") || server.contains("chromecast", ignoreCase = true) -> DeviceType.CHROMECAST
            server.contains("tv", ignoreCase = true) || st.contains("tv") -> DeviceType.SMART_TV
            server.contains("windows", ignoreCase = true) || server.contains("linux", ignoreCase = true) || server.contains("mac", ignoreCase = true) -> DeviceType.COMPUTER
            else -> DeviceType.GENERIC
        }

        var port = 80
        var hostName = senderIp
        if (!location.isNullOrBlank()) {
            try {
                val url = URL(location)
                port = if (url.port > 0) url.port else 80
                hostName = url.host
            } catch (e: Exception) {}
        }

        val name = when {
            server.isNotEmpty() -> server.substringBefore('/')
            deviceType == DeviceType.DLNA_RENDERER -> "DLNA Renderer ($senderIp)"
            deviceType == DeviceType.CHROMECAST -> "Chromecast Device ($senderIp)"
            deviceType == DeviceType.SMART_TV -> "Smart TV ($senderIp)"
            deviceType == DeviceType.COMPUTER -> "Network PC ($senderIp)"
            else -> "Network Device ($senderIp)"
        }

        return RendererDevice(
            id = usn,
            name = name,
            ipAddress = hostName,
            port = port,
            deviceType = deviceType,
            manufacturer = server,
            locationXmlUrl = location
        )
    }
}
