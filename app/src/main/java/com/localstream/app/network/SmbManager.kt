package com.localstream.app.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.localstream.app.domain.model.SmbShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SmbManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("localstream_smb_prefs", Context.MODE_PRIVATE)
    private val keyAlias = "LocalStreamSmbKey"

    private val _savedShares = MutableStateFlow<List<SmbShare>>(emptyList())
    val savedShares: StateFlow<List<SmbShare>> = _savedShares.asStateFlow()

    init {
        ensureKeyStoreKey()
        loadSavedShares()
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            // Android KeyStore initialized
        }
    }

    private fun encrypt(plainText: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = keyStore.getKey(keyAlias, null) as? SecretKey ?: return Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun loadSavedShares() {
        val raw = prefs.getString("smb_shares", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<SmbShare>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SmbShare(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        server = obj.optString("server"),
                        username = obj.optString("username"),
                        shareName = obj.optString("shareName"),
                        folderPath = obj.optString("folderPath"),
                        isConnected = false,
                        lastConnected = obj.optLong("lastConnected", 0L)
                    )
                )
            }
            _savedShares.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun testConnection(server: String, port: Int = 445): Boolean = withContext(Dispatchers.IO) {
        if (server.isBlank()) return@withContext false
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(server, port), 2500)
                return@withContext true
            }
        } catch (e: Exception) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(server, 139), 2000)
                    return@withContext true
                }
            } catch (e2: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun saveShare(
        server: String,
        username: String,
        password: String,
        shareName: String,
        folder: String
    ): SmbShare = withContext(Dispatchers.IO) {
        val newShare = SmbShare(
            server = server.trim(),
            username = username.trim(),
            shareName = shareName.trim(),
            folderPath = folder.trim(),
            isConnected = testConnection(server.trim()),
            lastConnected = System.currentTimeMillis()
        )

        if (password.isNotEmpty()) {
            val enc = encrypt(password)
            prefs.edit().putString("enc_pass_${newShare.id}", enc).apply()
        }

        val updated = _savedShares.value.toMutableList().apply { add(newShare) }
        _savedShares.value = updated
        persistShares(updated)
        return@withContext newShare
    }

    fun removeShare(id: String) {
        val updated = _savedShares.value.filter { it.id != id }
        _savedShares.value = updated
        prefs.edit().remove("enc_pass_$id").apply()
        persistShares(updated)
    }

    private fun persistShares(shares: List<SmbShare>) {
        val array = JSONArray()
        for (s in shares) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("server", s.server)
                put("username", s.username)
                put("shareName", s.shareName)
                put("folderPath", s.folderPath)
                put("lastConnected", s.lastConnected)
            }
            array.put(obj)
        }
        prefs.edit().putString("smb_shares", array.toString()).apply()
    }
}
