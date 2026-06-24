
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
    private val credentialsManager: CredentialsManager,
    private val xtreamRepository: com.hasanege.materialtv.repository.XtreamRepository
) : ViewModel() {

    private val _liveStreams = MutableStateFlow<List<com.hasanege.materialtv.model.LiveStream>>(emptyList())
    val liveStreams: StateFlow<List<com.hasanege.materialtv.model.LiveStream>> = _liveStreams

    private val _channelsEpg = MutableStateFlow<Map<Int, List<com.hasanege.materialtv.model.EpgListing>>>(emptyMap())
    val channelsEpg: StateFlow<Map<Int, List<com.hasanege.materialtv.model.EpgListing>>> = _channelsEpg

    private val _channelsEpgLoading = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelsEpgLoading: StateFlow<Map<Int, Boolean>> = _channelsEpgLoading


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
            UserLevels.forHours(hours).title
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = UserLevels.ALL.first().title
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

    fun setProfileImageFromBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            profilePreferences.setProfileImageFromBitmap(bitmap)
        }
    }

    val recentLiveStreams: StateFlow<List<ContinueWatchingItem>> = WatchHistoryManager.historyFlow
        .map { history ->
            history.filter { it.type == "live" }
                .reversed()
                .take(5)
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val activeInterests: StateFlow<List<String>> = WatchHistoryManager.historyFlow
        .map { history ->
            // In a real app we'd map genre IDs to names. We'll use mocked categories for now.
            val mockInterests = listOf("Sci-Fi", "Noir", "Thriller", "Action", "Comedy", "Drama")
            val count = history.size
            if (count == 0) listOf("Sci-Fi", "Noir", "Thriller", "Mystery")
            else mockInterests.shuffled().take(4) // Mock logic based on history size to show dynamism
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), listOf("Sci-Fi", "Noir", "Thriller", "Mystery"))

    val selectedUpcomingChannels: StateFlow<List<ContinueWatchingItem>> = profilePreferences.selectedUpcomingChannels
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedUpcomingChannel(id: String, name: String, icon: String) {
        val channel = ContinueWatchingItem(
            streamId = id.toIntOrNull() ?: 0,
            name = name,
            streamIcon = icon,
            type = "live",
            position = 0L,
            duration = 0L,
            actualWatchTime = 0L
        )
        setSelectedUpcomingChannels(listOf(channel))
    }

    fun setSelectedUpcomingChannels(channels: List<ContinueWatchingItem>) {
        viewModelScope.launch {
            profilePreferences.setSelectedUpcomingChannels(channels)
        }
    }

    init {
        viewModelScope.launch {
            val username = credentialsManager.getUsername()
            val password = credentialsManager.getPassword()
            if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                xtreamRepository.getLiveStreams(username, password, "all").collect { result ->
                    if (result is com.hasanege.materialtv.network.Resource.Success) {
                        _liveStreams.value = result.data
                    }
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(selectedUpcomingChannels, recentLiveStreams) { selected, recent ->
                selected.ifEmpty { recent.firstOrNull()?.let { listOf(it) } ?: emptyList() }
            }.collect { activeChannels ->
                val username = credentialsManager.getUsername()
                val password = credentialsManager.getPassword()
                if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    activeChannels.forEach { channel ->
                        viewModelScope.launch {
                            _channelsEpgLoading.value = _channelsEpgLoading.value + (channel.streamId to true)
                            try {
                                val epgList = xtreamRepository.getShortEpg(username, password, channel.streamId)
                                _channelsEpg.value = _channelsEpg.value + (channel.streamId to epgList)
                            } catch (e: Exception) {
                                _channelsEpg.value = _channelsEpg.value + (channel.streamId to emptyList())
                            } finally {
                                _channelsEpgLoading.value = _channelsEpgLoading.value + (channel.streamId to false)
                            }
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        com.hasanege.materialtv.network.SessionManager.clear()
        credentialsManager.clearCredentials()
    }
}

data class UserLevel(
    val title: String,
    val emoji: String,
    val minHours: Long,          // bu seviyeye ulaşmak için gereken min saat
    val maxHours: Long,          // bir sonraki seviyenin başladığı saat (son seviye = Long.MAX_VALUE)
    val description: String
)

object UserLevels {
    val ALL: List<UserLevel> = listOf(
        UserLevel("Yeni Başlayan",  "🌱", 0,   2,    "Yolculuğuna hoş geldin!"),
        UserLevel("Meraklı",        "👀", 2,   5,    "İzlemeyi sevmeye başladın."),
        UserLevel("Seyirci",        "🪑", 5,   10,   "Düzenli bir izleyici olmaya başlıyorsun."),
        UserLevel("Amatör",         "🐣", 10,  20,   "İzleme alışkanlığın oturmaya başladı."),
        UserLevel("Tutkulu",        "❤️", 20,  35,   "Ekran başından kalkmak zorlaşıyor."),
        UserLevel("Dizi Kurdu",     "📺", 35,  50,   "Dizilere bağımlılık seviyesi: tehlikeli."),
        UserLevel("Film Keyfi",     "🎬", 50,  70,   "Sinema zevki gelişmiş biri."),
        UserLevel("Binge Watcher",  "🍿", 70,  100,  "Bir oturuşta tüm sezonu bitirebilirsin."),
        UserLevel("Gece Kuşu",      "🦉", 100, 150,  "Gece yarısı bölüm bitirmek sıradan."),
        UserLevel("Profesyonel",    "🎭", 150, 200,  "İzleme konusunda profesyonelleştin."),
        UserLevel("Uzman",          "🏆", 200, 300,  "Neredeyse her şeyi izlediniz."),
        UserLevel("Eleştirmen",     "🎖️", 300, 400, "İzlediklerini değerlendiren biri."),
        UserLevel("Efsane",         "👑", 400, 500,  "Efsane statüsüne ulaştın!"),
        UserLevel("Binge Master",   "💎", 500, 750,  "Binge izlemenin zirvesi."),
        UserLevel("Hall of Fame",   "🌟", 750, Long.MAX_VALUE, "İzleme tarihine geçtin.")
    )

    fun forHours(hours: Long): UserLevel =
        ALL.lastOrNull { hours >= it.minHours } ?: ALL.first()

    fun indexOfHours(hours: Long): Int =
        ALL.indexOfLast { hours >= it.minHours }.coerceAtLeast(0)
}
