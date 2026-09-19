package com.localstream.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.localstream.app.domain.model.MediaFile
import com.localstream.app.domain.model.MediaType

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["mediaType"]),
        Index(value = ["folder"]),
        Index(value = ["name"])
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val uriString: String,
    val mimeType: String,
    val size: Long,
    val modifiedDate: Long,
    val folder: String,
    val mediaType: String,
    val durationMs: Long = 0L
) {
    fun toDomain(): MediaFile {
        return MediaFile(
            id = id,
            name = name,
            path = path,
            uriString = uriString,
            mimeType = mimeType,
            size = size,
            modifiedDate = modifiedDate,
            folder = folder,
            mediaType = try {
                MediaType.valueOf(mediaType)
            } catch (e: Exception) {
                MediaType.OTHER
            },
            durationMs = durationMs
        )
    }

    companion object {
        fun fromDomain(file: MediaFile): FileEntity {
            return FileEntity(
                id = file.id,
                name = file.name,
                path = file.path,
                uriString = file.uriString,
                mimeType = file.mimeType,
                size = file.size,
                modifiedDate = file.modifiedDate,
                folder = file.folder,
                mediaType = file.mediaType.name,
                durationMs = file.durationMs
            )
        }
    }
}
