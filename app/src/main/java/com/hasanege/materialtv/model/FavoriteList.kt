package com.hasanege.materialtv.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteList(
    val listId: Long = 0,
    val listName: String,
    val createdAt: Long,
    val orderIndex: Int = 0,
    val iconName: String? = null,
    val colorHex: String? = null,
    val itemCount: Int = 0 // populated separately
)
