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

const val DEFAULT_OPENSUBTITLES_API_KEY = "nBSt40xO1Hlh6eI007d4p11lB11jD2uK"

fun cleanMediaTitle(raw: String): String {
    var text = raw
    text = text.replace(Regex("""\.(mkv|mp4|avi|ts|m3u8|flv|wmv|mov)$""", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("""[._\-]"""), " ")
    text = text.replace(Regex("""(?i)\b(s\d+e\d+|\d+x\d+|season\s*\d+|episode\s*\d+)\b"""), " ")
    text = text.replace(Regex("""(?i)\b(1080p|720p|4k|2160p|480p|hdr|hdr10|web-dl|webrip|bluray|hdtv|x264|x265|hevc|aac|ac3|dvdrip|remux|repack|unrated|extended)\b"""), " ")
    text = text.replace(Regex("""[\(\[\{]\d{4}[\)\]\}]"""), " ")
    text = text.replace(Regex("""\s+"""), " ").trim()
    return text.ifBlank { raw }
}

@Singleton
class OpenSubtitlesRepository @Inject constructor(
    private val apiService: OpenSubtitlesApiService,
    private val okHttpClient: OkHttpClient
) {

    suspend fun searchSubtitles(
        apiKey: String?,
        imdbId: String? = null,
        tmdbId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        languages: String? = null,
        query: String? = null
    ): List<OpenSubtitlesItem> {
        val effectiveKey = apiKey?.takeIf { it.isNotBlank() } ?: DEFAULT_OPENSUBTITLES_API_KEY
        val params = mutableMapOf<String, String>()

        val cleanImdbId = imdbId?.removePrefix("tt")?.trim()?.trimStart('0')?.takeIf { it.isNotBlank() }
        val isTvShow = (seasonNumber != null && seasonNumber > 0) || (episodeNumber != null && episodeNumber > 0)

        if (!cleanImdbId.isNullOrBlank()) {
            if (isTvShow) {
                params["parent_imdb_id"] = cleanImdbId
                Log.d("OpenSubtitlesRepo", "Using parent_imdb_id param for TV series episode: parent_imdb_id=$cleanImdbId (raw=$imdbId)")
            } else {
                params["imdb_id"] = cleanImdbId
                Log.d("OpenSubtitlesRepo", "Using imdb_id param for Movie: imdb_id=$cleanImdbId (raw=$imdbId)")
            }
        } else if (!tmdbId.isNullOrBlank()) {
            if (isTvShow) {
                params["parent_tmdb_id"] = tmdbId
                Log.d("OpenSubtitlesRepo", "Using parent_tmdb_id param for TV series episode: parent_tmdb_id=$tmdbId")
            } else {
                params["tmdb_id"] = tmdbId
                Log.d("OpenSubtitlesRepo", "Using tmdb_id param for Movie: tmdb_id=$tmdbId")
            }
        } else {
            Log.w("OpenSubtitlesRepo", "No valid IMDb ID or TMDB ID provided for OpenSubtitles. Will fallback to title query.")
        }

        if (seasonNumber != null && seasonNumber > 0) {
            params["season_number"] = seasonNumber.toString()
        }

        if (episodeNumber != null && episodeNumber > 0) {
            params["episode_number"] = episodeNumber.toString()
        }

        // Dil filtresi tamamen kaldırıldı: OpenSubtitles API mevcut tüm dillerdeki altyazıları filtrelemeden getirir.

        val cleanedQuery = query?.let { cleanMediaTitle(it) }
        if (!params.containsKey("imdb_id") && !params.containsKey("parent_imdb_id") && !params.containsKey("tmdb_id") && !params.containsKey("parent_tmdb_id") && !cleanedQuery.isNullOrBlank()) {
            params["query"] = cleanedQuery
        }

        if (params.isEmpty()) {
            Log.w("OpenSubtitlesRepo", "No search criteria provided (neither ID nor title query).")
            return emptyList()
        }

        Log.d("OpenSubtitlesRepo", "Searching subtitles with params: $params")
        val response = apiService.searchSubtitles(effectiveKey, params)

        if (response.code() == 429 || response.code() == 406) {
            throw OpenSubtitlesQuotaException("OpenSubtitles indirme limitiniz (kullanım hakkınız) doldu.")
        }
        if (response.code() == 401 || response.code() == 403) {
            val errBody = response.errorBody()?.string() ?: ""
            Log.e("OpenSubtitlesRepo", "API Key rejected (${response.code()}): $errBody")
            throw Exception("OpenSubtitles API erişimi reddedildi (${response.code()}). Lütfen Ayarlar menüsünden geçerli bir OpenSubtitles API Anahtarı tanımlayın.")
        }

        var results = if (response.isSuccessful) {
            response.body()?.data ?: emptyList()
        } else {
            Log.e("OpenSubtitlesRepo", "Error searching subtitles: ${response.code()} - ${response.errorBody()?.string()}")
            emptyList()
        }

        // Fallback 1: If TV show search with parent_imdb_id or parent_tmdb_id returned empty, retry with imdb_id/tmdb_id (in case provided ID is specific episode ID)
        if (results.isEmpty() && (params.containsKey("parent_imdb_id") || params.containsKey("parent_tmdb_id"))) {
            val fallbackParams1 = params.toMutableMap()
            val pid = fallbackParams1.remove("parent_imdb_id")
            val ptmdb = fallbackParams1.remove("parent_tmdb_id")
            if (pid != null) fallbackParams1["imdb_id"] = pid
            if (ptmdb != null) fallbackParams1["tmdb_id"] = ptmdb
            fallbackParams1.remove("season_number")
            fallbackParams1.remove("episode_number")
            Log.d("OpenSubtitlesRepo", "Fallback 1: Retrying TV show search with direct episode imdb_id/tmdb_id: $fallbackParams1")
            val fbResp1 = apiService.searchSubtitles(effectiveKey, fallbackParams1)
            if (fbResp1.isSuccessful && !fbResp1.body()?.data.isNullOrEmpty()) {
                results = fbResp1.body()!!.data!!
            }
        }

        // Fallback 2: If ID search returned empty, retry by clean title query with season/episode!
        if (results.isEmpty() && !cleanedQuery.isNullOrBlank()) {
            val fallbackParams2 = mutableMapOf<String, String>()
            fallbackParams2["query"] = cleanedQuery
            if (seasonNumber != null && seasonNumber > 0) fallbackParams2["season_number"] = seasonNumber.toString()
            if (episodeNumber != null && episodeNumber > 0) fallbackParams2["episode_number"] = episodeNumber.toString()
            Log.d("OpenSubtitlesRepo", "Fallback 2: Retrying search with title query params: $fallbackParams2")
            val fallbackResp2 = apiService.searchSubtitles(effectiveKey, fallbackParams2)
            if (fallbackResp2.isSuccessful && !fallbackResp2.body()?.data.isNullOrEmpty()) {
                results = fallbackResp2.body()!!.data!!
            }
        }

        // Fallback 3: If still empty, retry clean title query without season/episode filter
        if (results.isEmpty() && !cleanedQuery.isNullOrBlank()) {
            val fallbackParams3 = mutableMapOf<String, String>()
            fallbackParams3["query"] = cleanedQuery
            Log.d("OpenSubtitlesRepo", "Fallback 3: Retrying search with title-only query params: $fallbackParams3")
            val fallbackResp3 = apiService.searchSubtitles(effectiveKey, fallbackParams3)
            if (fallbackResp3.isSuccessful && !fallbackResp3.body()?.data.isNullOrEmpty()) {
                results = fallbackResp3.body()!!.data!!
            }
        }

        return results
    }

    suspend fun downloadSubtitle(
        apiKey: String?,
        fileId: Int,
        context: Context
    ): File {
        val effectiveKey = apiKey?.takeIf { it.isNotBlank() } ?: DEFAULT_OPENSUBTITLES_API_KEY
        Log.d("OpenSubtitlesRepo", "Requesting download link for fileId: $fileId")
        val response = apiService.downloadSubtitle(effectiveKey, OpenSubtitlesDownloadRequest(fileId))

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
            .header("User-Agent", "MaterialTV v3.1")
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
