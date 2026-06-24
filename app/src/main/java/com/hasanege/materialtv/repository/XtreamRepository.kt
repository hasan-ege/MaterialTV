@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.hasanege.materialtv.repository

import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.entities.toEntity
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import javax.inject.Singleton

@Singleton
class XtreamRepository @javax.inject.Inject constructor(
    private val database: AppDatabase
) {
    private val apiService: XtreamApiService?
        get() = com.hasanege.materialtv.network.SessionManager.serverUrl?.let {
            com.hasanege.materialtv.network.RetrofitClient.getClient(it)
        }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
        isLenient = true
    }

    suspend fun getVodCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext database.xtreamDao().getCategories("vod").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        try {
            val response = service.getVodCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            database.xtreamDao().updateCategories("vod", result.map { it.toEntity("vod") })
            result
        } catch (e: Exception) {
            database.xtreamDao().getCategories("vod").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext database.xtreamDao().getCategories("series").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        try {
            val response = service.getSeriesCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            database.xtreamDao().updateCategories("series", result.map { it.toEntity("series") })
            result
        } catch (e: Exception) {
            database.xtreamDao().getCategories("series").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        }
    }

    suspend fun getLiveCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext database.xtreamDao().getCategories("live").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        try {
            val response = service.getLiveCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            database.xtreamDao().updateCategories("live", result.map { it.toEntity("live") })
            result
        } catch (e: Exception) {
            database.xtreamDao().getCategories("live").firstOrNull()?.map { it.toCategory() } ?: emptyList()
        }
    }

    fun getVodStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<VodItem>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)
        
        database.xtreamDao().getVodItems().collect { entities ->
            val items = entities.map { it.toVodItem() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getSeries(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<SeriesItem>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)

        database.xtreamDao().getSeriesItems().collect { entities ->
            val items = entities.map { it.toSeriesItem() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getLiveStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<LiveStream>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)

        database.xtreamDao().getLiveStreams().collect { entities ->
            val items = entities.map { it.toLiveStream() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun syncData(username: String, password: String) = withContext(Dispatchers.IO) {
        val service = apiService ?: throw Exception("API service not initialized")
        
        // Fetch categories and streams in parallel to speed up sync
        val jobs = listOf(
            launch {
                try {
                    val response = service.getVodCategories(username, password)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
                        database.xtreamDao().updateCategories("vod", result.map { it.toEntity("vod") })
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            launch {
                try {
                    val response = service.getSeriesCategories(username, password)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
                        database.xtreamDao().updateCategories("series", result.map { it.toEntity("series") })
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            launch {
                try {
                    val response = service.getLiveCategories(username, password)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val result = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
                        database.xtreamDao().updateCategories("live", result.map { it.toEntity("live") })
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            launch {
                try {
                    val response = service.getVodStreams(username, password, categoryId = null)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val data = json.decodeFromJsonElement(ListSerializer(VodItem.serializer()), response)
                        database.xtreamDao().updateVodItems(data.map { it.toEntity() })
                    } else if (response is JsonArray && response.isEmpty()) {
                        database.xtreamDao().updateVodItems(emptyList())
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            launch {
                try {
                    val response = service.getSeries(username, password, categoryId = null)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val data = json.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), response)
                        database.xtreamDao().updateSeriesItems(data.map { it.toEntity() })
                    } else if (response is JsonArray && response.isEmpty()) {
                        database.xtreamDao().updateSeriesItems(emptyList())
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            launch {
                try {
                    val response = service.getLiveStreams(username, password, categoryId = null)
                    if (response !is JsonNull && (response !is JsonArray || !response.isEmpty())) {
                        val data = json.decodeFromJsonElement(ListSerializer(LiveStream.serializer()), response)
                        database.xtreamDao().updateLiveStreams(data.map { it.toEntity() })
                    } else if (response is JsonArray && response.isEmpty()) {
                        database.xtreamDao().updateLiveStreams(emptyList())
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        )
        // Wait for all sync jobs to complete
        jobs.forEach { it.join() }
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

    private fun decodeBase64Safe(text: String?): String? {
        if (text.isNullOrBlank()) return text
        if (!text.matches(Regex("^[a-zA-Z0-9+/]*={0,2}$")) || text.contains(" ")) return text
        return try {
            val decodedBytes = android.util.Base64.decode(text, android.util.Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            text
        }
    }

    suspend fun getShortEpg(username: String, password: String, streamId: Int, limit: Int = 10): List<com.hasanege.materialtv.model.EpgListing> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext emptyList()
        try {
            var response = service.getShortEpg(username, password, streamId = streamId, limit = limit)
            var parsedList = parseEpgResponse(response)

            // Fallback to get_simple_data_table if get_short_epg returns empty
            if (parsedList.isEmpty()) {
                response = service.getSimpleDataTable(username, password, streamId = streamId)
                parsedList = parseEpgResponse(response)
            }

            parsedList
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseEpgResponse(response: kotlinx.serialization.json.JsonElement): MutableList<com.hasanege.materialtv.model.EpgListing> {
        val parsedList = mutableListOf<com.hasanege.materialtv.model.EpgListing>()
        
        val listingsArray = if (response is kotlinx.serialization.json.JsonObject && response.containsKey("epg_listings")) {
            response["epg_listings"] as? kotlinx.serialization.json.JsonArray
        } else if (response is kotlinx.serialization.json.JsonArray) {
            response
        } else {
            null
        }

        listingsArray?.forEach { item ->
            if (item is kotlinx.serialization.json.JsonObject) {
                parsedList.add(
                    com.hasanege.materialtv.model.EpgListing(
                        id = item["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null },
                        epg_id = item["epg_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null },
                        title = decodeBase64Safe(item["title"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null }),
                        description = decodeBase64Safe(item["description"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null }),
                        start = item["start"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null },
                        end = item["end"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.contentOrNull else null }
                    )
                )
            }
        }
        return parsedList
    }
}
