package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VodInfoResponse(
    @SerialName("info") val info: VodInfo? = null,
    @SerialName("movie_data") val movieData: MovieData? = null
)
