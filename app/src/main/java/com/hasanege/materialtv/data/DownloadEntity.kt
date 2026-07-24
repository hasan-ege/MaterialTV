package com.hasanege.materialtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for downloads
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val filePath: String = "",
    val thumbnailUrl: String = "",
    val seriesCoverUrl: String = "", // Dizi kapak fotoğrafı (grup için)
    val contentType: String = "MOVIE", // MOVIE or EPISODE
    val seriesName: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val status: String = "PENDING", // PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    val progress: Int = 0,
    val downloadSpeed: Long = 0,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val duration: Long = 0, // Video süresi (ms)
    val createdAt: Long = System.currentTimeMillis(),
    val error: String = "",
    val imdbId: String = "",
    val skipDbSegmentsJson: String? = null
)
