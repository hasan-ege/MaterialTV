package com.hasanege.materialtv.repository

import com.hasanege.materialtv.data.dao.UserDao
import com.hasanege.materialtv.data.entities.WatchHistoryEntity
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepository @Inject constructor(
    private val userDao: UserDao
) {
    private val currentProfileId: String
        get() = SessionManager.username ?: "default"

    fun observeHistory(): Flow<List<ContinueWatchingItem>> {
        return userDao.observeHistory(currentProfileId).map { entities ->
            entities.map { it.toContinueWatchingItem() }
        }
    }

    suspend fun getHistorySync(): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        userDao.getHistorySync(currentProfileId).map { it.toContinueWatchingItem() }
    }

    suspend fun saveItem(item: ContinueWatchingItem) = withContext(Dispatchers.IO) {
        val existingItem = userDao.getHistoryItem(
            streamId = item.streamId.toString(),
            type = item.type,
            profileId = currentProfileId,
            episodeId = item.episodeId ?: ""
        )
        val effectiveIcon = item.streamIcon.takeIf { !it.isNullOrEmpty() } ?: existingItem?.streamIcon
        val entity = item.copy(streamIcon = effectiveIcon).toEntity(currentProfileId)
        userDao.insertOrUpdateHistory(entity)
    }
    
    suspend fun saveItemWithWatchTime(item: ContinueWatchingItem, additionalWatchTime: Long) = withContext(Dispatchers.IO) {
        val existingItem = userDao.getHistoryItem(
            streamId = item.streamId.toString(),
            type = item.type,
            profileId = currentProfileId,
            episodeId = item.episodeId ?: ""
        )
        
        val newActualWatchTime = (existingItem?.actualWatchTimeMs ?: 0L) + additionalWatchTime
        val isPinned = existingItem?.isPinned ?: item.isPinned
        val dismissed = existingItem?.dismissedFromContinueWatching ?: item.dismissedFromContinueWatching
        val effectiveIcon = item.streamIcon.takeIf { !it.isNullOrEmpty() } ?: existingItem?.streamIcon
        
        val updatedEntity = item.copy(
            streamIcon = effectiveIcon,
            actualWatchTime = newActualWatchTime,
            isPinned = isPinned,
            dismissedFromContinueWatching = dismissed
        ).toEntity(currentProfileId)
        
        userDao.insertOrUpdateHistory(updatedEntity)
    }

    suspend fun dismissItem(item: ContinueWatchingItem) = withContext(Dispatchers.IO) {
        val existing = userDao.getHistoryItem(
            item.streamId.toString(), 
            item.type, 
            currentProfileId, 
            item.episodeId ?: ""
        )
        if (existing != null) {
            userDao.insertOrUpdateHistory(existing.copy(dismissedFromContinueWatching = true))
        }
    }

    suspend fun togglePin(item: ContinueWatchingItem) = withContext(Dispatchers.IO) {
        val existing = userDao.getHistoryItem(
            item.streamId.toString(), 
            item.type, 
            currentProfileId, 
            item.episodeId ?: ""
        )
        if (existing != null) {
            userDao.insertOrUpdateHistory(existing.copy(isPinned = !existing.isPinned))
        }
    }

    suspend fun removeItem(item: ContinueWatchingItem) = withContext(Dispatchers.IO) {
        userDao.deleteHistoryByContent(item.streamId.toString(), item.type, currentProfileId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        userDao.clearHistory(currentProfileId)
    }

    // Converters
    private fun WatchHistoryEntity.toContinueWatchingItem(): ContinueWatchingItem {
        return ContinueWatchingItem(
            streamId = streamId.toIntOrNull() ?: streamId.hashCode(),
            name = name,
            streamIcon = streamIcon,
            duration = durationMs,
            position = positionMs,
            type = type,
            seriesId = seriesId?.toIntOrNull(),
            isPinned = isPinned,
            episodeId = episodeId.takeIf { it.isNotEmpty() },
            containerExtension = containerExtension,
            dismissedFromContinueWatching = dismissedFromContinueWatching,
            actualWatchTime = actualWatchTimeMs,
            isDownloaded = isDownloaded,
            localPath = localPath
        )
    }

    private fun ContinueWatchingItem.toEntity(profileId: String): WatchHistoryEntity {
        return WatchHistoryEntity(
            streamId = streamId.toString(),
            type = type,
            profileId = profileId,
            episodeId = episodeId ?: "",
            name = name,
            streamIcon = streamIcon,
            durationMs = duration,
            positionMs = position,
            actualWatchTimeMs = actualWatchTime,
            seriesId = seriesId?.toString(),
            containerExtension = containerExtension,
            isPinned = isPinned,
            dismissedFromContinueWatching = dismissedFromContinueWatching,
            isDownloaded = isDownloaded,
            localPath = localPath,
            lastWatchedAt = System.currentTimeMillis()
        )
    }
}
