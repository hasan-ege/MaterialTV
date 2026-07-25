package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.ContinueWatchingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor() : ViewModel() {

    private val _filterType = MutableStateFlow<String?>(null)
    val filterType: StateFlow<String?> = _filterType

    val history: StateFlow<List<ContinueWatchingItem>> = combine(
        WatchHistoryManager.historyFlow,
        _filterType
    ) { historyList, filter ->
        val filtered = if (filter.isNullOrEmpty() || filter == "ALL") {
            historyList
        } else {
            val typeStr = when (filter) {
                "MOVIES" -> "movie"
                "SERIES" -> "series"
                "LIVE" -> "live"
                else -> filter.lowercase()
            }
            historyList.filter { it.type == typeStr }
        }
        filtered.sortedByDescending { it.isPinned }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(type: String?) {
        _filterType.value = type
    }

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
