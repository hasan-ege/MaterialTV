package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntroDbSegment(
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    @SerialName("start_sec") val startSec: Float,
    @SerialName("end_sec") val endSec: Float,
    val confidence: Float,
    @SerialName("submission_count") val submissionCount: Int
)

@Serializable
data class IntroDbSegmentsResponse(
    @SerialName("imdb_id") val imdbId: String,
    val season: Int,
    val episode: Int,
    val intro: IntroDbSegment? = null,
    val recap: IntroDbSegment? = null,
    val outro: IntroDbSegment? = null
)
