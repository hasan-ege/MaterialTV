package com.hasanege.materialtv

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.CastMember
import com.hasanege.materialtv.network.RetrofitClient
import com.hasanege.materialtv.repository.XtreamRepository
import com.hasanege.materialtv.utils.TitleUtils
import com.hasanege.materialtv.utils.ContentRatingUtils
import com.hasanege.materialtv.utils.ImdbScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class SeriesDetailViewModel(
    private val repository: XtreamRepository,
    private val settingsRepository: com.hasanege.materialtv.data.SettingsRepository
) : ViewModel() {

    var seriesInfoState by mutableStateOf<UiState<SeriesInfoResponse>>(UiState.Loading)
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

        Log.d("SeriesDetailViewModel", "🚀 Light Speed Primary Discovery: '$cleanedName'")

        // 1. GREEDY DISCOVERY via Suggestion API
        try {
            val firstChar = normalized.take(1)
            val suggestions = imdbService.searchSuggestion(firstChar, normalized)
            
            val bestMatch = suggestions.d?.firstOrNull { result ->
                if (type == "movie") result.q == "feature" || result.q == "video"
                else if (type == "series") result.q == "TV series" || result.q == "TV mini-series"
                else true
            } ?: suggestions.d?.firstOrNull()

            if (bestMatch != null) {
                Log.d("SeriesDetailViewModel", "✅ Light Speed Found: ${bestMatch.id}. Returning basic data...")
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
            Log.e("SeriesDetailViewModel", "Suggestion API error: ${e.message}")
        }

        // 2. EMERGENCY FALLBACK to sub-queries
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

    fun loadSeriesInfo(username: String, password: String, seriesId: Int, initialName: String? = null) {
        viewModelScope.launch {
            seriesInfoState = UiState.Loading
            
            coroutineScope {
                val isScrapingEnabled = settingsRepository.enableImdbScraping.first()

                // 1. Start IMDb discovery immediately if initialName is provided
                val initialImdbTask = if (!initialName.isNullOrBlank() && isScrapingEnabled) {
                    async { fetchImdbMetadata(initialName, type = "series") }
                } else null

                // 2. Fetch Xtream Series Info in parallel
                val xtreamTask = async { repository.getSeriesInfo(username, password, seriesId) }

                try {
                    val seriesInfoResponse = xtreamTask.await()
                    if (seriesInfoResponse != null) {
                        // Initial Success from Xtream
                        seriesInfoState = UiState.Success(seriesInfoResponse)
                        
                        // 3. Handle IMDb Discovery
                        val partialOmdb = initialImdbTask?.await() ?: run {
                            if (!isScrapingEnabled) return@run null
                            val cleanedName = seriesInfoResponse.info?.name ?: return@run null
                            fetchImdbMetadata(cleanedName, type = "series")
                        }

                        if (partialOmdb != null) {
                            val imdbId = partialOmdb.imdbID
                            
                            // STEP 1: Instant UI Update with Suggestion Data
                            val instantUpdate = seriesInfoResponse.info?.copy(
                                cover = partialOmdb.Poster ?: seriesInfoResponse.info.cover,
                                imdbRating = partialOmdb.imdbRating,
                                rated = partialOmdb.Rated
                            )
                            if (instantUpdate != null) {
                                seriesInfoState = UiState.Success(seriesInfoResponse.copy(info = instantUpdate))
                            }

                            if (imdbId != null) {
                                // STEP 2: Background Full enrichment (Incremental Updates)
                                
                                // 2a. Full Details (Plot, Rating, Genres)
                                launch {
                                    val fullDetails = ImdbScraper.scrapeDetails(imdbId)
                                    if (fullDetails != null) {
                                        val currentSeries = (seriesInfoState as? UiState.Success)?.data ?: seriesInfoResponse
                                        val initialContentRating = ContentRatingUtils.calculateContentRating(
                                            rated = fullDetails.Rated,
                                            genre = fullDetails.Genre ?: currentSeries.info?.genre
                                        )
                                        val updatedInfo = currentSeries.info?.copy(
                                            imdbRating = fullDetails.imdbRating ?: currentSeries.info.imdbRating,
                                            fullPlot = if (fullDetails.Plot != null && fullDetails.Plot != "N/A") fullDetails.Plot else currentSeries.info.fullPlot,
                                            runtime = fullDetails.Runtime ?: currentSeries.info.runtime,
                                            writer = fullDetails.Writer ?: currentSeries.info.writer,
                                            language = fullDetails.Language ?: currentSeries.info.language,
                                            country = fullDetails.Country ?: currentSeries.info.country,
                                            awards = fullDetails.Awards ?: currentSeries.info.awards,
                                            metascore = fullDetails.Metascore ?: currentSeries.info.metascore,
                                            imdbVotes = fullDetails.imdbVotes ?: currentSeries.info.imdbVotes,
                                            cover = if (fullDetails.Poster != null && fullDetails.Poster != "N/A") fullDetails.Poster else currentSeries.info.cover,
                                            director = if (fullDetails.Director != null && fullDetails.Director != "N/A") fullDetails.Director else currentSeries.info.director,
                                            genre = if (fullDetails.Genre != null && fullDetails.Genre != "N/A") fullDetails.Genre else currentSeries.info.genre,
                                            rated = fullDetails.Rated ?: currentSeries.info.rated,
                                            contentRating = initialContentRating
                                        )
                                        if (updatedInfo != null) {
                                            seriesInfoState = UiState.Success(currentSeries.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2b. Cast
                                launch {
                                    val cast = ImdbScraper.scrapeCast(imdbId)
                                    if (cast.isNotEmpty()) {
                                        val currentSeries = (seriesInfoState as? UiState.Success)?.data ?: seriesInfoResponse
                                        val updatedInfo = currentSeries.info?.copy(
                                            imdbCast = cast
                                        )
                                        if (updatedInfo != null) {
                                            seriesInfoState = UiState.Success(currentSeries.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2c. Warnings
                                launch {
                                    val scrapedWarnings = ImdbScraper.scrapeParentalGuide(imdbId)
                                    if (scrapedWarnings.isNotEmpty()) {
                                        val currentSeries = (seriesInfoState as? UiState.Success)?.data ?: seriesInfoResponse
                                        val currentContentRating = currentSeries.info?.contentRating ?: ContentRatingUtils.calculateContentRating("N/A", null)
                                        val updatedContentRating = currentContentRating.copy(
                                            warnings = (currentContentRating.warnings + scrapedWarnings).distinct()
                                        )
                                        val updatedInfo = currentSeries.info?.copy(
                                            contentRating = updatedContentRating
                                        )
                                        if (updatedInfo != null) {
                                            seriesInfoState = UiState.Success(currentSeries.copy(info = updatedInfo))
                                        }
                                    }
                                }

                                // 2d. Reviews
                                launch {
                                    val reviews = ImdbScraper.scrapeReviews(imdbId)
                                    if (reviews.isNotEmpty()) {
                                        val currentSeries = (seriesInfoState as? UiState.Success)?.data ?: seriesInfoResponse
                                        val updatedInfo = currentSeries.info?.copy(
                                            imdbReviews = reviews
                                        )
                                        if (updatedInfo != null) {
                                            seriesInfoState = UiState.Success(currentSeries.copy(info = updatedInfo))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        seriesInfoState = UiState.Error("Series not found")
                    }
                } catch (e: Exception) {
                    seriesInfoState = UiState.Error("Failed to load series details: ${e.message}")
                }
            }
        }
    }
}

object SeriesDetailViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
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
        return SeriesDetailViewModel(repository, settingsRepository!!) as T
    }
}
