@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.hasanege.materialtv

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.model.EpgListing
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import com.hasanege.materialtv.ui.utils.ImageConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// -------------------------------------------------------------------
// Route wrapper (called from navigation)
// -------------------------------------------------------------------
@Composable
fun LiveDetailScreenRoute(
    streamId: Int,
    channelName: String,
    streamIcon: String?,
    onBack: () -> Unit,
    epgViewModel: EpgViewModel = hiltViewModel()
) {
    val epgUiState by epgViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(streamId) {
        epgViewModel.fetchEpg(streamId)
    }

    LiveDetailScreen(
        streamId = streamId,
        channelName = channelName,
        streamIcon = streamIcon,
        epgUiState = epgUiState,
        onBack = onBack
    )
}

// -------------------------------------------------------------------
// Main Screen
// -------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDetailScreen(
    streamId: Int,
    channelName: String,
    streamIcon: String?,
    epgUiState: EpgUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val safeTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var isFavorite by remember { mutableStateOf(false) }
    val addedToFavoritesMsg = stringResource(R.string.favorites_added)
    val removedFromFavoritesMsg = stringResource(R.string.favorites_removed)

    LaunchedEffect(streamId) {
        isFavorite = FavoritesManager.isFavorite(streamId, "live")
    }

    // Resolve stream URL
    val streamUrl = remember(streamId) {
        if (SessionManager.loginType == SessionManager.LoginType.M3U) {
            com.hasanege.materialtv.data.M3uRepository.getStreamUrl(streamId)
        } else {
            "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/$streamId.ts"
        }
    }

    // Current/Next programme from EPG
    val currentProgram by remember(epgUiState) {
        derivedStateOf {
            if (epgUiState is EpgUiState.Success) {
                epgUiState.epgList.firstOrNull { epg ->
                    epg.start != null && epg.end != null && isCurrentlyAiring(epg.start, epg.end)
                } ?: epgUiState.epgList.firstOrNull()
            } else null
        }
    }
    val progress by remember(currentProgram) {
        derivedStateOf { currentProgram?.let { calculateProgress(it.start, it.end) } ?: 0f }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            WideScreenLiveDetail(
                streamId = streamId,
                channelName = channelName,
                streamIcon = streamIcon,
                streamUrl = streamUrl,
                epgUiState = epgUiState,
                currentProgram = currentProgram,
                progress = progress,
                isFavorite = isFavorite,
                safeTopPadding = safeTopPadding,
                onBack = onBack,
                onFavoriteToggle = {
                    scope.launch {
                        val wasAdded = FavoritesManager.toggleFavorite(
                            contentId = streamId,
                            contentType = "live",
                            name = channelName,
                            thumbnailUrl = streamIcon,
                            streamIcon = streamIcon
                        )
                        isFavorite = wasAdded
                        snackbarHostState.showSnackbar(if (wasAdded) addedToFavoritesMsg else removedFromFavoritesMsg)
                    }
                },
                onPlay = {
                    if (!streamUrl.isNullOrEmpty()) {
                        val intent = Intent(context, PlayerActivity::class.java).apply {
                            putExtra("url", streamUrl)
                            putExtra("TITLE", channelName)
                            putExtra("LIVE_STREAM_ID", streamId)
                            putExtra("STREAM_ICON", streamIcon)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        } else {
            PhoneScreenLiveDetail(
                streamId = streamId,
                channelName = channelName,
                streamIcon = streamIcon,
                streamUrl = streamUrl,
                epgUiState = epgUiState,
                currentProgram = currentProgram,
                progress = progress,
                isFavorite = isFavorite,
                safeTopPadding = safeTopPadding,
                onBack = onBack,
                onFavoriteToggle = {
                    scope.launch {
                        val wasAdded = FavoritesManager.toggleFavorite(
                            contentId = streamId,
                            contentType = "live",
                            name = channelName,
                            thumbnailUrl = streamIcon,
                            streamIcon = streamIcon
                        )
                        isFavorite = wasAdded
                        snackbarHostState.showSnackbar(if (wasAdded) addedToFavoritesMsg else removedFromFavoritesMsg)
                    }
                },
                onPlay = {
                    if (!streamUrl.isNullOrEmpty()) {
                        val intent = Intent(context, PlayerActivity::class.java).apply {
                            putExtra("url", streamUrl)
                            putExtra("TITLE", channelName)
                            putExtra("LIVE_STREAM_ID", streamId)
                            putExtra("STREAM_ICON", streamIcon)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// -------------------------------------------------------------------
// Wide / Tablet layout
// -------------------------------------------------------------------
@Composable
private fun WideScreenLiveDetail(
    streamId: Int,
    channelName: String,
    streamIcon: String?,
    streamUrl: String?,
    epgUiState: EpgUiState,
    currentProgram: EpgListing?,
    progress: Float,
    isFavorite: Boolean,
    safeTopPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxSize()) {
        // ---- Left panel (45%) ----
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .padding(top = safeTopPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Channel logo (1:1)
                Surface(
                    shape = ExpressiveShapes.Medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(240.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(streamIcon)
                            .crossfade(true)
                            .build(),
                        imageLoader = ImageConfig.getImageLoader(context),
                        contentDescription = channelName,
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.padding(16.dp).fillMaxSize()
                    )
                }

                Spacer(Modifier.height(32.dp))

                Spacer(Modifier.height(8.dp))

                // Channel name
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(32.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = ExpressiveShapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.action_play),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    FilledTonalIconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(64.dp),
                        shape = ExpressiveShapes.Medium,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(if (isFavorite) R.string.favorites_remove else R.string.favorites_add),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Back button
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }

        // ---- Right panel (55%): EPG schedule ----
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .padding(top = safeTopPadding)
        ) {
            EpgScheduleSection(epgUiState = epgUiState)
        }
    }
}

// -------------------------------------------------------------------
// Phone layout
// -------------------------------------------------------------------
@Composable
private fun PhoneScreenLiveDetail(
    streamId: Int,
    channelName: String,
    streamIcon: String?,
    streamUrl: String?,
    epgUiState: EpgUiState,
    currentProgram: EpgListing?,
    progress: Float,
    isFavorite: Boolean,
    safeTopPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Parallax-like alpha for the back button overlay
    val headerAlpha by remember {
        derivedStateOf { (scrollState.value / 300f).coerceIn(0f, 1f) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(safeTopPadding + 24.dp))

            // --- Hero Section (1:1 Prominent Square) ---
            Surface(
                shape = ExpressiveShapes.Medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(streamIcon)
                        .crossfade(true)
                        .build(),
                    imageLoader = ImageConfig.getImageLoader(context),
                    contentDescription = channelName,
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.ic_placeholder),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    modifier = Modifier.padding(16.dp).fillMaxSize()
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- Channel Name ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(24.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = ExpressiveShapes.Medium
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.action_play),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.width(12.dp))

                FilledTonalIconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(56.dp),
                    shape = ExpressiveShapes.Medium,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // --- Content below hero ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Current program
                currentProgram?.let { program ->
                    CurrentProgramCard(program = program, progress = progress, isCompact = false)
                    Spacer(Modifier.height(24.dp))
                }

                // EPG section
                EpgScheduleSection(epgUiState = epgUiState)

                Spacer(Modifier.height(32.dp))
            }
        }

        // --- Sticky back button ---
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 12.dp + safeTopPadding, start = 16.dp)
                .size(44.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
    }
}

// -------------------------------------------------------------------
// LIVE badge
// -------------------------------------------------------------------
@Composable
private fun LiveBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// -------------------------------------------------------------------
// Current programme card with progress bar
// -------------------------------------------------------------------
@Composable
private fun CurrentProgramCard(
    program: EpgListing,
    progress: Float,
    isCompact: Boolean
) {
    val cardContainerColor = if (isCompact)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Şu An Yayında",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = program.title ?: "Program",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )



            Spacer(Modifier.height(12.dp))

            // Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${formatEpgTime(program.start)} – ${formatEpgTime(program.end)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Percent remaining text
                val pct = (progress * 100).toInt()
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

// -------------------------------------------------------------------
// EPG schedule list section
// -------------------------------------------------------------------
@Composable
private fun EpgScheduleSection(epgUiState: EpgUiState) {
    // Header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Today,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Yayın Akışı",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    AnimatedContent(
        targetState = epgUiState,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "epg_content"
    ) { state ->
        when (state) {
            is EpgUiState.Initial, is EpgUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is EpgUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Yayın akışı bilgisi bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is EpgUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is EpgUiState.Success -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    state.epgList.forEachIndexed { index, epg ->
                        EpgRow(
                            epg = epg,
                            isFirst = index == 0,
                            isLast = index == state.epgList.lastIndex
                        )
                        if (index < state.epgList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// Single EPG row
// -------------------------------------------------------------------
@Composable
private fun EpgRow(epg: EpgListing, isFirst: Boolean, isLast: Boolean) {
    val isCurrentlyOn = epg.start != null && epg.end != null &&
            isCurrentlyAiring(epg.start, epg.end)

    val shape = when {
        isFirst && isLast -> ExpressiveShapes.Large
        isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        shape = shape,
        color = if (isCurrentlyOn)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(72.dp)
            ) {
                Text(
                    text = formatEpgTime(epg.start),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isCurrentlyOn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatEpgTime(epg.end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Vertical divider accent
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentlyOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )

            // Programme title + description
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = epg.title ?: "Program",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentlyOn) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                        ) {
                            Text(
                                text = "CANLI",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}

// -------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------
private val epgSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).also {
    it.timeZone = TimeZone.getDefault()
}
private val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun parseEpgDate(raw: String?): Date? = raw?.let {
    try { epgSdf.parse(it) } catch (e: Exception) { null }
}

fun formatEpgTime(raw: String?): String = raw?.let {
    parseEpgDate(it)?.let { d -> timeSdf.format(d) }
} ?: "--:--"

fun isCurrentlyAiring(start: String?, end: String?): Boolean {
    val s = parseEpgDate(start) ?: return false
    val e = parseEpgDate(end) ?: return false
    val now = Date()
    return now.after(s) && now.before(e)
}

fun calculateProgress(start: String?, end: String?): Float {
    val s = parseEpgDate(start)?.time ?: return 0f
    val e = parseEpgDate(end)?.time ?: return 0f
    val now = System.currentTimeMillis()
    if (now < s || e <= s) return 0f
    return ((now - s).toFloat() / (e - s).toFloat()).coerceIn(0f, 1f)
}
