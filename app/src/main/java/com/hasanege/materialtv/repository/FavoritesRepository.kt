package com.hasanege.materialtv.repository

import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.data.entities.FavoriteEntity
import com.hasanege.materialtv.data.entities.FavoriteWithContent
import com.hasanege.materialtv.data.entities.ListFolderEntity
import com.hasanege.materialtv.model.FavoriteItem
import com.hasanege.materialtv.model.FavoriteList
import com.hasanege.materialtv.network.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    
    private val profileId: String
        get() = SessionManager.username ?: "default"

    private fun parseContentType(type: String): ContentType {
        return when (type.lowercase()) {
            "movie", "vod" -> ContentType.VOD
            "series" -> ContentType.SERIES
            "live" -> ContentType.LIVE
            else -> ContentType.VOD
        }
    }

    private fun stringifyContentType(type: ContentType): String {
        return when (type) {
            ContentType.VOD -> "movie"
            ContentType.SERIES -> "series"
            ContentType.LIVE -> "live"
        }
    }

    // Favorites operations
    suspend fun addFavorite(favorite: FavoriteItem): Long {
        return userDao.insertFavorite(favorite.toEntity())
    }

    suspend fun updateFavorite(favorite: FavoriteItem) {
        userDao.insertFavorite(favorite.toEntity()) // upsert
    }

    suspend fun removeFavorite(favorite: FavoriteItem) {
        userDao.deleteFavorite(favorite.toEntity())
    }

    suspend fun removeFavoriteByContent(contentId: Int, contentType: String) {
        userDao.deleteFavoriteByContent(contentId.toString(), parseContentType(contentType), profileId)
    }

    suspend fun isFavorite(contentId: Int, contentType: String): Boolean {
        return userDao.isFavorite(contentId.toString(), parseContentType(contentType), profileId)
    }

    suspend fun getFavoriteByContent(contentId: Int, contentType: String): FavoriteItem? {
        val type = parseContentType(contentType)
        val entity = userDao.getFavorite(contentId.toString(), type, profileId) ?: return null
        return entity.toModel()
    }

    fun getAllFavorites(): Flow<List<FavoriteItem>> {
        return userDao.observeFavoritesWithContent(profileId).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByList(listId: Long): Flow<List<FavoriteItem>> {
        val folderId = listId.toString()
        return userDao.observeFavoritesByFolderWithContent(profileId, folderId).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByType(type: String): Flow<List<FavoriteItem>> {
        return userDao.observeFavoritesByTypeWithContent(profileId, parseContentType(type)).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByGenre(genre: String): Flow<List<FavoriteItem>> {
        return userDao.observeFavoritesWithContent(profileId).map { list -> 
            list.filter { it.genre?.contains(genre, ignoreCase = true) == true }.map { it.toModel() } 
        }
    }

    fun getFavoritesByWatchedStatus(watched: Boolean): Flow<List<FavoriteItem>> {
        // Watch history is now in a separate table, so this is just a dummy filter for now
        return userDao.observeFavoritesWithContent(profileId).map { list -> list.map { it.toModel() } }
    }

    suspend fun updateOrderIndex(favoriteId: Long, newIndex: Int) {
        // Order index is no longer supported on favorites, relying on addedAt
    }

    suspend fun reorderFavorites(favorites: List<FavoriteItem>) {
        // Order index is no longer supported on favorites
    }

    // Custom lists operations
    suspend fun createList(list: FavoriteList): Long {
        userDao.insertFolder(list.toEntity())
        return list.listId
    }

    suspend fun updateList(list: FavoriteList) {
        userDao.updateFolder(list.toEntity())
    }

    suspend fun deleteList(list: FavoriteList) {
        val folderId = list.listId.toString()
        userDao.deleteAllFavoritesByFolder(profileId, folderId)
        userDao.deleteFolder(list.toEntity())
    }

    fun getAllLists(): Flow<List<FavoriteList>> {
        return userDao.observeFolders(profileId).map { list -> 
            list.map { entity ->
                val count = userDao.getFavoritesCountByFolder(profileId, entity.folderId)
                entity.toModel(count)
            }
        }
    }

    suspend fun getListById(listId: Long): FavoriteList? {
        val folderId = listId.toString()
        val entity = userDao.getFolderById(profileId, folderId) ?: return null
        val count = userDao.getFavoritesCountByFolder(profileId, folderId)
        return entity.toModel(count)
    }

    suspend fun updateListOrderIndex(listId: Long, newIndex: Int) {
        userDao.updateFolderOrder(listId.toString(), profileId, newIndex)
    }

    suspend fun reorderLists(lists: List<FavoriteList>) {
        lists.forEachIndexed { index, list ->
            userDao.updateFolderOrder(list.listId.toString(), profileId, index)
        }
    }

    // Extension functions for conversions
    private fun FavoriteItem.toEntity() = FavoriteEntity(
        streamId = contentId.toString(),
        type = parseContentType(contentType),
        profileId = profileId,
        folderId = if (listId == 0L) "default" else listId.toString(),
        addedAt = addedAt
    )

    private fun FavoriteEntity.toModel() = FavoriteItem(
        id = streamId.hashCode().toLong(), // dummy
        contentId = streamId.toIntOrNull() ?: 0,
        contentType = stringifyContentType(type),
        name = streamId, // fallback if content missing
        thumbnailUrl = null,
        addedAt = addedAt,
        listId = folderId.toLongOrNull() ?: 0L
    )

    private fun FavoriteWithContent.toModel() = FavoriteItem(
        id = streamId.hashCode().toLong(),
        contentId = streamId.toIntOrNull() ?: 0,
        contentType = stringifyContentType(type),
        name = name ?: streamId,
        thumbnailUrl = posterUrl,
        addedAt = addedAt,
        listId = folderId.toLongOrNull() ?: 0L,
        genre = genre,
        year = releaseDate,
        categoryId = categoryId,
        rating = rating ?: 0f,
        streamIcon = posterUrl
    )

    private fun FavoriteList.toEntity() = ListFolderEntity(
        folderId = listId.toString(),
        profileId = profileId,
        name = listName,
        iconKey = iconName ?: "",
        sortOrder = orderIndex
    )

    private fun ListFolderEntity.toModel(count: Int) = FavoriteList(
        listId = folderId.toLongOrNull() ?: folderId.hashCode().toLong(),
        listName = name,
        createdAt = System.currentTimeMillis(),
        orderIndex = sortOrder,
        iconName = iconKey,
        colorHex = "#FF0000",
        itemCount = count
    )
}
