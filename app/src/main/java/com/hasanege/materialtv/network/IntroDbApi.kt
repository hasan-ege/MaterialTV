package com.hasanege.materialtv.network

import com.hasanege.materialtv.model.IntroDbSegmentsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface IntroDbApi {
    @GET("segments")
    suspend fun getSegments(
        @Query("imdb_id") imdbId: String,
        @Query("season") season: Int,
        @Query("episode") episode: Int
    ): IntroDbSegmentsResponse
}
