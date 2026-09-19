package com.localstream.app.domain.model

enum class DeviceType {
    SMART_TV,
    CHROMECAST,
    DLNA_RENDERER,
    UPNP_MEDIA_SERVER,
    COMPUTER,
    PHONE_RECEIVER,
    GENERIC
}

data class RendererDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 0,
    val deviceType: DeviceType = DeviceType.GENERIC,
    val manufacturer: String = "",
    val modelName: String = "",
    val isReceiver: Boolean = false,
    val isConnected: Boolean = false,
    val locationXmlUrl: String? = null
)

data class NetworkDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val deviceType: DeviceType,
    val serviceType: String,
    val locationUrl: String? = null,
    val isOnline: Boolean = true
)

data class SmbShare(
    val id: String = java.util.UUID.randomUUID().toString(),
    val server: String,
    val username: String = "",
    val shareName: String = "",
    val folderPath: String = "",
    val isConnected: Boolean = false,
    val lastConnected: Long = 0L
)
