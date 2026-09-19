package com.localstream.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "localstream_settings")

data class AppSettings(
    val serverPort: Int = 8080,
    val autoStart: Boolean = false,
    val stopOnWifiDisconnect: Boolean = true,
    val requirePassword: Boolean = false,
    val isPasswordSet: Boolean = false,
    val localNetworkOnly: Boolean = true,
    val darkMode: Boolean = true,
    val amoledMode: Boolean = false,
    val receiverEnabled: Boolean = false,
    val bufferSizeKb: Int = 512,
    val streamingQuality: String = "Original",
    val selectedFolders: Set<String> = emptySet(),
    val upnpEnabled: Boolean = true,
    val dlnaEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val SERVER_PORT = intPreferencesKey("server_port")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val STOP_ON_WIFI_DISCONNECT = booleanPreferencesKey("stop_on_wifi_disconnect")
        val REQUIRE_PASSWORD = booleanPreferencesKey("require_password")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val LOCAL_NETWORK_ONLY = booleanPreferencesKey("local_network_only")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val RECEIVER_ENABLED = booleanPreferencesKey("receiver_enabled")
        val BUFFER_SIZE_KB = intPreferencesKey("buffer_size_kb")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val SELECTED_FOLDERS = stringSetPreferencesKey("selected_folders")
        val UPNP_ENABLED = booleanPreferencesKey("upnp_enabled")
        val DLNA_ENABLED = booleanPreferencesKey("dlna_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val port = prefs[PreferenceKeys.SERVER_PORT] ?: 8080
        val autoStart = prefs[PreferenceKeys.AUTO_START] ?: false
        val stopWifi = prefs[PreferenceKeys.STOP_ON_WIFI_DISCONNECT] ?: true
        val requirePass = prefs[PreferenceKeys.REQUIRE_PASSWORD] ?: false
        val passHash = prefs[PreferenceKeys.PASSWORD_HASH]
        val localOnly = prefs[PreferenceKeys.LOCAL_NETWORK_ONLY] ?: true
        val dark = prefs[PreferenceKeys.DARK_MODE] ?: true
        val amoled = prefs[PreferenceKeys.AMOLED_MODE] ?: false
        val receiver = prefs[PreferenceKeys.RECEIVER_ENABLED] ?: false
        val buffer = prefs[PreferenceKeys.BUFFER_SIZE_KB] ?: 512
        val quality = prefs[PreferenceKeys.STREAMING_QUALITY] ?: "Original"
        val folders = prefs[PreferenceKeys.SELECTED_FOLDERS] ?: emptySet()
        val upnp = prefs[PreferenceKeys.UPNP_ENABLED] ?: true
        val dlna = prefs[PreferenceKeys.DLNA_ENABLED] ?: true

        AppSettings(
            serverPort = port,
            autoStart = autoStart,
            stopOnWifiDisconnect = stopWifi,
            requirePassword = requirePass,
            isPasswordSet = !passHash.isNullOrEmpty(),
            localNetworkOnly = localOnly,
            darkMode = dark,
            amoledMode = amoled,
            receiverEnabled = receiver,
            bufferSizeKb = buffer,
            streamingQuality = quality,
            selectedFolders = folders,
            upnpEnabled = upnp,
            dlnaEnabled = dlna
        )
    }

    suspend fun setServerPort(port: Int) {
        context.dataStore.edit { it[PreferenceKeys.SERVER_PORT] = port }
    }

    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.AUTO_START] = enabled }
    }

    suspend fun setStopOnWifiDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.STOP_ON_WIFI_DISCONNECT] = enabled }
    }

    suspend fun setRequirePassword(required: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.REQUIRE_PASSWORD] = required }
    }

    suspend fun setServerPassword(password: String) {
        val hash = hashPassword(password)
        context.dataStore.edit { it[PreferenceKeys.PASSWORD_HASH] = hash }
    }

    suspend fun verifyServerPassword(password: String): Boolean {
        var isValid = false
        context.dataStore.data.collect { prefs ->
            val storedHash = prefs[PreferenceKeys.PASSWORD_HASH]
            isValid = storedHash != null && storedHash == hashPassword(password)
            return@collect
        }
        return isValid
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.DARK_MODE] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.AMOLED_MODE] = enabled }
    }

    suspend fun setReceiverEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.RECEIVER_ENABLED] = enabled }
    }

    suspend fun setBufferSize(sizeKb: Int) {
        context.dataStore.edit { it[PreferenceKeys.BUFFER_SIZE_KB] = sizeKb }
    }

    suspend fun setStreamingQuality(quality: String) {
        context.dataStore.edit { it[PreferenceKeys.STREAMING_QUALITY] = quality }
    }

    suspend fun addSelectedFolder(folderUriOrPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferenceKeys.SELECTED_FOLDERS] ?: emptySet()
            prefs[PreferenceKeys.SELECTED_FOLDERS] = current + folderUriOrPath
        }
    }

    suspend fun removeSelectedFolder(folderUriOrPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferenceKeys.SELECTED_FOLDERS] ?: emptySet()
            prefs[PreferenceKeys.SELECTED_FOLDERS] = current - folderUriOrPath
        }
    }

    suspend fun setDlnaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.DLNA_ENABLED] = enabled }
    }

    suspend fun setUpnpEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.UPNP_ENABLED] = enabled }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
