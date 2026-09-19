package com.localstream.app.network

import com.localstream.app.domain.model.DeviceType
import com.localstream.app.domain.model.RendererDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DlnaDiscovery(private val upnpDiscovery: UpnpDiscovery) {

    suspend fun discoverDlnaRenderers(): List<RendererDevice> = withContext(Dispatchers.IO) {
        val allDevices = upnpDiscovery.discoverDevices(timeoutMs = 2500)
        return@withContext allDevices.filter {
            it.deviceType == DeviceType.DLNA_RENDERER ||
            it.deviceType == DeviceType.SMART_TV ||
            it.deviceType == DeviceType.CHROMECAST
        }
    }
}
