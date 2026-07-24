package com.hasanege.materialtv

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.FavoriteItem
import com.hasanege.materialtv.model.FavoriteList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {
    
    private val _favoritesState = MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> = _favoritesState.asStateFlow()

    private val _listsState = MutableStateFlow<UiState<List<FavoriteList>>>(UiState.Loading)
    val listsState: StateFlow<UiState<List<FavoriteList>>> = _listsState.asStateFlow()

    // Filter states
    val selectedListId = mutableStateOf<Long?>(null)
    val selectedType = mutableStateOf<String?>(null)
    val selectedGenre = mutableStateOf<String?>(null)
    val showWatchedOnly = mutableStateOf<Boolean?>(null)
    val searchQuery = mutableStateOf("")

    // Sort options
    val sortBy = mutableStateOf(SortOption.DATE_ADDED)
    val sortAscending = mutableStateOf(false)

    init {
        loadFavorites()
        loadLists()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                FavoritesManager.favoritesFlow.collect { favorites ->
                    refreshFavorites()
                }
            } catch (e: Exception) {
                _favoritesState.value = UiState.Error(e.message ?: "Failed to load favorites")
            }
        }
    }

    private fun refreshFavorites() {
        val favorites = FavoritesManager.favoritesFlow.value
        val filtered = applyFilters(favorites)
        val sorted = applySorting(filtered)
        _favoritesState.value = UiState.Success(sorted)
    }

    fun loadLists() {
        viewModelScope.launch {
            try {
                FavoritesManager.listsFlow.collect { lists ->
                    _listsState.value = UiState.Success(lists)
                }
            } catch (e: Exception) {
                _listsState.value = UiState.Error(e.message ?: "Failed to load lists")
            }
        }
    }

    private fun applyFilters(favorites: List<FavoriteItem>): List<FavoriteItem> {
        var filtered = favorites

        // List filtering is now handled in the UI (HorizontalPager) to prevent paging glitches

        // Filter by type
        selectedType.value?.let { type ->
            filtered = filtered.filter { it.contentType == type }
        }

        // Filter by genre
        selectedGenre.value?.let { genre ->
            filtered = filtered.filter { it.genre?.contains(genre, ignoreCase = true) == true }
        }

        // Filter by watched status
        showWatchedOnly.value?.let { watched ->
            filtered = filtered.filter { it.isWatched == watched }
        }

        // Filter by search query
        if (searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.contains(searchQuery.value, ignoreCase = true) ||
                it.notes.contains(searchQuery.value, ignoreCase = true)
            }
        }

        return filtered
    }

    private fun applySorting(favorites: List<FavoriteItem>): List<FavoriteItem> {
        val typeOrder = mapOf(
            "live" to 0,
            "series" to 1,
            "movie" to 2
        )
        
        val sorted = when (sortBy.value) {
            SortOption.DATE_ADDED -> favorites.sortedBy { it.addedAt }
            SortOption.NAME -> favorites.sortedWith(
                compareBy<FavoriteItem> { typeOrder[it.contentType] ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            SortOption.RATING -> favorites.sortedBy { it.rating }
        }
        
        return if (sortAscending.value) sorted else sorted.reversed()
    }

    fun addFavorite(favorite: FavoriteItem) {
        viewModelScope.launch {
            try {
                FavoritesManager.addFavorite(favorite)
            } catch (e: Exception) {
                _favoritesState.value = UiState.Error(e.message ?: "Failed to add favorite")
            }
        }
    }

    fun removeFavorite(favorite: FavoriteItem) {
        viewModelScope.launch {
            try {
                FavoritesManager.removeFavorite(favorite)
            } catch (e: Exception) {
                _favoritesState.value = UiState.Error(e.message ?: "Failed to remove favorite")
            }
        }
    }

    fun updateFavorite(favorite: FavoriteItem) {
        viewModelScope.launch {
            try {
                FavoritesManager.updateFavorite(favorite)
            } catch (e: Exception) {
                _favoritesState.value = UiState.Error(e.message ?: "Failed to update favorite")
            }
        }
    }

    fun reorderFavorites(favorites: List<FavoriteItem>) {
        viewModelScope.launch {
            try {
                FavoritesManager.reorderFavorites(favorites)
            } catch (e: Exception) {
                _favoritesState.value = UiState.Error(e.message ?: "Failed to reorder favorites")
            }
        }
    }

    fun createList(listName: String, iconName: String? = null, colorHex: String? = null) {
        viewModelScope.launch {
            try {
                FavoritesManager.createList(listName, iconName, colorHex)
            } catch (e: Exception) {
                _listsState.value = UiState.Error(e.message ?: "Failed to create list")
            }
        }
    }

    fun updateList(list: FavoriteList) {
        viewModelScope.launch {
            try {
                FavoritesManager.updateList(list)
            } catch (e: Exception) {
                _listsState.value = UiState.Error(e.message ?: "Failed to update list")
            }
        }
    }

    fun deleteList(list: FavoriteList) {
        viewModelScope.launch {
            try {
                FavoritesManager.deleteList(list)
            } catch (e: Exception) {
                _listsState.value = UiState.Error(e.message ?: "Failed to delete list")
            }
        }
    }

    fun setFilter(
        listId: Long? = selectedListId.value,
        type: String? = selectedType.value,
        genre: String? = selectedGenre.value,
        watchedOnly: Boolean? = showWatchedOnly.value
    ) {
        selectedListId.value = listId
        selectedType.value = type
        selectedGenre.value = genre
        showWatchedOnly.value = watchedOnly
        
        // Only refresh if non-paging filters changed
        refreshFavorites()
    }

    fun clearFilters() {
        selectedListId.value = null
        selectedType.value = null
        selectedGenre.value = null
        showWatchedOnly.value = null
        searchQuery.value = ""
        refreshFavorites()
    }

    fun setSortOption(option: SortOption, ascending: Boolean = false) {
        sortBy.value = option
        sortAscending.value = ascending
        refreshFavorites()
    }

    enum class SortOption {
        DATE_ADDED,
        NAME,
        RATING
    }
}
