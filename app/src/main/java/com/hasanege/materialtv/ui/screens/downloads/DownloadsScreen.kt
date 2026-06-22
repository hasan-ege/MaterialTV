@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hasanege.materialtv.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.DownloadFilter
import com.hasanege.materialtv.DownloadsViewModel
import com.hasanege.materialtv.download.ContentType
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadStatus
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import com.hasanege.materialtv.ui.theme.animateStaggeredEntry
import com.hasanege.materialtv.utils.TitleUtils
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hasanege.materialtv.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Define unified display groups for sorting
sealed class DisplayGroup {
    data class Movie(val download: DownloadItem) : DisplayGroup()
    data class Series(val name: String, val episodes: List<DownloadItem>) : DisplayGroup()
    
    val latestTimestamp: Long get() = when(this) {
        is Movie -> download.createdAt
        is Series -> episodes.maxOfOrNull { it.createdAt } ?: 0L
    }
    
    val isActive: Boolean get() = when(this) {
        is Movie -> download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PENDING
        is Series -> episodes.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Rename Dialog State
    var renamingItem by remember { mutableStateOf<DownloadItem?>(null) }

    // Folder Picker
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Take persistable permission
            val contentResolver = context.contentResolver
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.setCustomDownloadFolder(uri)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.rescanDownloads()
                delay(1500) // Tarama için minimum bekleme
                isRefreshing = false
            }
        }
    )
    
    val scanMessage by viewModel.scanMessage.collectAsState()
    
    // Toast notification removed explicitly by user request
    // LaunchedEffect(scanMessage) { ... }
    
    // Rotation animation for scan button
    val infiniteTransition = rememberInfiniteTransition(label = "scanRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    // Permission Check for Android 11+
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Check permission on resume/start
    DisposableEffect(Unit) {
        val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    if (!android.os.Environment.isExternalStorageManager()) {
                        showPermissionDialog = true
                    } else {
                        showPermissionDialog = false
                    }
                }
            }
        }
        val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
        lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            lifecycle?.removeObserver(lifecycleObserver)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to decide, or maybe allow dismiss if they really want */ showPermissionDialog = false },
            title = { Text("İzin Gerekli") },
            text = { Text("Otomatik dosya taraması için 'Tüm Dosyalara Erişim' izni gereklidir. Lütfen ayarlardan bu izni verin.") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data = Uri.parse(String.format("package:%s", context.packageName))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent()
                            intent.action = android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("İzin Ver")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Animation State
    // User wants: Menu appears -> Wait -> Content drops down
    var isContentVisible by remember { mutableStateOf(false) }
    
    // Logic: Wait for loading to finish, then trigger visibility
    LaunchedEffect(isLoading) {
        if (!isLoading) {
             // Slight delay to ensure "loading" feel is perceived before dropping content
             delay(100) 
             isContentVisible = true
        } else {
             isContentVisible = false
        }
    }
    
    // Group and sort downloads into a unified timeline
    val displayGroups = remember(downloads) {
        val movies = downloads.filter { it.contentType == ContentType.MOVIE }
            .map { DisplayGroup.Movie(it) }
            
        val series = downloads
            .filter { it.contentType == ContentType.EPISODE && it.seriesName != null }
            .groupBy { it.seriesName!! }
            .map { (name, episodes) -> 
                DisplayGroup.Series(
                    name, 
                    episodes.sortedWith(compareBy({ it.seasonNumber ?: 0 }, { it.episodeNumber ?: 0 }))
                ) 
            }
            
        (movies + series).sortedWith(
            compareByDescending<DisplayGroup> { it.isActive }
                .thenByDescending { it.latestTimestamp }
        )
    }
    
    // Track expanded series
    var expandedSeries by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 800.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (downloads.isEmpty() && !isRefreshing && !isLoading) {
                // Empty State with simple fade in
                item {
                    EmptyDownloadsView(
                        modifier = Modifier.fillParentMaxSize(),
                        isScanning = isLoading,
                        rotationAngle = rotationAngle,
                        onScanClick = { viewModel.rescanDownloads() },
                        onFolderSelect = { folderLauncher.launch(null) }
                    )
                }
            } else if (isLoading) {
                 // Loading State - Use a subtle shimmer or just empty space while waiting for "drop"
                 // Or we can show the stats card locally if we want, but user asked for "wait"
                 // Let's show a centered CircularWavyProgressIndicator or similar effectively
                 item {
                     Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                         // Fallback to standard if Expressive unavailable in this scope, or usage of CircularProgressIndicator
                         CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                     }
                 }
            } else {
            // Stats Card - Drops down first
                item {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = slideInVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            ),
                            initialOffsetY = { -it } // Comes from top
                        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + expandVertically() 
                    ) {
                        DownloadStatsCard(
                            downloads = downloads,
                            isScanning = isLoading,
                            onScanClick = { viewModel.rescanDownloads() },
                            onFolderSelect = { folderLauncher.launch(null) }
                        )
                    }
                }
                
                // Unified Unified Timeline (Movies and Series intermingled)
                displayGroups.forEachIndexed { index, group ->
                    when (group) {
                        is DisplayGroup.Movie -> {
                            val download = group.download
                            item(key = "movie_${download.id}", contentType = "movie") {
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = slideInVertically(
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                        ),
                                        initialOffsetY = { -it }
                                    ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + expandVertically()
                                ) {
                                    DownloadItemCard(
                                        download = download,
                                        onPause = { viewModel.pauseDownload(download.id) },
                                        onResume = { viewModel.resumeDownload(download.id) },
                                        onCancel = { viewModel.cancelDownload(download.id) },
                                        onDelete = { viewModel.deleteDownload(download.id) },
                                        onPlay = { viewModel.playDownload(context, download) },
                                        onRename = { renamingItem = download },
                                        modifier = Modifier.animateStaggeredEntry(index)
                                    )
                                }
                            }
                        }
                        is DisplayGroup.Series -> {
                            val seriesName = group.name
                            val episodes = group.episodes
                            val isExpanded = expandedSeries.contains(seriesName)
                            
                            // Series header
                            item(key = "series_header_$seriesName", contentType = "series_header") {
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = slideInVertically(
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                        ),
                                        initialOffsetY = { -it }
                                    ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + expandVertically()
                                ) {
                                    SeriesGroupHeader(
                                        seriesName = seriesName,
                                        episodeCount = episodes.size,
                                        isExpanded = isExpanded,
                                        downloadingCount = episodes.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING },
                                        completedCount = episodes.count { it.status == DownloadStatus.COMPLETED },
                                        thumbnailUrl = episodes.firstOrNull()?.seriesCoverUrl 
                                            ?: episodes.firstOrNull()?.thumbnailUrl,
                                        episodeFilePath = episodes.firstOrNull()?.filePath,
                                        totalSize = episodes.sumOf { it.totalBytes },
                                        onClick = {
                                            expandedSeries = if (isExpanded) {
                                                expandedSeries - seriesName
                                            } else {
                                                expandedSeries + seriesName
                                            }
                                        },
                                        modifier = Modifier.animateStaggeredEntry(index)
                                    )
                                }
                            }
                            
                            // Episodes (Flattened)
                            if (isExpanded) {
                                itemsIndexed(
                                    items = episodes,
                                    key = { episodeIndex, it -> "episode_${it.id}" },
                                    contentType = { episodeIndex, it -> "episode" }
                                ) { episodeIndex, download ->
                                    DownloadItemCard(
                                        download = download,
                                        onPause = { viewModel.pauseDownload(download.id) },
                                        onResume = { viewModel.resumeDownload(download.id) },
                                        onCancel = { viewModel.cancelDownload(download.id) },
                                        onDelete = { viewModel.deleteDownload(download.id) },
                                        onPlay = { viewModel.playDownload(context, download) },
                                        onRename = { renamingItem = download },
                                        isEpisode = true,
                                        modifier = Modifier.animateStaggeredEntry(index + episodeIndex + 1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )

        if (renamingItem != null) {
            RenameDialog(
                currentTitle = renamingItem!!.title,
                onDismiss = { renamingItem = null },
                onConfirm = { newName ->
                    viewModel.renameDownload(renamingItem!!.id, newName)
                    renamingItem = null
                }
            )
        }
    }
}

/**
 * Dizi grubu başlık komponenti - kapak fotoğrafı ile
 */
@Composable
fun SeriesGroupHeader(
    seriesName: String,
    episodeCount: Int,
    isExpanded: Boolean,
    downloadingCount: Int,
    completedCount: Int,
    thumbnailUrl: String? = null,
    episodeFilePath: String? = null,
    totalSize: Long = 0L,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth rotation for expand icon
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    // Yerel kapak dosyası yolunu hesapla
    // Yerel kapak dosyası yolunu hesapla
    val localCoverPath = remember(episodeFilePath, thumbnailUrl) {
        if (episodeFilePath != null) {
            val videoFile = java.io.File(episodeFilePath)
            val seasonDir = videoFile.parentFile
            val seriesDir = seasonDir?.parentFile
            
            val png = java.io.File(seriesDir, "cover.png")
            val jpg = java.io.File(seriesDir, "cover.jpg")
            
            if (png.exists()) png 
            else if (jpg.exists()) jpg
            else null
        } else null
    }
    
    val coverExists = remember(localCoverPath) { localCoverPath?.exists() == true }
    val coverModel = if (coverExists) localCoverPath else thumbnailUrl
    
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Series cover thumbnail - matching movie aspect ratio
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 135.dp)
                        .clip(ExpressiveShapes.Medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (coverModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverModel)
                                .size(270, 405) // 90dp x 135dp @ 3x density approx
                                .crossfade(true)
                                .build(),
                            contentDescription = seriesName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Episode count badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = ExpressiveShapes.ExtraSmall,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$episodeCount",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = TitleUtils.cleanTitle(seriesName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (totalSize > 0) {
                        Text(
                            text = formatTotalSize(totalSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Completed badge
                        if (completedCount > 0) {
                            Surface(
                                shape = ExpressiveShapes.Full,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$completedCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        
                        // Downloading badge
                        if (downloadingCount > 0) {
                            Surface(
                                shape = ExpressiveShapes.Full,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "$downloadingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Expand/collapse icon with smooth rotation
            Surface(
                shape = ExpressiveShapes.Full,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }
    }
}

@Composable
private fun DownloadStatsCard(
    downloads: List<DownloadItem>,
    isScanning: Boolean = false,
    onScanClick: () -> Unit = {},
    onFolderSelect: () -> Unit = {}
) {
    val downloadingCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }
    val completedCount = downloads.count { it.status == DownloadStatus.COMPLETED }
    val pausedCount = downloads.count { it.status == DownloadStatus.PAUSED }
    val totalBytes = downloads.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.totalBytes }
    
    // Animated scale for entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ExpressiveShapes.Large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.downloads_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scan Button
                    val rotation by animateFloatAsState(
                        targetValue = if (isScanning) 360f else 0f,
                        animationSpec = if (isScanning) infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ) else tween(0)
                    )
                    
                    IconButton(
                        onClick = onScanClick,
                        enabled = !isScanning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Scan Local Files",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (totalBytes > 0) {
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = formatTotalSize(totalBytes),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = downloadingCount, label = stringResource(R.string.downloads_status_downloading), icon = Icons.Default.Download, isActive = downloadingCount > 0)
                StatItem(count = completedCount, label = stringResource(R.string.downloads_status_completed), icon = Icons.Default.CheckCircle, isActive = false)
                StatItem(count = pausedCount, label = stringResource(R.string.downloads_status_paused), icon = Icons.Default.Pause, isActive = false)
            }
        }
    }
}

