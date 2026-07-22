package com.hasanege.materialtv.ui.screens.profile

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.ProfileViewModel
import com.hasanege.materialtv.R
import com.hasanege.materialtv.SettingsActivity
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.navigation.LocalNavController
import com.hasanege.materialtv.navigation.Screen
import com.hasanege.materialtv.ui.activities.WatchHistoryActivity
import com.hasanege.materialtv.ui.components.EpgBottomSheet
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import com.hasanege.materialtv.HomeViewModel
import com.hasanege.materialtv.ManageCategoriesBottomSheet
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val totalMovies by viewModel.totalMoviesWatched.collectAsStateWithLifecycle()
    val totalSeries by viewModel.totalSeriesWatched.collectAsStateWithLifecycle()
    val totalLive by viewModel.totalLiveWatched.collectAsStateWithLifecycle()
    val customName by viewModel.profileName.collectAsStateWithLifecycle()
    val customImage by viewModel.profileImageUrl.collectAsStateWithLifecycle()
    val totalWatchTimeMs by viewModel.totalWatchTime.collectAsStateWithLifecycle()
    val userLevel by viewModel.userLevel.collectAsStateWithLifecycle()
    val activeInterests by viewModel.activeInterests.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recentLiveStreams by viewModel.recentLiveStreams.collectAsStateWithLifecycle()
    val selectedUpcomingChannels by viewModel.selectedUpcomingChannels.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionRate.collectAsStateWithLifecycle()
    val totalItems by viewModel.totalItemsWatched.collectAsStateWithLifecycle()
    val channelsEpg by viewModel.channelsEpg.collectAsStateWithLifecycle()
    val channelsEpgLoading by viewModel.channelsEpgLoading.collectAsStateWithLifecycle()

    var showChannelSelectionDialog by remember { mutableStateOf(false) }
    var showManageCategoriesBottomSheet by remember { mutableStateOf(false) }
    val homeViewModel: HomeViewModel = hiltViewModel()

    val displayUsername = customName.takeIf { it != "User" && it.isNotBlank() }
        ?: SessionManager.username ?: "User"
    val hoursWatched = totalWatchTimeMs / (1000 * 60 * 60)
    val minutesWatched = totalWatchTimeMs / (1000 * 60)
    val watchTimeDisplay = when {
        hoursWatched >= 24 -> "${hoursWatched / 24}g ${hoursWatched % 24}s"
        hoursWatched >= 1  -> "${hoursWatched}s"
        else               -> "${minutesWatched}dk"
    }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> selectedImageUri = uri }

    if (selectedImageUri != null) {
        com.hasanege.materialtv.ui.components.ImageCropperDialog(
            uri = selectedImageUri!!,
            onDismiss = { selectedImageUri = null },
            onCrop = { bitmap ->
                viewModel.setProfileImageFromBitmap(bitmap)
                selectedImageUri = null
            }
        )
    }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ──────────────────────────────────────────────────────
            // HERO — Avatar + İsim + Rozet (Level Entegreli)
            // ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -it / 4 })
            ) {
                ProfileHeroSection(
                    displayUsername = displayUsername,
                    customImage = customImage,
                    onPickImage = { imagePickerLauncher.launch("image/*") }
                )
            }

            // ──────────────────────────────────────────────────────
            // İSTATİSTİKLER
            // ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 80)) + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "İzleme İstatistikleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = ExpressiveShapes.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.Movie, value = totalMovies.toString(), label = "Film") {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java).putExtra("FILTER_TYPE", "MOVIES"))
                                }
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.Tv, value = totalSeries.toString(), label = "Dizi") {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java).putExtra("FILTER_TYPE", "SERIES"))
                                }
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.LiveTv, value = totalLive.toString(), label = "Canlı") {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java).putExtra("FILTER_TYPE", "LIVE"))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.Schedule, value = watchTimeDisplay, label = "Süre") {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java))
                                }
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, value = "%$completionRate", label = "Tamamlama") {}
                                MinimalStatItem(modifier = Modifier.weight(1f), icon = Icons.Default.PlayCircle, value = totalItems.toString(), label = "Toplam") {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java))
                                }
                            }
                        }
                    }
                }
            }

            // ──────────────────────────────────────────────────────
            // İZLEMEYE DEVAM ET
            // ──────────────────────────────────────────────────────
            if (continueWatching.isNotEmpty()) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(400, delayMillis = 140)) + slideInVertically(initialOffsetY = { it / 4 })
                ) {
                    ProfileSection(
                        title = "İzlemeye Devam Et",
                        icon = Icons.Default.PlayArrow,
                        trailingContent = {
                            TextButton(onClick = {
                                context.startActivity(Intent(context, WatchHistoryActivity::class.java))
                            }) {
                                Text("Geçmiş", style = MaterialTheme.typography.labelLarge)
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(continueWatching.take(6)) { item ->
                                ContinueWatchingCard(item)
                            }
                        }
                    }
                }
            }

            // ──────────────────────────────────────────────────────
            // YAKLAŞAN YAYINLAR
            // ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ProfileSection(
                    title = "Yaklaşan Yayınlar",
                    icon = Icons.Default.LiveTv,
                    trailingContent = {
                        TextButton(onClick = { showChannelSelectionDialog = true }) {
                            Text("Kanal Seç", style = MaterialTheme.typography.labelLarge)
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                ) {
                    val activeChannels = selectedUpcomingChannels.ifEmpty {
                        recentLiveStreams.firstOrNull()?.let { listOf(it) } ?: emptyList()
                    }

                    if (activeChannels.isNotEmpty()) {
                        if (selectedUpcomingChannels.isEmpty()) {
                            Text(
                                "Son izlenen kanal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeChannels.forEach { channel ->
                                val epg = channelsEpg[channel.streamId] ?: emptyList()
                                val isLoading = channelsEpgLoading[channel.streamId] ?: false
                                SelectedChannelEpgCard(
                                    item = channel,
                                    epgList = epg,
                                    isLoading = isLoading
                                )
                            }
                        }
                    } else {
                        UpcomingEmptyState(
                            onSelectChannel = { showChannelSelectionDialog = true }
                        )
                    }
                }
            }



            // ──────────────────────────────────────────────────────
            // HIZLI EYLEMLER
            // ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 320)) + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ProfileSection(title = "Hızlı Erişim", icon = Icons.Default.Apps) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            ProfileActionItem(
                                icon = Icons.Default.History,
                                title = "İzleme Geçmişi",
                                subtitle = "Tüm geçmişi görüntüle ve yönet",
                                onClick = {
                                    context.startActivity(Intent(context, WatchHistoryActivity::class.java))
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ProfileActionItem(
                                icon = Icons.Default.Settings,
                                title = "Ayarlar",
                                subtitle = "Uygulama tercihlerini düzenle",
                                onClick = {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // KANAL SEÇİM DİYALOĞU
    // ──────────────────────────────────────────────────────────────
    if (showChannelSelectionDialog) {
        ChannelSelectionDialog(
            liveStreams = liveStreams,
            selectedChannels = selectedUpcomingChannels,
            onDismiss = { showChannelSelectionDialog = false },
            onSave = { channels ->
                viewModel.setSelectedUpcomingChannels(channels)
                showChannelSelectionDialog = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// HERO SECTION — Avatar + Kullanıcı Adı + Rozet (Level/XP Entegreli)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeroSection(
    displayUsername: String,
    customImage: String,
    onPickImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (customImage.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(customImage)
                        .crossfade(true)
                        .build(),
                    imageLoader = com.hasanege.materialtv.ui.utils.ImageConfig.getImageLoader(LocalContext.current),
                    contentDescription = "Profil Fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = displayUsername.take(1).uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Edit icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.BottomEnd),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.background)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Fotoğraf Değiştir",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // İsim
        Text(
            text = displayUsername,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// BÖLÜM SARMALAYICI — Daha şık ve hafif
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileSection(
    title: String,
    icon: ImageVector,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            trailingContent?.invoke()
        }
        content()
    }
}

// ════════════════════════════════════════════════════════════════════
// İSTATİSTİK KARTI (Minimalist)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun MinimalStatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// İZLEMEYE DEVAM ET KARTI
// ════════════════════════════════════════════════════════════════════

@Composable
fun ContinueWatchingCard(item: ContinueWatchingItem) {
    val context = LocalContext.current
    val progress = if (item.duration > 0)
        (item.position.toFloat() / item.duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    val intent = Intent(context, PlayerActivity::class.java).apply {
        putExtra("AUTO_PLAY", true)
        putExtra("STREAM_ID", item.streamId)
        if (item.type == "series") {
            putExtra("SERIES_ID", item.seriesId)
            putExtra("EPISODE_ID", item.streamId.toString())
        }
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable { context.startActivity(intent) }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .width(148.dp)
                .height(210.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.streamIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Progress bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val remainingMin = if (item.duration > 0) ((item.duration - item.position) / 60000) else 0
        val typeLabel = if (item.type == "series") "Bölüm" else "Film"
        Text(
            "$typeLabel • ${remainingMin}dk kaldı",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// YAKIN YAYIN KARTI
// ════════════════════════════════════════════════════════════════════

@Composable
fun SelectedChannelEpgCard(
    item: ContinueWatchingItem,
    epgList: List<com.hasanege.materialtv.model.EpgListing>,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val serverUrl = SessionManager.serverUrl ?: ""
    val username = SessionManager.username ?: ""
    val password = SessionManager.password ?: ""
    val liveUrl = "$serverUrl/live/$username/$password/${item.streamId}.m3u8"

    var showEpgSheet by remember { mutableStateOf(false) }

    Card(
        onClick = {
            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                putExtra("LIVE_URL", liveUrl)
                putExtra("LIVE_STREAM_ID", item.streamId)
                putExtra("LIVE_STREAM_NAME", item.name)
            })
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 1. Channel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.streamIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = ExpressiveShapes.Full
                    ) {
                        Text(
                            text = "● CANLI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { showEpgSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "EPG",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 2. EPG content — Zaman Çizelgesi
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (epgList.isNotEmpty()) {
                val now = java.util.Date()
                val currentIdx = epgList.indexOfFirst { epg ->
                    val s = parseEpgDate(epg.start)
                    val e = parseEpgDate(epg.end)
                    s != null && e != null && now.after(s) && now.before(e)
                }
                val upcomingEpg = epgList.filter { epg ->
                    val e = parseEpgDate(epg.end)
                    e != null && e.after(now) || (currentIdx >= 0 && epg == epgList[currentIdx])
                }.take(6)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    upcomingEpg.forEachIndexed { index, epg ->
                        val isCurrent = epg == epgList.getOrNull(currentIdx)
                        val startD = parseEpgDate(epg.start)
                        val endD = parseEpgDate(epg.end)
                        val total = if (startD != null && endD != null) endD.time - startD.time else 0L
                        val elapsed = if (startD != null && isCurrent) (now.time - startD.time).coerceAtLeast(0L) else 0L
                        val progress = if (total > 0 && isCurrent) (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                        Surface(
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            shape = ExpressiveShapes.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                   verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "ŞU AN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        if (startD != null) {
                                            Text(
                                                formatEpgTime(startD),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (startD != null && endD != null) {
                                        Text(
                                            "${formatEpgTime(startD)} – ${formatEpgTime(endD)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = epg.title ?: "Bilinmeyen Program",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!epg.description.isNullOrEmpty() && isCurrent) {
                                    Text(
                                        text = epg.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isCurrent && total > 0) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                    shape = ExpressiveShapes.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Yayın akışı bilgisi bulunamadı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEpgSheet) {
        EpgBottomSheet(
            streamId = item.streamId,
            onDismissRequest = { showEpgSheet = false }
        )
    }
}

private fun parseEpgDate(dateStr: String?): java.util.Date? {
    if (dateStr.isNullOrBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (format in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            return sdf.parse(dateStr)
        } catch (e: Exception) {
            // try next
        }
    }
    return null
}

private fun formatEpgTime(date: java.util.Date?): String {
    if (date == null) return ""
    return try {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    } catch (e: Exception) {
        ""
    }
}

// ════════════════════════════════════════════════════════════════════
// BOŞ DURUM — Kanal seçilmemiş
// ════════════════════════════════════════════════════════════════════

@Composable
private fun UpcomingEmptyState(onSelectChannel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Tv,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Text(
            "Kanal Seçilmedi",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Yaklaşan yayınları takip etmek için bir kanal seçin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        FilledTonalButton(
            onClick = onSelectChannel,
            shape = ExpressiveShapes.Small
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Kanal Seç")
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// İLGİ ALANI ÇİPİ
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileInterestChip(text: String, isSelected: Boolean = false) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ) else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// AKSİYON ÖĞESİ — Modern ve Temiz Liste Elemanı
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// KANAL SEÇİM DİYALOĞU
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelSelectionDialog(
    liveStreams: List<com.hasanege.materialtv.model.LiveStream>,
    selectedChannels: List<ContinueWatchingItem>,
    onDismiss: () -> Unit,
    onSave: (List<ContinueWatchingItem>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedIds by remember(selectedChannels) {
        mutableStateOf(selectedChannels.map { it.streamId }.toSet())
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = ExpressiveShapes.ExtraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Kanal Seç",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Kanal Ara...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = ExpressiveShapes.Medium,
                    singleLine = true
                )

                val filtered = liveStreams.filter {
                    it.name?.contains(searchQuery, ignoreCase = true) == true
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Eşleşen kanal bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filtered) { channel ->
                            val isSelected = tempSelectedIds.contains(channel.streamId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ExpressiveShapes.Medium)
                                    .clickable {
                                        tempSelectedIds = if (isSelected) {
                                            tempSelectedIds - (channel.streamId ?: 0)
                                        } else {
                                            tempSelectedIds + (channel.streamId ?: 0)
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = channel.streamIcon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )
                                Text(
                                    text = channel.name ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        tempSelectedIds = if (checked) {
                                            tempSelectedIds + (channel.streamId ?: 0)
                                        } else {
                                            tempSelectedIds - (channel.streamId ?: 0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalSelected = liveStreams.filter { tempSelectedIds.contains(it.streamId) }
                                .map {
                                    ContinueWatchingItem(
                                        streamId = it.streamId ?: 0,
                                        name = it.name ?: "",
                                        streamIcon = it.streamIcon ?: "",
                                        type = "live",
                                        position = 0L,
                                        duration = 0L,
                                        actualWatchTime = 0L
                                    )
                                }
                            onSave(finalSelected)
                        },
                        shape = ExpressiveShapes.Small
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// FlowRow WRAPPER
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
