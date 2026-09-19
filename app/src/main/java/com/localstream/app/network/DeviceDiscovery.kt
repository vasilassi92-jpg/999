package com.localstream.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.localstream.app.domain.model.DeviceType
import com.localstream.app.domain.model.NetworkDevice
import com.localstream.app.domain.model.RendererDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class DeviceDiscovery(
    private val context: Context,
    private val upnpDiscovery: UpnpDiscovery
) {
    private val tag = "DeviceDiscovery"

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredRenderers = MutableStateFlow<List<RendererDevice>>(emptyList())
    val discoveredRenderers: StateFlow<List<RendererDevice>> = _discoveredRenderers.asStateFlow()

    private val _discoveredNetworkDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val discoveredNetworkDevices: StateFlow<List<NetworkDevice>> = _discoveredNetworkDevices.asStateFlow()

    suspend fun discoverAll(includeSubnetScan: Boolean = false) = withContext(Dispatchers.IO) {
        _isDiscovering.value = true
        val renderers = mutableListOf<RendererDevice>()
        val networkDevices = mutableListOf<NetworkDevice>()

        try {
            // 1. SSDP / UPnP Discovery
            val ssdpDevices = upnpDiscovery.discoverDevices(timeoutMs = 3000)
            renderers.addAll(ssdpDevices)

            for (d in ssdpDevices) {
                networkDevices.add(
                    NetworkDevice(
                        id = d.id,
                        name = d.name,
                        ipAddress = d.ipAddress,
                        port = d.port,
                        deviceType = d.deviceType,
                        serviceType = "UPnP/DLNA",
                        locationUrl = d.locationXmlUrl,
                        isOnline = true
                    )
                )
            }

            // 2. Subnet probe for active media/casting services if requested or no devices found
            if (includeSubnetScan || renderers.isEmpty()) {
                val subnetPrefix = NetworkUtils.getSubnetPrefix(NetworkUtils.getLocalIpAddress())
                val probedDevices = probeSubnetCommonPorts(subnetPrefix)
                for (p in probedDevices) {
                    if (renderers.none { it.ipAddress == p.ipAddress }) {
                        renderers.add(p)
                    }
                }
            }

            _discoveredRenderers.value = renderers.distinctBy { it.ipAddress + it.port }
            _discoveredNetworkDevices.value = networkDevices.distinctBy { it.ipAddress + it.port }
        } catch (e: Exception) {
            Log.e(tag, "Device discovery failed", e)
        } finally {
            _isDiscovering.value = false
        }
    }

    /**
     * Fast non-blocking subnet probe for well-known media/casting ports:
     * Port 8008 / 8009: Chromecast / Google Cast
     * Port 1900 / 2869: DLNA / UPnP
     * Port 8080: Web media server
     * Port 445: SMB
     */
    private suspend fun probeSubnetCommonPorts(subnetPrefix: String): List<RendererDevice> = coroutineScope {
        val results = mutableListOf<RendererDevice>()
        val ownIp = NetworkUtils.getLocalIpAddress()

        // Probe high-probability host addresses concurrently with short timeout
        val targets = (1..30).map { "$subnetPrefix.$it" }.filter { it != ownIp }

        val deferreds = targets.map { ip ->
            async(Dispatchers.IO) {
                checkHostForServices(ip)
            }
        }

        val found = deferreds.awaitAll().filterNotNull()
        results.addAll(found)
        results
    }

    private fun checkHostForServices(ip: String): RendererDevice? {
        val portsToCheck = listOf(8008 to DeviceType.CHROMECAST, 1900 to DeviceType.DLNA_RENDERER, 8080 to DeviceType.UPNP_MEDIA_SERVER)
        for ((port, type) in portsToCheck) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), 200)
                    val name = when (type) {
                        DeviceType.CHROMECAST -> "Google Cast / TV ($ip)"
                        DeviceType.DLNA_RENDERER -> "Media Renderer ($ip)"
                        else -> "Network Media Device ($ip)"
                    }
                    return RendererDevice(
                        id = "probed_$ip:$port",
                        name = name,
                        ipAddress = ip,
                        port = port,
                        deviceType = type,
                        manufacturer = "LAN Discovered"
                    )
                }
            } catch (e: Exception) {
                // Not open, continue
            }
        }
        return null
    }
}
