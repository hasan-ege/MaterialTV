package com.hasanege.materialtv.utils

import com.hasanege.materialtv.model.CastMember
import com.hasanege.materialtv.network.OmdbResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImdbScraper {
    suspend fun scrapeDetails(imdbId: String): OmdbResponse? = withContext(Dispatchers.IO) {
        null // Fallback for missing implementation
    }

    suspend fun scrapeCast(imdbId: String): List<CastMember> = withContext(Dispatchers.IO) {
        emptyList()
    }

    suspend fun scrapeParentalGuide(imdbId: String): List<String> = withContext(Dispatchers.IO) {
        emptyList()
    }
}
