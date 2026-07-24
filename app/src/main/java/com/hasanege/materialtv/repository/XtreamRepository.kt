@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.hasanege.materialtv.repository

import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.mapper.XtreamMappers.toCategory
import com.hasanege.materialtv.mapper.XtreamMappers.toLiveStream
import com.hasanege.materialtv.mapper.XtreamMappers.toSeriesItem
import com.hasanege.materialtv.mapper.XtreamMappers.toVodItem
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
import kotlinx.coroutines.flow.map
import androidx.paging.map
import javax.inject.Singleton

@Singleton
class XtreamRepository @javax.inject.Inject constructor(
    private val catalogRepository: CatalogRepository
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

    private val profileId: String
        get() = com.hasanege.materialtv.network.SessionManager.username ?: "default"

    fun observeVodCategories(username: String): kotlinx.coroutines.flow.Flow<List<Category>> {
        return catalogRepository.observeCategories(ContentType.VOD, username).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    fun observeSeriesCategories(username: String): kotlinx.coroutines.flow.Flow<List<Category>> {
        return catalogRepository.observeCategories(ContentType.SERIES, username).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    fun observeLiveCategories(username: String): kotlinx.coroutines.flow.Flow<List<Category>> {
        return catalogRepository.observeCategories(ContentType.LIVE, username).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    fun getVodStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<VodItem>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)
        
        catalogRepository.observeAllContents(ContentType.VOD, username).collect { entities ->
            val items = entities.map { it.toVodItem() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getVodStreamsPaged(username: String, categoryId: String?): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<VodItem>> {
        val catId = if (categoryId == "all") null else categoryId
        return catalogRepository.observeContents(catId, ContentType.VOD, username)
            .map { pagingData -> pagingData.map { it.toVodItem() } }
    }

    fun getSeries(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<SeriesItem>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)

        catalogRepository.observeAllContents(ContentType.SERIES, username).collect { entities ->
            val items = entities.map { it.toSeriesItem() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getSeriesPaged(username: String, categoryId: String?): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<SeriesItem>> {
        val catId = if (categoryId == "all") null else categoryId
        return catalogRepository.observeContents(catId, ContentType.SERIES, username)
            .map { pagingData -> pagingData.map { it.toSeriesItem() } }
    }

    fun getLiveStreams(
        username: String,
        password: String,
        categoryId: String?
    ): kotlinx.coroutines.flow.Flow<com.hasanege.materialtv.network.Resource<List<LiveStream>>> = channelFlow {
        send(com.hasanege.materialtv.network.Resource.Loading)

        catalogRepository.observeAllContents(ContentType.LIVE, username).collect { entities ->
            val items = entities.map { it.toLiveStream() }
            if (categoryId != null && categoryId != "all") {
                send(com.hasanege.materialtv.network.Resource.Success(items.filter { it.categoryId == categoryId }))
            } else {
                send(com.hasanege.materialtv.network.Resource.Success(items))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getLiveStreamsPaged(username: String, categoryId: String?): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<LiveStream>> {
        val catId = if (categoryId == "all") null else categoryId
        return catalogRepository.observeContents(catId, ContentType.LIVE, username)
            .map { pagingData -> pagingData.map { it.toLiveStream() } }
    }

    suspend fun syncData(username: String, password: String, forceSync: Boolean = false) = withContext(Dispatchers.IO) {
        catalogRepository.triggerBackgroundSync(username, username, password, forceSync)
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
