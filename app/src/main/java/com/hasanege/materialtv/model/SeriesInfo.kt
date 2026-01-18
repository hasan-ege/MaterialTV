package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesInfo(
    @SerialName("name") val name: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Float? = null,
    @SerialName("imdbRating") val imdbRating: String? = null,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("trailer") val trailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("full_plot") val fullPlot: String? = null,
    @SerialName("imdb_cast") val imdbCast: List<CastMember>? = null,
    @SerialName("runtime") val runtime: String? = null,
    @SerialName("writer") val writer: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("awards") val awards: String? = null,
    @SerialName("metascore") val metascore: String? = null,
    @SerialName("imdb_votes") val imdbVotes: String? = null,
    val rated: String? = null,  // MPAA Rating
    val contentRating: ContentRating? = null,  // Turkish Akıllı İşaretler
    val imdbReviews: List<ImdbReview>? = null,
    val imdbID: String? = null
)
