package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString

data class SeriesDetailData(
    val xtreamData: SeriesInfoResponse,
    val tmdbData: com.hasanege.materialtv.data.entities.TmdbContentEntity? = null
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repository: XtreamRepository,
    private val settingsRepository: com.hasanege.materialtv.data.SettingsRepository,
    private val tmdbDao: com.hasanege.materialtv.data.dao.TmdbDao,
    private val tmdbApiService: com.hasanege.materialtv.network.tmdb.TmdbApiService
) : ViewModel() {

    var seriesInfoState by mutableStateOf<UiState<SeriesDetailData>>(UiState.Loading)
        private set

    fun loadSeriesInfo(username: String, password: String, seriesId: Int, initialName: String? = null) {
        viewModelScope.launch {
            seriesInfoState = UiState.Loading
            try {
                val seriesInfoResponse = repository.getSeriesInfo(username, password, seriesId)
                if (seriesInfoResponse != null) {
                    seriesInfoState = UiState.Success(SeriesDetailData(seriesInfoResponse))
                    
                    // TMDB Fetch
                    val tmdbKey = settingsRepository.tmdbApiKey.firstOrNull()
                    if (!tmdbKey.isNullOrBlank()) {
                        android.util.Log.d("TMDB_LOG", "TMDB API Key found. Checking local DB for series ID: $seriesId")
                        val profileId = com.hasanege.materialtv.network.SessionManager.username ?: "default"
                        val localTmdb = tmdbDao.getTmdbContent(seriesId.toString(), com.hasanege.materialtv.data.entities.ContentType.SERIES, profileId)
                        if (localTmdb != null) {
                            android.util.Log.d("TMDB_LOG", "Loaded TMDB data from local DB for series '$seriesId': ${localTmdb.title}, Banner: ${localTmdb.backdropPath}")
                            seriesInfoState = UiState.Success(SeriesDetailData(seriesInfoResponse, localTmdb))
                        } else {
                            var bestMatch: com.hasanege.materialtv.model.tmdb.TmdbTvResult? = null
                            val imdbId = seriesInfoResponse.info?.imdbID?.takeIf { it != "N/A" && it.isNotBlank() }

                            if (!imdbId.isNullOrBlank()) {
                                try {
                                    android.util.Log.d("TMDB_LOG", "Finding TMDB TV series by IMDb ID: $imdbId")
                                    val findResponse = tmdbApiService.findByExternalId(imdbId, tmdbKey)
                                    bestMatch = findResponse.tvResults?.firstOrNull()
                                } catch (e: Exception) {
                                    android.util.Log.e("TMDB_LOG", "Error finding TV series by IMDb ID", e)
                                }
                            }

                            if (bestMatch == null) {
                                val rawTitle = seriesInfoResponse.info?.name ?: initialName ?: ""
                                val (extractedYear, cleanTitle) = com.hasanege.materialtv.network.tmdb.TmdbTitleCleaner.extractYearAndCleanTitle(rawTitle)
                                val yearToSearch = extractedYear ?: seriesInfoResponse.info?.releaseDate?.take(4)

                                if (cleanTitle.isNotBlank()) {
                                    android.util.Log.d("TMDB_LOG", "Searching TMDB API for series: '$cleanTitle' (Year: $yearToSearch, Raw: '$rawTitle')")
                                    try {
                                        // 1st attempt: Search with cleanTitle and year
                                        var searchResponse = tmdbApiService.searchTv(cleanTitle, tmdbKey, yearToSearch)
                                        var results = searchResponse.results?.filter { !it.backdropPath.isNullOrBlank() || !it.posterPath.isNullOrBlank() }

                                        // 2nd attempt fallback: If no result with year, search cleanTitle WITHOUT year
                                        if (results.isNullOrEmpty() && yearToSearch != null) {
                                            android.util.Log.d("TMDB_LOG", "Retrying TMDB series search for '$cleanTitle' without year constraint")
                                            searchResponse = tmdbApiService.searchTv(cleanTitle, tmdbKey, null)
                                            results = searchResponse.results?.filter { !it.backdropPath.isNullOrBlank() || !it.posterPath.isNullOrBlank() }
                                        }

                                        // Pick popularity or first match
                                        bestMatch = results?.maxByOrNull { it.popularity ?: 0.0 } ?: searchResponse.results?.firstOrNull()
                                    } catch (e: Exception) {
                                        android.util.Log.e("TMDB_LOG", "Error searching TMDB series API", e)
                                    }
                                }
                            }

                            if (bestMatch != null) {
                                var finalBackdropPath = bestMatch.backdropPath
                                var directorName: String? = null
                                var directorAvatar: String? = null
                                var castJsonStr: String? = null
                                var extractedImdbId: String? = null

                                try {
                                    val tvDetail = tmdbApiService.getTvDetail(bestMatch.id, tmdbKey)
                                    if (finalBackdropPath.isNullOrBlank()) {
                                        finalBackdropPath = tvDetail.backdropPath
                                    }
                                    extractedImdbId = tvDetail.externalIds?.imdbId?.takeIf { it.isNotBlank() }

                                    val directorObj = tvDetail.credits?.crew?.firstOrNull { it.job.equals("Director", ignoreCase = true) || it.job.equals("Executive Producer", ignoreCase = true) }
                                    directorName = directorObj?.name
                                    directorAvatar = directorObj?.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }

                                    val castList = tvDetail.credits?.cast?.take(15)?.map {
                                        com.hasanege.materialtv.model.CastMember(
                                            name = it.name,
                                            character = it.character,
                                            profileImageUrl = it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }
                                        )
                                    }
                                    if (!castList.isNullOrEmpty()) {
                                        castJsonStr = kotlinx.serialization.json.Json.encodeToString(castList)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("TMDB_LOG", "Failed to fetch TV detail/credits", e)
                                }

                                android.util.Log.d("TMDB_LOG", "TMDB Series Match found: '${bestMatch.name}' (ID: ${bestMatch.id}, IMDb: $extractedImdbId)")
                                val newEntity = com.hasanege.materialtv.data.entities.TmdbContentEntity(
                                    streamId = seriesId.toString(),
                                    type = com.hasanege.materialtv.data.entities.ContentType.SERIES,
                                    profileId = profileId,
                                    tmdbId = bestMatch.id,
                                    title = bestMatch.name,
                                    overview = bestMatch.overview,
                                    posterPath = bestMatch.posterPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" },
                                    backdropPath = finalBackdropPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/original$it" },
                                    voteAverage = bestMatch.voteAverage,
                                    releaseDate = bestMatch.firstAirDate,
                                    director = directorName,
                                    directorAvatar = directorAvatar,
                                    castJson = castJsonStr,
                                    imdbId = extractedImdbId,
                                    fetchedAt = System.currentTimeMillis()
                                )
                                tmdbDao.insert(newEntity)
                                seriesInfoState = UiState.Success(SeriesDetailData(seriesInfoResponse, newEntity))
                            } else {
                                android.util.Log.w("TMDB_LOG", "No TMDB result found for series")
                            }
                        }
                    } else {
                        android.util.Log.i("TMDB_LOG", "TMDB API Key is empty or not configured in Settings.")
                    }
                    
                } else {
                    seriesInfoState = UiState.Error("Series data not found")
                }
            } catch (e: Exception) {
                seriesInfoState = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }
}
