package com.localstream.app.data.repository

import com.localstream.app.data.database.FileDao
import com.localstream.app.data.database.FileEntity
import com.localstream.app.domain.model.MediaFile
import com.localstream.app.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FileRepository(private val fileDao: FileDao) {

    val allFilesFlow: Flow<List<MediaFile>> = fileDao.getAllFlow().map { list ->
        list.map { it.toDomain() }
    }

    val totalCountFlow: Flow<Int> = fileDao.getCountFlow()

    fun getFilesByType(type: MediaType): Flow<List<MediaFile>> {
        return fileDao.getByType(type.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getFilesByFolder(folder: String): Flow<List<MediaFile>> {
        return fileDao.getByFolder(folder).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllFolders(): Flow<List<String>> {
        return fileDao.getAllFolders()
    }

    fun getRecentFiles(limit: Int = 20): Flow<List<MediaFile>> {
        return fileDao.getRecent(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchFiles(query: String): Flow<List<MediaFile>> {
        return fileDao.search(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun searchFilesSync(query: String): List<MediaFile> {
        return fileDao.searchSync(query).map { it.toDomain() }
    }

    suspend fun getAllFilesSync(): List<MediaFile> {
        return fileDao.getAll().map { it.toDomain() }
    }

    suspend fun getFileById(id: Long): MediaFile? {
        return fileDao.getById(id)?.toDomain()
    }

    suspend fun getFileByPath(path: String): MediaFile? {
        return fileDao.getByPath(path)?.toDomain()
    }

    suspend fun insertFile(file: MediaFile): Long {
        return fileDao.insert(FileEntity.fromDomain(file))
    }

    suspend fun insertFiles(files: List<MediaFile>): List<Long> {
        return fileDao.insertAll(files.map { FileEntity.fromDomain(it) })
    }

    suspend fun deleteFile(file: MediaFile) {
        fileDao.delete(FileEntity.fromDomain(file))
    }

    suspend fun deleteFileById(id: Long) {
        fileDao.deleteById(id)
    }

    suspend fun deleteAllFiles() {
        fileDao.deleteAll()
    }

    suspend fun getCount(): Int {
        return fileDao.getCount()
    }
}
