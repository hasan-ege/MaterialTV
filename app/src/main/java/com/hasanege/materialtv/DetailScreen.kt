@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hasanege.materialtv

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadStatus
import com.hasanege.materialtv.model.CastMember
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.VodItem
import android.view.View
import android.content.Intent
import android.net.Uri
import com.hasanege.materialtv.model.VodInfo
import com.hasanege.materialtv.ui.ContentRatingBadges
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Composable
fun DetailScreen(
    movie: VodItem? = null,
    movieDetails: VodInfo? = null,
    series: SeriesInfoResponse? = null,
    lastWatchedEpisode: Episode? = null,
    watchProgress: Float = 0f, // 0f to 1f, or -1 if not started
    resumePosition: Long = 0L,
    nextEpisodeThresholdMinutes: Int = 5,
    activeDownloads: List<DownloadItem> = emptyList(),
    onBack: () -> Unit,
    onPlayMovie: ((VodItem) -> Unit)? = null,
    onPlayEpisode: ((Episode) -> Unit)? = null,
    onDownloadMovie: ((VodItem) -> Unit)? = null,
    onDownloadEpisode: ((Episode) -> Unit)? = null,
    onCancelDownload: ((String) -> Unit)? = null,
    onDownloadSeason: ((Int, List<Episode>) -> Unit)? = null,
    seriesId: Int = -1
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val scope = rememberCoroutineScope()
    var seasonDownloadStarted by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val downloadStartedMsg = stringResource(R.string.download_started)
    val addedToFavoritesMsg = stringResource(R.string.favorites_added)
    val removedFromFavoritesMsg = stringResource(R.string.favorites_removed)
    
    // Favorites State
    var isFavorite by remember { mutableStateOf(false) }
    val contentId = movie?.streamId ?: seriesId
    val contentType = if (movie != null) "movie" else "series"
    
    LaunchedEffect(contentId, contentType) {
        if (contentId != -1) {
            isFavorite = FavoritesManager.isFavorite(contentId, contentType)
        }
    }
    
    // Helper to format milliseconds to MM:SS or HH:MM:SS
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    // Helper for Turkish grammar: determines the correct ablative suffix ('dan, 'den, 'tan, 'ten)
    fun getTurkishAblativeSuffix(millis: Long): String {
        val seconds = (millis / 1000) % 60
        return if (seconds % 10 != 0L) {
            when (seconds % 10) {
                1L -> context.getString(R.string.detail_suffix_den) // bir-den
                2L -> context.getString(R.string.detail_suffix_den) // iki-den
                3L -> context.getString(R.string.detail_suffix_ten) // üç-ten
                4L -> context.getString(R.string.detail_suffix_ten) // dört-ten
                5L -> context.getString(R.string.detail_suffix_ten) // beş-ten
                6L -> context.getString(R.string.detail_suffix_dan) // altı-dan
                7L -> context.getString(R.string.detail_suffix_den) // yedi-den
                8L -> context.getString(R.string.detail_suffix_den) // sekiz-den
                9L -> context.getString(R.string.detail_suffix_dan) // dokuz-dan
                else -> context.getString(R.string.detail_suffix_dan)
            }
        } else {
            when (seconds) {
                0L  -> context.getString(R.string.detail_suffix_dan) // sıfır-dan
                10L -> context.getString(R.string.detail_suffix_dan) // on-dan
                20L -> context.getString(R.string.detail_suffix_den) // yirmi-den
                30L -> context.getString(R.string.detail_suffix_dan) // otuz-dan
                40L -> context.getString(R.string.detail_suffix_tan) // kırk-tan
                50L -> context.getString(R.string.detail_suffix_den) // elli-den
                else -> context.getString(R.string.detail_suffix_dan)
            }
        }
    }

    // Parsing Episodes for Series
    val episodesMap = remember(series) {
        if (series?.episodes != null) {
            try {
                val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                
                // Inspect the JsonElement type
                if (series.episodes is kotlinx.serialization.json.JsonObject) {
                    val entries = series.episodes.jsonObject.entries
                    entries.associate { (key, element) ->
                        val list = try {
                            json.decodeFromJsonElement<List<Episode>>(element)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        
                        // Assign season number to episodes if missing
                        val seasonNum = key.toIntOrNull()
                        if (seasonNum != null) {
                             key to list.map { it.copy(season = seasonNum) }
                        } else {
                            key to list
                        }
                    }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap<String, List<Episode>>()
            }
        } else {
            emptyMap()
        }
    }

    // State for Series
    val initialSeasonKey = remember(episodesMap, lastWatchedEpisode) {
        if (lastWatchedEpisode != null) {
             val foundSeason = episodesMap.entries.find { entry -> 
                 entry.value.any { it.id == lastWatchedEpisode.id } 
             }?.key
             
             foundSeason ?: lastWatchedEpisode.season?.toString() ?: "1"
        } else {
            if (episodesMap.containsKey("1")) "1" else episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }.firstOrNull() ?: "1"
        }
    }

    var selectedSeasonKey by remember(initialSeasonKey) { 
        mutableStateOf(initialSeasonKey) 
    }
    
    // State for scraped reviews
    var scrapedReviews by remember { mutableStateOf<List<com.hasanege.materialtv.model.ImdbReview>>(emptyList()) }
    val currentImdbId = movieDetails?.imdbID ?: series?.info?.imdbID ?: ""
    
    LaunchedEffect(currentImdbId) {
        if (currentImdbId.isNotEmpty() && scrapedReviews.isEmpty()) {
            com.hasanege.materialtv.utils.ImdbScraper.scrapeReviewsFlow(currentImdbId).collect { chunk ->
                scrapedReviews = scrapedReviews + chunk
            }
        }
    }
    
    val currentEpisodes = episodesMap[selectedSeasonKey] ?: emptyList()
    
    val title = movie?.name ?: series?.info?.name ?: stringResource(R.string.detail_unknown_title)
    val plot = movieDetails?.fullPlot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.fullPlot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: movieDetails?.plot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.plot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: stringResource(R.string.detail_no_description)
        
    val rating = movieDetails?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: movieDetails?.rating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.rating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: stringResource(R.string.unknown)
        
    val hasImdbRating = (movieDetails?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } != null || 
                         series?.info?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } != null)
    val imdbVotes = movieDetails?.imdbVotes ?: series?.info?.imdbVotes ?: "0"
    val metascore = movieDetails?.metascore ?: series?.info?.metascore ?: "N/A"
    val awards = movieDetails?.awards ?: series?.info?.awards ?: ""
    val backdropUrl = movieDetails?.backdropPath?.firstOrNull() 
        ?: series?.info?.backdropPath?.firstOrNull() 
        ?: series?.info?.cover 
        ?: movieDetails?.movieImage
        ?: movie?.streamIcon
    val genres = movieDetails?.genre ?: series?.info?.genre ?: ""
    val releaseDate = movieDetails?.releaseDate ?: series?.info?.releaseDate ?: ""
    val director = movieDetails?.director ?: series?.info?.director ?: ""
    val castMembers = (movieDetails?.imdbCast ?: series?.info?.imdbCast ?: emptyList())
        .filter { it.name.trim().length > 1 }
        .distinctBy { it.name }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val safeTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // Dynamic Hero Height - 50% of screen but at least 300dp
    val heroHeight = remember(screenHeight) { (screenHeight * 0.55f).coerceAtLeast(300.dp) }
    // Content starts below hero, adjusted for safe area
    val contentSpacerHeight = remember(heroHeight) { heroHeight - 80.dp }

    // Play button onClick - defined in outer scope for FloatingToolbar access
    val onPlayClick: () -> Unit = {
        if (movie != null) onPlayMovie?.invoke(movie)
        else if (lastWatchedEpisode != null) onPlayEpisode?.invoke(lastWatchedEpisode)
        else if (currentEpisodes.isNotEmpty()) onPlayEpisode?.invoke(currentEpisodes.first())
        else {
            val firstSeason = episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }.firstOrNull()
            val firstEp = episodesMap[firstSeason]?.firstOrNull()
            if (firstEp != null) onPlayEpisode?.invoke(firstEp)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Background Image with Gradient
        // Fixed at top, not scrolling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight + 100.dp) // Extra for overlap
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay - Top to Bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // 2. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Spacer to push content down so image is visible
            Spacer(modifier = Modifier.height(contentSpacerHeight))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Solid background for content to slide over image
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = 100f
                        )
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 50.dp) // Bottom padding
            ) {
                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Title
                    Text(
                        text = title,
                        style = (if (configuration.screenWidthDp < 360) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall).copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 12f
                            )
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Metadata Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // IMDB Rating
                        if (rating != "N/A" && rating.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = stringResource(R.string.detail_rating),
                                    tint = Color(0xFFFFC107), // Amber for star
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val displayRating = remember(rating) {
                                    if (rating.contains("/") || rating.isEmpty()) {
                                        rating
                                    } else {
                                        val isNumeric = rating.any { it.isDigit() }
                                        if (isNumeric) "$rating/10" else rating
                                    }
                                }
                                Text(
                                    text = displayRating,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (hasImdbRating) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "IMDb",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color(0xFFE6B91E), // IMDb Yellow
                                        modifier = Modifier
                                            .background(Color.Black, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        
                        // Genre
                        if (genres.isNotEmpty()) {
                             Text(
                                text = genres.split(",").take(2).joinToString(", "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
    
                        // Date
                        if (releaseDate.isNotEmpty()) {
                             Text(
                                text = releaseDate.take(4),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        // Metascore
                        if (metascore != "N/A" && metascore.isNotEmpty()) {
                            Text(
                                text = "Meta: $metascore",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = if ((metascore.toIntOrNull() ?: 0) >= 60) Color(0xFF66CC33) else Color(0xFFFFCC33),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (awards.isNotEmpty() && awards != "N/A") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = awards,
                            style = MaterialTheme.typography.labelMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Akıllı İşaretler - Turkish Content Rating Badges
                    val contentRating = movieDetails?.contentRating ?: series?.info?.contentRating
                    if (contentRating != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ContentRatingBadges(contentRating = contentRating)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Adaptive Button Layout - Column for phones (< 600dp), Row for tablets
                    val isPhoneScreen = configuration.screenWidthDp < 600
                    
                    // Responsive text style
                    val buttonTextStyle = when {
                        configuration.screenWidthDp < 400 -> MaterialTheme.typography.labelLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    
                    // Play button text
                    val playButtonText = if (movie != null) {
                        if (resumePosition > 0) {
                            if (context.resources.configuration.locales[0].language == "tr") {
                                stringResource(
                                    R.string.detail_play_resume, 
                                    formatDuration(resumePosition), 
                                    getTurkishAblativeSuffix(resumePosition)
                                )
                            } else {
                                stringResource(R.string.detail_play_resume, formatDuration(resumePosition))
                            }
                        } else {
                            stringResource(R.string.detail_play)
                        }
                    } else if (lastWatchedEpisode != null) {
                        val sNum = lastWatchedEpisode.season?.toString() ?: initialSeasonKey
                        val eNum = lastWatchedEpisode.episodeNum ?: ""
                        if (resumePosition > 0) {
                            if (context.resources.configuration.locales[0].language == "tr") {
                                stringResource(
                                    R.string.detail_play_resume_episode, 
                                    sNum, 
                                    eNum, 
                                    formatDuration(resumePosition),
                                    getTurkishAblativeSuffix(resumePosition)
                                )
                            } else {
                                stringResource(R.string.detail_play_resume_episode, sNum, eNum, formatDuration(resumePosition))
                            }
                        } else {
                            stringResource(R.string.detail_play_episode, sNum, eNum)
                        }
                    } else {
                        stringResource(R.string.detail_play_episode, selectedSeasonKey, "1")
                    }
                    
                    // Play button onClick already defined in outer scope
                    
                    @Composable
                    fun PlayButtonContent() {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = playButtonText,
                                style = buttonTextStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Action Buttons Row - Expressive styling
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (series != null) 12.dp else 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play Button
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .weight(if (movie != null) 0.5f else 0.75f)
                                .height(56.dp),
                            shape = ExpressiveShapes.Medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(playButtonText, style = buttonTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        
                        // Favorites Button
                        FilledTonalIconButton(
                            onClick = {
                                scope.launch {
                                    val thumbnailUrl = movieDetails?.movieImage ?: series?.info?.cover
                                    val genre = movieDetails?.genre ?: series?.info?.genre
                                    val year = movieDetails?.releaseDate?.take(4) ?: series?.info?.releaseDate?.take(4)
                                    
                                    val wasAdded = FavoritesManager.toggleFavorite(
                                        contentId = contentId,
                                        contentType = contentType,
                                        name = title,
                                        thumbnailUrl = thumbnailUrl,
                                        genre = genre,
                                        year = year,
                                        seriesId = if (movie == null) seriesId else null
                                    )
                                    isFavorite = wasAdded
                                    snackbarHostState.showSnackbar(
                                        if (wasAdded) addedToFavoritesMsg else removedFromFavoritesMsg
                                    )
                                }
                            },
                            modifier = Modifier.size(56.dp),
                            shape = ExpressiveShapes.Medium,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(if (isFavorite) R.string.favorites_remove else R.string.favorites_add)
                            )
                        }

                        // Download Button (Only for movies)
                        if (movie != null) {
                            val download = activeDownloads.find { 
                                it.title == movie.name || (it.url.contains(movie.streamId.toString()))
                            }
                            val isDownloaded = download?.status == DownloadStatus.COMPLETED
                            val isDownloading = download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PENDING

                            FilledTonalButton(
                                onClick = { 
                                    if (isDownloaded) onPlayMovie?.invoke(movie)
                                    else if (!isDownloading) {
                                        onDownloadMovie?.invoke(movie)
                                        scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.25f)
                                    .height(56.dp),
                                shape = ExpressiveShapes.Medium,
                                enabled = !isDownloading
                            ) {
                                if (isDownloading) {
                                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                                    CircularWavyProgressIndicator(
                                        progress = { (download?.progress ?: 0) / 100f },
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Rounded.Check else Icons.Rounded.Download,
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                        // Reviews Button Removed as requested
                    }

                    // Series Download Season Button - Expanded to full width
                    if (series != null) {
                        FilledTonalButton(
                            onClick = { 
                                val seasonNum = selectedSeasonKey.toIntOrNull() ?: 1
                                onDownloadSeason?.invoke(seasonNum, currentEpisodes)
                                seasonDownloadStarted = true
                                scope.launch { snackbarHostState.showSnackbar(downloadStartedMsg) }
                                scope.launch {
                                    delay(2000)
                                    seasonDownloadStarted = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(bottom = 16.dp),
                            shape = ExpressiveShapes.Medium
                        ) {
                            Icon(if (seasonDownloadStarted) Icons.Rounded.Check else Icons.Rounded.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_download_season), style = buttonTextStyle, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Storyline
                    Text(
                        text = stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                    
                    // YouTube Trailer Section
                    val rawTrailerUrl = movieDetails?.youtubeTrailer ?: movieDetails?.trailer ?: series?.info?.youtubeTrailer ?: series?.info?.trailer
                    if (!rawTrailerUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.detail_trailer_header),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // URL Normalize: Eğer sadece ID gelirse tam linke çeviriyoruz
                        val finalUrl = if (!rawTrailerUrl.contains("http")) {
                            "https://www.youtube.com/watch?v=$rawTrailerUrl"
                        } else {
                            rawTrailerUrl
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("DetailScreen", "Failed to open trailer: $finalUrl", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveShapes.Medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.detail_trailer_header))
                        }
                    }

                    // Cast Section
                    if (castMembers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.detail_cast_header),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val castCarouselState = rememberCarouselState { castMembers.size }
                        
                        HorizontalMultiBrowseCarousel(
                            state = castCarouselState,
                            preferredItemWidth = 80.dp,
                            itemSpacing = 16.dp,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) { index ->
                            CastMemberItem(
                                member = castMembers[index],
                                modifier = Modifier.maskClip(CircleShape)
                            )
                        }
                    }
                    
                    if (director.isNotEmpty() || (castMembers.isEmpty() && movieDetails?.cast != null)) {
                        Spacer(modifier = Modifier.height(16.dp))
                         if (director.isNotEmpty()) {
                             Text(
                                text = stringResource(R.string.detail_director, director),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                         }
                         if (castMembers.isEmpty() && movieDetails?.cast != null) {
                             Text(
                                text = stringResource(R.string.detail_cast, movieDetails.cast),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                         }
                    }

                    // User Reviews Section - Expressive Carousel
                    val reviews = if (scrapedReviews.isNotEmpty()) scrapedReviews else (movieDetails?.imdbReviews ?: series?.info?.imdbReviews ?: emptyList())
                    if (reviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.detail_reviews_header),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            TextButton(onClick = {
                                if (currentImdbId.isNotEmpty()) {
                                    val intent = Intent(context, ReviewsActivity::class.java).apply {
                                        putExtra("imdb_id", currentImdbId)
                                        putExtra("title", movie?.name ?: series?.info?.name ?: "")
                                    }
                                    context.startActivity(intent)
                                }
                            }) {
                                Text(text = stringResource(R.string.detail_view_all_reviews))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val carouselState = rememberCarouselState { reviews.size }
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            preferredItemWidth = 320.dp,
                            itemSpacing = 16.dp,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) { index ->
                            ReviewCarouselItem(
                                review = reviews[index],
                                modifier = Modifier.maskClip(ExpressiveShapes.Large)
                            )
                        }
                    }
                }
                
                // Series Specific Content
                if (series != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Season Selector with ExpressiveTabSlider
                    if (episodesMap.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.tab_seasons),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val sortedSeasons = episodesMap.keys.sortedBy { it.toIntOrNull() ?: 999 }
                        val seasonTabs = sortedSeasons.map { stringResource(R.string.detail_season, it) }
                        val selectedIndex = sortedSeasons.indexOf(selectedSeasonKey).coerceAtLeast(0)
                        
                        com.hasanege.materialtv.ui.ExpressiveTabSlider(
                            tabs = seasonTabs,
                            selectedIndex = selectedIndex,
                            onTabSelected = { index ->
                                selectedSeasonKey = sortedSeasons.getOrNull(index) ?: selectedSeasonKey
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            scrollable = sortedSeasons.size > 4 // Scroll only if many seasons
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Episodes List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentEpisodes.forEach { episode ->
                             EpisodeItem(
                                episode = episode,
                                seriesName = series?.info?.name,
                                activeDownloads = activeDownloads,
                                onPlay = { onPlayEpisode?.invoke(episode) },
                                onDownload = { onDownloadEpisode?.invoke(episode) },
                                onCancel = { downloadId -> onCancelDownload?.invoke(downloadId) }
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Action Bar (Custom implementation)
        val isToolbarVisible = scrollState.value > 500
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            AnimatedVisibility(
                visible = isToolbarVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Bottom),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Surface(
                    modifier = Modifier.clip(ExpressiveShapes.ExtraLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, null)
                        }
                        
                        // Play action in toolbar
                        Button(
                            onClick = onPlayClick,
                            shape = ExpressiveShapes.Large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_play), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 3. Back Button (Fixed - Hide when toolbar is shown to avoid clutter)
        if (!isToolbarVisible) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp + safeTopPadding, start = 24.dp)
                    .size(48.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (if (isToolbarVisible) 80.dp else 24.dp))
        )
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    seriesName: String?,
    activeDownloads: List<DownloadItem>,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancel: ((String) -> Unit)? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Find download for this episode
    val download = activeDownloads.find { downloadItem ->
        val sameSeries = seriesName != null && downloadItem.seriesName == seriesName
        val matchBySeasonEp = sameSeries && 
                              downloadItem.seasonNumber == episode.season && 
                              downloadItem.episodeNumber == episode.episodeNum?.toIntOrNull()
        val matchById = downloadItem.url.contains("/${episode.id}.")
        matchBySeasonEp || matchById
    }
    
    val isDownloaded = download?.status == DownloadStatus.COMPLETED
    val isDownloading = download?.status == DownloadStatus.DOWNLOADING || download?.status == DownloadStatus.PENDING

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onPlay
            ),
        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = episode.info?.movieImage
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(if (configuration.screenWidthDp < 360) 100.dp else 130.dp)
                        .aspectRatio(16f/9f)
                        .clip(ExpressiveShapes.Small)
                )
            } else {
                 Surface(modifier = Modifier.size(48.dp), shape = ExpressiveShapes.Medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = episode.episodeNum ?: "?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (!imageUrl.isNullOrEmpty()) {
                     Text(text = stringResource(R.string.detail_episode, episode.episodeNum ?: "?"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(text = episode.title ?: stringResource(R.string.detail_episode, episode.episodeNum ?: "?"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                
                val duration = episode.duration
                if (duration != null) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.detail_play), tint = MaterialTheme.colorScheme.primary)
            }
            
            if (isDownloaded) {
                 IconButton(onClick = { /* Already downloaded */ }) {
                     Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.downloads_completed), tint = MaterialTheme.colorScheme.primary)
                 }
            } else if (isDownloading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                     CircularWavyProgressIndicator(
                         progress = { (download?.progress ?: 0) / 100f },
                         modifier = Modifier.size(40.dp),
                         trackStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }),
                     )
                     IconButton(onClick = { download?.id?.let { onCancel?.invoke(it) } }, modifier = Modifier.size(32.dp)) {
                         Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                     }
                }
            } else {
                IconButton(onClick = onDownload) {
                   Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.detail_download), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun CastMemberItem(member: CastMember, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(80.dp)
    ) {
        if (member.profileImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(member.profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        if (!member.role.isNullOrEmpty()) {
            Text(
                text = member.role,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReviewCarouselItem(
    review: com.hasanege.materialtv.model.ImdbReview,
    modifier: Modifier = Modifier
) {
    var isRevealed by remember { mutableStateOf(!review.spoilers) }
    
    ElevatedCard(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = review.spoilers && !isRevealed) { isRevealed = true },
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (review.rating != null) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = review.rating,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = review.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = review.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "by ${review.author}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box {
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.blur(if (!isRevealed) 12.dp else 0.dp)
                )
                
                if (review.spoilers && !isRevealed) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "Spoiler - Tap to show",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            if (review.spoilers && isRevealed) {
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "ℹ️ Spoiler revealed",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}


