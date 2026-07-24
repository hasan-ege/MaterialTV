package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SeriesInfoResponse(
    @SerialName("info") val info: SeriesInfo? = null,
    @SerialName("episodes") val episodes: JsonElement? = null,
    @SerialName("seasons") val seasons: List<SeasonInfo>? = null
)
