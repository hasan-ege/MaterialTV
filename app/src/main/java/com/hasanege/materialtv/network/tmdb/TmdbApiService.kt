package com.hasanege.materialtv.network.tmdb

import com.hasanege.materialtv.model.tmdb.TmdbConfigurationResponse
import com.hasanege.materialtv.model.tmdb.TmdbFindResponse
import com.hasanege.materialtv.model.tmdb.TmdbSearchMovieResponse
import com.hasanege.materialtv.model.tmdb.TmdbSearchTvResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

interface TmdbApiService {

    companion object {
        /** Returns the device locale as a TMDB-compatible language tag (e.g. "tr-TR", "en-US"). */
        fun getDeviceLanguage(): String {
            val locale = Locale.getDefault()
            val language = locale.language   // e.g. "tr"
            val country = locale.country     // e.g. "TR"
            return if (country.isNotBlank()) "$language-$country" else language
        }
    }

    @GET("configuration")
    suspend fun getConfiguration(
        @Query("api_key") apiKey: String
    ): TmdbConfigurationResponse

    @GET("find/{external_id}")
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("api_key") apiKey: String,
        @Query("external_source") externalSource: String = "imdb_id",
        @Query("language") language: String = getDeviceLanguage()
    ): TmdbFindResponse

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("year") year: String? = null,
        @Query("language") language: String = getDeviceLanguage()
    ): TmdbSearchMovieResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("first_air_date_year") year: String? = null,
        @Query("language") language: String = getDeviceLanguage()
    ): TmdbSearchTvResponse

    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = getDeviceLanguage(),
        @Query("append_to_response") appendToResponse: String = "credits,external_ids"
    ): com.hasanege.materialtv.model.tmdb.TmdbMovieDetailResponse

    @GET("tv/{id}")
    suspend fun getTvDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = getDeviceLanguage(),
        @Query("append_to_response") appendToResponse: String = "credits,external_ids"
    ): com.hasanege.materialtv.model.tmdb.TmdbTvDetailResponse
}
