package com.hasanege.materialtv.repository

import android.content.Context
import android.util.Log
import com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesDownloadRequest
import com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesItem
import com.hasanege.materialtv.network.opensubtitles.OpenSubtitlesApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

class OpenSubtitlesQuotaException(message: String) : Exception(message)

@Singleton
class OpenSubtitlesRepository @Inject constructor(
    private val apiService: OpenSubtitlesApiService,
    private val okHttpClient: OkHttpClient
) {

    suspend fun searchSubtitles(
        apiKey: String,
        imdbId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        languages: String? = null,
        query: String? = null
    ): List<OpenSubtitlesItem> {
        val params = mutableMapOf<String, String>()

        val cleanImdbId = imdbId?.removePrefix("tt")?.trim()
        if (!cleanImdbId.isNullOrBlank()) {
            params["imdb_id"] = cleanImdbId
        }

        if (seasonNumber != null && seasonNumber > 0) {
            params["season_number"] = seasonNumber.toString()
        }

        if (episodeNumber != null && episodeNumber > 0) {
            params["episode_number"] = episodeNumber.toString()
        }

        if (!languages.isNullOrBlank()) {
            params["languages"] = languages
        }

        if (params["imdb_id"] == null && !query.isNullOrBlank()) {
            params["query"] = query
        }

        if (params.isEmpty()) {
            Log.w("OpenSubtitlesRepo", "No search criteria provided (neither IMDb ID nor title query).")
            return emptyList()
        }

        Log.d("OpenSubtitlesRepo", "Searching subtitles with params: $params")
        val response = apiService.searchSubtitles(apiKey, params)

        if (response.code() == 429 || response.code() == 406) {
            throw OpenSubtitlesQuotaException("OpenSubtitles indirme limitiniz (kullanım hakkınız) doldu.")
        }

        if (!response.isSuccessful) {
            Log.e("OpenSubtitlesRepo", "Search failed with code ${response.code()}: ${response.errorBody()?.string()}")
            return emptyList()
        }

        val searchResponse = response.body()
        val items = searchResponse?.data ?: emptyList()
        Log.d("OpenSubtitlesRepo", "Found ${items.size} subtitles.")
        return items
    }

    suspend fun downloadSubtitle(
        apiKey: String,
        fileId: Int,
        context: Context
    ): File {
        Log.d("OpenSubtitlesRepo", "Requesting download link for fileId: $fileId")
        val response = apiService.downloadSubtitle(apiKey, OpenSubtitlesDownloadRequest(fileId))

        if (response.code() == 429 || response.code() == 406) {
            throw OpenSubtitlesQuotaException("OpenSubtitles indirme limitiniz (kullanım hakkınız) doldu.")
        }

        if (!response.isSuccessful) {
            val errorStr = response.errorBody()?.string() ?: ""
            if (errorStr.contains("limit", ignoreCase = true) || errorStr.contains("quota", ignoreCase = true)) {
                throw OpenSubtitlesQuotaException("OpenSubtitles indirme limitiniz (kullanım hakkınız) doldu.")
            }
            throw Exception("Alt yazı indirme bağlantısı alınamadı (${response.code()})")
        }

        val downloadData = response.body()
        val link = downloadData?.link
        if (link.isNullOrBlank()) {
            throw Exception("Alt yazı indirme bağlantısı boş döndü.")
        }

        if (downloadData.remaining != null && downloadData.remaining <= 0) {
            Log.w("OpenSubtitlesRepo", "Remaining downloads is 0")
        }

        Log.d("OpenSubtitlesRepo", "Downloading file from link: $link")

        // Create target directory in cache
        val subDir = File(context.cacheDir, "subtitles")
        if (!subDir.exists()) {
            subDir.mkdirs()
        }

        val targetFile = File(subDir, "sub_${fileId}.srt")

        val okRequest = Request.Builder()
            .url(link)
            .header("User-Agent", "MaterialTV v1.0")
            .build()

        val okResponse = okHttpClient.newCall(okRequest).execute()
        if (!okResponse.isSuccessful) {
            throw Exception("Dosya indirilemedi: ${okResponse.code}")
        }

        okResponse.body?.byteStream()?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        targetFile.setLastModified(System.currentTimeMillis())

        Log.d("OpenSubtitlesRepo", "Subtitle saved to: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
        cleanOldSubtitlesCache(context)
        return targetFile
    }

    fun cleanOldSubtitlesCache(context: Context) {
        try {
            val subDir = File(context.cacheDir, "subtitles")
            if (subDir.exists() && subDir.isDirectory) {
                val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000L // 30 days
                subDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoff) {
                        val deleted = file.delete()
                        Log.d("OpenSubtitlesRepo", "Deleted expired subtitle file: ${file.name}, success: $deleted")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OpenSubtitlesRepo", "Error cleaning subtitle cache: ${e.message}")
        }
    }
}
