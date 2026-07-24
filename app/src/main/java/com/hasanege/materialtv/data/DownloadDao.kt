package com.hasanege.materialtv.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for downloads
 */
@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt ASC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING') ORDER BY createdAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: String): DownloadEntity?
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)
    
    @Update
    suspend fun update(download: DownloadEntity)
    
    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, downloadedBytes: Long, speed: Long)
    
    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String)
    
    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatusOnly(id: String, status: String)
    
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
