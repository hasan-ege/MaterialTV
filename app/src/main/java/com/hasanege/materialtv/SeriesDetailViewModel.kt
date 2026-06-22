package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repository: XtreamRepository,
    private val settingsRepository: com.hasanege.materialtv.data.SettingsRepository
) : ViewModel() {

    var seriesInfoState by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)
        private set

    fun loadSeriesInfo(username: String, password: String, seriesId: Int, initialName: String? = null) {
        viewModelScope.launch {
            seriesInfoState = UiState.Loading
            try {
                val seriesInfoResponse = repository.getSeriesInfo(username, password, seriesId)
                if (seriesInfoResponse != null) {
                    seriesInfoState = UiState.Success(seriesInfoResponse)
                } else {
                    seriesInfoState = UiState.Error("Series data not found")
                }
            } catch (e: Exception) {
                seriesInfoState = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }
}
