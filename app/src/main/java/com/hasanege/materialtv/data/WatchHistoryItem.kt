package com.hasanege.materialtv.data

data class WatchHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val watchedAt: String,
    val duration: Long,
    val watchedDuration: Long
)
