package com.hasanege.materialtv.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.WatchHistoryViewModel
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WatchHistoryActivity : ComponentActivity() {
    private val viewModel: WatchHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WatchHistoryScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchHistoryScreen(
    viewModel: WatchHistoryViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.hasanege.materialtv.R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(stringResource(com.hasanege.materialtv.R.string.watch_history_clear_all))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(com.hasanege.materialtv.R.string.history_empty_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { "${it.streamId}_${it.type}" }) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = {
                            if (item.type == "live") {
                                val streamUrl = if (com.hasanege.materialtv.network.SessionManager.loginType == com.hasanege.materialtv.network.SessionManager.LoginType.M3U) {
                                    com.hasanege.materialtv.data.M3uRepository.getStreamUrl(item.streamId)
                                } else {
                                    "${com.hasanege.materialtv.network.SessionManager.serverUrl}/live/${com.hasanege.materialtv.network.SessionManager.username}/${com.hasanege.materialtv.network.SessionManager.password}/${item.streamId}.ts"
                                }
                                if (!streamUrl.isNullOrEmpty()) {
                                    context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("url", streamUrl)
                                        putExtra("TITLE", item.name)
                                        putExtra("LIVE_STREAM_ID", item.streamId)
                                        putExtra("STREAM_ICON", item.streamIcon)
                                        putExtra("TYPE", "live")
                                    })
                                }
                            } else {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("STREAM_ID", item.streamId)
                                    putExtra("TITLE", item.name)
                                    putExtra("STREAM_ICON", item.streamIcon)
                                    putExtra("AUTO_PLAY", true)
                                    putExtra("position", item.position)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onDelete = { viewModel.removeItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
            ) {
                if (!item.streamIcon.isNullOrEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(item.streamIcon)
                            .crossfade(300)
                            .build(),
                        imageLoader = com.hasanege.materialtv.ui.utils.ImageConfig.getImageLoader(context),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (item.type == "live") ContentScale.Fit else ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Progress Bar
                if (item.duration > 0) {
                    val progress = item.position.toFloat() / item.duration.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressPercent = if (item.duration > 0) {
                    (item.position * 100 / item.duration).toInt()
                } else 0
                
                Text(
                    text = stringResource(com.hasanege.materialtv.R.string.history_watched_percent, progressPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
