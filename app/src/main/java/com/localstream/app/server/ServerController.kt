package com.localstream.app.server

import android.content.Context
import android.content.Intent
import com.localstream.app.data.preferences.SettingsRepository
import com.localstream.app.data.repository.FileRepository
import com.localstream.app.domain.model.ServerState
import com.localstream.app.network.NetworkUtils
import com.localstream.app.service.StreamingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerController(
    private val context: Context,
    private val httpServer: LocalHttpServer,
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private var uptimeTickerJob: Job? = null

    init {
        scope.launch {
            combine(
                httpServer.isRunning,
                httpServer.connectedClients,
                fileRepository.totalCountFlow,
                settingsRepository.settingsFlow
            ) { isRunning, clients, filesCount, settings ->
                val ip = if (isRunning) NetworkUtils.getLocalIpAddress() else "127.0.0.1"
                ServerState(
                    isRunning = isRunning,
                    ipAddress = ip,
                    port = httpServer.currentPort,
                    indexedFilesCount = filesCount,
                    uptimeMillis = httpServer.uptimeMillis,
                    connectedClients = clients,
                    isAuthRequired = settings.requirePassword,
                    currentSubnet = NetworkUtils.getSubnetPrefix(ip)
                )
            }.collect { state ->
                _serverState.value = state
            }
        }
    }

    fun startServer(port: Int = 8080): Boolean {
        val success = httpServer.start(port)
        if (success) {
            val ip = NetworkUtils.getLocalIpAddress()
            _serverState.value = _serverState.value.copy(
                isRunning = true,
                ipAddress = ip,
                port = port
            )
            startUptimeTicker()
            try {
                val serviceIntent = Intent(context, StreamingForegroundService::class.java).apply {
                    action = StreamingForegroundService.ACTION_START
                    putExtra(StreamingForegroundService.EXTRA_PORT, port)
                    putExtra(StreamingForegroundService.EXTRA_IP, ip)
                }
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return success
    }

    fun stopServer() {
        httpServer.stop()
        stopUptimeTicker()
        _serverState.value = _serverState.value.copy(
            isRunning = false,
            uptimeMillis = 0L,
            connectedClients = 0
        )
        try {
            val serviceIntent = Intent(context, StreamingForegroundService::class.java).apply {
                action = StreamingForegroundService.ACTION_STOP
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startUptimeTicker() {
        uptimeTickerJob?.cancel()
        uptimeTickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (httpServer.isRunning.value) {
                    _serverState.value = _serverState.value.copy(
                        uptimeMillis = httpServer.uptimeMillis
                    )
                }
            }
        }
    }

    private fun stopUptimeTicker() {
        uptimeTickerJob?.cancel()
        uptimeTickerJob = null
    }
}