private fun formatTotalSize(bytes: Long): String {
    return when {
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

@Composable
private fun StatItem(
    count: Int, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false
) {
    // Pulse animation for active downloads
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { if (isActive) { scaleX = pulse; scaleY = pulse } }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ExpressiveShapes.Full)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: DownloadFilter,
    onFilterSelected: (DownloadFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        DownloadFilter.ALL to stringResource(R.string.downloads_all),
        DownloadFilter.DOWNLOADING to stringResource(R.string.downloads_status_downloading),
        DownloadFilter.COMPLETED to stringResource(R.string.downloads_status_completed),
        DownloadFilter.PAUSED to stringResource(R.string.downloads_status_paused)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = ExpressiveShapes.ExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = ExpressiveShapes.ExtraLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.clickable { onFilterSelected(filter) }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DownloadItemCard(
    download: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit, // New Callback
    isEpisode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = interactionScale; scaleY = interactionScale }
            .combinedClickable(
                onClick = { /* Could act as Play here if desired, but we have strict button actions */ },
                onLongClick = onRename // Trigger rename
            ),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail - önce yerel kapak dosyasını dene (film için), bölüm için video'dan çıkar
            Box(
                modifier = Modifier
                    .size(
                        width = if (download.contentType == ContentType.MOVIE) 90.dp else 120.dp,
                        height = if (download.contentType == ContentType.MOVIE) 135.dp else 68.dp
                    )
                    .clip(ExpressiveShapes.Medium)
            ) {
                // Kapak/thumbnail kaynağını belirle
                // Kapak/thumbnail kaynağını belirle
                val coverModel = remember(download.filePath, download.contentType, download.thumbnailUrl) {
                    when (download.contentType) {
                        ContentType.MOVIE -> {
                            // Film: Yerel kapak dosyası (FilmAdi.png veya .jpg)
                            val videoFile = java.io.File(download.filePath)
                            val parentDir = videoFile.parentFile
                            val baseName = videoFile.nameWithoutExtension
                            
                            val pngFile = java.io.File(parentDir, "$baseName.png")
                            val jpgFile = java.io.File(parentDir, "$baseName.jpg")
                            
                            if (pngFile.exists()) pngFile 
                            else if (jpgFile.exists()) jpgFile
                            else download.thumbnailUrl
                        }
                        ContentType.EPISODE -> {
                            // Bölüm: Yerel thumbnail dosyasını ara
                            // Video: E24_Bolumadi.mp4 -> Thumbnail: E24_thumbnail.png
                            val videoFile = java.io.File(download.filePath)
                            val parentDir = videoFile.parentFile
                            val fileName = videoFile.nameWithoutExtension
                            
                            // Bölüm numarasını çıkar (E01, E24 vb.)
                            val episodePrefix = fileName.split("_").firstOrNull() ?: fileName
                            
                            // 1. E24_thumbnail.png
                            val thumbnailFile = java.io.File(parentDir, "${episodePrefix}_thumbnail.png")
                            if (thumbnailFile.exists()) return@remember thumbnailFile
                            
                            // 2. Original Name + _thumb.jpg (Scraper default?)
                            // Scraper saves as: videoName.jpg (sibling)
                            val siblingJpg = java.io.File(parentDir, "$fileName.jpg")
                            if (siblingJpg.exists()) return@remember siblingJpg

                            // 3. Other variants
                            val variants = listOf(
                                "${fileName}_thumb.jpg",
                                "${fileName}_thumbnail.png",
                                "$fileName.png"
                            )
                            
                            for (v in variants) {
                                val f = java.io.File(parentDir, v)
                                if (f.exists()) return@remember f
                            }
                            
                            // Fallback to series cover
                             val seasonDir = parentDir // S01
                             val seriesDir = seasonDir?.parentFile // Attack on Titan
                             val seriesCover = seriesDir?.let { java.io.File(it, "cover.png") }
                             if (seriesCover?.exists() == true) {
                                 seriesCover
                             } else {
                                 download.thumbnailUrl
                             }
                        }
                    }
                }
                
                if (coverModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverModel)
                            .size(360, 210) // Approx max size for episode/movie thumb
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (download.contentType == ContentType.MOVIE) 
                                Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Progress overlay removed from here
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TitleUtils.cleanTitle(download.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                download.displaySubtitle()?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                
                if ((download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) && download.progress >= 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     androidx.compose.material3.LinearWavyProgressIndicator(
                         progress = { download.progress / 100f },
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(10.dp),
                         trackColor = MaterialTheme.colorScheme.surfaceVariant,
                         color = MaterialTheme.colorScheme.primary,
                         waveSpeed = if (download.status == DownloadStatus.DOWNLOADING) 80.dp else 0.dp,
                         amplitude = { 0.6f } // Force steady wave from 0%
                     )
                     Spacer(modifier = Modifier.height(4.dp))
                } else {
                     Spacer(modifier = Modifier.height(4.dp))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (download.status != DownloadStatus.DOWNLOADING) {
                        StatusChip(status = download.status)
                    }
                    
                    val totalSizeFormatted = download.formatFileSize(download.totalBytes)
                    if (download.status != DownloadStatus.DOWNLOADING && download.totalBytes > 0) {
                        Text(
                            text = totalSizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (download.status == DownloadStatus.DOWNLOADING) {
                        // Spacer removed as StatusChip is hidden
                        // İlerleme ve hız
                        val speedText = download.formatSpeed()
                        // Kalan süre hesapla
                        val remainingBytes = download.totalBytes - download.downloadedBytes
                        val etaText = download.estimatedTimeRemaining()
                        
                        val downloadedFormatted = download.formatFileSize(download.downloadedBytes)
                        
                        Text(
                            text = "${download.progress}% • ${download.formatSpeed()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    }
                    
                    if (download.status == DownloadStatus.COMPLETED && download.totalBytes > 0) {
                        // Moved up to info column
                    }
                }
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Pause, stringResource(R.string.action_pause), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, stringResource(R.string.action_resume), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, stringResource(R.string.action_play), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.action_retry), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> stringResource(R.string.downloads_status_pending) to MaterialTheme.colorScheme.tertiary
        DownloadStatus.DOWNLOADING -> stringResource(R.string.downloads_status_downloading) to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> stringResource(R.string.downloads_status_paused) to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> stringResource(R.string.downloads_status_completed) to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> stringResource(R.string.downloads_status_failed) to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> stringResource(R.string.downloads_status_cancelled) to MaterialTheme.colorScheme.error
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyDownloadsView(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    rotationAngle: Float = 0f,
    onScanClick: () -> Unit = {},
    onFolderSelect: () -> Unit = {}
) {
    // Subtle pulse animation instead of floating
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Use BoxWithConstraints for responsive layout
        BoxWithConstraints(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 500.dp) // Max width for tablets
        ) {
            val isCompact = maxWidth < 360.dp
            val cardFraction = if (isCompact) 1f else 0.9f
            val cardPadding = if (isCompact) 24.dp else 40.dp
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(cardFraction),
                shape = ExpressiveShapes.ExtraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(cardPadding)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Static icon with subtle pulse
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer { 
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(ExpressiveShapes.Full)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder Select Button
                        IconButton(
                            onClick = onFolderSelect,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = stringResource(R.string.action_select_folder),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Scan/Sync Button
                        IconButton(
                            onClick = onScanClick,
                            enabled = !isScanning,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rotationAngle }
                            )
                        }
                    }
                    
                    Text(
                        text = stringResource(R.string.downloads_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = stringResource(R.string.downloads_empty_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hint chips - responsive with FlowRow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.tab_movies),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Surface(
                            shape = ExpressiveShapes.Full,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.tab_series),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentTitle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeniden Adlandır") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("İsim") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != currentTitle
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
