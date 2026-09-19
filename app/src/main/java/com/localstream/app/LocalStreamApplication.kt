package com.localstream.app

import android.app.Application
import com.localstream.app.data.database.AppDatabase
import com.localstream.app.data.preferences.SettingsRepository
import com.localstream.app.data.repository.FileRepository
import com.localstream.app.data.repository.ServerRepository
import com.localstream.app.media.MediaPlayerManager
import com.localstream.app.media.MediaQueueManager
import com.localstream.app.network.DeviceDiscovery
import com.localstream.app.network.DlnaDiscovery
import com.localstream.app.network.SmbManager
import com.localstream.app.network.UpnpDiscovery
import com.localstream.app.server.LocalHttpServer
import com.localstream.app.server.ServerController
import com.localstream.app.storage.FileScanner
import com.localstream.app.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LocalStreamApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var fileRepository: FileRepository
        private set

    lateinit var serverRepository: ServerRepository
        private set

    lateinit var storageManager: StorageManager
        private set

    lateinit var fileScanner: FileScanner
        private set

    lateinit var localHttpServer: LocalHttpServer
        private set

    lateinit var serverController: ServerController
        private set

    lateinit var queueManager: MediaQueueManager
        private set

    lateinit var playerManager: MediaPlayerManager
        private set

    lateinit var upnpDiscovery: UpnpDiscovery
        private set

    lateinit var deviceDiscovery: DeviceDiscovery
        private set

    lateinit var dlnaDiscovery: DlnaDiscovery
        private set

    lateinit var smbManager: SmbManager
        private set

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // 1. Storage & Database
        database = AppDatabase.getInstance(this)
        fileRepository = FileRepository(database.fileDao())
        settingsRepository = SettingsRepository(this)
        storageManager = StorageManager(this)
        fileScanner = FileScanner(this, fileRepository)

        // 2. Media Player & Queue
        queueManager = MediaQueueManager()
        playerManager = MediaPlayerManager(this, queueManager)

        // 3. HTTP Server & Controller
        localHttpServer = LocalHttpServer(this, fileRepository).apply {
            onRemotePlayRequested = { media ->
                appScope.launch(Dispatchers.Main) {
                    playerManager.playMedia(media)
                }
            }
        }
        serverController = ServerController(this, localHttpServer, settingsRepository, fileRepository)
        serverRepository = ServerRepository(serverController)

        // 4. Network Discovery & Protocols
        upnpDiscovery = UpnpDiscovery(this)
        deviceDiscovery = DeviceDiscovery(this, upnpDiscovery)
        dlnaDiscovery = DlnaDiscovery(upnpDiscovery)
        smbManager = SmbManager(this)

        // 5. Initial background tasks: initial media scan & check auto-start
        appScope.launch {
            // Initial media scan
            fileScanner.scanAllMedia()

            // Check auto-start preference
            val settings = settingsRepository.settingsFlow.firstOrNull()
            if (settings?.autoStart == true) {
                serverController.startServer(settings.serverPort)
            }
        }
    }
}
