package com.hasanege.materialtv.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    val id: Long = 0,
    val contentId: Int,
    val contentType: String, // "movie", "series", "live"
    val name: String,
    val thumbnailUrl: String?,
    val addedAt: Long,
    val customThumbnail: String? = null,
    val rating: Float = 0f,
    val notes: String = "",
    val listId: Long = 0,
    val orderIndex: Int = 0,
    val genre: String? = null,
    val year: String? = null,
    val isWatched: Boolean = false,
    val categoryId: String? = null,
    val seriesId: Int? = null,
    val streamIcon: String? = null
)
