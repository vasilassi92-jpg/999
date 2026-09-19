package com.localstream.app.server

import android.content.Context
import android.net.Uri
import android.util.Log
import com.localstream.app.data.repository.FileRepository
import com.localstream.app.domain.model.MediaFile
import com.localstream.app.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicInteger

class LocalHttpServer(
    private val context: Context,
    private val fileRepository: FileRepository
) {
    private val tag = "LocalHttpServer"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val activeClientsCounter = AtomicInteger(0)
    private var startTimeMillis = 0L

    var currentPort: Int = 8080
        private set

    var requirePassword: Boolean = false
    var passwordHash: String? = null

    // Callback for receiver events (e.g. play remote media)
    var onRemotePlayRequested: ((MediaFile) -> Unit)? = null

    val uptimeMillis: Long
        get() = if (_isRunning.value && startTimeMillis > 0L) {
            System.currentTimeMillis() - startTimeMillis
        } else {
            0L
        }

    fun start(port: Int = 8080): Boolean {
        if (_isRunning.value) return true
        currentPort = port

        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress("0.0.0.0", port))
            serverSocket = socket
            startTimeMillis = System.currentTimeMillis()
            _isRunning.value = true

            serverJob = scope.launch {
                Log.i(tag, "LocalStream HTTP server started on port $port")
                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        launch {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.e(tag, "Error accepting client connection", e)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start HTTP server on port $port", e)
            _isRunning.value = false
            return false
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket", e)
        } finally {
            _isRunning.value = false
            _connectedClients.value = 0
            activeClientsCounter.set(0)
            startTimeMillis = 0L
            Log.i(tag, "LocalStream HTTP server stopped")
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        val clientCount = activeClientsCounter.incrementAndGet()
        _connectedClients.value = clientCount

        try {
            socket.soTimeout = 30000 // 30 second socket timeout
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val outputStream = BufferedOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val requestParts = requestLine.split(" ")
            if (requestParts.size < 2) return

            val method = requestParts[0].uppercase()
            val rawPath = requestParts[1]

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim().lowercase()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
                line = reader.readLine()
            }

            // Path Traversal Security Protection
            if (rawPath.contains("..")) {
                sendErrorResponse(outputStream, 403, "Forbidden: Path traversal not allowed")
                return
            }

            val decodedPath = URLDecoder.decode(rawPath, "UTF-8")
            val uri = Uri.parse("http://localhost$decodedPath")
            val path = uri.path ?: "/"

            // Authentication check if enabled
            if (requirePassword && !passwordHash.isNullOrEmpty()) {
                val authHeader = headers["authorization"]
                val authParam = uri.getQueryParameter("auth")
                val isAuthorized = checkAuth(authHeader, authParam)
                if (!isAuthorized && path != "/api/status") {
                    sendAuthRequiredResponse(outputStream)
                    return
                }
            }

            // Handle endpoints
            when {
                path == "/" || path == "/index.html" -> {
                    sendUtf8Content(outputStream, 200, "text/html; charset=utf-8", WebAssets.INDEX_HTML)
                }
                path == "/style.css" -> {
                    sendUtf8Content(outputStream, 200, "text/css; charset=utf-8", WebAssets.STYLE_CSS)
                }
                path == "/app.js" -> {
                    sendUtf8Content(outputStream, 200, "application/javascript; charset=utf-8", WebAssets.APP_JS)
                }
                path == "/api/status" -> {
                    handleApiStatus(outputStream)
                }
                path == "/api/files" -> {
                    handleApiFiles(outputStream)
                }
                path == "/api/search" -> {
                    val query = uri.getQueryParameter("q") ?: ""
                    handleApiSearch(outputStream, query)
                }
                path == "/api/stream" -> {
                    val fileId = uri.getQueryParameter("id")?.toLongOrNull()
                    if (fileId != null) {
                        handleFileStreaming(fileId, headers["range"], outputStream, isDownload = false)
                    } else {
                        sendErrorResponse(outputStream, 400, "Bad Request: Missing 'id' parameter")
                    }
                }
                path == "/api/download" -> {
                    val fileId = uri.getQueryParameter("id")?.toLongOrNull()
                    if (fileId != null) {
                        handleFileStreaming(fileId, headers["range"], outputStream, isDownload = true)
                    } else {
                        sendErrorResponse(outputStream, 400, "Bad Request: Missing 'id' parameter")
                    }
                }
                path == "/api/receiver/play" && method == "POST" -> {
                    val fileId = uri.getQueryParameter("id")?.toLongOrNull()
                    if (fileId != null) {
                        val file = fileRepository.getFileById(fileId)
                        if (file != null) {
                            onRemotePlayRequested?.invoke(file)
                            sendUtf8Content(outputStream, 200, "application/json", """{"status":"playing","file":"${file.name}"}""")
                        } else {
                            sendErrorResponse(outputStream, 404, "File not found")
                        }
                    } else {
                        sendErrorResponse(outputStream, 400, "Missing id parameter")
                    }
                }
                else -> {
                    sendErrorResponse(outputStream, 404, "Not Found")
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client connection exception: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
            _connectedClients.value = activeClientsCounter.decrementAndGet().coerceAtLeast(0)
        }
    }

    private suspend fun handleApiStatus(out: OutputStream) {
        val totalFiles = fileRepository.getCount()
        val json = JSONObject().apply {
            put("name", "LocalStream")
            put("status", "ONLINE")
            put("version", "1.0.0")
            put("port", currentPort)
            put("localIp", NetworkUtils.getLocalIpAddress())
            put("filesCount", totalFiles)
            put("uptimeMillis", uptimeMillis)
            put("requirePassword", requirePassword)
        }
        sendUtf8Content(out, 200, "application/json; charset=utf-8", json.toString())
    }

    private suspend fun handleApiFiles(out: OutputStream) {
        val files = fileRepository.getAllFilesSync()
        val array = JSONArray()
        for (f in files) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("mediaType", f.mediaType.name)
                put("mimeType", f.mimeType)
                put("size", f.size)
                put("formattedSize", f.formattedSize)
                put("folder", f.folder)
                put("durationMs", f.durationMs)
                put("streamUrl", "/api/stream?id=${f.id}")
                put("downloadUrl", "/api/download?id=${f.id}")
            }
            array.put(obj)
        }
        sendUtf8Content(out, 200, "application/json; charset=utf-8", array.toString())
    }

    private suspend fun handleApiSearch(out: OutputStream, query: String) {
        val files = fileRepository.searchFilesSync(query)
        val array = JSONArray()
        for (f in files) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("mediaType", f.mediaType.name)
                put("mimeType", f.mimeType)
                put("size", f.size)
                put("formattedSize", f.formattedSize)
                put("folder", f.folder)
                put("streamUrl", "/api/stream?id=${f.id}")
                put("downloadUrl", "/api/download?id=${f.id}")
            }
            array.put(obj)
        }
        sendUtf8Content(out, 200, "application/json; charset=utf-8", array.toString())
    }

    private suspend fun handleFileStreaming(
        fileId: Long,
        rangeHeader: String?,
        out: OutputStream,
        isDownload: Boolean
    ) {
        val mediaFile = fileRepository.getFileById(fileId)
        if (mediaFile == null) {
            sendErrorResponse(out, 404, "File Not Found")
            return
        }

        // Open input stream from path or content URI
        val fileOnDisk = File(mediaFile.path)
        val totalLength: Long
        val inputStreamSupplier: () -> InputStream?

        if (fileOnDisk.exists() && fileOnDisk.canRead()) {
            totalLength = fileOnDisk.length()
            inputStreamSupplier = { FileInputStream(fileOnDisk) }
        } else if (mediaFile.uriString.isNotEmpty()) {
            val contentUri = Uri.parse(mediaFile.uriString)
            val pfd = try {
                context.contentResolver.openFileDescriptor(contentUri, "r")
            } catch (e: Exception) {
                null
            }
            if (pfd != null) {
                totalLength = pfd.statSize
                pfd.close()
                inputStreamSupplier = { context.contentResolver.openInputStream(contentUri) }
            } else {
                sendErrorResponse(out, 404, "Unable to access file resource")
                return
            }
        } else {
            sendErrorResponse(out, 404, "File resource unavailable")
            return
        }

        if (totalLength <= 0L) {
            sendErrorResponse(out, 404, "Empty file")
            return
        }

        val mimeType = mediaFile.mimeType.ifBlank { MimeTypeResolver.getMimeType(mediaFile.name) }

        // Parse Range request
        val byteRange: ByteRange?
        try {
            byteRange = RangeRequestParser.parseRange(rangeHeader, totalLength)
        } catch (e: RangeRequestParser.InvalidRangeException) {
            val headers = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                    "Content-Range: bytes */$totalLength\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(headers.toByteArray(Charsets.US_ASCII))
            out.flush()
            return
        }

        val rawInput = inputStreamSupplier()
        if (rawInput == null) {
            sendErrorResponse(out, 500, "Failed to open input stream")
            return
        }

        val responseCode = if (byteRange != null) 206 else 200
        val responseStatus = if (byteRange != null) "206 Partial Content" else "200 OK"
        val startByte = byteRange?.start ?: 0L
        val endByte = byteRange?.end ?: (totalLength - 1L)
        val contentLength = (endByte - startByte) + 1L

        val headerBuilder = StringBuilder()
        headerBuilder.append("HTTP/1.1 $responseStatus\r\n")
        headerBuilder.append("Accept-Ranges: bytes\r\n")
        headerBuilder.append("Content-Type: $mimeType\r\n")
        headerBuilder.append("Content-Length: $contentLength\r\n")
        headerBuilder.append("Access-Control-Allow-Origin: *\r\n")

        if (byteRange != null) {
            headerBuilder.append("Content-Range: ${byteRange.contentRangeHeader}\r\n")
        }

        if (isDownload) {
            val safeName = mediaFile.name.replace("\"", "")
            headerBuilder.append("Content-Disposition: attachment; filename=\"$safeName\"\r\n")
        }

        headerBuilder.append("Connection: keep-alive\r\n\r\n")
        out.write(headerBuilder.toString().toByteArray(Charsets.US_ASCII))
        out.flush()

        // Stream range bytes in chunks
        BufferedInputStream(rawInput).use { stream ->
            if (startByte > 0) {
                var skipped = 0L
                while (skipped < startByte) {
                    val count = stream.skip(startByte - skipped)
                    if (count <= 0) break
                    skipped += count
                }
            }

            val buffer = ByteArray(64 * 1024) // 64KB buffer
            var remaining = contentLength
            while (remaining > 0) {
                val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                val read = stream.read(buffer, 0, toRead)
                if (read == -1) break
                out.write(buffer, 0, read)
                remaining -= read
            }
            out.flush()
        }
    }

    private fun checkAuth(authHeader: String?, authParam: String?): Boolean {
        // Simple token or Basic auth comparison
        val expected = passwordHash ?: return true
        if (authParam == expected) return true
        if (authHeader != null && authHeader.contains(expected)) return true
        return false
    }

    private fun sendUtf8Content(out: OutputStream, code: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val status = if (code == 200) "200 OK" else "$code OK"
        val header = "HTTP/1.1 $status\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun sendErrorResponse(out: OutputStream, code: Int, message: String) {
        val body = """{"error": "$message", "status": $code}"""
        val bytes = body.toByteArray(Charsets.UTF_8)
        val status = when (code) {
            400 -> "400 Bad Request"
            403 -> "403 Forbidden"
            404 -> "404 Not Found"
            416 -> "416 Range Not Satisfiable"
            else -> "500 Internal Server Error"
        }
        val header = "HTTP/1.1 $status\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private fun sendAuthRequiredResponse(out: OutputStream) {
        val body = """{"error": "Authentication required", "status": 401}"""
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 401 Unauthorized\r\n" +
                "WWW-Authenticate: Basic realm=\"LocalStream\"\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }
}
