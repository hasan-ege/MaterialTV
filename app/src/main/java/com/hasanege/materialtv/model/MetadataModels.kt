package com.hasanege.materialtv.model

import kotlinx.serialization.Serializable

@Serializable
data class CastMember(
    val name: String,
    val character: String? = null,
    val profileImageUrl: String? = null
)

@Serializable
data class ContentRating(
    val rating: String,
    val icons: List<String> = emptyList(),
    val description: String? = null
)

@Serializable
data class ImdbReview(
    val author: String,
    val rating: String? = null,
    val content: String,
    val date: String? = null
)
