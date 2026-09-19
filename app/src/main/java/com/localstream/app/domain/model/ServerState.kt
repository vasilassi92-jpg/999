package com.localstream.app.domain.model

data class ServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "127.0.0.1",
    val port: Int = 8080,
    val indexedFilesCount: Int = 0,
    val uptimeMillis: Long = 0L,
    val connectedClients: Int = 0,
    val isAuthRequired: Boolean = false,
    val currentSubnet: String = "",
    val error: String? = null
) {
    val serverUrl: String
        get() = if (ipAddress.isNotEmpty() && ipAddress != "0.0.0.0") {
            "http://$ipAddress:$port"
        } else {
            "http://127.0.0.1:$port"
        }

    val formattedUptime: String
        get() {
            if (uptimeMillis <= 0L) return "00:00:00"
            val totalSeconds = uptimeMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
}
