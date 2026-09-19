package com.localstream.app.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.localstream.app.domain.model.MediaFile
import java.io.File

class StorageManager(private val context: Context) {

    fun deleteMediaFile(file: MediaFile): Boolean {
        try {
            val localFile = File(file.path)
            if (localFile.exists()) {
                return localFile.delete()
            }
            if (file.uriString.isNotEmpty()) {
                val uri = Uri.parse(file.uriString)
                val rows = context.contentResolver.delete(uri, null, null)
                return rows > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun renameMediaFile(file: MediaFile, newName: String): MediaFile? {
        try {
            val oldFile = File(file.path)
            if (oldFile.exists()) {
                val parent = oldFile.parentFile ?: return null
                val ext = FileTypeResolver.getExtension(file.name)
                val finalName = if (!newName.contains(".") && ext.isNotEmpty()) "$newName.$ext" else newName
                val newFile = File(parent, finalName)
                if (oldFile.renameTo(newFile)) {
                    return file.copy(
                        name = finalName,
                        path = newFile.absolutePath,
                        modifiedDate = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun shareMediaFile(file: MediaFile) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                val uri = if (file.uriString.isNotEmpty()) {
                    Uri.parse(file.uriString)
                } else {
                    val localFile = File(file.path)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        localFile
                    )
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                type = file.mimeType.ifBlank { "*/*" }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Share ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createFolder(parentPath: String, folderName: String): Boolean {
        return try {
            val dir = File(parentPath, folderName)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
