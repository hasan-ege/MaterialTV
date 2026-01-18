package com.hasanege.materialtv

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.model.CastMember
import com.hasanege.materialtv.network.RetrofitClient
import com.hasanege.materialtv.repository.XtreamRepository
import com.hasanege.materialtv.utils.TitleUtils
import com.hasanege.materialtv.utils.ContentRatingUtils
import com.hasanege.materialtv.utils.ImdbScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel(
    private val repository: XtreamRepository,
    private val settingsRepository: com.hasanege.materialtv.data.SettingsRepository
) : ViewModel() {
    var movie by mutableStateOf<UiState<VodInfoResponse>>(UiState.Loading)
    var series by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)

    private val omdbService = RetrofitClient.getOmdbClient()
    private val imdbService = RetrofitClient.getImdbSuggestionClient()

    private fun normalizeForImdb(input: String): String {
        var text = input.lowercase()
            .replace("ç", "c")
            .replace("ğ", "g")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ü", "u")
            .replace(" ", "_")
        val regex = Regex("[^a-z0-9_]")
        text = regex.replace(text, "")
        return text
    }

    private suspend fun fetchImdbMetadata(inputTitle: String, type: String? = null): com.hasanege.materialtv.network.OmdbResponse? = withContext(Dispatchers.IO) {
        val cleanedName = TitleUtils.cleanTitle(inputTitle)
        val normalized = normalizeForImdb(cleanedName)
        
        if (normalized.isEmpty()) return@withContext null

        Log.d("DetailViewModel", "🚀 Light Speed Primary Discovery: '$cleanedName'")

        // 1. GREEDY DISCOVERY via Suggestion API (Direct hit, no parallel sub-queries if possible)
        try {
            val firstChar = normalized.take(1)
            val suggestions = imdbService.searchSuggestion(firstChar, normalized)
            
            val bestMatch = suggestions.d?.firstOrNull { result ->
                if (type == "movie") result.q == "feature" || result.q == "video"
                else if (type == "series") result.q == "TV series" || result.q == "TV mini-series"
                else true
            } ?: suggestions.d?.firstOrNull()

            if (bestMatch != null) {
                Log.d("DetailViewModel", "✅ Light Speed Found: ${bestMatch.id}. Returning basic data...")
                return@withContext com.hasanege.materialtv.network.OmdbResponse(
                    Title = bestMatch.l,
                    Year = bestMatch.y?.toString(),
                    Poster = bestMatch.i?.imageUrl,
                    imdbID = bestMatch.id,
                    Actors = bestMatch.s,
                    Response = "True"
                )
            }
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Suggestion API error: ${e.message}")
        }

        // 2. EMERGENCY FALLBACK to sub-queries (Only if direct hit failed)
        val words = cleanedName.split(" ").filter { it.isNotEmpty() }
        if (words.size > 1) {
            val subQueries = (words.size - 1 downTo 1).map { words.take(it).joinToString(" ") }
            val fallbackMatch = coroutineScope {
                subQueries.take(2).map { query ->
                    async {
                        try {
                            val norm = normalizeForImdb(query)
                            if (norm.isEmpty()) return@async null
                            val firstChar = norm.take(1)
                            val suggestions = imdbService.searchSuggestion(firstChar, norm)
                            suggestions.d?.firstOrNull()
                        } catch (e: Exception) { null }
                    }
                }.mapNotNull { it.await() }.firstOrNull()
            }

            if (fallbackMatch != null) {
                return@withContext com.hasanege.materialtv.network.OmdbResponse(
                    Title = fallbackMatch.l,
                    Year = fallbackMatch.y?.toString(),
                    Poster = fallbackMatch.i?.imageUrl,
                    imdbID = fallbackMatch.id,
                    Actors = fallbackMatch.s,
                    Response = "True"
                )
            }
        }

        // 3. LAST RESORT: OMDb
        try {
            val resDirect = omdbService.searchMedia(title = cleanedName, type = type)
            if (resDirect.Response == "True" && resDirect.imdbID != null) return@withContext resDirect
        } catch (e: Exception) {}

        null
    }

    private suspend fun fetchCastImages(actorsString: String?): List<CastMember> = withContext(Dispatchers.IO) {
        if (actorsString.isNullOrBlank()) return@withContext emptyList()
        val actors = actorsString.split(",").map { it.trim() }
        
        coroutineScope {
            actors.map { actorName ->
                async {
                    try {
                        val normalized = normalizeForImdb(actorName)
                        if (normalized.isNotEmpty()) {
                            val firstChar = normalized.first().toString()
                            val suggestion = imdbService.searchSuggestion(firstChar, normalized)
                            val result = suggestion.d?.firstOrNull { it.l.equals(actorName, ignoreCase = true) } 
                                        ?: suggestion.d?.firstOrNull()
                            
                            CastMember(
                                name = actorName,
                                profileImageUrl = result?.i?.imageUrl
                            )
                        } else {
                            CastMember(name = actorName)
                        }
                    } catch (e: Exception) {
                        CastMember(name = actorName)
                    }
                }
            }.map { it.await() }
        }
    }

    fun loadMovieDetails(username: String, password: String, streamId: Int, initialName: String? = null) {
        viewModelScope.launch {
            movie = UiState.Loading
            
            coroutineScope {
                val isScrapingEnabled = settingsRepository.enableImdbScraping.first()

                // 1. Start IMDb discovery immediately if initialName is provided
                val initialImdbTask = if (!initialName.isNullOrBlank() && isScrapingEnabled) {
                    async { fetchImdbMetadata(initialName, type = "movie") }
                } else null

                // 2. Fetch Xtream Details in parallel
                val xtreamTask = async { repository.getVodDetails(username, password, streamId) }

                try {
                    val foundMovie = xtreamTask.await()
                    if (foundMovie != null) {
                        // Initial UI state from Xtream
                        movie = UiState.Success(foundMovie)
                        
                        // 3. Handle IMDb Discovery Result (Instant or Delayed)
                        val partialOmdb = initialImdbTask?.await() ?: run {
                            if (!isScrapingEnabled) return@run null
                            val cleanedName = foundMovie.info?.name ?: return@run null
                            fetchImdbMetadata(cleanedName, type = "movie")
                        }

                        if (partialOmdb != null) {
                            val imdbId = partialOmdb.imdbID
                            
                            // STEP 1: Instant UI Update with Suggestion Data
                            val instantUpdate = foundMovie.info?.copy(
                                year = partialOmdb.Year ?: foundMovie.info.year,
                                movieImage = partialOmdb.Poster ?: foundMovie.info.movieImage,
                                imdbRating = partialOmdb.imdbRating,
                                imdbID = imdbId
                            )
                            if (instantUpdate != null) {
                                movie = UiState.Success(foundMovie.copy(info = instantUpdate))
                            }

                            if (imdbId != null) {
                                // STEP 2: Background Full enrichment (Incremental Updates)
                                // We launch separate coroutines so each piece of data updates the UI as soon as it's ready.
                                
                                // 2a. Full Details (Plot, Rating, Genres) - Highest Priority
                                launch {
                                    val fullDetails = ImdbScraper.scrapeDetails(imdbId)
                                    if (fullDetails != null) {
                                        val currentMovie = (movie as? UiState.Success)?.data ?: foundMovie
                                        val initialContentRating = ContentRatingUtils.calculateContentRating(
                                            rated = fullDetails.Rated,
                                            genre = fullDetails.Genre ?: currentMovie.info?.genre
                                        )
                                        val updatedInfo = currentMovie.info?.copy(
                                            imdbRating = fullDetails.imdbRating ?: currentMovie.info.imdbRating,
                                            fullPlot = if (fullDetails.Plot != null && fullDetails.Plot != "N/A") fullDetails.Plot else currentMovie.info.fullPlot,
                                            runtime = fullDetails.Runtime ?: currentMovie.info.runtime,
                                            writer = fullDetails.Writer ?: currentMovie.info.writer,
                                            language = fullDetails.Language ?: currentMovie.info.language,
                                            country = fullDetails.Country ?: currentMovie.info.country,
                                            awards = fullDetails.Awards ?: currentMovie.info.awards,
                                            metascore = fullDetails.Metascore ?: currentMovie.info.metascore,
                                            imdbVotes = fullDetails.imdbVotes ?: currentMovie.info.imdbVotes,
                                            movieImage = if (fullDetails.Poster != null && fullDetails.Poster != "N/A") fullDetails.Poster else currentMovie.info.movieImage,
                                            director = if (fullDetails.Director != null && fullDetails.Director != "N/A") fullDetails.Director else currentMovie.info.director,
                                            genre = if (fullDetails.Genre != null && fullDetails.Genre != "N/A") fullDetails.Genre else currentMovie.info.genre,
                                            rated = fullDetails.Rated ?: currentMovie.info.rated,
                                            contentRating = initialContentRating
                                        )
                                        if (updatedInfo != null) {
                                            movie = UiState.Success(currentMovie.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2b. Cast Members
                                launch {
                                    val cast = ImdbScraper.scrapeCast(imdbId)
                                    if (cast.isNotEmpty()) {
                                        val currentMovie = (movie as? UiState.Success)?.data ?: foundMovie
                                        val updatedInfo = currentMovie.info?.copy(
                                            imdbCast = cast
                                        )
                                        if (updatedInfo != null) {
                                            movie = UiState.Success(currentMovie.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2c. Parental Guide / Warnings
                                launch {
                                    val scrapedWarnings = ImdbScraper.scrapeParentalGuide(imdbId)
                                    if (scrapedWarnings.isNotEmpty()) {
                                        val currentMovie = (movie as? UiState.Success)?.data ?: foundMovie
                                        val currentContentRating = currentMovie.info?.contentRating ?: ContentRatingUtils.calculateContentRating("N/A", null)
                                        val updatedContentRating = currentContentRating.copy(
                                            warnings = (currentContentRating.warnings + scrapedWarnings).distinct()
                                        )
                                        val updatedInfo = currentMovie.info?.copy(
                                            contentRating = updatedContentRating
                                        )
                                        if (updatedInfo != null) {
                                            movie = UiState.Success(currentMovie.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2d. Reviews
                                launch {
                                    val reviews = ImdbScraper.scrapeReviews(imdbId)
                                    if (reviews.isNotEmpty()) {
                                        val currentMovie = (movie as? UiState.Success)?.data ?: foundMovie
                                        val updatedInfo = currentMovie.info?.copy(
                                            imdbReviews = reviews
                                        )
                                        if (updatedInfo != null) {
                                            movie = UiState.Success(currentMovie.copy(info = updatedInfo))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        movie = UiState.Error("Movie not found")
                    }
                } catch (e: Exception) {
                    movie = UiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun loadSeriesDetails(username: String, password: String, seriesId: Int, episodeId: String? = null) {
        viewModelScope.launch {
            series = UiState.Loading
            try {
                val seriesDetails = repository.getSeriesInfo(username, password, seriesId)
                if (seriesDetails != null) {
                    series = UiState.Success(seriesDetails)

                    // Background fetch full IMDb metadata and cast
                    viewModelScope.launch {
                        val cleanedName = seriesDetails.info?.name ?: return@launch
                        
                        // STEP 1: Instant Discovery via Suggestions (Instant UI Update)
                        val partialOmdb = fetchImdbMetadata(cleanedName, type = "series")
                        if (partialOmdb != null) {
                            val imdbId = partialOmdb.imdbID
                            
                            // Immediately update UI with suggestion info
                            val instantUpdate = seriesDetails.info?.copy(
                                cover = partialOmdb.Poster ?: seriesDetails.info.cover,
                                imdbRating = partialOmdb.imdbRating,
                                rated = partialOmdb.Rated
                            )
                            if (instantUpdate != null) {
                                series = UiState.Success(seriesDetails.copy(info = instantUpdate))
                            }

                            if (imdbId != null) {
                                // STEP 2: Full Scrape in parallel
                                coroutineScope {
                                    val detailsDeferred = async { ImdbScraper.scrapeDetails(imdbId) }
                                    val castDeferred = async { ImdbScraper.scrapeCast(imdbId) }
                                    
                                    val fullDetails = detailsDeferred.await()
                                    val cast = castDeferred.await()

                                    val finalOmdb = fullDetails ?: partialOmdb

                                    // Calculate Turkish content rating
                                    val contentRating = ContentRatingUtils.calculateContentRating(
                                        rated = finalOmdb.Rated,
                                        genre = finalOmdb.Genre ?: seriesDetails.info?.genre
                                    )
                                    
                                    val updatedInfo = seriesDetails.info?.copy(
                                        imdbRating = finalOmdb.imdbRating,
                                        fullPlot = if (finalOmdb.Plot != null && finalOmdb.Plot != "N/A") finalOmdb.Plot else seriesDetails.info.plot,
                                        imdbCast = if (cast.isNotEmpty()) cast else fetchCastImages(finalOmdb.Actors),
                                        runtime = finalOmdb.Runtime,
                                        writer = finalOmdb.Writer,
                                        language = finalOmdb.Language,
                                        country = finalOmdb.Country,
                                        awards = finalOmdb.Awards,
                                        metascore = finalOmdb.Metascore,
                                        imdbVotes = finalOmdb.imdbVotes,
                                        cover = if (finalOmdb.Poster != null && finalOmdb.Poster != "N/A") finalOmdb.Poster else seriesDetails.info.cover,
                                        director = if (finalOmdb.Director != null && finalOmdb.Director != "N/A") finalOmdb.Director else seriesDetails.info.director,
                                        genre = if (finalOmdb.Genre != null && finalOmdb.Genre != "N/A") finalOmdb.Genre else seriesDetails.info.genre,
                                        rated = finalOmdb.Rated,
                                        contentRating = contentRating,
                                        imdbID = imdbId
                                    )
                                    if (updatedInfo != null) {
                                        series = UiState.Success(seriesDetails.copy(info = updatedInfo))
                                    }
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

object DetailViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    private var settingsRepository: com.hasanege.materialtv.data.SettingsRepository? = null

    fun initialize(settingsRepository: com.hasanege.materialtv.data.SettingsRepository) {
        this.settingsRepository = settingsRepository
    }

    private val apiService by lazy {
        com.hasanege.materialtv.network.SessionManager.serverUrl?.let { 
            com.hasanege.materialtv.network.RetrofitClient.getClient(it) 
        }
    }

    private val repository by lazy {
        XtreamRepository(apiService)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return DetailViewModel(repository, settingsRepository!!) as T
    }
}
