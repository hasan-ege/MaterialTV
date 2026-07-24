package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonInfo(
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("cover_big") val coverBig: String? = null
)
