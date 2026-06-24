package com.hasanege.materialtv.download

import android.content.Context
import android.os.Environment
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * İndirme yöneticisi interface
 */
interface DownloadManager {
    
    /**
     * Mevcut indirmeleri tara ve veritabanına ekle
     * @return Bulunan ve eklenen öğe sayısı
     */
    suspend fun scanExistingDownloads(): Int
    
    /**
     * Film indirme başlat
     */
    fun startDownload(movie: VodItem)
    
    /**
     * Bölüm indirme başlat
     */
    fun startDownload(episode: Episode, seriesName: String, seasonNumber: Int, episodeNumber: Int, seriesCoverUrl: String? = null)
    
    /**
     * Tüm sezonu sıralı olarak indir (20ms gecikme ile)
     */
    fun downloadSeason(seriesName: String, seasonNumber: Int, episodes: List<Episode>, seriesCoverUrl: String? = null)
    
    /**
     * İndirmeyi duraklat
     */
    fun pauseDownload(id: String)
    
    /**
     * İndirmeye devam et
     */
    fun resumeDownload(id: String)
    
    /**
     * İndirmeyi iptal et
     */
    fun cancelDownload(id: String)
    
    /**
     * İndirmeyi sil
     */
    fun deleteDownload(id: String)
    
    /**
     * İndirmeyi yeniden adlandır (Sadece görüntüyü değiştirir, dosyayı değil)
     */
    fun renameDownload(id: String, newTitle: String)
    
    /**
     * Tüm indirmeleri getir
     */
    val downloads: Flow<List<DownloadItem>>
    
    /**
     * Aktif indirmeleri getir
     */
    val activeDownloads: Flow<List<DownloadItem>>
    
    /**
     * Tarama ve işlem durumu mesajları
     */
    val scanStatus: kotlinx.coroutines.flow.StateFlow<String?>
    
    companion object {
        private const val BASE_FOLDER = "MaterialTV"
        private const val MOVIES_FOLDER = "Movies"
        private const val SERIES_FOLDER = "Series"
        
        /**
         * Film için indirme yolu oluştur
         * Downloads/MaterialTV/Movies/FilmAdi.mp4
         */
        fun getMovieFilePath(context: Context, title: String): String {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val movieDir = File(downloadsDir, "$BASE_FOLDER/$MOVIES_FOLDER")
            if (!movieDir.exists()) {
                movieDir.mkdirs()
            }
            val safeTitle = sanitizeFileName(title)
            return File(movieDir, "$safeTitle.mp4").absolutePath
        }
        
        fun getEpisodeFilePath(context: Context, seriesName: String, seasonNumber: Int, episodeNumber: Int, episodeTitle: String?): String {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            // Cleanup series name to be safe for file system
            val safeSeriesName = sanitizeFileName(seriesName)
            
            // New Structure: MaterialTV/[SeriesName]/S01/
            // Removed intermediate "Series" folder for simplicity
            val seasonDir = File(downloadsDir, "$BASE_FOLDER/$safeSeriesName/S${seasonNumber.toString().padStart(2, '0')}")
            
            if (!seasonDir.exists()) {
                seasonDir.mkdirs()
            }
            val episodeFileName = if (episodeTitle != null) {
                // E01_EpisodeTitle.mp4
                "E${episodeNumber.toString().padStart(2, '0')}_${sanitizeFileName(episodeTitle)}"
            } else {
                // E01.mp4
                "E${episodeNumber.toString().padStart(2, '0')}"
            }
            return File(seasonDir, "$episodeFileName.mp4").absolutePath
        }
        
        /**
         * Film için stream URL oluştur
         */
        fun getMovieStreamUrl(context: Context, streamId: Int?, extension: String?): String? {
            if (streamId == null) return null
            val serverUrl = SessionManager.serverUrl ?: return null
            val username = SessionManager.username ?: return null
            val password = SessionManager.password ?: return null
            val ext = extension ?: "mp4"
            // Ensure serverUrl ends with /
            val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            return "${baseUrl}movie/$username/$password/$streamId.$ext"
        }
        
        /**
         * Bölüm için stream URL oluştur
         */
        fun getEpisodeStreamUrl(context: Context, episodeId: String?, containerExtension: String?): String? {
            if (episodeId == null) return null
            val serverUrl = SessionManager.serverUrl ?: return null
            val username = SessionManager.username ?: return null
            val password = SessionManager.password ?: return null
            val ext = containerExtension ?: "mp4"
            // Ensure serverUrl ends with /
            val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            return "${baseUrl}series/$username/$password/$episodeId.$ext"
        }
        
        /**
         * Dosya adını güvenli hale getir
         */
        fun sanitizeFileName(name: String): String {
            return name
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), "_")
                .take(100)
        }
    }
}
