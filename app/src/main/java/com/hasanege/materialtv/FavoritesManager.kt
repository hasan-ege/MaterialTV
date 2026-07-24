package com.hasanege.materialtv

import android.content.Context
import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.model.FavoriteItem
import com.hasanege.materialtv.model.FavoriteList
import com.hasanege.materialtv.repository.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FavoritesManager {
    private lateinit var repository: FavoritesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _favoritesFlow = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favoritesFlow: StateFlow<List<FavoriteItem>> = _favoritesFlow.asStateFlow()

    private val _listsFlow = MutableStateFlow<List<FavoriteList>>(emptyList())
    val listsFlow: StateFlow<List<FavoriteList>> = _listsFlow.asStateFlow()

    fun initialize(context: Context) {
        val database = AppDatabase.getDatabase(context)
        repository = FavoritesRepository(database)

        // Collect favorites
        scope.launch {
            repository.getAllFavorites().collect { favorites ->
                _favoritesFlow.value = favorites
            }
        }

        // Collect lists
        scope.launch {
            repository.getAllLists().collect { lists ->
                _listsFlow.value = lists
            }
        }
    }

    suspend fun addFavorite(favorite: FavoriteItem): Long {
        return repository.addFavorite(favorite)
    }

    suspend fun removeFavorite(favorite: FavoriteItem) {
        repository.removeFavorite(favorite)
    }

    suspend fun removeFavoriteByContent(contentId: Int, contentType: String) {
        repository.removeFavoriteByContent(contentId, contentType)
    }

    suspend fun updateFavorite(favorite: FavoriteItem) {
        repository.updateFavorite(favorite)
    }

    suspend fun isFavorite(contentId: Int, contentType: String): Boolean {
        return repository.isFavorite(contentId, contentType)
    }

    suspend fun getFavoriteByContent(contentId: Int, contentType: String): FavoriteItem? {
        return repository.getFavoriteByContent(contentId, contentType)
    }

    suspend fun toggleFavorite(
        contentId: Int,
        contentType: String,
        name: String,
        thumbnailUrl: String?,
        genre: String? = null,
        year: String? = null,
        categoryId: String? = null,
        seriesId: Int? = null,
        streamIcon: String? = null,
        listId: Long = 0
    ): Boolean {
        return if (isFavorite(contentId, contentType)) {
            removeFavoriteByContent(contentId, contentType)
            false
        } else {
            val favorite = FavoriteItem(
                contentId = contentId,
                contentType = contentType,
                name = name,
                thumbnailUrl = thumbnailUrl,
                addedAt = System.currentTimeMillis(),
                genre = genre,
                year = year,
                categoryId = categoryId,
                seriesId = seriesId,
                streamIcon = streamIcon,
                listId = listId
            )
            addFavorite(favorite)
            true
        }
    }

    suspend fun reorderFavorites(favorites: List<FavoriteItem>) {
        repository.reorderFavorites(favorites)
    }

    // Custom lists
    suspend fun createList(listName: String, iconName: String? = null, colorHex: String? = null): Long {
        val list = FavoriteList(
            listName = listName,
            createdAt = System.currentTimeMillis(),
            iconName = iconName,
            colorHex = colorHex
        )
        return repository.createList(list)
    }

    suspend fun updateList(list: FavoriteList) {
        repository.updateList(list)
    }

    suspend fun deleteList(list: FavoriteList) {
        repository.deleteList(list)
    }

    suspend fun reorderLists(lists: List<FavoriteList>) {
        repository.reorderLists(lists)
    }

    fun getFavoritesByList(listId: Long) = repository.getFavoritesByList(listId)
    fun getFavoritesByType(type: String) = repository.getFavoritesByType(type)
    fun getFavoritesByGenre(genre: String) = repository.getFavoritesByGenre(genre)
    fun getFavoritesByWatchedStatus(watched: Boolean) = repository.getFavoritesByWatchedStatus(watched)
}
