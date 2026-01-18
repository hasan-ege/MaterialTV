package com.hasanege.materialtv.repository

import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.serializer

import java.io.File
import java.io.FileReader
import java.io.FileWriter

class XtreamRepository(
    private val apiService: XtreamApiService?, 
    private val cacheDir: File? = null
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    suspend fun getVodCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getVodCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getSeriesCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getLiveCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getVodStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<VodItem>>> = kotlinx.coroutines.flow.flow {
        val cacheFile = "vod_streams_${categoryId ?: "all"}.json"
        
        emit(com.hasanege.materialtv.network.Resource.Loading)
        
        // 1. Load from cache
        val cached = loadFromCache<VodItem>(cacheFile)
        if (cached.isNotEmpty()) {
            emit(com.hasanege.materialtv.network.Resource.Success(cached))
        }

        // 2. Fetch from network
        if (apiService != null) {
            try {
                val response = apiService.getVodStreams(username, password, categoryId = categoryId)
                if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                    val data = json.decodeFromJsonElement(ListSerializer(VodItem.serializer()), response)
                    saveToCache(cacheFile, data)
                    emit(com.hasanege.materialtv.network.Resource.Success(data))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(com.hasanege.materialtv.network.Resource.Error(e.message ?: "Network error", e))
                }
            }
        }
    }

    fun getSeries(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<SeriesItem>>> = kotlinx.coroutines.flow.flow {
        val cacheFile = "series_${categoryId ?: "all"}.json"

        emit(com.hasanege.materialtv.network.Resource.Loading)

        // 1. Load from cache
        val cached = loadFromCache<SeriesItem>(cacheFile)
        if (cached.isNotEmpty()) {
            emit(com.hasanege.materialtv.network.Resource.Success(cached))
        }

        // 2. Fetch from network
        if (apiService != null) {
            try {
                val response = apiService.getSeries(username, password, categoryId = categoryId)
                if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                    val data = json.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), response)
                    saveToCache(cacheFile, data)
                    emit(com.hasanege.materialtv.network.Resource.Success(data))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(com.hasanege.materialtv.network.Resource.Error(e.message ?: "Network error", e))
                }
            }
        }
    }

    fun getLiveStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<LiveStream>>> = kotlinx.coroutines.flow.flow {
        val cacheFile = "live_streams_${categoryId ?: "all"}.json"

        emit(com.hasanege.materialtv.network.Resource.Loading)

        // 1. Load from cache
        val cached = loadFromCache<LiveStream>(cacheFile)
        if (cached.isNotEmpty()) {
            emit(com.hasanege.materialtv.network.Resource.Success(cached))
        }

        // 2. Fetch from network
        if (apiService != null) {
            try {
                val response = apiService.getLiveStreams(username, password, categoryId = categoryId)
                if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                    val data = json.decodeFromJsonElement(ListSerializer(LiveStream.serializer()), response)
                    saveToCache(cacheFile, data)
                    emit(com.hasanege.materialtv.network.Resource.Success(data))
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    emit(com.hasanege.materialtv.network.Resource.Error(e.message ?: "Network error", e))
                }
            }
        }
    }

    suspend fun getSeriesInfo(
        username: String,
        password: String,
        seriesId: Int
    ): SeriesInfoResponse? = withContext(Dispatchers.IO) {
        val response = apiService?.getSeriesInfo(username, password, seriesId = seriesId) ?: return@withContext null
        try {
            if (response is kotlinx.serialization.json.JsonObject) {
                json.decodeFromJsonElement(SeriesInfoResponse.serializer(), response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVodInfo(username: String, password: String, vodId: Int): VodItem? = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext null
        try {
            val response = apiService.getVodInfo(username, password, vodId = vodId)
            val info = response.info
            val movieData = response.movieData
            if (info != null && movieData != null) {
                VodItem(
                    streamId = movieData.streamId?.toIntOrNull() ?: 0,
                    name = info.name ?: "",
                    streamIcon = info.movieImage,
                    rating5Based = try { info.rating5based?.toDouble() } catch(e: Exception) { 0.0 },
                    categoryId = movieData.categoryId,
                    containerExtension = movieData.containerExtension,
                    year = info.year
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVodDetails(username: String, password: String, vodId: Int): VodInfoResponse? = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext null
        try {
            apiService.getVodInfo(username, password, vodId = vodId)
        } catch (e: Exception) {
            null
        }
    }
    private inline fun <reified T> saveToCache(fileName: String, data: List<T>) {
        if (cacheDir == null) return
        try {
            val file = File(cacheDir, fileName)
            val jsonString = json.encodeToString(ListSerializer(json.serializersModule.serializer<T>()), data)
            FileWriter(file).use { it.write(jsonString) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inline fun <reified T> loadFromCache(fileName: String): List<T> {
        if (cacheDir == null) return emptyList()
        val file = File(cacheDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            val jsonString = FileReader(file).use { it.readText() }
            json.decodeFromString(ListSerializer(json.serializersModule.serializer<T>()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
