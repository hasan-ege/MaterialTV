
package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.hasanege.materialtv.data.Playlist
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.network.CredentialsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val profilePreferences: com.hasanege.materialtv.data.ProfilePreferences,
    private val credentialsManager: CredentialsManager
) : ViewModel() {

    // Profile Customization Flows
    val profileName = profilePreferences.profileName.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = "User"
    )

    val profileImageUrl = profilePreferences.profileImageUrl.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val watchHistory: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
// ... (rest of stats logic same as before, skipping lines for brevity if possible, or just re-pasting the whole needed block)
// I better use replace_file_content carefully to avoid deleting stats logic.
// The file is small enough, I will replace the class def and factory. 

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
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalWatchTime: StateFlow<Long> = WatchHistoryManager.historyFlow
        .map { history ->
            history.sumOf { it.actualWatchTime }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )
    
    val totalItemsWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.size }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    val totalMoviesWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.count { it.type == "movie" } }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    val totalSeriesWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.count { it.type == "series" } }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val completionRate: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history ->
            if (history.isEmpty()) 0
            else {
                val completed = history.count { it.duration > 0 && (it.position.toFloat() / it.duration.toFloat()) > 0.95f }
                ((completed.toFloat() / history.size.toFloat()) * 100).toInt()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val userLevel: StateFlow<String> = totalWatchTime
        .map { timeMs ->
            val hours = timeMs / (1000 * 60 * 60)
            when {
                hours > 100 -> "Binge Master \uD83D\uDC51" // Crown
                hours > 50 -> "Pro Watcher \uD83C\uDFAC" // Clapper board
                hours > 20 -> "Regular \uD83D\uDC40" // Eyes
                hours > 5 -> "Novice \uD83D\uDC23" // Chick
                else -> "Newbie \uD83C\uDF31" // Seedling
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = "Newbie"
        )
        
    val averageWatchTimePerItem: StateFlow<String> = kotlinx.coroutines.flow.combine(totalWatchTime, totalItemsWatched) { time, count ->
        if (count == 0) "0m"
        else {
            val avgMs = time / count
            val minutes = avgMs / (1000 * 60)
            "${minutes}m"
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "0m")

    // Percentage of movies vs series vs live
    // We will compute ratios for all 3.
    // If we want a simple visual bar, we need weights.
    // Let's expose raw counts primarily, but since UI used ratio, let's just make sure we have all of them.
    
    val totalLiveWatched: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history -> history.count { it.type == "live" } }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        
    // Ratios (0-100)
    val movieRatio: StateFlow<Int> = kotlinx.coroutines.flow.combine(totalMoviesWatched, totalItemsWatched) { movies, total ->
        if (total == 0) 0 else ((movies.toFloat() / total.toFloat()) * 100).toInt()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    val seriesRatio: StateFlow<Int> = kotlinx.coroutines.flow.combine(totalSeriesWatched, totalItemsWatched) { series, total ->
        if (total == 0) 0 else ((series.toFloat() / total.toFloat()) * 100).toInt()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    val liveRatio: StateFlow<Int> = kotlinx.coroutines.flow.combine(totalLiveWatched, totalItemsWatched) { live, total ->
        if (total == 0) 0 else ((live.toFloat() / total.toFloat()) * 100).toInt()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    // Items started but not finished (< 90% and > 5%)
    val unfinishedItemsCount: StateFlow<Int> = WatchHistoryManager.historyFlow
        .map { history ->
            history.count { item ->
                val progress = if (item.duration > 0) (item.position.toFloat() / item.duration.toFloat()) else 0f
                progress in 0.05f..0.90f
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    fun logout() {
        com.hasanege.materialtv.network.SessionManager.clear()
        credentialsManager.clearCredentials()
    }
}


