package com.hasanege.materialtv.model

data class Achievement(
    val id: Int,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val category: AchievementCategory,
    val requirement: AchievementRequirement,
    val maxProgress: Int
)

enum class AchievementCategory(val displayName: String) {
    WATCH_TIME("İzleme Süresi"),
    CONTENT_COUNT("İçerik Sayısı"),
    GENRE("Tür Keşfi"),
    SPECIAL("Özel"),
    ENGAGEMENT("Etkileşim"),
    STREAK("Seri")
}

sealed class AchievementRequirement {
    data class WatchHours(val hours: Long) : AchievementRequirement()
    data class ItemsWatched(val count: Int) : AchievementRequirement()
    data class MoviesWatched(val count: Int) : AchievementRequirement()
    data class SeriesWatched(val count: Int) : AchievementRequirement()
    data class LiveWatched(val count: Int) : AchievementRequirement()
    data class CompletionRate(val percent: Int) : AchievementRequirement()
    data class UniqueDays(val days: Int) : AchievementRequirement()
    data class Custom(val check: (stats: UserStats) -> Boolean) : AchievementRequirement() {
        override fun equals(other: Any?): Boolean = other is Custom
        override fun hashCode(): Int = javaClass.hashCode()
    }
}

data class UserStats(
    val totalWatchTimeMs: Long = 0L,
    val totalItemsWatched: Int = 0,
    val totalMoviesWatched: Int = 0,
    val totalSeriesWatched: Int = 0,
    val totalLiveWatched: Int = 0,
    val completionRate: Int = 0,
    val uniqueDaysActive: Int = 0,
    val moviesCompleted: Int = 0,
    val seriesCompleted: Int = 0,
    val bingeCount: Int = 0,
    val favoritesCount: Int = 0,
    val downloadsCount: Int = 0
)
