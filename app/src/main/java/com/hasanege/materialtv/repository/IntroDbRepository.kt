package com.hasanege.materialtv.repository

import com.hasanege.materialtv.model.IntroDbSegmentsResponse
import com.hasanege.materialtv.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntroDbRepository @Inject constructor() {

    private val apiService by lazy {
        RetrofitClient.getIntroDbClient()
    }

    suspend fun getSegments(imdbId: String, season: Int, episode: Int): IntroDbSegmentsResponse? = withContext(Dispatchers.IO) {
        try {
            apiService.getSegments(imdbId, season, episode)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
