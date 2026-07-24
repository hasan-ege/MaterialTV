package com.hasanege.materialtv.cache

import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized content cache for fast local search and data sharing across the app.
 * All content is loaded once and shared between ViewModels.
 */
object ContentCache {
    
    // Movies
    private val _movies = MutableStateFlow<List<VodItem>>(emptyList())
    val movies: StateFlow<List<VodItem>> = _movies.asStateFlow()
    
    // Series
    private val _series = MutableStateFlow<List<SeriesItem>>(emptyList())
    val series: StateFlow<List<SeriesItem>> = _series.asStateFlow()
    
    // Live Streams
    private val _liveStreams = MutableStateFlow<List<LiveStream>>(emptyList())
    val liveStreams: StateFlow<List<LiveStream>> = _liveStreams.asStateFlow()
    
    // Categories
    private val _movieCategories = MutableStateFlow<List<Category>>(emptyList())
    val movieCategories: StateFlow<List<Category>> = _movieCategories.asStateFlow()
    
    private val _seriesCategories = MutableStateFlow<List<Category>>(emptyList())
    val seriesCategories: StateFlow<List<Category>> = _seriesCategories.asStateFlow()
    
    private val _liveCategories = MutableStateFlow<List<Category>>(emptyList())
    val liveCategories: StateFlow<List<Category>> = _liveCategories.asStateFlow()
    
    // Loading state
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Cache timestamp for refresh logic
    private var lastLoadTime: Long = 0
    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
    
    fun isCacheValid(): Boolean {
        return _isLoaded.value && (System.currentTimeMillis() - lastLoadTime) < CACHE_VALIDITY_MS
    }
    
    // Update functions
    fun updateMovies(movies: List<VodItem>) {
        _movies.value = movies
    }
    
    fun updateSeries(series: List<SeriesItem>) {
        _series.value = series
    }
    
    fun updateLiveStreams(liveStreams: List<LiveStream>) {
        _liveStreams.value = liveStreams
    }
    
    fun updateMovieCategories(categories: List<Category>) {
        _movieCategories.value = categories
    }
    
    fun updateSeriesCategories(categories: List<Category>) {
        _seriesCategories.value = categories
    }
    
    fun updateLiveCategories(categories: List<Category>) {
        _liveCategories.value = categories
    }
    
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    fun setLoaded(loaded: Boolean) {
        _isLoaded.value = loaded
        if (loaded) {
            lastLoadTime = System.currentTimeMillis()
        }
    }
    
    // Search functions - fast local search
    fun searchMovies(query: String): List<VodItem> {
        if (query.isBlank()) return _movies.value
        return _movies.value.filter { 
            it.name?.contains(query, ignoreCase = true) == true 
        }
    }
    
    fun searchSeries(query: String): List<SeriesItem> {
        if (query.isBlank()) return _series.value
        return _series.value.filter { 
            it.name?.contains(query, ignoreCase = true) == true 
        }
    }
    
    fun searchLiveStreams(query: String): List<LiveStream> {
        if (query.isBlank()) return _liveStreams.value
        return _liveStreams.value.filter { 
            it.name?.contains(query, ignoreCase = true) == true 
        }
    }
    
    // Filter by category
    fun getMoviesByCategory(categoryId: String?): List<VodItem> {
        if (categoryId == null) return _movies.value
        return _movies.value.filter { it.categoryId == categoryId }
    }
    
    fun getSeriesByCategory(categoryId: String?): List<SeriesItem> {
        if (categoryId == null) return _series.value
        return _series.value.filter { it.categoryId == categoryId }
    }
    
    fun getLiveStreamsByCategory(categoryId: String?): List<LiveStream> {
        if (categoryId == null) return _liveStreams.value
        return _liveStreams.value.filter { it.categoryId == categoryId }
    }
    
    // Find by ID
    fun findMovieById(streamId: Int): VodItem? {
        return _movies.value.find { it.streamId == streamId }
    }
    
    fun findSeriesById(seriesId: Int): SeriesItem? {
        return _series.value.find { it.seriesId == seriesId }
    }
    
    fun findLiveStreamById(streamId: Int): LiveStream? {
        return _liveStreams.value.find { it.streamId == streamId }
    }
    
    // Clear cache
    fun clear() {
        _movies.value = emptyList()
        _series.value = emptyList()
        _liveStreams.value = emptyList()
        _movieCategories.value = emptyList()
        _seriesCategories.value = emptyList()
        _liveCategories.value = emptyList()
        _isLoaded.value = false
        _isLoading.value = false
        lastLoadTime = 0
    }
    
    // Get counts for UI
    val movieCount: Int get() = _movies.value.size
    val seriesCount: Int get() = _series.value.size
    val liveStreamCount: Int get() = _liveStreams.value.size
}
