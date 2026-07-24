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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.collectAsState
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Composable
fun DetailScreen(
    movie: VodItem? = null,
    movieDetails: VodInfo? = null,
    series: SeriesInfoResponse? = null,
    tmdbData: com.hasanege.materialtv.data.entities.TmdbContentEntity? = null,
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
    

    val currentEpisodes = episodesMap[selectedSeasonKey] ?: emptyList()
    
    val title = movie?.name ?: series?.info?.name ?: tmdbData?.title ?: stringResource(R.string.detail_unknown_title)
    val plot = tmdbData?.overview?.takeIf { it.isNotBlank() }
        ?: movieDetails?.fullPlot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.fullPlot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: movieDetails?.plot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.plot?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: stringResource(R.string.detail_no_description)
        
    val rating = tmdbData?.voteAverage?.takeIf { it > 0.0 }?.let { String.format(java.util.Locale.US, "%.1f", it) }
        ?: movieDetails?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.imdbRating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: movieDetails?.rating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.rating?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: stringResource(R.string.unknown)
        

    val metascore = movieDetails?.metascore ?: series?.info?.metascore ?: "N/A"
    val awards = movieDetails?.awards ?: series?.info?.awards ?: ""
    val rawBackdrop = tmdbData?.backdropPath 
        ?: movieDetails?.backdropPath?.firstOrNull() 
        ?: series?.info?.backdropPath?.firstOrNull() 
        ?: series?.info?.cover 
        ?: movieDetails?.movieImage
        ?: movie?.streamIcon
    val backdropUrl = remember(rawBackdrop) {
        when {
            rawBackdrop.isNullOrBlank() -> null
            rawBackdrop.startsWith("/") -> "https://image.tmdb.org/t/p/original$rawBackdrop"
            else -> rawBackdrop
        }
    }
    val genres = movieDetails?.genre ?: series?.info?.genre ?: ""
    val releaseDate = tmdbData?.releaseDate ?: movieDetails?.releaseDate ?: series?.info?.releaseDate ?: ""
    val director = tmdbData?.director ?: movieDetails?.director ?: series?.info?.director ?: ""

    val tmdbCastList: List<CastMember> = remember(tmdbData?.castJson) {
        if (!tmdbData?.castJson.isNullOrBlank()) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<CastMember>>(tmdbData.castJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    val castMembers = (tmdbCastList.ifEmpty { movieDetails?.imdbCast ?: series?.info?.imdbCast ?: emptyList() })
        .filter { it.name.trim().length > 1 }
        .distinctBy { it.name }

    val imdbId = tmdbData?.imdbId?.takeIf { it.isNotBlank() }
        ?: movieDetails?.imdbID?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.imdbID?.takeIf { it != "N/A" && it.isNotBlank() }
    val tmdbId = tmdbData?.tmdbId?.takeIf { it > 0 }?.toString()
    val streamIdStr = (movie?.streamId ?: seriesId)?.takeIf { it > 0 }?.toString()
    val imdbVotes = movieDetails?.imdbVotes?.takeIf { it != "N/A" && it.isNotBlank() } 
        ?: series?.info?.imdbVotes?.takeIf { it != "N/A" && it.isNotBlank() }

    val rawTrailerUrl = movieDetails?.youtubeTrailer ?: movieDetails?.trailer ?: series?.info?.youtubeTrailer ?: series?.info?.trailer

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

    val buttonTextStyle = when {
        configuration.screenWidthDp < 400 -> MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.titleMedium
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            // Tablet / TV / Widescreen Split Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Sticky Info and Action Buttons (45% Width)
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    // Left Panel Background Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Left Panel Gradient Overlay (Vibrant, high-contrast, premium look)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )

                    // Left Panel Content Container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                            .padding(top = safeTopPadding),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    blurRadius = 16f
                                )
                            ),
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Metadata Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (rating != "N/A" && rating.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = stringResource(R.string.detail_rating),
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val displayRating = if (rating.contains("/") || rating.isEmpty()) rating else "$rating/10"
                                    Text(
                                        text = displayRating,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            
                            if (genres.isNotEmpty()) {
                                Text(
                                    text = genres.split(",").take(2).joinToString(", "),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
        
                            if (releaseDate.isNotEmpty()) {
                                Text(
                                    text = releaseDate.take(4),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                            

                        }

                        // ID Chips Row (IMDb ID, TMDB ID, Stream ID)
                        if (!imdbId.isNullOrBlank() || !tmdbId.isNullOrBlank() || !streamIdStr.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!imdbId.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFF5C518),
                                        contentColor = Color.Black,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "IMDb $imdbId",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (!tmdbId.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFF0D253F),
                                        contentColor = Color(0xFF01B4E4),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "TMDB $tmdbId",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (!streamIdStr.isNullOrBlank()) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ID: $streamIdStr",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (awards.isNotEmpty() && awards != "N/A") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = awards,
                                style = MaterialTheme.typography.titleSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Button
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = ExpressiveShapes.Medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(playButtonText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            
                            // Favorites Button
                            FilledTonalIconButton(
                                onClick = {
                                    scope.launch {
                                        val thumbnailUrl = tmdbData?.posterPath ?: movieDetails?.movieImage ?: series?.info?.cover
                                        val genre = movieDetails?.genre ?: series?.info?.genre
                                        val year = tmdbData?.releaseDate?.take(4) ?: movieDetails?.releaseDate?.take(4) ?: series?.info?.releaseDate?.take(4)
                                        
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
                                modifier = Modifier.size(64.dp),
                                shape = ExpressiveShapes.Medium,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = stringResource(if (isFavorite) R.string.favorites_remove else R.string.favorites_add),
                                    modifier = Modifier.size(28.dp)
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
                                        .width(72.dp)
                                        .height(64.dp),
                                    shape = ExpressiveShapes.Medium,
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularWavyProgressIndicator(
                                            progress = { (download?.progress ?: 0) / 100f },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isDownloaded) Icons.Rounded.Check else Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Series Download Season Button
                        if (series != null) {
                            Spacer(modifier = Modifier.height(16.dp))
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
                                    .height(56.dp),
                                shape = ExpressiveShapes.Medium
                            ) {
                                Icon(if (seasonDownloadStarted) Icons.Rounded.Check else Icons.Rounded.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.detail_download_season), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                        }
                    }

                    // Sticky Back Button at the top left of the left panel
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = 16.dp + safeTopPadding, start = 24.dp)
                            .size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }

                // Right Panel: Scrollable Description, Trailers, Cast, Episodes (55% Width)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .padding(top = safeTopPadding)
                ) {
                    // Storyline
                    Text(
                        text = stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 28.sp
                    )

                    // YouTube Trailer Section
                    if (!rawTrailerUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.detail_trailer_header),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val finalUrl = if (!rawTrailerUrl.contains("http")) "https://www.youtube.com/watch?v=$rawTrailerUrl" else rawTrailerUrl
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.detail_trailer_header))
                        }
                    }

                    // Cast Section
                    if (castMembers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.detail_cast_header),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(castMembers) { member ->
                                CastMemberItem(member = member)
                            }
                        }
                    }

                    if (director.isNotEmpty() || (castMembers.isEmpty() && movieDetails?.cast != null)) {
                        Spacer(modifier = Modifier.height(20.dp))
                        if (director.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!tmdbData?.directorAvatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(tmdbData.directorAvatar)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = director,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.detail_director, director),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                )
                            }
                        }
                        if (castMembers.isEmpty() && movieDetails?.cast != null) {
                            Text(
                                text = stringResource(R.string.detail_cast, movieDetails.cast),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Series Seasons and Episode Lists
                    if (series != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (episodesMap.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.tab_seasons),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                scrollable = sortedSeasons.size > 4
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            currentEpisodes.forEach { episode ->
                                EpisodeItem(
                                    episode = episode,
                                    seriesName = series.info?.name,
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
        } else {
            // Original / Phone Layout (Butter smooth and responsive)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Background Image with Parallax / Fixed Top Layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight + 100.dp)
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
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                // Main Content Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(contentSpacerHeight))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = 80.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
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
                                if (rating != "N/A" && rating.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = stringResource(R.string.detail_rating),
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val displayRating = if (rating.contains("/") || rating.isEmpty()) rating else "$rating/10"
                                        Text(
                                            text = displayRating,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
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
            
                                if (releaseDate.isNotEmpty()) {
                                    Text(
                                        text = releaseDate.take(4),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

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

                            // ID Chips Row (IMDb ID, TMDB ID, Stream ID)
                            if (!imdbId.isNullOrBlank() || !tmdbId.isNullOrBlank() || !streamIdStr.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!imdbId.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0xFFF5C518),
                                            contentColor = Color.Black,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "IMDb $imdbId",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (!tmdbId.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0xFF0D253F),
                                            contentColor = Color(0xFF01B4E4),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "TMDB $tmdbId",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (!streamIdStr.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ID: $streamIdStr",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
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
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Action Buttons Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (series != null) 12.dp else 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                            }

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
                            
                            // Trailer
                            if (!rawTrailerUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.detail_trailer_header),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val finalUrl = if (!rawTrailerUrl.contains("http")) "https://www.youtube.com/watch?v=$rawTrailerUrl" else rawTrailerUrl
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
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.detail_trailer_header))
                                }
                            }

                            // Cast
                            if (castMembers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.detail_cast_header),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(castMembers) { member ->
                                        CastMemberItem(member = member)
                                    }
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
                        }

                        if (series != null) {
                            Spacer(modifier = Modifier.height(24.dp))
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    scrollable = sortedSeasons.size > 4
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                currentEpisodes.forEach { episode ->
                                    EpisodeItem(
                                        episode = episode,
                                        seriesName = series.info?.name,
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
                
                // Floating Action Bar for compact scrolling layout
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

                // Fixed Back Button for compact layout when toolbar not visible
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
            }
        }
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
        modifier = modifier.width(96.dp)
    ) {
        if (!member.profileImageUrl.isNullOrBlank()) {
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
                    text = member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        if (!member.character.isNullOrBlank()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MovieDetailScreenRoute(
    streamId: Int,
    initialTitle: String,
    viewModel: DetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToPlayer: (String, String, Int, Int, Long, String?) -> Unit
) {
    val downloadManager = com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(androidx.compose.ui.platform.LocalContext.current)
    
    androidx.compose.runtime.LaunchedEffect(streamId) {
        if (streamId != -1) {
            val username = com.hasanege.materialtv.network.SessionManager.username ?: ""
            val password = com.hasanege.materialtv.network.SessionManager.password ?: ""
            viewModel.loadMovieDetails(username, password, streamId, initialTitle)
        }
    }

    val activeDownloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val watchHistory by com.hasanege.materialtv.WatchHistoryManager.historyFlow.collectAsState()
    
    when (val state = viewModel.movie) {
        is UiState.Success -> {
            val movieData = state.data.xtreamData.movieData
            val coverIcon = state.data.xtreamData.info?.movieImage ?: state.data.tmdbData?.posterPath
            val vodItem = com.hasanege.materialtv.model.VodItem(
                streamId = movieData?.streamId?.toIntOrNull(),
                name = movieData?.name,
                streamIcon = coverIcon,
                containerExtension = movieData?.containerExtension
            )
            val historyItem = watchHistory.find { it.streamId == streamId && it.type == "movie" && !it.dismissedFromContinueWatching }
            val resumePosition = historyItem?.position ?: 0L

            DetailScreen(
                movie = vodItem,
                movieDetails = state.data.xtreamData.info,
                tmdbData = state.data.tmdbData,
                activeDownloads = activeDownloads,
                resumePosition = resumePosition,
                onBack = onBack,
                onPlayMovie = {
                    val ext = movieData?.containerExtension ?: "mp4"
                    val url = "${com.hasanege.materialtv.network.SessionManager.serverUrl}/movie/${com.hasanege.materialtv.network.SessionManager.username}/${com.hasanege.materialtv.network.SessionManager.password}/${streamId}.${ext}"
                    onNavigateToPlayer(url, movieData?.name ?: initialTitle, streamId, -1, resumePosition, coverIcon)
                },
                onDownloadMovie = {
                    downloadManager.startDownload(vodItem, imdbId = state.data.tmdbData?.imdbId)
                },
                onCancelDownload = { downloadId ->
                    downloadManager.cancelDownload(downloadId)
                }
            )
        }
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
    }
}

@Composable
fun SeriesDetailScreenRoute(
    seriesId: Int,
    initialTitle: String,
    viewModel: SeriesDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToPlayer: (String, String, Int, Int, Long, String?) -> Unit
) {
    val downloadManager = com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(androidx.compose.ui.platform.LocalContext.current)
    
    androidx.compose.runtime.LaunchedEffect(seriesId) {
        if (seriesId != -1) {
            val username = com.hasanege.materialtv.network.SessionManager.username ?: ""
            val password = com.hasanege.materialtv.network.SessionManager.password ?: ""
            viewModel.loadSeriesInfo(username, password, seriesId, initialTitle)
        }
    }

    val activeDownloads by downloadManager.downloads.collectAsState(initial = emptyList())
    val watchHistory by com.hasanege.materialtv.WatchHistoryManager.historyFlow.collectAsState()
    val nextEpisodeThreshold by com.hasanege.materialtv.data.SettingsRepository.getInstance(androidx.compose.ui.platform.LocalContext.current)
        .nextEpisodeThresholdMinutes.collectAsState(initial = 5)

    when (val state = viewModel.seriesInfoState) {
        is UiState.Success -> {
            val seriesCover = state.data.xtreamData.info?.cover ?: state.data.tmdbData?.posterPath
            val resumeData = androidx.compose.runtime.remember(state.data, watchHistory, nextEpisodeThreshold) {
                val historyItem = watchHistory.find { 
                    it.seriesId == seriesId && it.type == "series" && !it.dismissedFromContinueWatching
                }
                if (historyItem != null) {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                    val epElement = state.data.xtreamData.episodes
                    val allEpisodes = try {
                        if (epElement is kotlinx.serialization.json.JsonObject) {
                            epElement.entries.flatMap { (key, element) ->
                                val sNum = key.toIntOrNull() ?: 0
                                try {
                                    json.decodeFromJsonElement<List<com.hasanege.materialtv.model.Episode>>(element)
                                        .map { it.copy(season = sNum) }
                                } catch (e: Exception) { emptyList() }
                            }.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNum?.toIntOrNull() ?: 0 }))
                        } else emptyList()
                    } catch (e: Exception) { emptyList() }
                    
                    val currentEp = allEpisodes.find { it.id == historyItem.streamId.toString() }
                    if (currentEp != null) {
                        if (com.hasanege.materialtv.WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                            val idx = allEpisodes.indexOf(currentEp)
                            val nextEp = if (idx >= 0 && idx < allEpisodes.size - 1) allEpisodes[idx + 1] else currentEp
                            Pair(nextEp, 0L)
                        } else {
                            Pair(currentEp, historyItem.position)
                        }
                    } else null
                } else null
            }
            
            val resumeEpisode = resumeData?.first
            val resumePosition = resumeData?.second ?: 0L

            DetailScreen(
                series = state.data.xtreamData,
                tmdbData = state.data.tmdbData,
                lastWatchedEpisode = resumeEpisode,
                resumePosition = resumePosition,
                nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                activeDownloads = activeDownloads,
                onBack = onBack,
                onPlayEpisode = { episode ->
                    // Just navigate directly with position 0
                    val url = "${com.hasanege.materialtv.network.SessionManager.serverUrl}/series/${com.hasanege.materialtv.network.SessionManager.username}/${com.hasanege.materialtv.network.SessionManager.password}/${episode.id}.${episode.containerExtension}"
                    onNavigateToPlayer(url, episode.title ?: "", episode.id.toIntOrNull() ?: -1, seriesId, 0L, seriesCover)
                },
                onDownloadEpisode = { episode ->
                    val seriesName = state.data.tmdbData?.title ?: state.data.xtreamData.info?.name ?: "Unknown Series"
                    downloadManager.startDownload(
                        episode = episode, 
                        seriesName = seriesName, 
                        seasonNumber = episode.season ?: 1, 
                        episodeNumber = episode.episodeNum?.toIntOrNull() ?: 1, 
                        seriesCoverUrl = state.data.tmdbData?.posterPath ?: state.data.xtreamData.info?.cover,
                        imdbId = state.data.tmdbData?.imdbId
                    )
                },
                onCancelDownload = { downloadManager.cancelDownload(it) },
                onDownloadSeason = { seasonNum, episodes ->
                    downloadManager.downloadSeason(
                        seriesName = state.data.tmdbData?.title ?: state.data.xtreamData.info?.name ?: "Unknown Series",
                        seasonNumber = seasonNum,
                        episodes = episodes,
                        seriesCoverUrl = state.data.tmdbData?.posterPath ?: state.data.xtreamData.info?.cover,
                        imdbId = state.data.tmdbData?.imdbId
                    )
                },
                seriesId = seriesId
            )
        }
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }
    }
}
