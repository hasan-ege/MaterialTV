package com.hasanege.materialtv.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface ImdbSuggestionApiService {
    @GET("suggestion/{firstChar}/{query}.json")
    suspend fun searchSuggestion(
        @Path("firstChar") firstChar: String,
        @Path("query") query: String
    ): ImdbSuggestionResponse
}

@Serializable
data class ImdbSuggestionResponse(
    val d: List<ImdbSuggestionItem>? = null
)

@Serializable
data class ImdbSuggestionItem(
    val q: String? = null,
    val l: String? = null,
    val y: Int? = null,
    val id: String? = null,
    val s: String? = null,
    val i: ImdbImage? = null
)

@Serializable
data class ImdbImage(
    val imageUrl: String? = null
)
