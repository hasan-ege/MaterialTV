package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.EpgListing
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpgUiState>(EpgUiState.Initial)
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private var currentStreamId: Int? = null

    fun fetchEpg(streamId: Int) {
        if (_uiState.value is EpgUiState.Loading && currentStreamId == streamId) return
        currentStreamId = streamId
        
        val username = SessionManager.username ?: return
        val password = SessionManager.password ?: return

        viewModelScope.launch {
            _uiState.value = EpgUiState.Loading
            try {
                val epgList = xtreamRepository.getShortEpg(username, password, streamId)
                if (epgList.isEmpty()) {
                    _uiState.value = EpgUiState.Empty
                } else {
                    _uiState.value = EpgUiState.Success(epgList)
                }
            } catch (e: Exception) {
                _uiState.value = EpgUiState.Error(e.message ?: "Failed to fetch EPG")
            }
        }
    }
}

sealed class EpgUiState {
    object Initial : EpgUiState()
    object Loading : EpgUiState()
    object Empty : EpgUiState()
    data class Success(val epgList: List<EpgListing>) : EpgUiState()
    data class Error(val message: String) : EpgUiState()
}
