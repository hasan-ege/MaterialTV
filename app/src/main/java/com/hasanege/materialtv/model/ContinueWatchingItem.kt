package com.hasanege.materialtv.model

import kotlinx.serialization.Serializable

@Serializable
data class ContinueWatchingItem(
    val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val duration: Long, // Total duration in ms
    val position: Long, // Last watched position in ms
    val type: String, // "movie" or "series"
    val seriesId: Int? = null,
    var isPinned: Boolean = false,
    val episodeId: String? = null,
    val containerExtension: String? = null,
    var dismissedFromContinueWatching: Boolean = false,
    var actualWatchTime: Long = 0L, // Actual time spent watching in ms (excluding seeking/skipping)
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)
