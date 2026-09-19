package com.localstream.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FileEntity>): List<Long>

    @Update
    suspend fun update(entity: FileEntity)

    @Delete
    suspend fun delete(entity: FileEntity)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_files")
    suspend fun deleteAll()

    @Query("SELECT * FROM media_files ORDER BY modifiedDate DESC")
    fun getAllFlow(): Flow<List<FileEntity>>

    @Query("SELECT * FROM media_files ORDER BY modifiedDate DESC")
    suspend fun getAll(): List<FileEntity>

    @Query("SELECT * FROM media_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FileEntity?

    @Query("SELECT * FROM media_files WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): FileEntity?

    @Query("SELECT * FROM media_files WHERE mediaType = :mediaType ORDER BY modifiedDate DESC")
    fun getByType(mediaType: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM media_files WHERE folder = :folder ORDER BY name ASC")
    fun getByFolder(folder: String): Flow<List<FileEntity>>

    @Query("SELECT DISTINCT folder FROM media_files ORDER BY folder ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM media_files ORDER BY modifiedDate DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<FileEntity>>

    @Query("SELECT * FROM media_files WHERE name LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM media_files WHERE name LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchSync(query: String): List<FileEntity>

    @Query("SELECT COUNT(*) FROM media_files")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_files")
    suspend fun getCount(): Int
}
