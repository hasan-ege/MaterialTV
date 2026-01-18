package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.media3.common.util.UnstableApi
import com.hasanege.materialtv.download.DownloadManager
import com.hasanege.materialtv.download.DownloadManagerImpl
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.coroutines.launch

@UnstableApi
class SeriesDetailActivity : ComponentActivity() {

    private val viewModel: SeriesDetailViewModel by viewModels { SeriesDetailViewModelFactory }
    private lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hasanege.materialtv.SeriesDetailViewModelFactory.initialize(com.hasanege.materialtv.data.SettingsRepository.getInstance(this))
        downloadManager = DownloadManagerImpl.getInstance(applicationContext)

        val seriesId = intent.getIntExtra("SERIES_ID", -1)
        val initialTitle = intent.getStringExtra("TITLE")
        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (seriesId != -1) {
            viewModel.loadSeriesInfo(username, password, seriesId, initialTitle)
        }

        setContent {
            MaterialTVTheme {
                val activeDownloads by downloadManager.downloads.collectAsState(initial = emptyList())
                val watchHistory by com.hasanege.materialtv.WatchHistoryManager.historyFlow.collectAsState()
                val nextEpisodeThreshold by com.hasanege.materialtv.data.SettingsRepository.getInstance(LocalContext.current)
                    .nextEpisodeThresholdMinutes.collectAsState(initial = 5)

                when (val state = viewModel.seriesInfoState) {
                    is UiState.Success -> {
                        // Calculate Resume Episode and Position
                        val resumeData = remember(state.data, watchHistory, nextEpisodeThreshold) {
                            val historyItem = watchHistory.find { 
                                it.seriesId == seriesId && it.type == "series" && !it.dismissedFromContinueWatching
                            }

                            if (historyItem != null) {
                                // We need to find the episode in the data to know what's next
                                // Helper to parse ALL episodes flat
                                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                                val epElement = state.data.episodes
                                val allEpisodes = try {
                                    if (epElement is kotlinx.serialization.json.JsonObject) {
                                        epElement.entries.flatMap { (key, element) ->
                                            val sNum = key.toIntOrNull() ?: 0
                                            try {
                                                json.decodeFromJsonElement<List<com.hasanege.materialtv.model.Episode>>(element)
                                                    .map { it.copy(season = sNum) }
                                            } catch (e: Exception) { emptyList() }
                                        }
                                        .sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNum?.toIntOrNull() ?: 0 }))
                                    } else emptyList()
                                } catch (e: Exception) { emptyList() }
                                
                                val currentEp = allEpisodes.find { it.id == historyItem.streamId.toString() }
                                if (currentEp != null) {
                                    if (com.hasanege.materialtv.WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)) {
                                        // Episode is finished (last 5 min), advance to next
                                        val idx = allEpisodes.indexOf(currentEp)
                                        val nextEp = if (idx >= 0 && idx < allEpisodes.size - 1) allEpisodes[idx + 1] else currentEp
                                        // Start from beginning for next episode
                                        Pair(nextEp, 0L)
                                    } else {
                                        // Resume from position
                                        Pair(currentEp, historyItem.position)
                                    }
                                } else null
                            } else null
                        }
                        
                        val resumeEpisode = resumeData?.first
                        val resumePosition = resumeData?.second ?: 0L

                        DetailScreen(
                            series = state.data,
                            lastWatchedEpisode = resumeEpisode,
                            resumePosition = resumePosition,
                            nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                            activeDownloads = activeDownloads,
                            onBack = { finish() },
                            onPlayEpisode = { episode ->
                                // Calculate position for this specific episode
                                val episodeHistoryItem = watchHistory.find { 
                                    it.streamId.toString() == episode.id && it.type == "series"
                                }
                                val episodePosition = if (episodeHistoryItem != null && 
                                    !WatchHistoryManager.isFinished(episodeHistoryItem, nextEpisodeThreshold)) {
                                    episodeHistoryItem.position
                                } else 0L
                                
                                val intent = Intent(this, PlayerActivity::class.java).apply {
                                    putExtra(
                                        "url",
                                        "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.${episode.containerExtension}"
                                    )
                                    putExtra("TITLE", episode.title)
                                    putExtra("STREAM_ID", episode.id.toIntOrNull() ?: -1)
                                    putExtra("SERIES_ID", seriesId)
                                    putExtra("position", episodePosition)
                                }
                                startActivity(intent)
                            },
                            onDownloadEpisode = { episode ->
                                val seriesName = state.data.info?.name ?: "Unknown Series"
                                val seasonNum = episode.season ?: 1
                                val episodeNum = episode.episodeNum?.toIntOrNull() ?: 1
                                downloadManager.startDownload(
                                    episode = episode, 
                                    seriesName = seriesName, 
                                    seasonNumber = seasonNum, 
                                    episodeNumber = episodeNum, 
                                    seriesCoverUrl = state.data.info?.cover
                                )
                            },
                            onCancelDownload = { downloadId ->
                                downloadManager.cancelDownload(downloadId)
                            },
                            onDownloadSeason = { seasonNum, episodes ->
                                val seriesName = state.data.info?.name ?: "Unknown Series"
                                downloadManager.downloadSeason(
                                    seriesName = seriesName,
                                    seasonNumber = seasonNum,
                                    episodes = episodes,
                                    seriesCoverUrl = state.data.info?.cover
                                )
                            },
                            seriesId = seriesId
                        )
                    }
                    is UiState.Loading -> {


                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                            androidx.compose.material3.CircularWavyProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message)
                        }
                    }
                }
            }
        }
    }
}
