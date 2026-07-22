package com.hasanege.materialtv

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val repository: XtreamRepository
) : ViewModel() {

    private var _allMovies: List<VodItem> = emptyList()
    private var _allSeries: List<SeriesItem> = emptyList()
    private var _allLiveStreams: List<LiveStream> = emptyList()

    var moviesState by mutableStateOf<UiState<List<VodItem>>>(UiState.Loading)
    var seriesState by mutableStateOf<UiState<List<SeriesItem>>>(UiState.Loading)
    var liveState by mutableStateOf<UiState<List<LiveStream>>>(UiState.Loading)
    
    private val _continueWatchingState = MutableStateFlow<UiState<List<ContinueWatchingItem>>>(UiState.Loading)
    val continueWatchingState: StateFlow<UiState<List<ContinueWatchingItem>>> = _continueWatchingState.asStateFlow()

    var moviesByCategoriesState by mutableStateOf<UiState<Map<Category, List<VodItem>>>>(UiState.Loading)
    var seriesByCategoriesState by mutableStateOf<UiState<Map<Category, List<SeriesItem>>>>(UiState.Loading)
    var liveByCategoriesState by mutableStateOf<UiState<Map<Category, List<LiveStream>>>>(UiState.Loading)

    var movieCategories by mutableStateOf<List<Category>>(emptyList())
    var seriesCategories by mutableStateOf<List<Category>>(emptyList())
    var liveCategories by mutableStateOf<List<Category>>(emptyList())

    var selectedMovieCategoryId by mutableStateOf<String?>(null)
    var selectedSeriesCategoryId by mutableStateOf<String?>(null)
    var selectedLiveCategoryId by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var isRefreshing by mutableStateOf(false)

    private var isInitialDataLoaded = false
    private val removedContinueWatchingItems = mutableSetOf<Int>() // Track removed stream IDs

    init {
        loadRemovedItems()
        loadContinueWatching()
        loadCategoryPreferences()
        viewModelScope.launch { loadFeaturedSeedPreferences() }
    }
    
    private fun loadRemovedItems() {
        try {
            val prefs = application.getSharedPreferences("home_preferences", Context.MODE_PRIVATE)
            val removedItemsSet = prefs.getStringSet("removed_continue_watching_items", emptySet())
            removedContinueWatchingItems.addAll(removedItemsSet?.map { it.toInt() } ?: emptyList())
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error loading removed items", e)
        }
    }
    
    private fun saveRemovedItems() {
        try {
            val prefs = application.getSharedPreferences("home_preferences", Context.MODE_PRIVATE)
            val removedItemsSet = removedContinueWatchingItems.map { it.toString() }.toSet()
            prefs.edit().putStringSet("removed_continue_watching_items", removedItemsSet).apply()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error saving removed items", e)
        }
    }

    fun loadInitialData(username: String, password: String, forceRefresh: Boolean = false) {
        if (isInitialDataLoaded && !forceRefresh) return

        viewModelScope.launch {
            android.util.Log.e("HomeViewModel", "loadInitialData started: forceRefresh=$forceRefresh, loginType=${SessionManager.loginType}")
            isRefreshing = true

            moviesState = UiState.Loading
            seriesState = UiState.Loading
            liveState = UiState.Loading
            moviesByCategoriesState = UiState.Loading
            seriesByCategoriesState = UiState.Loading
            liveByCategoriesState = UiState.Loading

            loadContinueWatching()

            if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                try {
                    android.util.Log.d("HomeViewModel", "Loading M3U data...")
                    
                    withContext(Dispatchers.Default) {
                        // Ensure playlist is fetched if it's empty
                        if (M3uRepository.getPlaylistSize() == 0 || forceRefresh) {
                            android.util.Log.d("HomeViewModel", "Playlist is empty or refresh requested, fetching...")
                            val m3uUrl = SessionManager.m3uUrl
                            if (m3uUrl != null) {
                                M3uRepository.fetchPlaylist(m3uUrl, application, forceRefresh)
                                android.util.Log.d("HomeViewModel", "Playlist fetched, size: ${M3uRepository.getPlaylistSize()}")
                            } else {
                                android.util.Log.e("HomeViewModel", "M3U URL is null!")
                                throw IllegalStateException("M3U URL not found")
                            }
                        }
                        
                        val movies = M3uRepository.getMovies()
                        val series = M3uRepository.getSeries()
                        val live = M3uRepository.getLiveStreams()

                        android.util.Log.d("HomeViewModel", "M3U data loaded - Movies: ${movies.size} groups, Series: ${series.size} groups, Live: ${live.size} groups")

                        val movieCats = movies.keys.map { Category(it.hashCode().toString(), it, 0) }
                        val seriesCats = series.keys.map { Category(it.hashCode().toString(), it, 0) }
                        val liveCats = live.keys.map { Category(it.hashCode().toString(), it, 0) }

                        withContext(Dispatchers.Main) {
                            movieCategories = movieCats
                            seriesCategories = seriesCats
                            liveCategories = liveCats
                        }

                        _allMovies = movies.values.flatten()
                        _allSeries = series.values.flatten()
                        _allLiveStreams = live.values.flatten()

                        android.util.Log.d("HomeViewModel", "Total items - Movies: ${_allMovies.size}, Series: ${_allSeries.size}, Live: ${_allLiveStreams.size}")

                        applyFilters()
                        android.util.Log.d("HomeViewModel", "M3U data loaded successfully")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to load M3U data: ${e.javaClass.simpleName}")
                    val rawMsg = "Failed to load M3U data: ${e.javaClass.simpleName}: ${e.message}"
                    val errorMsg = com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(rawMsg)
                    moviesState = UiState.Error(errorMsg)
                    seriesState = UiState.Error(errorMsg)
                    liveState = UiState.Error(errorMsg)
                    moviesByCategoriesState = UiState.Success(emptyMap())
                    seriesByCategoriesState = UiState.Success(emptyMap())
                    liveByCategoriesState = UiState.Success(emptyMap())
                }
            } else {
                if (!isInitialDataLoaded) {
                    viewModelScope.launch { loadMovieCategories(username, password) }
                    viewModelScope.launch { loadAllMovies(username, password) }
                    viewModelScope.launch { loadSeriesCategories(username, password) }
                    viewModelScope.launch { loadAllSeries(username, password) }
                    viewModelScope.launch { loadLiveCategories(username, password) }
                    viewModelScope.launch { loadAllLiveStreams(username, password) }
                }
                
                if (forceRefresh || !isInitialDataLoaded) {
                    try {
                        repository.syncData(username, password, forceRefresh)
                        val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(application)
                        settingsRepo.setLastUpdatedDate(System.currentTimeMillis())
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Failed to sync data", e)
                    }
                }
            }

            isInitialDataLoaded = true
            isRefreshing = false
            
            // Reload continue watching to update icons with fresh data
            loadContinueWatching()
        }
    }

    fun loadContinueWatching() {
        viewModelScope.launch {
            try {
                _continueWatchingState.value = UiState.Loading
                
                val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(application)
                val threshold = settingsRepo.nextEpisodeThresholdMinutes.firstOrNull() ?: 5
                
                val history = WatchHistoryManager.getContinueWatching(threshold)
                
                // Update icons and names from current live data if available
                val updatedHistory = history.map { item ->
                    if (item.type == "live") {
                        val currentLiveStream = _allLiveStreams.find { it.streamId == item.streamId }
                        if (currentLiveStream != null) {
                            item.copy(
                                name = currentLiveStream.name ?: item.name,
                                streamIcon = currentLiveStream.streamIcon ?: item.streamIcon
                            )
                        } else {
                            item
                        }
                    } else if (item.type == "movie") {
                         val currentMovie = _allMovies.find { it.streamId == item.streamId }
                         if (currentMovie != null) {
                             item.copy(
                                 name = currentMovie.name ?: item.name,
                                 streamIcon = currentMovie.streamIcon ?: item.streamIcon
                             )
                         } else {
                             item
                         }
                    } else if (item.type == "series") {
                        val currentSeries = _allSeries.find { it.seriesId == item.seriesId }
                        if (currentSeries != null) {
                            item.copy(
                                name = currentSeries.name ?: item.name,
                                streamIcon = currentSeries.cover ?: item.streamIcon
                            )
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }

                // Filter out items that were manually removed from continue watching
                val filteredHistory = updatedHistory.filter { item ->
                    !removedContinueWatchingItems.contains(item.streamId)
                }
                // Sort: pinned items first, then by last watched
                val sortedHistory = filteredHistory.sortedWith(compareBy<ContinueWatchingItem> { !it.isPinned }.thenByDescending { it.position })
                _continueWatchingState.value = UiState.Success(sortedHistory)
            } catch (e: Exception) {
                _continueWatchingState.value = UiState.Error("Failed to load watch history: ${e.message}")
            }
        }
    }

    private suspend fun applyFilters() {
        withContext(Dispatchers.Default) {
            // Movies
            val filteredMovies = _allMovies.filter { movie ->
                (selectedMovieCategoryId == null || movie.categoryId == selectedMovieCategoryId) && movie.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                moviesState = UiState.Success(filteredMovies)
            }

            val moviesByCategory = filteredMovies
                .groupBy { movie -> movieCategories.find { it.categoryId == movie.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                moviesByCategoriesState = UiState.Success(moviesByCategory)
            }

            // Series
            val filteredSeries = _allSeries.filter { series ->
                (selectedSeriesCategoryId == null || series.categoryId == selectedSeriesCategoryId) && series.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                seriesState = UiState.Success(filteredSeries)
            }

            val seriesByCategory = filteredSeries
                .groupBy { series -> seriesCategories.find { it.categoryId == series.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                seriesByCategoriesState = UiState.Success(seriesByCategory)
            }

            // Live TV
            val filteredLive = _allLiveStreams.filter { stream ->
                (selectedLiveCategoryId == null || stream.categoryId == selectedLiveCategoryId) && stream.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
            withContext(Dispatchers.Main) {
                liveState = UiState.Success(filteredLive)
            }

            val liveByCategory = filteredLive
                .groupBy { stream -> liveCategories.find { it.categoryId == stream.categoryId } }
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            withContext(Dispatchers.Main) {
                liveByCategoriesState = UiState.Success(liveByCategory)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onMovieCategorySelected(categoryId: String?) {
        selectedMovieCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onSeriesCategorySelected(categoryId: String?) {
        selectedSeriesCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    fun onLiveCategorySelected(categoryId: String?) {
        selectedLiveCategoryId = categoryId
        viewModelScope.launch {
            applyFilters()
        }
    }

    private suspend fun loadMovieCategories(username: String, password: String) {
        repository.observeVodCategories(username).collect { cats ->
            movieCategories = cats
            applyFilters()
        }
    }

    private suspend fun loadSeriesCategories(username: String, password: String) {
        repository.observeSeriesCategories(username).collect { cats ->
            seriesCategories = cats
            applyFilters()
        }
    }

    private suspend fun loadLiveCategories(username: String, password: String) {
        repository.observeLiveCategories(username).collect { cats ->
            liveCategories = cats
            applyFilters()
        }
    }

    private suspend fun loadAllMovies(username: String, password: String) {
        repository.getVodStreams(username, password, null).collect { resource ->
            when (resource) {
                is com.hasanege.materialtv.network.Resource.Loading -> {
                    if (_allMovies.isEmpty()) moviesState = UiState.Loading
                }
                is com.hasanege.materialtv.network.Resource.Success -> {
                    _allMovies = resource.data
                    applyFilters()
                }
                is com.hasanege.materialtv.network.Resource.Error -> {
                    if (_allMovies.isEmpty()) {
                        moviesState = UiState.Error(resource.message)
                        moviesByCategoriesState = UiState.Error(resource.message)
                    }
                }
            }
        }
    }

    private suspend fun loadAllSeries(username: String, password: String) {
        repository.getSeries(username, password, null).collect { resource ->
            when (resource) {
                is com.hasanege.materialtv.network.Resource.Loading -> {
                    if (_allSeries.isEmpty()) seriesState = UiState.Loading
                }
                is com.hasanege.materialtv.network.Resource.Success -> {
                    _allSeries = resource.data
                    applyFilters()
                }
                is com.hasanege.materialtv.network.Resource.Error -> {
                    android.util.Log.e("HomeViewModel", "Error loading series: ${resource.message}")
                    if (_allSeries.isEmpty()) {
                        seriesState = UiState.Error(resource.message)
                        seriesByCategoriesState = UiState.Error(resource.message)
                    }
                }
            }
        }
    }

    private suspend fun loadAllLiveStreams(username: String, password: String) {
        repository.getLiveStreams(username, password, null).collect { resource ->
            when (resource) {
                is com.hasanege.materialtv.network.Resource.Loading -> {
                    if (_allLiveStreams.isEmpty()) liveState = UiState.Loading
                }
                is com.hasanege.materialtv.network.Resource.Success -> {
                    _allLiveStreams = resource.data
                    applyFilters()
                }
                is com.hasanege.materialtv.network.Resource.Error -> {
                    android.util.Log.e("HomeViewModel", "Error loading live streams: ${resource.message}")
                    if (_allLiveStreams.isEmpty()) {
                        liveState = UiState.Error(resource.message)
                        liveByCategoriesState = UiState.Error(resource.message)
                    }
                }
            }
        }
    }

    fun removeFromContinueWatching(item: ContinueWatchingItem) {
        viewModelScope.launch {
            try {
                // Add to removed items tracking
                removedContinueWatchingItems.add(item.streamId)
                saveRemovedItems() // Save to persistent storage
                
                // Remove from continue watching list
                val currentItems = when (val state = _continueWatchingState.value) {
                    is UiState.Success -> state.data
                    else -> emptyList()
                }
                
                val updatedItems = currentItems.filter { it.streamId != item.streamId }
                _continueWatchingState.value = UiState.Success(updatedItems)
                
                // Note: This only removes from continue watching display
                // Full watch history remains intact in WatchHistoryManager
                // Item will be re-added only if user watches it again
                
            } catch (e: Exception) {
                 android.util.Log.e("HomeViewModel", "Error removing from CW", e)
            }
        }
    }

    fun updateContinueWatchingItems(items: List<ContinueWatchingItem>) {
        viewModelScope.launch {
            try {
                // Check if any previously removed items are being watched again
                items.forEach { item ->
                    if (removedContinueWatchingItems.contains(item.streamId)) {
                        // Remove from removed list since user is watching it again
                        removedContinueWatchingItems.remove(item.streamId)
                    }
                }
                saveRemovedItems() // Save to persistent storage
                
                // Sort: pinned items first, then by last watched
                val sortedItems = items.sortedWith(compareBy<ContinueWatchingItem> { !it.isPinned }.thenBy { it.position })
                _continueWatchingState.value = UiState.Success(sortedItems)
            } catch (e: Exception) {
                 android.util.Log.e("HomeViewModel", "Error updating CW items", e)
            }
        }
    }

    // Category Reordering & Hiding Preferences
    var hiddenCategoryIdsMovies by mutableStateOf<Set<String>>(emptySet())
    var hiddenCategoryIdsSeries by mutableStateOf<Set<String>>(emptySet())
    var hiddenCategoryIdsLive by mutableStateOf<Set<String>>(emptySet())

    var orderedCategoryIdsMovies by mutableStateOf<List<String>>(emptyList())
    var orderedCategoryIdsSeries by mutableStateOf<List<String>>(emptyList())
    var orderedCategoryIdsLive by mutableStateOf<List<String>>(emptyList())

    fun toggleCategoryVisibility(tab: Int, categoryId: String) {
        when (tab) {
            0 -> {
                hiddenCategoryIdsMovies = if (hiddenCategoryIdsMovies.contains(categoryId)) {
                    hiddenCategoryIdsMovies - categoryId
                } else {
                    hiddenCategoryIdsMovies + categoryId
                }
            }
            1 -> {
                hiddenCategoryIdsSeries = if (hiddenCategoryIdsSeries.contains(categoryId)) {
                    hiddenCategoryIdsSeries - categoryId
                } else {
                    hiddenCategoryIdsSeries + categoryId
                }
            }
            2 -> {
                hiddenCategoryIdsLive = if (hiddenCategoryIdsLive.contains(categoryId)) {
                    hiddenCategoryIdsLive - categoryId
                } else {
                    hiddenCategoryIdsLive + categoryId
                }
            }
        }
        saveCategoryPreferences()
    }

    fun moveCategoryUp(tab: Int, categoryList: List<Category>, index: Int) {
        if (index <= 0 || index >= categoryList.size) return
        val currentOrder = when (tab) {
            0 -> orderedCategoryIdsMovies.ifEmpty { categoryList.map { it.categoryId ?: "" } }
            1 -> orderedCategoryIdsSeries.ifEmpty { categoryList.map { it.categoryId ?: "" } }
            else -> orderedCategoryIdsLive.ifEmpty { categoryList.map { it.categoryId ?: "" } }
        }.toMutableList()

        val temp = currentOrder[index]
        currentOrder[index] = currentOrder[index - 1]
        currentOrder[index - 1] = temp

        when (tab) {
            0 -> orderedCategoryIdsMovies = currentOrder
            1 -> orderedCategoryIdsSeries = currentOrder
            2 -> orderedCategoryIdsLive = currentOrder
        }
        saveCategoryPreferences()
    }

    fun moveCategoryDown(tab: Int, categoryList: List<Category>, index: Int) {
        if (index < 0 || index >= categoryList.size - 1) return
        val currentOrder = when (tab) {
            0 -> orderedCategoryIdsMovies.ifEmpty { categoryList.map { it.categoryId ?: "" } }
            1 -> orderedCategoryIdsSeries.ifEmpty { categoryList.map { it.categoryId ?: "" } }
            else -> orderedCategoryIdsLive.ifEmpty { categoryList.map { it.categoryId ?: "" } }
        }.toMutableList()

        val temp = currentOrder[index]
        currentOrder[index] = currentOrder[index + 1]
        currentOrder[index + 1] = temp

        when (tab) {
            0 -> orderedCategoryIdsMovies = currentOrder
            1 -> orderedCategoryIdsSeries = currentOrder
            2 -> orderedCategoryIdsLive = currentOrder
        }
        saveCategoryPreferences()
    }

    fun resetCategoryPreferences(tab: Int) {
        when (tab) {
            0 -> {
                hiddenCategoryIdsMovies = emptySet()
                orderedCategoryIdsMovies = emptyList()
            }
            1 -> {
                hiddenCategoryIdsSeries = emptySet()
                orderedCategoryIdsSeries = emptyList()
            }
            2 -> {
                hiddenCategoryIdsLive = emptySet()
                orderedCategoryIdsLive = emptyList()
            }
        }
        saveCategoryPreferences()
    }

    private fun saveCategoryPreferences() {
        try {
            val prefs = application.getSharedPreferences("category_preferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet("hidden_movies", hiddenCategoryIdsMovies)
                .putStringSet("hidden_series", hiddenCategoryIdsSeries)
                .putStringSet("hidden_live", hiddenCategoryIdsLive)
                .putString("order_movies", orderedCategoryIdsMovies.joinToString(","))
                .putString("order_series", orderedCategoryIdsSeries.joinToString(","))
                .putString("order_live", orderedCategoryIdsLive.joinToString(","))
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error saving category preferences", e)
        }
    }

    private fun loadCategoryPreferences() {
        try {
            val prefs = application.getSharedPreferences("category_preferences", Context.MODE_PRIVATE)
            hiddenCategoryIdsMovies = prefs.getStringSet("hidden_movies", emptySet()) ?: emptySet()
            hiddenCategoryIdsSeries = prefs.getStringSet("hidden_series", emptySet()) ?: emptySet()
            hiddenCategoryIdsLive = prefs.getStringSet("hidden_live", emptySet()) ?: emptySet()

            val orderMovies = prefs.getString("order_movies", null)
            orderedCategoryIdsMovies = if (!orderMovies.isNullOrBlank()) orderMovies.split(",") else emptyList()

            val orderSeries = prefs.getString("order_series", null)
            orderedCategoryIdsSeries = if (!orderSeries.isNullOrBlank()) orderSeries.split(",") else emptyList()

            val orderLive = prefs.getString("order_live", null)
            orderedCategoryIdsLive = if (!orderLive.isNullOrBlank()) orderLive.split(",") else emptyList()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error loading category preferences", e)
        }
    }

    // Featured Showcase Seed Preferences (Daily Refresh & Persistent Manual Reroll)
    var featuredSeedMovies by mutableIntStateOf(0)
    var featuredSeedSeries by mutableIntStateOf(0)

    private suspend fun loadFeaturedSeedPreferences() {
        try {
            val prefs = application.getSharedPreferences("featured_preferences", Context.MODE_PRIVATE)
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            
            val savedDate = prefs.getString("last_featured_date", "")
            var savedSeedMovies = prefs.getInt("seed_movies", 1)
            var savedSeedSeries = prefs.getInt("seed_series", 1)

            if (savedDate != todayStr) {
                // New day! Automatically refresh featured seeds once per day
                savedSeedMovies = (1..10000).random()
                savedSeedSeries = (1..10000).random()
                prefs.edit()
                    .putString("last_featured_date", todayStr)
                    .putInt("seed_movies", savedSeedMovies)
                    .putInt("seed_series", savedSeedSeries)
                    .apply()
            }

            withContext(Dispatchers.Main) {
                featuredSeedMovies = savedSeedMovies
                featuredSeedSeries = savedSeedSeries
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error loading featured seeds", e)
        }
    }

    fun rerollFeaturedItems(tab: Int) {
        val newSeed = (1..10000).random()
        val prefs = application.getSharedPreferences("featured_preferences", Context.MODE_PRIVATE)
        if (tab == 0) {
            featuredSeedMovies = newSeed
            prefs.edit().putInt("seed_movies", newSeed).apply()
        } else {
            featuredSeedSeries = newSeed
            prefs.edit().putInt("seed_series", newSeed).apply()
        }
    }
}


