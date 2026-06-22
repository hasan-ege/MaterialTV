package com.hasanege.materialtv

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

sealed class CategoryData {
    data class Movies(val items: List<VodItem>) : CategoryData()
    data class Series(val items: List<com.hasanege.materialtv.model.SeriesItem>) : CategoryData()
    data class LiveStreams(val items: List<com.hasanege.materialtv.model.LiveStream>) : CategoryData()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: XtreamRepository
) : ViewModel() {

    val uiState = mutableStateOf<UiState<CategoryData>>(UiState.Loading as UiState<CategoryData>)

    fun loadCategoryItems(categoryId: String, categoryType: String) {
        viewModelScope.launch {
            val username = SessionManager.username ?: ""
            val password = SessionManager.password ?: ""

            when (categoryType) {
                "movie" -> {
                    repository.getVodStreams(username, password, categoryId).collect { resource ->
                        uiState.value = when (resource) {
                            is com.hasanege.materialtv.network.Resource.Loading -> UiState.Loading
                            is com.hasanege.materialtv.network.Resource.Success -> UiState.Success(CategoryData.Movies(resource.data))
                            is com.hasanege.materialtv.network.Resource.Error -> UiState.Error(resource.message)
                        }
                    }
                }
                "series" -> {
                    repository.getSeries(username, password, categoryId).collect { resource ->
                        uiState.value = when (resource) {
                            is com.hasanege.materialtv.network.Resource.Loading -> UiState.Loading
                            is com.hasanege.materialtv.network.Resource.Success -> UiState.Success(CategoryData.Series(resource.data))
                            is com.hasanege.materialtv.network.Resource.Error -> UiState.Error(resource.message)
                        }
                    }
                }
                "live" -> {
                    repository.getLiveStreams(username, password, categoryId).collect { resource ->
                        uiState.value = when (resource) {
                            is com.hasanege.materialtv.network.Resource.Loading -> UiState.Loading
                            is com.hasanege.materialtv.network.Resource.Success -> UiState.Success(CategoryData.LiveStreams(resource.data))
                            is com.hasanege.materialtv.network.Resource.Error -> UiState.Error(resource.message)
                        }
                    }
                }
                else -> {
                    uiState.value = UiState.Error("Invalid category type")
                }
            }
        }
    }
}
