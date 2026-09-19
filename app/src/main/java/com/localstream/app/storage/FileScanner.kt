package com.localstream.app.storage

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.localstream.app.data.repository.FileRepository
import com.localstream.app.domain.model.MediaFile
import com.localstream.app.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class ScanProgress(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val currentFile: String = "",
    val error: String? = null
)

class FileScanner(
    private val context: Context,
    private val fileRepository: FileRepository
) {
    private val tag = "FileScanner"

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    suspend fun scanAllMedia(customFolders: Set<String> = emptySet()): Int = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgress(isScanning = true, scannedCount = 0, currentFile = "Starting media scan...")

        val scannedFiles = mutableListOf<MediaFile>()

        try {
            // 1. Scan Videos via MediaStore
            scanMediaStoreVideos(scannedFiles)

            // 2. Scan Audio via MediaStore
            scanMediaStoreAudio(scannedFiles)

            // 3. Scan Images via MediaStore
            scanMediaStoreImages(scannedFiles)

            // 4. Scan Documents & Downloads via MediaStore Files
            scanMediaStoreDocuments(scannedFiles)

            // 5. Scan standard app & device media folders
            scanStandardDirectories(scannedFiles)

            // 6. Scan user-specified custom folder paths
            for (folderPath in customFolders) {
                val folder = File(folderPath)
                if (folder.exists() && folder.isDirectory && folder.canRead()) {
                    scanDirectoryRecursive(folder, scannedFiles)
                }
            }

            // Deduplicate by path
            val uniqueFiles = scannedFiles.distinctBy { it.path.ifEmpty { it.uriString } }

            // Save to Room Database
            if (uniqueFiles.isNotEmpty()) {
                fileRepository.insertFiles(uniqueFiles)
            }

            val finalCount = uniqueFiles.size
            _scanProgress.value = ScanProgress(
                isScanning = false,
                scannedCount = finalCount,
                currentFile = "Scan completed. Found $finalCount items."
            )
            Log.i(tag, "Scan completed with $finalCount items")
            return@withContext finalCount
        } catch (e: Exception) {
            Log.e(tag, "Error during media scan", e)
            _scanProgress.value = ScanProgress(
                isScanning = false,
                scannedCount = scannedFiles.size,
                error = e.localizedMessage ?: "Unknown scan error"
            )
            return@withContext scannedFiles.size
        }
    }

    private fun scanMediaStoreVideos(output: MutableList<MediaFile>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        ) ?: return

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
            val mimeCol = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val modCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol) ?: "video_$id"
                val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                val mime = if (mimeCol >= 0) it.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                val size = it.getLong(sizeCol)
                val modDate = it.getLong(modCol) * 1000L
                val duration = if (durCol >= 0) it.getLong(durCol) else 0L

                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folder = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Videos" else "Videos"

                output.add(
                    MediaFile(
                        name = name,
                        path = path,
                        uriString = uri.toString(),
                        mimeType = mime,
                        size = size,
                        modifiedDate = modDate,
                        folder = folder,
                        mediaType = MediaType.VIDEO,
                        durationMs = duration
                    )
                )
            }
        }
    }

    private fun scanMediaStoreAudio(output: MutableList<MediaFile>) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        ) ?: return

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val mimeCol = it.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val modCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val durCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol) ?: "audio_$id"
                val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                val mime = if (mimeCol >= 0) it.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                val size = it.getLong(sizeCol)
                val modDate = it.getLong(modCol) * 1000L
                val duration = if (durCol >= 0) it.getLong(durCol) else 0L

                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val folder = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Music" else "Music"

                output.add(
                    MediaFile(
                        name = name,
                        path = path,
                        uriString = uri.toString(),
                        mimeType = mime,
                        size = size,
                        modifiedDate = modDate,
                        folder = folder,
                        mediaType = MediaType.AUDIO,
                        durationMs = duration
                    )
                )
            }
        }
    }

    private fun scanMediaStoreImages(output: MutableList<MediaFile>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        ) ?: return

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
            val mimeCol = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val modCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol) ?: "image_$id"
                val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                val mime = if (mimeCol >= 0) it.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                val size = it.getLong(sizeCol)
                val modDate = it.getLong(modCol) * 1000L

                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val folder = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Images" else "Images"

                output.add(
                    MediaFile(
                        name = name,
                        path = path,
                        uriString = uri.toString(),
                        mimeType = mime,
                        size = size,
                        modifiedDate = modDate,
                        folder = folder,
                        mediaType = MediaType.IMAGE
                    )
                )
            }
        }
    }

    private fun scanMediaStoreDocuments(output: MutableList<MediaFile>) {
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val mimeConditions = arrayOf(
                "application/pdf",
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            val selection = mimeConditions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.MIME_TYPE} = ?" }

            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                mimeConditions,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            ) ?: return

            cursor.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val mimeCol = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val modCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "doc_$id"
                    val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) it.getString(mimeCol) ?: "application/pdf" else "application/pdf"
                    val size = it.getLong(sizeCol)
                    val modDate = it.getLong(modCol) * 1000L

                    val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    val folder = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Documents" else "Documents"

                    output.add(
                        MediaFile(
                            name = name,
                            path = path,
                            uriString = uri.toString(),
                            mimeType = mime,
                            size = size,
                            modifiedDate = modDate,
                            folder = folder,
                            mediaType = MediaType.DOCUMENT
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Documents query exception: ${e.message}")
        }
    }

    private fun scanStandardDirectories(output: MutableList<MediaFile>) {
        val dirsToScan = mutableListOf<File?>()

        try {
            dirsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            dirsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            dirsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
            dirsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            dirsToScan.add(context.getExternalFilesDir(null))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (dir in dirsToScan) {
            if (dir != null && dir.exists() && dir.isDirectory && dir.canRead()) {
                scanDirectoryRecursive(dir, output, depth = 0, maxDepth = 3)
            }
        }
    }

    private fun scanDirectoryRecursive(
        directory: File,
        output: MutableList<MediaFile>,
        depth: Int = 0,
        maxDepth: Int = 4
    ) {
        if (depth > maxDepth) return
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryRecursive(file, output, depth + 1, maxDepth)
                }
            } else if (file.isFile && file.length() > 0 && !file.name.startsWith(".")) {
                val mediaType = FileTypeResolver.resolveMediaType(file.name)
                output.add(
                    MediaFile(
                        name = file.name,
                        path = file.absolutePath,
                        uriString = "",
                        mimeType = FileTypeResolver.getExtension(file.name),
                        size = file.length(),
                        modifiedDate = file.lastModified(),
                        folder = directory.name,
                        mediaType = mediaType
                    )
                )
            }
        }
    }
}
