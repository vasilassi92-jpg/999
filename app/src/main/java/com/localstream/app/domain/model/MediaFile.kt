package com.localstream.app.domain.model

enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    DOCUMENT,
    OTHER
}

data class MediaFile(
    val id: Long = 0,
    val name: String,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val size: Long,
    val modifiedDate: Long,
    val folder: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L
) {
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val formatVal = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.1f %s", formatVal, units[digitGroups])
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes % 60, remainingSeconds)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", minutes, remainingSeconds)
            }
        }
}
