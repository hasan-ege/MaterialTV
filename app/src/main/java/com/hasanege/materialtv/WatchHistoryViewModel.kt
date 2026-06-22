package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.ContinueWatchingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor() : ViewModel() {

    val history: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val continueWatching: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .map { history ->
            history.filter { item ->
                if (item.dismissedFromContinueWatching) return@filter false
                val progress = if (item.duration > 0) {
                    (item.position.toFloat() / item.duration.toFloat())
                } else {
                    0f
                }
                progress < 0.95f && progress > 0.01f
            }.sortedByDescending { it.isPinned }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeItem(item: ContinueWatchingItem) {
        WatchHistoryManager.removeItem(item)
    }

    fun clearHistory() {
        WatchHistoryManager.clearHistory()
    }
}
