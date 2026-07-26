package com.hasanege.materialtv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString

data class MovieDetailData(
    val xtreamData: VodInfoResponse,
    val tmdbData: com.hasanege.materialtv.data.entities.TmdbContentEntity? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: XtreamRepository,
    private val settingsRepository: com.hasanege.materialtv.data.SettingsRepository,
    private val tmdbDao: com.hasanege.materialtv.data.dao.TmdbDao,
    private val tmdbApiService: com.hasanege.materialtv.network.tmdb.TmdbApiService
) : ViewModel() {
    var movie by mutableStateOf<UiState<MovieDetailData>>(UiState.Loading)
    var series by mutableStateOf<UiState<SeriesDetailData>>(UiState.Loading)

    fun loadMovieDetails(username: String, password: String, streamId: Int, initialName: String? = null) {
        viewModelScope.launch {
            movie = UiState.Loading
            
            try {
                val foundMovie = repository.getVodDetails(username, password, streamId)
                if (foundMovie != null) {
                    movie = UiState.Success(MovieDetailData(foundMovie))
                    
                    // TMDB Fetch
                    val tmdbKey = settingsRepository.tmdbApiKey.firstOrNull()
                    if (!tmdbKey.isNullOrBlank()) {
                        android.util.Log.d("TMDB_LOG", "TMDB API Key found. Checking local DB for movie streamId: $streamId")
                        val profileId = com.hasanege.materialtv.network.SessionManager.username ?: "default"
                        val localTmdb = tmdbDao.getTmdbContent(streamId.toString(), com.hasanege.materialtv.data.entities.ContentType.VOD, profileId)
                        var currentTmdb = localTmdb
                        if (currentTmdb != null) {
                            val tmdbIdVal = currentTmdb.tmdbId
                            if (currentTmdb.imdbId.isNullOrBlank() && tmdbIdVal != null) {
                                try {
                                    val extIds = tmdbApiService.getMovieExternalIds(tmdbIdVal, tmdbKey)
                                    val fetchedImdb = extIds.imdbId?.takeIf { it.isNotBlank() }
                                    if (!fetchedImdb.isNullOrBlank()) {
                                        currentTmdb = currentTmdb.copy(imdbId = fetchedImdb)
                                        tmdbDao.insert(currentTmdb)
                                        android.util.Log.d("TMDB_LOG", "Backfilled IMDb ID for movie '$streamId': $fetchedImdb")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("TMDB_LOG", "Failed to backfill movie IMDb ID", e)
                                }
                            }
                            android.util.Log.d("TMDB_LOG", "Loaded TMDB data from local DB for '$streamId': ${currentTmdb.title}, IMDb: ${currentTmdb.imdbId}")
                            movie = UiState.Success(MovieDetailData(foundMovie, currentTmdb))
                        } else {
                            var bestMatch: com.hasanege.materialtv.model.tmdb.TmdbMovieResult? = null
                            val imdbId = foundMovie.info?.imdbID?.takeIf { it != "N/A" && it.isNotBlank() }

                            if (!imdbId.isNullOrBlank()) {
                                try {
                                    android.util.Log.d("TMDB_LOG", "Finding TMDB movie by IMDb ID: $imdbId")
                                    val findResponse = tmdbApiService.findByExternalId(imdbId, tmdbKey)
                                    bestMatch = findResponse.movieResults?.firstOrNull()
                                } catch (e: Exception) {
                                    android.util.Log.e("TMDB_LOG", "Error finding TMDB movie by IMDb ID", e)
                                }
                            }

                            if (bestMatch == null) {
                                val rawTitle = foundMovie.info?.name ?: initialName ?: foundMovie.movieData?.name ?: ""
                                val (extractedYear, cleanTitle) = com.hasanege.materialtv.network.tmdb.TmdbTitleCleaner.extractYearAndCleanTitle(rawTitle)
                                val yearToSearch = extractedYear ?: foundMovie.info?.releaseDate?.take(4)

                                if (cleanTitle.isNotBlank()) {
                                    android.util.Log.d("TMDB_LOG", "Searching TMDB API for movie: '$cleanTitle' (Year: $yearToSearch, Raw: '$rawTitle')")
                                    try {
                                        // 1st attempt: Search with cleanTitle and year
                                        var searchResponse = tmdbApiService.searchMovie(cleanTitle, tmdbKey, yearToSearch)
                                        var results = searchResponse.results?.filter { !it.backdropPath.isNullOrBlank() || !it.posterPath.isNullOrBlank() }

                                        // 2nd attempt fallback: If no result with year, search cleanTitle WITHOUT year
                                        if (results.isNullOrEmpty() && yearToSearch != null) {
                                            android.util.Log.d("TMDB_LOG", "Retrying TMDB search for '$cleanTitle' without year constraint")
                                            searchResponse = tmdbApiService.searchMovie(cleanTitle, tmdbKey, null)
                                            results = searchResponse.results?.filter { !it.backdropPath.isNullOrBlank() || !it.posterPath.isNullOrBlank() }
                                        }

                                        // Pick popularity or first match
                                        bestMatch = results?.maxByOrNull { it.popularity ?: 0.0 } ?: searchResponse.results?.firstOrNull()
                                    } catch (e: Exception) {
                                        android.util.Log.e("TMDB_LOG", "Error searching TMDB movie API", e)
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
                                    val movieDetail = tmdbApiService.getMovieDetail(bestMatch.id, tmdbKey)
                                    if (finalBackdropPath.isNullOrBlank()) {
                                        finalBackdropPath = movieDetail.backdropPath
                                    }
                                    extractedImdbId = movieDetail.externalIds?.imdbId?.takeIf { it.isNotBlank() }

                                    val directorObj = movieDetail.credits?.crew?.firstOrNull { it.job.equals("Director", ignoreCase = true) }
                                    directorName = directorObj?.name
                                    directorAvatar = directorObj?.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }

                                    val castList = movieDetail.credits?.cast?.take(15)?.map {
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
                                    android.util.Log.e("TMDB_LOG", "Failed to fetch movie detail/credits", e)
                                }

                                android.util.Log.d("TMDB_LOG", "TMDB Movie Match found: '${bestMatch.title}' (ID: ${bestMatch.id}, IMDb: $extractedImdbId)")
                                val newEntity = com.hasanege.materialtv.data.entities.TmdbContentEntity(
                                    streamId = streamId.toString(),
                                    type = com.hasanege.materialtv.data.entities.ContentType.VOD,
                                    profileId = profileId,
                                    tmdbId = bestMatch.id,
                                    title = bestMatch.title,
                                    overview = bestMatch.overview,
                                    posterPath = bestMatch.posterPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" },
                                    backdropPath = finalBackdropPath?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/original$it" },
                                    voteAverage = bestMatch.voteAverage,
                                    releaseDate = bestMatch.releaseDate,
                                    director = directorName,
                                    directorAvatar = directorAvatar,
                                    castJson = castJsonStr,
                                    imdbId = extractedImdbId,
                                    fetchedAt = System.currentTimeMillis()
                                )
                                tmdbDao.insert(newEntity)
                                movie = UiState.Success(MovieDetailData(foundMovie, newEntity))
                            } else {
                                android.util.Log.w("TMDB_LOG", "No TMDB result found for movie")
                            }
                        }
                    } else {
                        android.util.Log.i("TMDB_LOG", "TMDB API Key is empty or not configured in Settings.")
                    }
                    
                } else {
                    movie = UiState.Error("Movie detail could not be found.")
                }
            } catch (e: Exception) {
                movie = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun loadSeriesDetails(username: String, password: String, seriesId: Int, episodeId: String? = null) {
        viewModelScope.launch {
            series = UiState.Loading
            try {
                val seriesDetails = repository.getSeriesInfo(username, password, seriesId)
                if (seriesDetails != null) {
                    series = UiState.Success(SeriesDetailData(seriesDetails))
                    
                    // TMDB Fetch
                    val tmdbKey = settingsRepository.tmdbApiKey.firstOrNull()
                    if (!tmdbKey.isNullOrBlank()) {
                        val profileId = com.hasanege.materialtv.network.SessionManager.username ?: "default"
                        val localTmdb = tmdbDao.getTmdbContent(seriesId.toString(), com.hasanege.materialtv.data.entities.ContentType.SERIES, profileId)
                        var currentTmdb = localTmdb
                        if (currentTmdb != null) {
                            val tmdbIdVal = currentTmdb.tmdbId
                            if (currentTmdb.imdbId.isNullOrBlank() && tmdbIdVal != null) {
                                try {
                                    val extIds = tmdbApiService.getTvExternalIds(tmdbIdVal, tmdbKey)
                                    val fetchedImdb = extIds.imdbId?.takeIf { it.isNotBlank() }
                                    if (!fetchedImdb.isNullOrBlank()) {
                                        currentTmdb = currentTmdb.copy(imdbId = fetchedImdb)
                                        tmdbDao.insert(currentTmdb)
                                        android.util.Log.d("TMDB_LOG", "Backfilled IMDb ID for series '$seriesId': $fetchedImdb")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("TMDB_LOG", "Failed to backfill series IMDb ID", e)
                                }
                            }
                            android.util.Log.d("TMDB_LOG", "Loaded TMDB data from local DB for series '$seriesId': ${currentTmdb.title}, IMDb: ${currentTmdb.imdbId}")
                            series = UiState.Success(SeriesDetailData(seriesDetails, currentTmdb))
                        } else {
                            // Search TMDB
                            val titleToSearch = seriesDetails.info?.name
                            val yearToSearch = seriesDetails.info?.releaseDate?.take(4)?.toIntOrNull()
                            if (titleToSearch != null) {
                                try {
                                    val (extractedYear, cleanTitle) = com.hasanege.materialtv.network.tmdb.TmdbTitleCleaner.extractYearAndCleanTitle(titleToSearch)
                                    val yearVal = extractedYear?.toIntOrNull() ?: yearToSearch
                                    
                                    val searchResponse = tmdbApiService.searchTv(cleanTitle, tmdbKey, yearVal?.toString())
                                    val bestMatch = searchResponse.results?.firstOrNull()
                                    if (bestMatch != null) {
                                        var extractedImdbId: String? = null
                                        try {
                                            val tvDetail = tmdbApiService.getTvDetail(bestMatch.id, tmdbKey)
                                            extractedImdbId = tvDetail.externalIds?.imdbId?.takeIf { it.isNotBlank() }
                                        } catch (e: Exception) {
                                            android.util.Log.e("TMDB_LOG", "Failed to fetch TV detail/external_ids for series ${bestMatch.id}", e)
                                        }

                                        android.util.Log.d("TMDB_LOG", "TMDB Series Match found: '${bestMatch.name}' (ID: ${bestMatch.id}, IMDb: $extractedImdbId)")
                                        val newEntity = com.hasanege.materialtv.data.entities.TmdbContentEntity(
                                            streamId = seriesId.toString(),
                                            type = com.hasanege.materialtv.data.entities.ContentType.SERIES,
                                            profileId = profileId,
                                            tmdbId = bestMatch.id,
                                            title = bestMatch.name,
                                            overview = bestMatch.overview,
                                            posterPath = bestMatch.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                                            backdropPath = bestMatch.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" },
                                            voteAverage = bestMatch.voteAverage,
                                            releaseDate = bestMatch.firstAirDate,
                                            imdbId = extractedImdbId,
                                            fetchedAt = System.currentTimeMillis()
                                        )
                                        tmdbDao.insert(newEntity)
                                        series = UiState.Success(SeriesDetailData(seriesDetails, newEntity))
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("DetailViewModel", "TMDB error for series", e)
                                }
                            }
                        }
                    }
                } else {
                    series = UiState.Error("Series not found")
                }
            } catch (e: Exception) {
                series = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
