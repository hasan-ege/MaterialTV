package com.hasanege.materialtv.network.opensubtitles

import com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesDownloadRequest
import com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesDownloadResponse
import com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface OpenSubtitlesApiService {

    @Headers(
        "User-Agent: MaterialTV v3.1",
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @GET("api/v1/subtitles")
    suspend fun searchSubtitles(
        @Header("Api-Key") apiKey: String,
        @QueryMap params: Map<String, String>
    ): Response<OpenSubtitlesSearchResponse>

    @Headers(
        "User-Agent: MaterialTV v3.1",
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("api/v1/download")
    suspend fun downloadSubtitle(
        @Header("Api-Key") apiKey: String,
        @Body request: OpenSubtitlesDownloadRequest
    ): Response<OpenSubtitlesDownloadResponse>
}
