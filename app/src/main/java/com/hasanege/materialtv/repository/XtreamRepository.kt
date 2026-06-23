@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.hasanege.materialtv.repository

import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Singleton

@Singleton
class XtreamRepository @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val cacheDir: File? = context.cacheDir

    private val apiService: XtreamApiService?
        get() = com.hasanege.materialtv.network.SessionManager.serverUrl?.let {
            com.hasanege.materialtv.network.RetrofitClient.getClient(it)
        }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private val memoryCache = mutableMapOf<String, Any>()

    suspend fun getVodCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val cacheKey = "vod_categories"
        if (memoryCache.containsKey(cacheKey)) return@withContext memoryCache[cacheKey] as List<Category>
        
        val service = apiService ?: return@withContext emptyList()
        try {
            val response = service.getVodCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            memoryCache[cacheKey] = result
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val cacheKey = "series_categories"
        if (memoryCache.containsKey(cacheKey)) return@withContext memoryCache[cacheKey] as List<Category>

        val service = apiService ?: return@withContext emptyList()
        try {
            val response = service.getSeriesCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            memoryCache[cacheKey] = result
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val cacheKey = "live_categories"
        if (memoryCache.containsKey(cacheKey)) return@withContext memoryCache[cacheKey] as List<Category>

        val service = apiService ?: return@withContext emptyList()
        try {
            val response = service.getLiveCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            memoryCache[cacheKey] = result
            result
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
        val service = apiService
        if (service != null) {
            try {
                val response = service.getVodStreams(username, password, categoryId = categoryId)
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
    }.flowOn(Dispatchers.IO)

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
        val service = apiService
        if (service != null) {
            try {
                val response = service.getSeries(username, password, categoryId = categoryId)
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
    }.flowOn(Dispatchers.IO)

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
        val service = apiService
        if (service != null) {
            try {
                val response = service.getLiveStreams(username, password, categoryId = categoryId)
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
    }.flowOn(Dispatchers.IO)

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
        val service = apiService ?: return@withContext null
        try {
            val response = service.getVodInfo(username, password, vodId = vodId)
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
        val service = apiService ?: return@withContext null
        try {
            service.getVodInfo(username, password, vodId = vodId)
        } catch (e: Exception) {
            null
        }
    }
    private inline fun <reified T> saveToCache(fileName: String, data: List<T>) {
        if (cacheDir == null) return
        try {
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { outputStream ->
                json.encodeToStream(ListSerializer(json.serializersModule.serializer<T>()), data, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inline fun <reified T> loadFromCache(fileName: String): List<T> {
        if (cacheDir == null) return emptyList()
        val file = File(cacheDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            FileInputStream(file).use { inputStream ->
                json.decodeFromStream(ListSerializer(json.serializersModule.serializer<T>()), inputStream)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
