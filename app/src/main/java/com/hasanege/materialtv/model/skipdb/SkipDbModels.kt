package com.hasanege.materialtv.model.skipdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkipSegment(
    @SerialName("start_ms") val startMs: Long = 0,
    @SerialName("end_ms") val endMs: Long = 0,
    @SerialName("confidence") val confidence: Double? = null
)

@Serializable
data class SkipSegmentsContainer(
    @SerialName("intro") val intro: SkipSegment? = null,
    @SerialName("recap") val recap: SkipSegment? = null,
    @SerialName("outro") val outro: SkipSegment? = null,
    @SerialName("preview") val preview: SkipSegment? = null
)

@Serializable
data class SkipDbResponse(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("season") val season: Int? = null,
    @SerialName("episode") val episode: Int? = null,
    @SerialName("segments") val segments: SkipSegmentsContainer? = null
)
