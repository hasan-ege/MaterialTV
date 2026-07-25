
package com.hasanege.materialtv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.Episode
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.utils.StringUtils
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

import dagger.hilt.android.AndroidEntryPoint
import com.hasanege.materialtv.ui.player.FullscreenPlayer
import com.hasanege.materialtv.ui.player.PlayerControlsOverlay
import com.hasanege.materialtv.ui.player.formatDuration
import com.hasanege.materialtv.player.PlayerEngine
import com.hasanege.materialtv.player.LibVlcEngine
import com.hasanege.materialtv.player.ExoPlayerEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@AndroidEntryPoint
@UnstableApi
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class PlayerActivity : ComponentActivity() {

    // Lazy detailViewModel - only initialized when needed (not for local files)
    private val detailViewModel: DetailViewModel by viewModels()
    private val snackbarHostState = androidx.compose.material3.SnackbarHostState()
    @javax.inject.Inject
    lateinit var xtreamRepository: com.hasanege.materialtv.repository.XtreamRepository
    @javax.inject.Inject
    lateinit var skipDbApiService: com.hasanege.materialtv.network.skipdb.SkipDbApiService
    @javax.inject.Inject
    lateinit var tmdbDao: com.hasanege.materialtv.data.dao.TmdbDao
    @javax.inject.Inject
    lateinit var tmdbApiService: com.hasanege.materialtv.network.tmdb.TmdbApiService
    @javax.inject.Inject
    lateinit var openSubtitlesRepository: com.hasanege.materialtv.repository.OpenSubtitlesRepository

    private var skipDbSegments by mutableStateOf<com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer?>(null)
    private var resolvedImdbId by mutableStateOf<String?>(null)

    private var playerEngine by mutableStateOf<PlayerEngine?>(null)
    private var currentMovie by mutableStateOf<VodItem?>(null)
    private var currentSeriesEpisode by mutableStateOf<Episode?>(null)
    private var seriesId: Int = -1
    private var title by mutableStateOf<String?>(null)
    private var currentUrl: String? = null
    private var isVlc by mutableStateOf(false) // Default to false (ExoPlayer) initially
    private var lastPlaybackPosition: Long = 0L
    private var statsForNerds by mutableStateOf(false)
    private var liveStreamId: Int = -1
    private var liveStreamName: String? = null
    private var isLiveStream: Boolean = false
    private var isDownloadedFile: Boolean = false
    private var streamId: Int = -1
    private var uri: String? = null
    private var originalUrl: String? = null
    private var streamIcon: String? = null
    
    // Track actual watch time (excluding seeking/skipping)
    private var sessionStartTime: Long = 0L
    private var lastPosition: Long = 0L
    private var actualWatchTime: Long = 0L
    private var lastSavedActualWatchTime: Long = 0L
    private var isInPipMode by mutableStateOf(false)
    private var isEnteringPipMode by mutableStateOf(false)
    private var wasPlayingBeforePause: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable activity transition animations for VLC, ExoPlayer, and Hybrid modes
        overridePendingTransition(0, 0)
        
        // Keep screen on during playback
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Register PiP action receiver
        val filter = android.content.IntentFilter().apply {
            addAction(PIP_ACTION_PLAY_PAUSE_INTENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipActionReceiver, filter)
        }
        
        // Default Auto-Enter PiP to false. We will enable it only when playback actually starts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .build()
            )
        }

        streamId = intent.getIntExtra("STREAM_ID", -1)
        seriesId = intent.getIntExtra("SERIES_ID", -1)
        val episodeId = intent.getStringExtra("EPISODE_ID")
        this.title = intent.getStringExtra("TITLE")
        val position = intent.getLongExtra("position", 0L)
        val liveUrl = intent.getStringExtra("url")
        uri = intent.getStringExtra("URI")
        isDownloadedFile = intent.getBooleanExtra("IS_DOWNLOADED_FILE", false)
        originalUrl = intent.getStringExtra("ORIGINAL_URL")
        
        streamIcon = intent.getStringExtra("STREAM_ICON")
        
        // Check if this is a live stream
        val isLiveTypeExtra = intent.getStringExtra("TYPE") == "live"
        val hasLiveId = intent.hasExtra("LIVE_STREAM_ID")
        isLiveStream = liveUrl != null || hasLiveId || isLiveTypeExtra
        if (isLiveStream) {
            liveStreamId = intent.getIntExtra("LIVE_STREAM_ID", if (streamId != -1) streamId else -1)
            liveStreamName = this.title ?: "Live Stream"
        }

        // Removed early fetchSkipDbSegments() - will call in initializePlayer

        // Read default player synchronously using singleton
        val settingsRepository = com.hasanege.materialtv.data.SettingsRepository.getInstance(this)
        var useVlcForDownloads = true
        // Read settings instantly from StateFlow (Memory)
        val player = settingsRepository.defaultPlayer.value
        // Check if we're being forced to use VLC due to ExoPlayer failure
        val forceVlc = intent.getBooleanExtra("forceVlc", false)
        isVlc = forceVlc || (player == "VLC")
        statsForNerds = settingsRepository.statsForNerds.value
        useVlcForDownloads = settingsRepository.useVlcForDownloads.value

        // Indirilmis icerik (yerel dosya) aciliyorsa, ayara gore VLC zorla
        val currentUri = uri
        if (currentUri != null && useVlcForDownloads) {
            isVlc = true
        }

        if (currentUri != null) {
            initializePlayer(currentUri, position)
            setContent {
                MaterialTVTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val engine = playerEngine
                        if (engine != null) {
                            FullscreenPlayer(
                                engine = engine, 
                                title = this@PlayerActivity.title, 
                                showStats = statsForNerds,
                                inPipMode = isInPipMode,
                                onNext = {}, 
                                onPrevious = {}, 
                                onSwitchEngine = { switchEngine() },
                                skipDbSegments = skipDbSegments,
                                imdbId = intent.getStringExtra("IMDB_ID"),
                                openSubtitlesRepository = openSubtitlesRepository
                            )
                        }
                        androidx.compose.material3.SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        BackHandler {
                            finish()
                        }
                    }
                }
            }
            return
        }

        if (liveUrl != null) {
            android.util.Log.d("PlayerActivity", "Playing live URL: [REDACTED]")
            if (liveUrl.isEmpty()) {
                android.util.Log.e("PlayerActivity", "URL is empty!")
                android.widget.Toast.makeText(this, "Stream URL not found", android.widget.Toast.LENGTH_LONG).show()
                finish()
                return
            }
            initializePlayer(liveUrl, position)
            setContent {
                MaterialTVTheme {
                    var showEpgSheet by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val engine = playerEngine
                        if (engine != null) {
                            FullscreenPlayer(
                                engine = engine, 
                                title = this@PlayerActivity.title,
                                showStats = statsForNerds,
                                inPipMode = isInPipMode,
                                onNext = {}, 
                                onPrevious = {}, 
                                onSwitchEngine = { switchEngine() },
                                isLiveStream = true,
                                onShowEpg = {
                                    if (liveStreamId != -1) {
                                        showEpgSheet = true
                                    }
                                }
                            )
                        }
                        androidx.compose.material3.SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        BackHandler {
                            finish()
                        }
                    }

                    if (showEpgSheet && liveStreamId != -1) {
                        com.hasanege.materialtv.ui.components.EpgBottomSheet(
                            streamId = liveStreamId,
                            onDismissRequest = { showEpgSheet = false }
                        )
                    }
                }
            }
            return
        }


        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (streamId != -1) {
            detailViewModel.loadMovieDetails(username, password, streamId)
        } else if (seriesId != -1) {
            detailViewModel.loadSeriesDetails(username, password, seriesId, episodeId)
        }

        setContent {
            MaterialTVTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val movieState = detailViewModel.movie
                    val seriesState = detailViewModel.series
                    val watchHistory: List<ContinueWatchingItem> by WatchHistoryManager.historyFlow.collectAsState()
                    val nextEpisodeThreshold by com.hasanege.materialtv.data.SettingsRepository.getInstance(LocalContext.current)
                        .nextEpisodeThresholdMinutes.collectAsState(initial = 5)
                    var hasPlayed by remember { mutableStateOf(intent.getBooleanExtra("AUTO_PLAY", false)) }



                // Auto Play Logic
                LaunchedEffect(movieState, seriesState) {
                    if (intent.getBooleanExtra("AUTO_PLAY", false) && playerEngine == null) {
                        if (movieState is UiState.Success) {
                            val response = movieState.data.xtreamData
                             val vodItem = VodItem(
                                streamId = response.movieData?.streamId?.toIntOrNull() ?: 0,
                                name = response.info?.name ?: "",
                                streamIcon = response.info?.movieImage,
                                rating5Based = response.info?.rating5based?.toDouble() ?: 0.0,
                                categoryId = response.movieData?.categoryId,
                                containerExtension = response.movieData?.containerExtension,
                                year = response.info?.year
                            )
                            // Initialize logic copied from onPlayMovie
                            this@PlayerActivity.currentMovie = vodItem
                            this@PlayerActivity.currentSeriesEpisode = null
                            val historyItem = watchHistory
                                .find { it.streamId == vodItem.streamId }
                            val startPosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                 historyItem.position
                            } else 0L
                            initializePlayer(movieUrl(vodItem), startPosition)
                        } else if (seriesState is UiState.Success) {
                             val seriesData = seriesState.data.xtreamData
                             val targetEpisodeId = intent.getStringExtra("EPISODE_ID")
                             if (targetEpisodeId != null) {
                                 var targetEpisode: com.hasanege.materialtv.model.Episode? = null
                                 val epElement = seriesData.episodes
                                 if (epElement is kotlinx.serialization.json.JsonObject) {
                                     val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                                     epElement.entries.forEach { (key, element) ->
                                         val sNum = key.toIntOrNull() ?: 0
                                         try {
                                             val eps = json.decodeFromJsonElement<List<com.hasanege.materialtv.model.Episode>>(element)
                                             val found = eps.find { it.id == targetEpisodeId }
                                             if (found != null) targetEpisode = found.copy(season = sNum)
                                         } catch (e: Exception) {}
                                     }
                                 }
                                 if (targetEpisode != null) {
                                     this@PlayerActivity.currentMovie = null
                                     this@PlayerActivity.currentSeriesEpisode = targetEpisode
                                     this@PlayerActivity.title = targetEpisode?.title
                                     
                                     val url = "${com.hasanege.materialtv.network.SessionManager.serverUrl}/series/${com.hasanege.materialtv.network.SessionManager.username}/${com.hasanege.materialtv.network.SessionManager.password}/${targetEpisode?.id}.${targetEpisode?.containerExtension}"
                                     val startPosition = intent.getLongExtra("position", 0L)
                                     android.util.Log.e("PlayerActivity", "AutoPlay Series Episode: ${targetEpisode?.title}, ID: ${targetEpisode?.id}")
                                     initializePlayer(url, startPosition)
                                 }
                             }
                        } else if (movieState is UiState.Error && streamId != -1) {
                            val fallbackItem = VodItem(streamId = streamId, name = this@PlayerActivity.title ?: "")
                            this@PlayerActivity.currentMovie = fallbackItem
                            initializePlayer(movieUrl(fallbackItem), 0L)
                        }
                    }
                }

                if (hasPlayed) {
                    val engine = playerEngine
                    if (engine != null) {
                        val activeImdbId = resolvedImdbId
                            ?: intent.getStringExtra("IMDB_ID")
                            ?: (movieState as? UiState.Success)?.data?.xtreamData?.info?.imdbID
                            ?: (movieState as? UiState.Success)?.data?.tmdbData?.imdbId
                            ?: (seriesState as? UiState.Success)?.data?.xtreamData?.info?.imdbID
                            ?: (seriesState as? UiState.Success)?.data?.tmdbData?.imdbId

                        FullscreenPlayer(
                            engine = engine,
                            title = this@PlayerActivity.title,
                            showStats = statsForNerds,
                            inPipMode = isInPipMode,
                            nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                            onNext = { playNextEpisode() },
                            onPrevious = { playPreviousEpisode() },
                            onSwitchEngine = { switchEngine() },
                            skipDbSegments = skipDbSegments,
                            imdbId = activeImdbId,
                            seasonNumber = currentSeriesEpisode?.season,
                            episodeNumber = currentSeriesEpisode?.episodeNum?.toIntOrNull(),
                            openSubtitlesRepository = openSubtitlesRepository
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    BackHandler {
                        savePlaybackPosition() // Ensure save happens before finish/transition
                        if (intent.getBooleanExtra("AUTO_PLAY", false)) {
                            finish()
                        } else {
                            hasPlayed = false
                            playerEngine?.release()
                            playerEngine = null
                            // Disable Auto-Enter PiP when back to details
                            setPipAutoEnterEnabled(false)
                        }
                    }
                } else {
                    when {
                        movieState is UiState.Success -> {
                            val response = movieState.data.xtreamData
                            val vodItem = VodItem(
                                streamId = response.movieData?.streamId?.toIntOrNull() ?: 0,
                                name = response.info?.name ?: "",
                                streamIcon = response.info?.movieImage,
                                rating5Based = response.info?.rating5based?.toDouble() ?: 0.0,
                                categoryId = response.movieData?.categoryId,
                                containerExtension = response.movieData?.containerExtension,
                                year = response.info?.year
                            )

                            
                            val historyItem = watchHistory.find { it.streamId == vodItem.streamId }
                            val resumePosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                 historyItem.position
                            } else 0L
                            
                            val progress = if (historyItem != null && historyItem.duration > 0) {
                                 historyItem.position.toFloat() / historyItem.duration.toFloat()
                            } else 0f

                            DetailScreen(
                                movie = vodItem,
                                movieDetails = response.info,
                                tmdbData = movieState.data.tmdbData,
                                watchProgress = progress,
                                resumePosition = resumePosition,
                                nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                                onBack = { finish() },
                                onPlayMovie = { movie ->
                                    this@PlayerActivity.currentMovie = movie
                                    this@PlayerActivity.currentSeriesEpisode = null
                                    playerEngine?.release()
                                    // Use 'watchHistory' state to get latest
                                    val hItem = watchHistory.find { it.streamId == movie.streamId }
                                    val startPosition = if (hItem != null && !WatchHistoryManager.isFinished(hItem, nextEpisodeThreshold)) {
                                         hItem.position
                                    } else 0L
                                    initializePlayer(movieUrl(movie), startPosition)
                                    hasPlayed = true
                                },
                                onDownloadMovie = { movie ->
                                    com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(movie)
                                }
                            )
                        }

                        seriesState is UiState.Success -> {
                            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                            val seriesData = seriesState.data.xtreamData
                            val epElement = seriesData.episodes
                            
                            val episodesMapRaw: Map<String, List<Episode>> = try {
                                if (epElement is kotlinx.serialization.json.JsonObject) {
                                    epElement.entries.associate { (key, element) ->
                                        val list: List<Episode> = try {
                                            json.decodeFromJsonElement(element)
                                        } catch (e: Exception) { 
                                            emptyList() 
                                        }
                                        key to list
                                    }
                                } else {
                                    emptyMap()
                                }
                            } catch (e: Exception) { 
                                emptyMap() 
                            }
                            
                            // Flatten and sort episodes
                            val allEpisodes: List<Episode> = episodesMapRaw.flatMap { (seasonKey, list) -> 
                                val sNum = seasonKey.toIntOrNull() ?: 0
                                list.map { it.copy(season = sNum) }
                            }.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNum?.toIntOrNull() ?: 0 }))

                            val lastHistoryItem = watchHistory.find { 
                                it.seriesId == seriesId && it.type == "series" && !it.dismissedFromContinueWatching
                            }

                            var resumeEpisode: Episode? = null
                            var resumePosition: Long = 0L

                            if (lastHistoryItem != null) {
                                val currentEp = allEpisodes.find { it.id == lastHistoryItem.streamId.toString() }
                                if (currentEp != null) {
                                    val isFinished = WatchHistoryManager.isFinished(lastHistoryItem, nextEpisodeThreshold)
                                    
                                    if (isFinished) {
                                        // Find next episode
                                        val idx = allEpisodes.indexOf(currentEp)
                                        if (idx >= 0 && idx < allEpisodes.size - 1) {
                                            resumeEpisode = allEpisodes[idx + 1]
                                            resumePosition = 0L
                                        } else {
                                            // Logic for when the LAST episode is finished. 
                                            // Maybe show the first episode? Or just keep showing last one?
                                            // Let's show the last one, reset to 0.
                                            resumeEpisode = currentEp
                                            resumePosition = 0L
                                        }
                                    } else {
                                        resumeEpisode = currentEp
                                        resumePosition = lastHistoryItem.position
                                    }
                                }
                            }

                            DetailScreen(
                                series = seriesData,
                                tmdbData = seriesState.data.tmdbData,
                                lastWatchedEpisode = resumeEpisode,
                                resumePosition = resumePosition,
                                nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                                onBack = { finish() },
                                onPlayEpisode = { episode ->
                                    this@PlayerActivity.currentMovie = null
                                    this@PlayerActivity.currentSeriesEpisode = episode
                                    this@PlayerActivity.title = episode.title
                                    playerEngine?.release()
                                    val historyItem = watchHistory
                                        .find { it.streamId.toString() == episode.id }
                                    // Only resume if it is the EXACT same episode AND not finished (though user might force resume via list, so maybe just check history)
                                    // If user clicks "Play Next", historyItem refers to specific episode (likely none).
                                    // If user clicks "Resume", episode is the one with history.
                                    // But wait, if user clicks from the list below, we should respect that episode's history.
                                    val startPosition = if (historyItem != null && !WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                         historyItem.position
                                    } else 0L
                                    
                                    initializePlayer(episodeUrl(episode), startPosition)
                                    fetchSkipDbSegments(
                                        providedSeason = episode.season,
                                        providedEpisode = episode.episodeNum?.toIntOrNull()
                                    )
                                    hasPlayed = true
                                },
                                onDownloadEpisode = { episode ->
                                    val seriesName = seriesState.data.tmdbData?.title ?: seriesData.info?.name ?: "Unknown Series"
                                    val sNum = episode.season ?: 1
                                    val epNum = episode.episodeNum?.toIntOrNull() ?: 0
                                    
                                    com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(
                                        episode, 
                                        seriesName,
                                        sNum,
                                        epNum,
                                        seriesState.data.tmdbData?.posterPath ?: seriesData.info?.cover
                                    )
                                },
                                onDownloadSeason = { seasonNum, episodes ->
                                    val seriesName = seriesState.data.tmdbData?.title ?: seriesData.info?.name ?: "Unknown Series"
                                    episodes.forEach { ep ->
                                        val epNum = ep.episodeNum?.toIntOrNull() ?: 0
                                        com.hasanege.materialtv.download.DownloadManagerImpl.getInstance(applicationContext).startDownload(
                                            ep, 
                                            seriesName,
                                            seasonNum,
                                            epNum,
                                            seriesState.data.tmdbData?.posterPath ?: seriesData.info?.cover
                                        )
                                    }
                                },
                                seriesId = seriesId
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    movieState is UiState.Loading || seriesState is UiState.Loading -> CircularProgressIndicator()
                                    movieState is UiState.Error -> Text(
                                        movieState.message,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    seriesState is UiState.Error -> Text(
                                        seriesState.message,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
                androidx.compose.material3.SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

    private fun fetchSkipDbSegments(
        providedSeason: Int? = null,
        providedEpisode: Int? = null
    ) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // If this is a downloaded local file, try to read SkipDB segments from the sidecar JSON metadata file first
                if (isDownloadedFile && !currentUrl.isNullOrBlank()) {
                    val path = currentUrl!!.removePrefix("file://")
                    val jsonPath = if (path.contains(".")) path.substringBeforeLast(".") + ".json" else "$path.json"
                    val file = java.io.File(jsonPath)
                    if (file.exists()) {
                        try {
                            val text = file.readText()
                            val item = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<com.hasanege.materialtv.download.DownloadItem>(text)
                            if (item.skipDbSegments != null) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    skipDbSegments = item.skipDbSegments
                                    android.util.Log.d("SkipDB", "Loaded SkipDB segments from offline metadata: ${item.skipDbSegments}")
                                }
                                return@launch
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SkipDB", "Failed to parse offline metadata JSON", e)
                        }
                    }
                }

                var imdbId = intent.getStringExtra("IMDB_ID")?.takeIf { it.isNotBlank() }

                val ep = currentSeriesEpisode
                val season = providedSeason 
                    ?: ep?.season 
                    ?: intent.getIntExtra("SEASON", -1).takeIf { it > 0 }

                val episode = providedEpisode 
                    ?: ep?.episodeNum?.toIntOrNull() 
                    ?: intent.getIntExtra("EPISODE", -1).takeIf { it > 0 }

                // If imdbId is null, check Room DB for tmdb_content record
                if (imdbId.isNullOrBlank()) {
                    val profileId = com.hasanege.materialtv.network.SessionManager.username ?: "default"
                    val targetId = if (seriesId > 0) seriesId else streamId
                    val contentType = if (seriesId > 0) com.hasanege.materialtv.data.entities.ContentType.SERIES else com.hasanege.materialtv.data.entities.ContentType.VOD
                    if (targetId > 0) {
                        val dbEntity = tmdbDao.getTmdbContent(targetId.toString(), contentType, profileId)
                        imdbId = dbEntity?.imdbId?.takeIf { it.isNotBlank() }
                    }
                }

                // If still null, search TMDB API by title dynamically!
                val currentTitle = this@PlayerActivity.title
                if (imdbId.isNullOrBlank() && !currentTitle.isNullOrBlank()) {
                    try {
                        val cleanName = com.hasanege.materialtv.repository.cleanMediaTitle(currentTitle)
                        val tmdbKey = com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity).tmdbApiKey.firstOrNull()?.takeIf { it.isNotBlank() } ?: "8265a9a08a2a898d3632d4b2d308064a"

                        if (seriesId > 0 || season != null) {
                            val searchRes = tmdbApiService.searchTv(cleanName, tmdbKey)
                            val firstMatch = searchRes.results?.firstOrNull()
                            if (firstMatch != null) {
                                val detailRes = tmdbApiService.getTvDetail(firstMatch.id, tmdbKey)
                                imdbId = detailRes.externalIds?.imdbId?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            val searchRes = tmdbApiService.searchMovie(cleanName, tmdbKey)
                            val firstMatch = searchRes.results?.firstOrNull()
                            if (firstMatch != null) {
                                val detailRes = tmdbApiService.getMovieDetail(firstMatch.id, tmdbKey)
                                imdbId = detailRes.externalIds?.imdbId?.takeIf { it.isNotBlank() }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("PlayerActivity", "Dynamic IMDb search failed for: $currentTitle", e)
                    }
                }

                val formattedImdbId = imdbId?.trim()?.let { if (!it.startsWith("tt")) "tt$it" else it }
                if (!formattedImdbId.isNullOrBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        resolvedImdbId = formattedImdbId
                    }

                    var durationSeconds: Int? = null
                    for (i in 1..20) {
                        val dur = playerEngine?.getDuration() ?: 0L
                        if (dur > 0) {
                            durationSeconds = (dur / 1000).toInt()
                            break
                        }
                        kotlinx.coroutines.delay(500)
                    }

                    android.util.Log.d("SkipDB", "Querying SkipDB: imdb_id=$formattedImdbId, season=$season, episode=$episode, duration=$durationSeconds")
                    val response = skipDbApiService.getSegments(
                        imdbId = formattedImdbId,
                        season = season,
                        episode = episode,
                        duration = durationSeconds
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        skipDbSegments = response.segments
                        android.util.Log.d("SkipDB", "SkipDB segments loaded: ${response.segments}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SkipDB", "Error fetching SkipDB segments", e)
            }
        }
    }

    private fun initializePlayer(url: String, position: Long) {

        currentUrl = url
        playerEngine?.release()

        // Force VLC for downloaded files if configured
        val useVlcForDownloads = runBlocking { 
             com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity).useVlcForDownloads.first() 
        }
        
        if (isDownloadedFile && useVlcForDownloads) {
            isVlc = true
        }

        val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(this@PlayerActivity)
        val newEngine = if (isVlc) LibVlcEngine() else ExoPlayerEngine()

        newEngine.apply {
            initialize(this@PlayerActivity)
            setSubtitleSize("Normal")
            setOnErrorCallback { error ->
                 if (!isVlc) {
                     lifecycleScope.launch {
                         val pref = settingsRepo.defaultPlayerPreference.first()
                         if (pref == com.hasanege.materialtv.data.PlayerPreference.HYBRID) {
                             val currentPos = this@apply.getCurrentPosition()
                             // Recreate the activity with VLC forced
                             finish()
                             startActivity(intent.apply {
                                 putExtra("URI", url)
                                 putExtra("position", currentPos)
                                 putExtra("forceVlc", true) // Force VLC to prevent loop
                                 putExtra("IS_DOWNLOADED_FILE", isDownloadedFile)
                             })
                             overridePendingTransition(0, 0)
                         } else {
                              android.widget.Toast.makeText(this@PlayerActivity, "Playback error: ${StringUtils.sanitizeUrl(error.message)}", android.widget.Toast.LENGTH_LONG).show()
                          }
                      }
                 } else {
                     android.widget.Toast.makeText(this@PlayerActivity, "VLC playback error: ${StringUtils.sanitizeUrl(error.message)}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            
            // For local files, ensure we pass a valid URI string
            // If it's a file path, prefix with file:// if needed (ExoPlayer handles paths, but VLC might prefer URI)
            val prepareUrl = if (isDownloadedFile && !url.contains("://")) {
                "file://$url"
            } else {
                url
            }
            
            prepare(prepareUrl, position)
            
            // Update PiP actions when playback state changes
            setOnPlaybackStateChanged { isPlaying ->
                if (isInPipMode) {
                    updatePipActions()
                }
            }
            
            setOnPlaybackEndedCallback {
                lifecycleScope.launch {
                    if (currentSeriesEpisode != null) {
                        playNextEpisode()
                    }
                }
            }
            
            play()
        }
        playerEngine = newEngine
        
        // Enable Auto-Enter PiP now that we are playing
        setPipAutoEnterEnabled(true)
        
        // Fetch SkipDB segments now that player metadata is ready
        fetchSkipDbSegments()
        
        // Start tracking actual watch time
        startWatchSession()
    }

    private fun switchEngine() {
        val currentPos = playerEngine?.getCurrentPosition() ?: 0L
        val currentUrl = this.currentUrl ?: return
        
        // Release current player immediately
        playerEngine?.release()
        playerEngine = null
        
        // Switch engine type
        isVlc = !isVlc
        
        // Initialize new player instantly
        initializePlayer(currentUrl, currentPos)
    }

    private fun playNextEpisode() {
        val seriesData = (detailViewModel.series as? UiState.Success)?.data?.xtreamData ?: return
        val episodesElement = seriesData.episodes ?: return
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(episodesElement)
        val allEpisodes = episodesMap.values.flatten()
        val currentIndex = allEpisodes.indexOf(currentSeriesEpisode)
        if (currentIndex < allEpisodes.size - 1) {
            val nextEpisode = allEpisodes[currentIndex + 1]
            playEpisode(nextEpisode)
        }
    }

    private fun playPreviousEpisode() {
        val seriesData = (detailViewModel.series as? UiState.Success)?.data?.xtreamData ?: return
        val episodesElement = seriesData.episodes ?: return
        val episodesMap = json.decodeFromJsonElement<Map<String, List<Episode>>>(episodesElement)
        val allEpisodes = episodesMap.values.flatten()
        val currentIndex = allEpisodes.indexOf(currentSeriesEpisode)
        if (currentIndex > 0) {
            val previousEpisode = allEpisodes[currentIndex - 1]
            playEpisode(previousEpisode)
        }
    }

    private fun playEpisode(episode: Episode) {
        savePlaybackPosition()

        currentSeriesEpisode = episode
        this.title = episode.title

        playerEngine?.let { player ->
            val historyItem = WatchHistoryManager.getHistory()
                .find { it.streamId.toString() == episode.id }
            val startPosition = historyItem?.position ?: 0L
            player.prepare(episodeUrl(episode))
            player.seekTo(startPosition)
            player.play()
        }
    }


    private fun movieUrl(movie: VodItem): String {
        // For M3U, get URL from repository; for Xtream, construct it
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            return com.hasanege.materialtv.data.M3uRepository.getStreamUrl(movie.streamId ?: 0) ?: ""
        }
        val extension = movie.containerExtension ?: "mp4"
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.$extension"
    }

    private fun episodeUrl(episode: Episode): String {
        // For M3U, episodes would use their ID as stream ID
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            val streamId = episode.id?.toIntOrNull() ?: 0
            return com.hasanege.materialtv.data.M3uRepository.getStreamUrl(streamId) ?: ""
        }
        val extension = episode.containerExtension ?: "mkv"
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.$extension"
    }

    private fun movieSubtitleUrl(movie: VodItem): String {
        return "${SessionManager.serverUrl}/movie/${SessionManager.username}/${SessionManager.password}/${movie.streamId}.srt"
    }

    private fun episodeSubtitleUrl(episode: Episode): String {
        return "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.srt"
    }



    override fun onPause() {
        super.onPause()
        try {
            // Notify engine about pause (important for SurfaceView/PlayerView handling)
            playerEngine?.onPauseLifecycle()
            
            // Background Playback Enabled:
            // We do NOT pause here anymore. 
            // This fixes PiP transitions and allows background audio playback.
            savePlaybackPosition()
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onPause: ${e.message}")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter PiP when user presses home button
        // For Android 12+ (S), Auto-Enter is enabled, so we don't need to call this manually
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && playerEngine?.isPlaying() == true) {
            isEnteringPipMode = true
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        val wasInPipMode = isInPipMode
        isInPipMode = isInPictureInPictureMode
        isEnteringPipMode = false
        
        if (isInPictureInPictureMode) {
            // Entering PiP: Hide controls, update actions
            updatePipActions()
        } else if (wasInPipMode) {
            // Exiting PiP: Either user closed PiP window (X button) OR expanded back to fullscreen
            // If lifecycle is not at least STARTED, user clicked X to close
            // This check runs after the system updates lifecycle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    // User closed PiP with X button - activity is being destroyed
                    android.util.Log.d("PlayerActivity", "PiP closed - cleaning up player")
                    savePlaybackPosition()
                    playerEngine?.stop()
                    playerEngine?.release()
                    playerEngine = null
                    finish()
                }
                // If STARTED or above, user expanded PiP back to fullscreen - keep playing
            }, 100)
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Set flag BEFORE entering PiP so lifecycle methods don't pause player
                isEnteringPipMode = true
                isInPipMode = true
                
                // Calculate aspect ratio from video format
                var aspectRatio = Rational(16, 9)
                try {
                    val videoFormat = playerEngine?.getVideoFormat()
                    if (videoFormat != null) {
                        // Format is usually "WxH" or "WxH codecs"
                        val parts = videoFormat.split(" ")[0].split("x")
                        if (parts.size == 2) {
                            val width = parts[0].toInt()
                            val height = parts[1].toInt()
                            if (width > 0 && height > 0) {
                                aspectRatio = Rational(width, height)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to 16:9
                }

                val actions = buildPipActions()
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setActions(actions)
                    .build()
                val success = enterPictureInPictureMode(params)
                
                if (!success) {
                    // Failed to enter PiP, reset flags
                    isEnteringPipMode = false
                    isInPipMode = false
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error entering PiP: ${e.message}")
                isEnteringPipMode = false
                isInPipMode = false
            }
        }
    }

    private fun setPipAutoEnterEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(enabled)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error setting auto-enter PiP: ${e.message}")
            }
        }
    }

    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
            try {
                val actions = buildPipActions()
                val params = PictureInPictureParams.Builder()
                    .setActions(actions)
                    .build()
                setPictureInPictureParams(params)
            } catch (e: Exception) {
                android.util.Log.e("PlayerActivity", "Error updating PiP actions: ${e.message}")
            }
        }
    }

    private fun buildPipActions(): List<android.app.RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        
        val actions = mutableListOf<android.app.RemoteAction>()

        // Play/Pause
        val isPlaying = playerEngine?.isPlaying() == true
        val intent = android.content.Intent(PIP_ACTION_PLAY_PAUSE_INTENT).setPackage(packageName)
        val playPauseIntent = android.app.PendingIntent.getBroadcast(
            this,
            PIP_ACTION_PLAY_PAUSE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseAction = android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
            playPauseTitle,
            playPauseTitle,
            playPauseIntent
        )
        actions.add(playPauseAction)

        return actions
    }

    private val pipActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                PIP_ACTION_PLAY_PAUSE_INTENT -> {
                    if (playerEngine?.isPlaying() == true) {
                        playerEngine?.pause()
                    } else {
                        playerEngine?.play()
                    }
                    // updatePipActions() is called by the state change listener
                }
            }
        }
    }

    companion object {
        private const val PIP_ACTION_PLAY_PAUSE = 1
        private const val PIP_ACTION_PLAY_PAUSE_INTENT = "com.hasanege.materialtv.PIP_PLAY_PAUSE"
    }







    override fun onResume() {
        super.onResume()
        try {
            // Fix: Sync PiP state with system
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInPipMode = isInPictureInPictureMode
            } else {
                isInPipMode = false
            }
            // Ensure we are not stuck in "entering" state
            isEnteringPipMode = false

            if (playerEngine == null && currentUrl != null && !isFinishing) {
                initializePlayer(currentUrl!!, lastPlaybackPosition)
            } else if (playerEngine != null) {
                // Force reattach logic to fix black screen after screen off/on
                // IMPORTANT: Reattach first to create fresh PlayerView/Surface
                playerEngine?.reattach()
                
                // Then notify engine about resume (so new SurfaceView gets updated)
                playerEngine?.onResume()
                
                if (wasPlayingBeforePause) {
                    // Resume playback if it was playing before
                    playerEngine?.play()
                    wasPlayingBeforePause = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onResume: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            android.util.Log.d("PlayerActivity", "onStop: isInPipMode=$isInPipMode, isEnteringPipMode=$isEnteringPipMode, isFinishing=$isFinishing")
            
            // If in PiP mode and activity is stopping (user closed PiP window)
            if (isInPipMode && !isEnteringPipMode) {
                // User closed PiP window with X button
                android.util.Log.d("PlayerActivity", "onStop: PiP mode active, cleaning up")
                savePlaybackPosition()
                playerEngine?.stop()
                playerEngine?.release()
                playerEngine = null
            } else if (!isInPipMode && !isEnteringPipMode) {
                // Not in PiP mode, user navigated away or app backgrounded
                savePlaybackPosition()
                wasPlayingBeforePause = playerEngine?.isPlaying() == true
                playerEngine?.pause()
            } else {
                // Entering PiP, just save position
                savePlaybackPosition()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onStop: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {

            
            // Unregister PiP action receiver
            try {
                unregisterReceiver(pipActionReceiver)
            } catch (e: Exception) {
                // Receiver may not be registered
            }
            
            // Clear screen wake lock
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            savePlaybackPosition()
            playerEngine?.stop()
            playerEngine?.release()
            playerEngine = null
            currentUrl = null
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error in onDestroy: ${e.message}")
        }
    }

    private fun savePlaybackPosition() {
        android.util.Log.d("PlayerActivity", "savePlaybackPosition called. Engine: $playerEngine")
        try {
            playerEngine?.let { player ->
            val position = player.getCurrentPosition()
            val duration = player.getDuration()
            
            // Format for verification logs
            val posStr = String.format("%02d:%02d:%02d", 
               java.util.concurrent.TimeUnit.MILLISECONDS.toHours(position),
               java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(position) % 60,
               java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(position) % 60)
            val durStr = String.format("%02d:%02d:%02d", 
               java.util.concurrent.TimeUnit.MILLISECONDS.toHours(duration),
               java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(duration) % 60,
               java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(duration) % 60)
               
            android.util.Log.d("PlayerActivity", "Raw Position: $posStr, Duration: $durStr")
            lastPlaybackPosition = position
            
            // Calculate actual watch time for this session segment
            val currentTime = System.currentTimeMillis()
            val sessionWatchTime = calculateActualWatchTime(position)
            actualWatchTime += sessionWatchTime
            
            // Reset session tracking to current point to avoid double counting
            sessionStartTime = currentTime
            lastPosition = position
            
            // Calculate delta watch time to send (difference since last save)
            val deltaWatchTime = actualWatchTime - lastSavedActualWatchTime
            lastSavedActualWatchTime = actualWatchTime

            // Save if played for more than a second
            if (position > 1000) {
                // Save live stream watch time
                if (isLiveStream && liveStreamId > 0) {
                    val liveItem = ContinueWatchingItem(
                        streamId = liveStreamId,
                        name = liveStreamName ?: "Live Stream",
                        streamIcon = streamIcon,
                        duration = 0, // Live streams have no duration
                        position = position,
                        type = "live",
                        seriesId = null,
                        episodeId = null,
                        actualWatchTime = actualWatchTime // Store current session total just in case
                    )
                    WatchHistoryManager.saveItemWithWatchTime(liveItem, deltaWatchTime)
                }
                // Save downloaded file watch time
                else if (isDownloadedFile && uri != null) {
                    val currentUri = uri!! // Captured because uri is a mutable property
                    // Treat downloaded files exactly like regular content
                    val currentOriginalUrl = originalUrl
                    if (currentOriginalUrl != null && currentOriginalUrl.isNotEmpty()) {
                        // This was originally a series episode, save as series
                        val episodeInfo = com.hasanege.materialtv.data.EpisodeGroupingHelper.extractEpisodeInfo(this.title ?: "")
                        val downloadedItem = ContinueWatchingItem(
                            streamId = WatchHistoryManager.getDownloadId(currentUri),
                            name = this.title ?: "Downloaded File",
                            streamIcon = this.streamIcon, // Use original icon if available
                            duration = duration,
                            position = position,
                            type = if (episodeInfo != null) "series" else "movie",
                            seriesId = episodeInfo?.seriesName?.hashCode(),
                            episodeId = currentOriginalUrl, // Store original URL
                            containerExtension = "file",
                            isDownloaded = true,
                            localPath = currentUri,
                            actualWatchTime = actualWatchTime
                        )
                        WatchHistoryManager.saveItemWithWatchTime(downloadedItem, deltaWatchTime)
                    } else {
                        // Regular downloaded file
                        val downloadedItem = ContinueWatchingItem(
                            streamId = WatchHistoryManager.getDownloadId(currentUri),
                            name = this.title ?: "Downloaded File",
                            streamIcon = currentUri, // Store file path for playback
                            duration = duration,
                            position = position,
                            type = "downloaded",
                            seriesId = null,
                            episodeId = currentOriginalUrl,
                            containerExtension = "file",
                            isDownloaded = true,
                            localPath = currentUri,
                            actualWatchTime = actualWatchTime
                        )
                        WatchHistoryManager.saveItemWithWatchTime(downloadedItem, deltaWatchTime)
                    }
                }
                // Save VoD content watch time
                else if (seriesId != -1) {
                    android.util.Log.d("PlayerActivity", "Saving Episode Progress -> Title: $title, SeriesID: $seriesId, StreamID: $streamId, Position: $posStr, Duration: $durStr")
                    val episodeItem = ContinueWatchingItem(
                        streamId = streamId,
                        name = title ?: "Episode",
                        streamIcon = streamIcon,
                        duration = duration,
                        position = position,
                        type = "series",
                        seriesId = seriesId,
                        episodeId = null, // Can be added if needed
                        actualWatchTime = actualWatchTime
                    )
                    WatchHistoryManager.saveItemWithWatchTime(episodeItem, deltaWatchTime)
                } else {
                    android.util.Log.d("PlayerActivity", "Saving Movie Progress -> Title: $title, StreamID: $streamId, Position: $posStr, Duration: $durStr")
                    val movieItem = ContinueWatchingItem(
                        streamId = streamId,
                        name = title ?: "Movie",
                        streamIcon = streamIcon,
                        duration = duration,
                        position = position,
                        type = "movie",
                        seriesId = null,
                        episodeId = null,
                        actualWatchTime = actualWatchTime
                    )
                    WatchHistoryManager.saveItemWithWatchTime(movieItem, deltaWatchTime)
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error saving playback position: ${e.message}")
        }
    }

    private fun calculateActualWatchTime(currentPosition: Long): Long {
        val currentTime = System.currentTimeMillis()
        val elapsedSessionTime = currentTime - sessionStartTime
        
        // Calculate actual forward progress (excluding seeking backwards)
        val forwardProgress = if (currentPosition > lastPosition) {
            currentPosition - lastPosition
        } else {
            0L // User seeked backwards, don't count this time
        }
        
        // Use the minimum of elapsed time and forward progress to exclude seeking
        return minOf(elapsedSessionTime, forwardProgress)
    }

    private fun startWatchSession() {
        sessionStartTime = System.currentTimeMillis()
        lastPosition = playerEngine?.getCurrentPosition() ?: 0L
        actualWatchTime = 0L
    }

    private fun endWatchSession() {
        if (sessionStartTime > 0) {
            val currentPosition = playerEngine?.getCurrentPosition() ?: 0L
            val sessionWatchTime = calculateActualWatchTime(currentPosition)
            actualWatchTime += sessionWatchTime
            sessionStartTime = 0L
        }
    }
}
