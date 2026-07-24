package com.hasanege.materialtv.download

import java.util.UUID

/**
 * İndirme durumu
 */
import kotlinx.serialization.Serializable

/**
 * İndirme durumu
 */
@Serializable
enum class DownloadStatus {
    PENDING,      // Kuyrukta bekliyor
    DOWNLOADING,  // İndiriliyor
    PAUSED,       // Duraklatıldı
    COMPLETED,    // Tamamlandı
    FAILED,       // Başarısız
    CANCELLED     // İptal edildi
}

/**
 * İçerik türü
 */
@Serializable
enum class ContentType {
    MOVIE,
    EPISODE
}

/**
 * İndirme öğesi veri modeli
 */
@Serializable
data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val filePath: String,
    val thumbnailUrl: String? = null,
    val seriesCoverUrl: String? = null, // Dizi kapak fotoğrafı (grup için)
    val contentType: ContentType,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,           // 0-100
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speed: Long = 0L,            // bytes/sec
    val duration: Long = 0L,         // Video süresi (ms)
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null,
    val imdbId: String? = null,
    val skipDbSegments: com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer? = null
) {
    /**
     * İnsan okunabilir dosya boyutu
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * İnsan okunabilir indirme hızı
     */
    fun formatSpeed(): String {
        return when {
            speed < 1024 -> "$speed B/s"
            speed < 1024 * 1024 -> "%.1f KB/s".format(speed / 1024.0)
            else -> "%.1f MB/s".format(speed / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Video süresini format (HH:MM:SS veya MM:SS)
     */
    fun formatDuration(): String? {
        if (duration <= 0) return null
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
    
    /**
     * Kalan süre tahmini
     */
    fun estimatedTimeRemaining(): String? {
        if (speed <= 0 || totalBytes <= 0) return null
        val remainingBytes = totalBytes - downloadedBytes
        val seconds = remainingBytes / speed
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
    
    /**
     * Görüntülenecek alt başlık (dizi için S01E05 formatı)
     */
    fun displaySubtitle(): String? {
        return if (contentType == ContentType.EPISODE && seasonNumber != null && episodeNumber != null) {
            "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        } else {
            seriesName
        }
    }
}
