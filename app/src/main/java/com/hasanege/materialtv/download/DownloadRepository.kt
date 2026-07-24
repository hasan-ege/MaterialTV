package com.hasanege.materialtv.download

import android.content.Context
import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.DownloadDao
import com.hasanege.materialtv.data.DownloadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * İndirme veritabanı repository
 */
class DownloadRepository(context: Context) {
    
    private val dao: DownloadDao = AppDatabase.getInstance(context).downloadDao()
    
    /**
     * Tüm indirmeleri getir
     */
    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return dao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }
    
    /**
     * Duruma göre indirmeleri getir
     */
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> {
        return dao.getDownloadsByStatus(status.name).map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }
    
    /**
     * Aktif indirmeleri getir (PENDING + DOWNLOADING)
     */
    fun getActiveDownloads(): Flow<List<DownloadItem>> {
        return dao.getActiveDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }
    
    /**
     * İndirme ekle
     */
    suspend fun insertDownload(item: DownloadItem) {
        dao.insert(item.toEntity())
    }
    
    /**
     * İndirme güncelle
     */
    suspend fun updateDownload(item: DownloadItem) {
        dao.update(item.toEntity())
    }
    
    /**
     * Progress güncelle (optimize edilmiş)
     */
    suspend fun updateProgress(id: String, progress: Int, downloadedBytes: Long, speed: Long) {
        dao.updateProgress(id, progress, downloadedBytes, speed)
    }
    
    /**
     * Durum güncelle
     */
    suspend fun updateStatus(id: String, status: DownloadStatus, error: String? = null) {
        // Error can't be null in the database, use empty string as default
        dao.updateStatus(id, status.name, error ?: "")
    }
    
    /**
     * İndirme sil
     */
    suspend fun deleteDownload(id: String) {
        dao.deleteById(id)
    }
    
    /**
     * ID ile indirme getir
     */
    suspend fun getDownloadById(id: String): DownloadItem? {
        return dao.getById(id)?.toDownloadItem()
    }
    
    /**
     * Bekleyen indirmelerin sayısı
     */
    suspend fun getPendingCount(): Int {
        return dao.getCountByStatus(DownloadStatus.PENDING.name)
    }
    
    /**
     * DownloadEntity -> DownloadItem dönüşümü
     */
    private fun DownloadEntity.toDownloadItem(): DownloadItem {
        return DownloadItem(
            id = this.id,
            title = this.title,
            url = this.url,
            filePath = this.filePath,
            thumbnailUrl = this.thumbnailUrl.takeIf { it.isNotEmpty() },
            seriesCoverUrl = this.seriesCoverUrl.takeIf { it.isNotEmpty() },
            contentType = try { 
                ContentType.valueOf(this.contentType) 
            } catch (_: Exception) { 
                ContentType.MOVIE 
            },
            seriesName = this.seriesName.takeIf { it.isNotEmpty() },
            seasonNumber = this.seasonNumber.takeIf { it > 0 },
            episodeNumber = this.episodeNumber.takeIf { it > 0 },
            status = try { 
                DownloadStatus.valueOf(this.status) 
            } catch (_: Exception) { 
                DownloadStatus.PENDING 
            },
            progress = this.progress,
            downloadedBytes = this.downloadedBytes,
            totalBytes = this.fileSize,
            speed = this.downloadSpeed,
            duration = this.duration,
            createdAt = this.createdAt,
            error = this.error.takeIf { it.isNotEmpty() },
            imdbId = this.imdbId.takeIf { it.isNotEmpty() },
            skipDbSegments = this.skipDbSegmentsJson?.takeIf { it.isNotEmpty() }?.let { jsonStr ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer>(jsonStr)
                } catch (_: Exception) {
                    null
                }
            }
        )
    }
    
    /**
     * DownloadItem -> DownloadEntity dönüşümü
     */
    private fun DownloadItem.toEntity(): DownloadEntity {
        return DownloadEntity(
            id = this.id,
            title = this.title,
            url = this.url,
            filePath = this.filePath,
            thumbnailUrl = this.thumbnailUrl ?: "",
            seriesCoverUrl = this.seriesCoverUrl ?: "",
            contentType = this.contentType.name,
            seriesName = this.seriesName ?: "",
            seasonNumber = this.seasonNumber ?: 0,
            episodeNumber = this.episodeNumber ?: 0,
            status = this.status.name,
            progress = this.progress,
            downloadedBytes = this.downloadedBytes,
            fileSize = this.totalBytes,
            downloadSpeed = this.speed,
            duration = this.duration,
            createdAt = this.createdAt,
            error = this.error ?: "",
            imdbId = this.imdbId ?: "",
            skipDbSegmentsJson = this.skipDbSegments?.let {
                try {
                    kotlinx.serialization.json.Json.encodeToString(com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer.serializer(), it)
                } catch (_: Exception) {
                    null
                }
            }
        )
    }
}
