package com.hasanege.materialtv.network

import kotlinx.serialization.Serializable

@Serializable
data class OmdbResponse(
    val Title: String? = null,
    val Year: String? = null,
    val Rated: String? = null,
    val Released: String? = null,
    val Runtime: String? = null,
    val Genre: String? = null,
    val Director: String? = null,
    val Writer: String? = null,
    val Actors: String? = null,
    val Plot: String? = null,
    val Language: String? = null,
    val Country: String? = null,
    val Awards: String? = null,
    val Poster: String? = null,
    val Metascore: String? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val imdbID: String? = null,
    val Type: String? = null,
    val Response: String? = null
)
