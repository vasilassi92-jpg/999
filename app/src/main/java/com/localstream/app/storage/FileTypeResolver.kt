package com.localstream.app.storage

import com.localstream.app.domain.model.MediaType
import java.util.Locale

object FileTypeResolver {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts", "flv", "wmv", "m4v"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "aac", "flac", "ogg", "m4a", "opus", "wma", "mid", "x-flac"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "heif"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "csv", "json", "xml"
    )

    fun resolveMediaType(fileName: String, mimeType: String? = null): MediaType {
        if (!mimeType.isNullOrBlank()) {
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            when {
                lowerMime.startsWith("video/") -> return MediaType.VIDEO
                lowerMime.startsWith("audio/") -> return MediaType.AUDIO
                lowerMime.startsWith("image/") -> return MediaType.IMAGE
                lowerMime.startsWith("text/") ||
                lowerMime.contains("pdf") ||
                lowerMime.contains("document") ||
                lowerMime.contains("sheet") ||
                lowerMime.contains("presentation") -> return MediaType.DOCUMENT
            }
        }

        val extension = getExtension(fileName).lowercase(Locale.ROOT)
        return when {
            VIDEO_EXTENSIONS.contains(extension) -> MediaType.VIDEO
            AUDIO_EXTENSIONS.contains(extension) -> MediaType.AUDIO
            IMAGE_EXTENSIONS.contains(extension) -> MediaType.IMAGE
            DOCUMENT_EXTENSIONS.contains(extension) -> MediaType.DOCUMENT
            else -> MediaType.OTHER
        }
    }

    fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }
}
