package com.hasanege.materialtv.model

import kotlinx.serialization.Serializable

@Serializable
data class EpgListing(
    val id: String? = null,
    val epg_id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class EpgResponse(
    val epg_listings: List<EpgListing> = emptyList()
)
