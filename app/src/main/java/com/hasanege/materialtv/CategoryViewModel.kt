package com.hasanege.materialtv

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoryData {
    data class Movies(val items: Flow<PagingData<VodItem>>) : CategoryData()
    data class Series(val items: Flow<PagingData<SeriesItem>>) : CategoryData()
    data class LiveStreams(val items: Flow<PagingData<LiveStream>>) : CategoryData()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryData?>(null)
    val uiState = _uiState.asStateFlow()

    fun loadCategoryItems(categoryId: String, categoryType: String) {
        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        when (categoryType) {
            "movie" -> {
                val pagedFlow = repository.getVodStreamsPaged(username, categoryId).cachedIn(viewModelScope)
                _uiState.value = CategoryData.Movies(pagedFlow)
            }
            "series" -> {
                val pagedFlow = repository.getSeriesPaged(username, categoryId).cachedIn(viewModelScope)
                _uiState.value = CategoryData.Series(pagedFlow)
            }
            "live" -> {
                val pagedFlow = repository.getLiveStreamsPaged(username, categoryId).cachedIn(viewModelScope)
                _uiState.value = CategoryData.LiveStreams(pagedFlow)
            }
        }
    }
}
