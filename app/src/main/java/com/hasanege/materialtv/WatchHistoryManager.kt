package com.hasanege.materialtv

import android.content.Context
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.repository.WatchHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Legacy WatchHistoryManager wrapper that uses WatchHistoryRepository.
 * Gradually migrate classes to inject WatchHistoryRepository instead.
 */
object WatchHistoryManager {
    private lateinit var repository: WatchHistoryRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _historyFlow = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val historyFlow: StateFlow<List<ContinueWatchingItem>> = _historyFlow

    fun initialize(repo: WatchHistoryRepository) {
        repository = repo
        scope.launch {
            repository.observeHistory().collect { history ->
                _historyFlow.value = history
            }
        }
    }
    
    // For older compatibility in initialize(Context) which was used in MainApplication
    fun initialize(context: Context) {
        // Now handled by Hilt injection in MainApplication
    }

    fun getHistory(): List<ContinueWatchingItem> {
        return _historyFlow.value.sortedByDescending { it.isPinned }
    }

    fun getDownloadId(uri: String): Int {
        return "downloaded_${uri.hashCode()}".hashCode()
    }

    // Get only items that are not finished (for Continue Watching)
    fun getContinueWatching(thresholdMinutes: Int = 5): List<ContinueWatchingItem> {
        return _historyFlow.value
            .filter { item ->
                // Don't show dismissed items
                if (item.dismissedFromContinueWatching) return@filter false
                
                // Don't show downloaded items on Home screen
                if (item.isDownloaded) return@filter false

                // For series, only show the latest episode per series
                if (item.type == "series" && item.seriesId != null) {
                    val seriesItems = _historyFlow.value.filter { 
                        it.seriesId == item.seriesId && 
                        it.type == "series" && 
                        !it.dismissedFromContinueWatching 
                    }
                    // Only show if this is the most recently watched episode of this series
                    val latestItem = seriesItems.maxByOrNull { it.position }
                    return@filter item.streamId == latestItem?.streamId
                }
                
                // Show if NOT finished based on threshold
                !isFinished(item, thresholdMinutes)
            }
            .sortedByDescending { it.isPinned }
    }

    // Get full watch history (all items)
    fun getFullHistory(): List<ContinueWatchingItem> {
        return _historyFlow.value
    }

    fun saveItem(item: ContinueWatchingItem) {
        scope.launch {
            repository.saveItem(item)
        }
    }

    // Dismiss from Continue Watching (but keep in history)
    fun dismissItem(item: ContinueWatchingItem) {
        scope.launch {
            repository.dismissItem(item)
        }
    }

    // Completely remove from history
    fun removeItem(item: ContinueWatchingItem) {
        scope.launch {
            repository.removeItem(item)
        }
    }

    fun togglePin(item: ContinueWatchingItem) {
        scope.launch {
            repository.togglePin(item)
        }
    }

    fun clearHistory() {
        scope.launch {
            repository.clearHistory()
        }
    }

    // Get total actual watch time (excluding seeking/skipping)
    fun getTotalActualWatchTime(): Long {
        return getFullHistory().sumOf { it.actualWatchTime }
    }

    // Update item with actual watch time tracking
    fun saveItemWithWatchTime(item: ContinueWatchingItem, additionalWatchTime: Long) {
        scope.launch {
            repository.saveItemWithWatchTime(item, additionalWatchTime)
        }
    }

    // Helper to check if an item is considered "finished" based on threshold
    fun isFinished(item: ContinueWatchingItem, thresholdMinutes: Int): Boolean {
         if (item.duration <= 0) return false
         val remainingMillis = item.duration - item.position
         val thresholdMillis = thresholdMinutes * 60 * 1000L
         return remainingMillis <= thresholdMillis
    }
}
