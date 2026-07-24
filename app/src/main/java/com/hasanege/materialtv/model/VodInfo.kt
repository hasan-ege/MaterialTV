package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VodInfo(
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Float = 0f,
    @SerialName("imdbRating") val imdbRating: String? = null,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("trailer") val trailer: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("year") val year: String? = null,
    val fullPlot: String? = null,
    val imdbCast: List<CastMember>? = null,
    val runtime: String? = null,
    val writer: String? = null,
    val language: String? = null,
    val country: String? = null,
    val awards: String? = null,
    val metascore: String? = null,
    val imdbVotes: String? = null,
    val rated: String? = null,  // MPAA Rating (G, PG, PG-13, R, NC-17)
    val contentRating: ContentRating? = null,  // Turkish Akıllı İşaretler
    val imdbReviews: List<ImdbReview>? = null,
    val imdbID: String? = null
)
