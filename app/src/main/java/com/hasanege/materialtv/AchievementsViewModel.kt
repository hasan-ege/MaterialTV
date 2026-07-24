package com.hasanege.materialtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.hasanege.materialtv.data.AchievementsProvider
import com.hasanege.materialtv.model.Achievement
import com.hasanege.materialtv.model.AchievementRequirement
import com.hasanege.materialtv.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AchievementsViewModel @Inject constructor() : ViewModel() {

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats

    private val _unlockedIds = MutableStateFlow<Set<Int>>(emptySet())
    val unlockedIds: StateFlow<Set<Int>> = _unlockedIds

    val achievements: List<Achievement> = AchievementsProvider.ALL

    val unlockedCount: StateFlow<Int> = _unlockedIds.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentLevel: StateFlow<UserLevel> = _stats.map { stats ->
        val hours = stats.totalWatchTimeMs / (1000 * 60 * 60)
        UserLevels.forHours(hours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserLevels.ALL.first())

    val currentLevelIndex: StateFlow<Int> = _stats.map { stats ->
        val hours = stats.totalWatchTimeMs / (1000 * 60 * 60)
        UserLevels.indexOfHours(hours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            WatchHistoryManager.historyFlow.collect { history ->
                val completed = history.count { it.duration > 0 && (it.position.toFloat() / it.duration.toFloat()) > 0.95f }
                val newStats = UserStats(
                    totalWatchTimeMs = history.sumOf { it.actualWatchTime },
                    totalItemsWatched = history.size,
                    totalMoviesWatched = history.count { it.type == "movie" },
                    totalSeriesWatched = history.count { it.type == "series" },
                    totalLiveWatched = history.count { it.type == "live" },
                    completionRate = if (history.isEmpty()) 0
                        else ((completed.toFloat() / history.size.toFloat()) * 100).toInt(),
                    uniqueDaysActive = 0,
                    moviesCompleted = 0,
                    seriesCompleted = 0,
                    bingeCount = 0,
                    favoritesCount = 0,
                    downloadsCount = 0
                )
                _stats.value = newStats
                updateUnlockedAchievements(newStats)
            }
        }
    }

    private fun updateUnlockedAchievements(stats: UserStats) {
        val newlyUnlocked = mutableSetOf<Int>()
        val hours = stats.totalWatchTimeMs / (1000 * 60 * 60)

        for (achievement in achievements) {
            val unlocked = when (val req = achievement.requirement) {
                is AchievementRequirement.WatchHours -> hours >= req.hours
                is AchievementRequirement.ItemsWatched -> stats.totalItemsWatched >= req.count
                is AchievementRequirement.MoviesWatched -> stats.totalMoviesWatched >= req.count
                is AchievementRequirement.SeriesWatched -> stats.totalSeriesWatched >= req.count
                is AchievementRequirement.LiveWatched -> stats.totalLiveWatched >= req.count
                is AchievementRequirement.CompletionRate -> stats.completionRate >= req.percent
                is AchievementRequirement.UniqueDays -> stats.uniqueDaysActive >= req.days
                is AchievementRequirement.Custom -> req.check(stats)
            }
            if (unlocked) newlyUnlocked.add(achievement.id)
        }
        _unlockedIds.value = newlyUnlocked
    }

    fun getProgress(achievement: Achievement): Int {
        val stats = _stats.value
        val hours = stats.totalWatchTimeMs / (1000 * 60 * 60)
        return when (val req = achievement.requirement) {
            is AchievementRequirement.WatchHours -> (hours.coerceAtMost(req.hours)).toInt()
            is AchievementRequirement.ItemsWatched -> stats.totalItemsWatched.coerceAtMost(req.count)
            is AchievementRequirement.MoviesWatched -> stats.totalMoviesWatched.coerceAtMost(req.count)
            is AchievementRequirement.SeriesWatched -> stats.totalSeriesWatched.coerceAtMost(req.count)
            is AchievementRequirement.LiveWatched -> stats.totalLiveWatched.coerceAtMost(req.count)
            is AchievementRequirement.CompletionRate -> stats.completionRate.coerceAtMost(req.percent)
            is AchievementRequirement.UniqueDays -> stats.uniqueDaysActive.coerceAtMost(req.days)
            is AchievementRequirement.Custom -> if (req.check(stats)) 1 else 0
        }
    }

    fun getProgressMax(achievement: Achievement): Int {
        return when (val req = achievement.requirement) {
            is AchievementRequirement.WatchHours -> req.hours.toInt()
            is AchievementRequirement.ItemsWatched -> req.count
            is AchievementRequirement.MoviesWatched -> req.count
            is AchievementRequirement.SeriesWatched -> req.count
            is AchievementRequirement.LiveWatched -> req.count
            is AchievementRequirement.CompletionRate -> req.percent
            is AchievementRequirement.UniqueDays -> req.days
            is AchievementRequirement.Custom -> 1
        }
    }
}
