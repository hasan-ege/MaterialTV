package com.hasanege.materialtv.model.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbConfigurationResponse(
    @SerialName("images") val images: TmdbImagesConfig? = null
)

@Serializable
data class TmdbImagesConfig(
    @SerialName("secure_base_url") val secureBaseUrl: String? = null
)

@Serializable
data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbMovieResult>? = null,
    @SerialName("tv_results") val tvResults: List<TmdbTvResult>? = null
)

@Serializable
data class TmdbSearchMovieResponse(
    @SerialName("results") val results: List<TmdbMovieResult>? = null
)

@Serializable
data class TmdbSearchTvResponse(
    @SerialName("results") val results: List<TmdbTvResult>? = null
)

@Serializable
data class TmdbMovieResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("popularity") val popularity: Double? = null
)

@Serializable
data class TmdbTvResult(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("popularity") val popularity: Double? = null
)

@Serializable
data class TmdbCreditsResponse(
    @SerialName("cast") val cast: List<TmdbCastMember>? = null,
    @SerialName("crew") val crew: List<TmdbCrewMember>? = null
)

@Serializable
data class TmdbCastMember(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("character") val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class TmdbCrewMember(
    @SerialName("name") val name: String = "",
    @SerialName("job") val job: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class TmdbExternalIdsResponse(
    @SerialName("imdb_id") val imdbId: String? = null
)

@Serializable
data class TmdbMovieDetailResponse(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("credits") val credits: TmdbCreditsResponse? = null,
    @SerialName("external_ids") val externalIds: TmdbExternalIdsResponse? = null
)

@Serializable
data class TmdbTvDetailResponse(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("credits") val credits: TmdbCreditsResponse? = null,
    @SerialName("external_ids") val externalIds: TmdbExternalIdsResponse? = null
)
