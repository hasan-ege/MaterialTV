package com.hasanege.materialtv.download

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Dizi kapak fotoğrafı ve bölüm thumbnail'larını indiren yardımcı sınıf.
 * Ana indirme sisteminden bağımsız çalışır.
 */
object CoverImageDownloader {
    
    private const val TAG = "CoverImageDownloader"
    private const val BASE_FOLDER = "MaterialTV"
    private const val SERIES_FOLDER = "Series"
    private const val MOVIES_FOLDER = "Movies"
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    /**
     * Dizi kapak fotoğrafını indir
     * Kayıt yeri: Downloads/MaterialTV/Series/[DiziAdi]/cover.png
     */
    fun downloadSeriesCover(seriesName: String, coverUrl: String?) {
        if (coverUrl.isNullOrEmpty()) {
            Log.w(TAG, "Series cover URL is empty for: $seriesName")
            return
        }
        
        scope.launch {
            try {
                val safeSeriesName = sanitizeFileName(seriesName)
                val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val seriesDir = File(downloadsDir, "$BASE_FOLDER/$SERIES_FOLDER/$safeSeriesName")
                
                if (!seriesDir.exists()) {
                    seriesDir.mkdirs()
                }
                
                val coverFile = File(seriesDir, "cover.png")
                
                // Zaten varsa indirme
                if (coverFile.exists() && coverFile.length() > 0) {
                    Log.d(TAG, "Series cover already exists: ${coverFile.absolutePath}")
                    return@launch
                }
                
                downloadImage(coverUrl, coverFile)
                Log.d(TAG, "Series cover downloaded: ${coverFile.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading series cover for $seriesName: ${e.message}")
            }
        }
    }
    
    /**
     * Bölüm thumbnail'ını indir
     * Kayıt yeri: Downloads/MaterialTV/Series/[DiziAdi]/S01/E01_thumbnail.png
     */
    fun downloadEpisodeThumbnail(
        seriesName: String,
        seasonNumber: Int,
        episodeNumber: Int,
        thumbnailUrl: String?
    ) {
        if (thumbnailUrl.isNullOrEmpty()) {
            Log.w(TAG, "Episode thumbnail URL is empty for: S${seasonNumber}E${episodeNumber}")
            return
        }
        
        scope.launch {
            try {
                val safeSeriesName = sanitizeFileName(seriesName)
                val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val seasonDir = File(
                    downloadsDir, 
                    "$BASE_FOLDER/$SERIES_FOLDER/$safeSeriesName/S${seasonNumber.toString().padStart(2, '0')}"
                )
                
                if (!seasonDir.exists()) {
                    seasonDir.mkdirs()
                }
                
                val thumbnailFile = File(
                    seasonDir, 
                    "E${episodeNumber.toString().padStart(2, '0')}_thumbnail.png"
                )
                
                // Zaten varsa indirme
                if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                    Log.d(TAG, "Episode thumbnail already exists: ${thumbnailFile.absolutePath}")
                    return@launch
                }
                
                downloadImage(thumbnailUrl, thumbnailFile)
                Log.d(TAG, "Episode thumbnail downloaded: ${thumbnailFile.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading episode thumbnail: ${e.message}")
            }
        }
    }
    
    /**
     * Film kapak fotoğrafını indir
     * Kayıt yeri: Downloads/MaterialTV/Movies/[FilmAdi].png
     */
    fun downloadMovieCover(movieTitle: String, coverUrl: String?) {
        if (coverUrl.isNullOrEmpty()) {
            Log.w(TAG, "Movie cover URL is empty for: $movieTitle")
            return
        }
        
        scope.launch {
            try {
                val safeTitle = sanitizeFileName(movieTitle)
                val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val moviesDir = File(downloadsDir, "$BASE_FOLDER/$MOVIES_FOLDER")
                
                if (!moviesDir.exists()) {
                    moviesDir.mkdirs()
                }
                
                val coverFile = File(moviesDir, "$safeTitle.png")
                
                // Zaten varsa indirme
                if (coverFile.exists() && coverFile.length() > 0) {
                    Log.d(TAG, "Movie cover already exists: ${coverFile.absolutePath}")
                    return@launch
                }
                
                downloadImage(coverUrl, coverFile)
                Log.d(TAG, "Movie cover downloaded: ${coverFile.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading movie cover for $movieTitle: ${e.message}")
            }
        }
    }
    
    /**
     * Tüm sezon bölümlerinin thumbnail'larını toplu indir
     */
    fun downloadSeasonThumbnails(
        seriesName: String,
        seriesCoverUrl: String?,
        seasonNumber: Int,
        episodes: List<Pair<Int, String?>> // episodeNumber to thumbnailUrl
    ) {
        // Önce dizi kapağını indir
        downloadSeriesCover(seriesName, seriesCoverUrl)
        
        // Sonra tüm bölüm thumbnail'larını indir
        episodes.forEach { (episodeNumber, thumbnailUrl) ->
            downloadEpisodeThumbnail(seriesName, seasonNumber, episodeNumber, thumbnailUrl)
        }
    }
    
    /**
     * URL'den görsel indir ve dosyaya kaydet
     */
    private fun downloadImage(url: String, outputFile: File) {
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download image: ${response.code}")
            }
            
            val body = response.body ?: throw Exception("Empty response body")
            
            FileOutputStream(outputFile).use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
    
    /**
     * Dosya adını güvenli hale getir
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100)
    }
    
    /**
     * Dizi kapak dosyasının yolunu döndür
     */
    fun getSeriesCoverPath(seriesName: String): String {
        val safeSeriesName = sanitizeFileName(seriesName)
        val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, "$BASE_FOLDER/$SERIES_FOLDER/$safeSeriesName/cover.png").absolutePath
    }
    
    /**
     * Bölüm thumbnail dosyasının yolunu döndür
     */
    fun getEpisodeThumbnailPath(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
        val safeSeriesName = sanitizeFileName(seriesName)
        val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val seasonDir = "$BASE_FOLDER/$SERIES_FOLDER/$safeSeriesName/S${seasonNumber.toString().padStart(2, '0')}"
        return File(downloadsDir, "$seasonDir/E${episodeNumber.toString().padStart(2, '0')}_thumbnail.png").absolutePath
    }
    
    /**
     * Film kapak dosyasının yolunu döndür
     */
    fun getMovieCoverPath(movieTitle: String): String {
        val safeTitle = sanitizeFileName(movieTitle)
        val downloadsDir = com.hasanege.materialtv.MainApplication.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, "$BASE_FOLDER/$MOVIES_FOLDER/$safeTitle.png").absolutePath
    }
}
