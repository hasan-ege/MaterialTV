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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
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
        
        val initialFilter = intent.getStringExtra("FILTER_TYPE")
        if (initialFilter != null) {
            viewModel.setFilter(initialFilter)
        }
        
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
    val currentFilter by viewModel.filterType.collectAsState()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to com.hasanege.materialtv.R.string.history_filter_all,
                    "MOVIES" to com.hasanege.materialtv.R.string.history_filter_movies,
                    "SERIES" to com.hasanege.materialtv.R.string.history_filter_series,
                    "LIVE" to com.hasanege.materialtv.R.string.history_filter_live
                )
                items(filters) { (type, stringRes) ->
                    val isSelected = currentFilter == type || (currentFilter == null && type == null)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(stringResource(stringRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.fillMaxSize(),
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
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(170.dp)
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
                            Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Dark overlay gradient for image
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
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
                        trackColor = Color.Transparent,
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val positionMinutes = (item.position / 60000L).let { if (it == 0L && item.position > 0) 1L else it }
                val durationMinutes = item.duration / 60000L
                val progressText = if (durationMinutes > 0) {
                    stringResource(com.hasanege.materialtv.R.string.history_watched_minutes, positionMinutes, durationMinutes)
                } else {
                    stringResource(com.hasanege.materialtv.R.string.history_watched_minutes_only, positionMinutes)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (item.type) {
                        "live" -> Icons.Default.LiveTv
                        "series" -> Icons.Default.Tv
                        else -> Icons.Default.Movie
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
