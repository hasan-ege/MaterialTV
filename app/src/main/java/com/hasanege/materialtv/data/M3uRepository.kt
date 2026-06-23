package com.hasanege.materialtv.data

import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.utils.M3uEntry
import com.hasanege.materialtv.utils.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object M3uRepository {

    private var playlist: List<M3uEntry> = emptyList()

    private const val CACHE_FILE_NAME = "playlist_cache.m3u"

    suspend fun fetchPlaylist(url: String, context: android.content.Context, forceRefresh: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = java.io.File(context.filesDir, CACHE_FILE_NAME)
                var loadedFromCache = false

                // Try to load from cache first
                if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
                    loadedFromCache = true
                }

                // If not loaded from cache, fetch from network and stream directly to file
                if (!loadedFromCache) {
                    android.util.Log.d("M3uRepository", "Fetching playlist from: $url")
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.getInputStream().use { inputStream ->
                        java.io.FileOutputStream(cacheFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    android.util.Log.d("M3uRepository", "Playlist saved to cache")
                }
                
                // Parse directly from file using BufferedReader to avoid loading full text in memory
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    java.io.FileInputStream(cacheFile).use { fileIn ->
                        java.io.InputStreamReader(fileIn, kotlin.text.Charsets.UTF_8).buffered().use { reader ->
                            playlist = M3uParser.parse(reader)
                        }
                    }
                } else {
                    playlist = emptyList()
                }
                
                android.util.Log.d("M3uRepository", "Parsed ${playlist.size} entries (Source: ${if (loadedFromCache) "Cache" else "Network"})")
            } catch (e: Exception) {
                android.util.Log.e("M3uRepository", "Error fetching playlist", e)
                playlist = emptyList() // Reset on error
                throw e
            }
        }
    }

    fun getMovies(): Map<String, List<VodItem>> {
        return playlist.filter { isMovie(it) }
            .groupBy { it.group ?: "Uncategorized" }
            .mapValues { (group, entries) ->
                entries.map { entry ->
                    VodItem(
                        streamId = entry.url.hashCode(),
                        name = entry.title,
                        streamIcon = entry.logo,
                        rating5Based = 0.0,
                        categoryId = group.hashCode().toString(),  // Set to match category ID
                        containerExtension = "mp4", // Assumption
                        year = null,
                        seriesId = null
                    )
                }
            }
    }

    fun getSeries(): Map<String, List<SeriesItem>> {
        // M3U usually doesn't separate series well, but we can try to guess based on naming or groups
        // For now, let's assume everything that looks like a series (S01E01) is a series
        return playlist.filter { isSeries(it) }
            .groupBy { it.group ?: "Uncategorized" }
            .mapValues { (group, entries) ->
                entries.map { entry ->
                    SeriesItem(
                        seriesId = entry.url.hashCode(),
                        name = entry.title,
                        cover = entry.logo,
                        plot = "",
                        cast = "",
                        director = "",
                        genre = "",
                        releaseDate = "",
                        lastModified = "",
                        rating = "",
                        rating5Based = 0.0,
                        episodeRunTime = "",
                        youtubeTrailer = "",
                        categoryId = group.hashCode().toString(),  // Set to match category ID
                        year = ""
                    )
                }
            }
    }

    fun getLiveStreams(): Map<String, List<LiveStream>> {
        return playlist.filter { !isMovie(it) && !isSeries(it) }
            .groupBy { it.group ?: "Uncategorized" }
            .mapValues { (group, entries) ->
                entries.map { entry ->
                    LiveStream(
                        streamId = entry.url.hashCode(),
                        name = entry.title,
                        streamIcon = entry.logo,
                        epgChannelId = null,
                        categoryId = group.hashCode().toString()  // Set to match category ID
                    )
                }
            }
    }
    
    fun getStreamUrl(id: Int): String? {
        return playlist.find { it.url.hashCode() == id }?.url
    }
    
    fun getPlaylistSize(): Int {
        return playlist.size
    }

    private fun isMovie(entry: M3uEntry): Boolean {
        val lowerUrl = entry.url.lowercase()
        val group = entry.group?.lowercase() ?: ""
        
        // Check group title first for explicit "movie" or "vod" keywords
        if (group.contains("movie") || group.contains("vod") || group.contains("film")) return true
        
        // Check file extension
        if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi")) return true
        
        return false
    }

    private fun isSeries(entry: M3uEntry): Boolean {
        val group = entry.group?.lowercase() ?: ""
        val title = entry.title
        
        // Check group title for "series" or "show"
        if (group.contains("series") || group.contains("show") || group.contains("season")) return true
        
        // Check title for SxxExx pattern
        if (Regex("[Ss]\\d+[Ee]\\d+").containsMatchIn(title)) return true
        
        return false
    }
}
