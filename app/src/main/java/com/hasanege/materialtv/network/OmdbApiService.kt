package com.hasanege.materialtv.network

import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {
    @GET("?")
    suspend fun searchMedia(
        @Query("apikey") apiKey: String = "YOUR_OMDB_API_KEY", // Assuming default logic or it's injected
        @Query("t") title: String,
        @Query("type") type: String? = null
    ): OmdbResponse
}
