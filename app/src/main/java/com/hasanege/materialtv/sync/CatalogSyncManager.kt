package com.hasanege.materialtv.sync

import androidx.room.withTransaction
import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.dao.CategoryDao
import com.hasanege.materialtv.data.dao.ContentDao
import com.hasanege.materialtv.data.dao.SyncMetaDao
import com.hasanege.materialtv.data.entities.CategoryEntity
import com.hasanege.materialtv.data.entities.ContentEntity
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.data.entities.SyncMetaEntity
import com.hasanege.materialtv.mapper.XtreamMappers.toCategoryEntity
import com.hasanege.materialtv.mapper.XtreamMappers.toContentEntity
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.RetrofitClient
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogSyncManager @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val contentDao: ContentDao,
    private val syncMetaDao: SyncMetaDao
) {
    private val apiService: XtreamApiService?
        get() = SessionManager.serverUrl?.let { RetrofitClient.getClient(it) }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    suspend fun syncIfNeeded(profileId: String, username: String, password: String, forceSync: Boolean = false) = withContext(Dispatchers.IO) {
        if (!forceSync) {
            val lastSync = syncMetaDao.getSyncMeta(profileId, ContentType.VOD.name)
            val twelveHoursInMillis = 12 * 60 * 60 * 1000L
            if (lastSync != null && System.currentTimeMillis() - lastSync.lastSyncAt < twelveHoursInMillis) {
                // Already synced recently
                return@withContext
            }
        }
        
        syncCategories(ContentType.VOD, profileId, username, password)
        syncCategories(ContentType.SERIES, profileId, username, password)
        syncCategories(ContentType.LIVE, profileId, username, password)
        
        syncVodStreams(profileId, username, password)
        syncSeriesStreams(profileId, username, password)
        syncLiveStreams(profileId, username, password)
    }

    private suspend fun syncCategories(type: ContentType, profileId: String, username: String, password: String) {
        val service = apiService ?: return
        try {
            val response = when (type) {
                ContentType.VOD -> service.getVodCategories(username, password)
                ContentType.SERIES -> service.getSeriesCategories(username, password)
                ContentType.LIVE -> service.getLiveCategories(username, password)
            }
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return
            val dtos = json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
            
            var sortOrder = 0
            val incoming = dtos.map { it.toCategoryEntity(type, profileId, sortOrder++) }
            
            database.withTransaction {
                val existingMap = categoryDao.getIdToLastSeen(type, profileId)
                val incomingIds = incoming.map { it.categoryId }.toSet()
                
                // For simplicity, we just upsert all incoming and delete missing
                categoryDao.upsertAll(incoming)
                
                val toDelete = existingMap.keys - incomingIds
                if (toDelete.isNotEmpty()) {
                    categoryDao.deleteByIds(toDelete.toList(), type, profileId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncVodStreams(profileId: String, username: String, password: String) {
        val service = apiService ?: return
        try {
            val response = service.getVodStreams(username, password, categoryId = null)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return
            val dtos = json.decodeFromJsonElement(ListSerializer(VodItem.serializer()), response)
            
            val incoming = dtos.map { it.toContentEntity(profileId, it.categoryId ?: "") }
            applyContentDiff(ContentType.VOD, profileId, incoming)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncSeriesStreams(profileId: String, username: String, password: String) {
        val service = apiService ?: return
        try {
            val response = service.getSeries(username, password, categoryId = null)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return
            val dtos = json.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), response)
            
            val incoming = dtos.map { it.toContentEntity(profileId, it.categoryId ?: "") }
            applyContentDiff(ContentType.SERIES, profileId, incoming)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncLiveStreams(profileId: String, username: String, password: String) {
        val service = apiService ?: return
        try {
            val response = service.getLiveStreams(username, password, categoryId = null)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return
            val dtos = json.decodeFromJsonElement(ListSerializer(LiveStream.serializer()), response)
            
            val incoming = dtos.map { it.toContentEntity(profileId, it.categoryId ?: "") }
            applyContentDiff(ContentType.LIVE, profileId, incoming)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun applyContentDiff(type: ContentType, profileId: String, incoming: List<ContentEntity>) {
        database.withTransaction {
            val existingHashes = contentDao.getIdToHash(type, profileId)
            val incomingIds = incoming.map { it.streamId }.toSet()

            val toUpsert = incoming.filter { existingHashes[it.streamId] != it.contentHash }
            val toDelete = existingHashes.keys - incomingIds

            if (toUpsert.isNotEmpty()) {
                // Batch insert using chunks to avoid SQLite constraints
                toUpsert.chunked(500).forEach { chunk ->
                    contentDao.upsertAll(chunk)
                }
            }
            if (toDelete.isNotEmpty()) {
                toDelete.chunked(500).forEach { chunk ->
                    contentDao.deleteByIds(chunk, type, profileId)
                }
            }
            
            syncMetaDao.updateSyncMeta(
                SyncMetaEntity(
                    profileId = profileId,
                    syncScope = "content_${type.name}",
                    lastSyncAt = System.currentTimeMillis(),
                    lastSyncStatus = "SUCCESS",
                    itemCount = incomingIds.size
                )
            )
        }
    }

    suspend fun enrichPendingDetails(profileId: String, username: String, password: String) = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext
        
        // Enrich VOD
        val pendingVod = contentDao.getPendingDetails(ContentType.VOD, profileId, 50)
        pendingVod.forEach { content ->
            try {
                val response = service.getVodInfo(username, password, vodId = content.streamId.toIntOrNull() ?: return@forEach)
                val info = response.info
                val updated = content.copy(
                    plot = info?.plot ?: content.plot,
                    director = info?.director ?: content.director,
                    genre = info?.genre ?: content.genre,
                    releaseDate = info?.releaseDate ?: content.releaseDate,
                    durationSecs = info?.durationSecs ?: info?.duration?.toIntOrNull() ?: content.durationSecs,
                    rating = info?.rating?.toFloatOrNull() ?: content.rating,
                    backdropUrl = info?.backdropPath?.firstOrNull() ?: content.backdropUrl,
                    tmdbId = info?.imdbID,
                    detailsFetched = true
                )
                contentDao.upsertAll(listOf(updated))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Similarly for Series, wait we need to parse SeriesInfoResponse. 
        // We'll leave series and live for later or implement a stub.
    }
}
