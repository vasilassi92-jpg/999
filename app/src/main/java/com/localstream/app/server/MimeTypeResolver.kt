package com.localstream.app.server

import android.webkit.MimeTypeMap
import java.util.Locale

object MimeTypeResolver {

    private val MIME_MAP = mapOf(
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "3gp" to "video/3gpp",
        "ts" to "video/mp2t",
        "flv" to "video/x-flv",
        "wmv" to "video/x-ms-wmv",
        "m4v" to "video/x-m4v",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "aac" to "audio/aac",
        "flac" to "audio/flac",
        "ogg" to "audio/ogg",
        "m4a" to "audio/mp4",
        "opus" to "audio/opus",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "pdf" to "application/pdf",
        "txt" to "text/plain",
        "json" to "application/json",
        "html" to "text/html; charset=utf-8",
        "css" to "text/css; charset=utf-8",
        "js" to "application/javascript; charset=utf-8"
    )

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        MIME_MAP[extension]?.let { return it }

        val systemMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return systemMime ?: "application/octet-stream"
    }
}
