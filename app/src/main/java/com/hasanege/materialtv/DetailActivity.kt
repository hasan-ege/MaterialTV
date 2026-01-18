@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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

@UnstableApi
class DetailActivity : ComponentActivity() {

    private val viewModel: DetailViewModel by viewModels { DetailViewModelFactory }
    private lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.hasanege.materialtv.DetailViewModelFactory.initialize(com.hasanege.materialtv.data.SettingsRepository.getInstance(this))
        downloadManager = DownloadManagerImpl.getInstance(applicationContext)

        val streamId = intent.getIntExtra("STREAM_ID", -1)
        val initialTitle = intent.getStringExtra("TITLE")
        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (streamId != -1) {
            viewModel.loadMovieDetails(username, password, streamId, initialTitle)
        }

        setContent {
            MaterialTVTheme {
                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                val activeDownloads by downloadManager.downloads.collectAsState(initial = emptyList())
                val watchHistory by com.hasanege.materialtv.WatchHistoryManager.historyFlow.collectAsState()
                
                when (val state = viewModel.movie) {
                    is UiState.Success -> {
                        val movieData = state.data.movieData
                        val vodInfo = state.data.info
                        val vodItem = com.hasanege.materialtv.model.VodItem(
                            streamId = movieData?.streamId?.toIntOrNull(),
                            name = movieData?.name,
                            streamIcon = vodInfo?.movieImage,
                            rating5Based = vodInfo?.rating5based?.toDouble(),
                            categoryId = movieData?.categoryId,
                            containerExtension = movieData?.containerExtension,
                            year = vodInfo?.year
                        )

                        val historyItem = watchHistory.find { it.streamId == vodItem.streamId }
                        val progress = if (historyItem != null && historyItem.duration > 0) {
                             historyItem.position.toFloat() / historyItem.duration.toFloat()
                        } else 0f
                        
                        // Calculate resume position - if near end, start from beginning
                        val nextEpisodeThreshold by com.hasanege.materialtv.data.SettingsRepository.getInstance(this)
                            .nextEpisodeThresholdMinutes.collectAsState(initial = 5)
                        val resumePosition = if (historyItem != null && historyItem.duration > 0) {
                            val isNearEnd = WatchHistoryManager.isFinished(historyItem, nextEpisodeThreshold)
                            if (isNearEnd) 0L else historyItem.position
                        } else 0L

                        DetailScreen(
                            movie = vodItem,
                            movieDetails = state.data.info,
                            watchProgress = progress,
                            resumePosition = resumePosition,
                            nextEpisodeThresholdMinutes = nextEpisodeThreshold,
                            onBack = { finish() },
                            onPlayMovie = { movie ->
                                    val intent = Intent(this, PlayerActivity::class.java).apply {
                                    putExtra("STREAM_ID", movie.streamId)
                                    putExtra("TITLE", movie.name)
                                    putExtra("CONTAINER_EXTENSION", movie.containerExtension)
                                    putExtra("AUTO_PLAY", true)
                                    putExtra("position", resumePosition)
                                }
                                startActivity(intent)
                            },
                            onDownloadMovie = { movie ->
                                downloadManager.startDownload(movie)
                            },
                            activeDownloads = activeDownloads
                        )
                    }
                    is UiState.Loading -> {


                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
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
