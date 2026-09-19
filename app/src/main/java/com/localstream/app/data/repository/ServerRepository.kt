package com.localstream.app.data.repository

import com.localstream.app.domain.model.ServerState
import com.localstream.app.server.ServerController
import kotlinx.coroutines.flow.StateFlow

class ServerRepository(private val serverController: ServerController) {

    val serverState: StateFlow<ServerState> = serverController.serverState

    fun startServer(port: Int = 8080): Boolean {
        return serverController.startServer(port)
    }

    fun stopServer() {
        serverController.stopServer()
    }
}
