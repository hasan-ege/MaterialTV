package com.hasanege.materialtv

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: XtreamRepository
) : ViewModel() {

    private var allMovies: List<VodItem> = emptyList()
    private var allSeries: List<SeriesItem> = emptyList()
    private var allLiveStreams: List<LiveStream> = emptyList()

    private val _movies = mutableStateOf<UiState<List<VodItem>>>(UiState.Success(emptyList()))
    val movies: State<UiState<List<VodItem>>> = _movies

    private val _series = mutableStateOf<UiState<List<SeriesItem>>>(UiState.Success(emptyList()))
    val series: State<UiState<List<SeriesItem>>> = _series

    private val _liveStreams = mutableStateOf<UiState<List<LiveStream>>>(UiState.Success(emptyList()))
    val liveStreams: State<UiState<List<LiveStream>>> = _liveStreams

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadAllContent()
    }

    private fun loadAllContent() {
        viewModelScope.launch {
            val username = SessionManager.username ?: ""
            val password = SessionManager.password ?: ""
            
            _isLoading.value = true

            // Sequential for simplicity, but each flow will emit cache then network
            repository.getVodStreams(username, password, null).collect { resource ->
                when (resource) {
                    is com.hasanege.materialtv.network.Resource.Loading -> { } // Tracked by _isLoading if needed
                    is com.hasanege.materialtv.network.Resource.Success -> {
                        allMovies = resource.data
                        _movies.value = UiState.Success(allMovies)
                    }
                    is com.hasanege.materialtv.network.Resource.Error -> {
                        _movies.value = UiState.Error(resource.message)
                    }
                }
            }

            repository.getSeries(username, password, null).collect { resource ->
                 when (resource) {
                    is com.hasanege.materialtv.network.Resource.Loading -> { }
                    is com.hasanege.materialtv.network.Resource.Success -> {
                        allSeries = resource.data
                        _series.value = UiState.Success(allSeries)
                    }
                    is com.hasanege.materialtv.network.Resource.Error -> {
                        _series.value = UiState.Error(resource.message)
                    }
                }
            }

            repository.getLiveStreams(username, password, null).collect { resource ->
                when (resource) {
                    is com.hasanege.materialtv.network.Resource.Loading -> { }
                    is com.hasanege.materialtv.network.Resource.Success -> {
                        allLiveStreams = resource.data
                        _liveStreams.value = UiState.Success(allLiveStreams)
                    }
                    is com.hasanege.materialtv.network.Resource.Error -> {
                        _liveStreams.value = UiState.Error(resource.message)
                    }
                }
            }
            
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            if (query.isBlank()) {
                _movies.value = UiState.Success(allMovies)
                _series.value = UiState.Success(allSeries)
                _liveStreams.value = UiState.Success(allLiveStreams)
                return@launch
            }

            val filteredMovies = allMovies.filter { it.name?.contains(query, ignoreCase = true) == true }
            val filteredSeries = allSeries.filter { it.name?.contains(query, ignoreCase = true) == true }
            val filteredLiveStreams = allLiveStreams.filter { it.name?.contains(query, ignoreCase = true) == true }

            _movies.value = UiState.Success(filteredMovies)
            _series.value = UiState.Success(filteredSeries)
            _liveStreams.value = UiState.Success(filteredLiveStreams)
        }
    }
}
