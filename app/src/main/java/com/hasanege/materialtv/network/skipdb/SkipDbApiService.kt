package com.hasanege.materialtv.network.skipdb

import com.hasanege.materialtv.model.skipdb.SkipDbResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SkipDbApiService {
    @GET("api/segments")
    suspend fun getSegments(
        @Query("imdb_id") imdbId: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null,
        @Query("duration") duration: Int? = null
    ): SkipDbResponse
}
