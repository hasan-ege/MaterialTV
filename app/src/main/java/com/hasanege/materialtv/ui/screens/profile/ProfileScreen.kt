package com.hasanege.materialtv.ui.screens.profile

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.HistoryActivity
import com.hasanege.materialtv.MainActivity
import com.hasanege.materialtv.ProfileViewModel
import com.hasanege.materialtv.R
import com.hasanege.materialtv.SettingsActivity
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.ExpressiveShapes

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // State collection
    val totalWatchTime by viewModel.totalWatchTime.collectAsStateWithLifecycle()
    val totalMovies by viewModel.totalMoviesWatched.collectAsStateWithLifecycle()
    val totalSeries by viewModel.totalSeriesWatched.collectAsStateWithLifecycle()
    val totalLive by viewModel.totalLiveWatched.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionRate.collectAsStateWithLifecycle()
    val userLevel by viewModel.userLevel.collectAsStateWithLifecycle()
    val customName by viewModel.profileName.collectAsStateWithLifecycle()
    val customImage by viewModel.profileImageUrl.collectAsStateWithLifecycle()
    val avgTimePerItem by viewModel.averageWatchTimePerItem.collectAsStateWithLifecycle()
    val movieRatio by viewModel.movieRatio.collectAsStateWithLifecycle()
    val seriesRatio by viewModel.seriesRatio.collectAsStateWithLifecycle()
    val liveRatio by viewModel.liveRatio.collectAsStateWithLifecycle()
    val unfinished by viewModel.unfinishedItemsCount.collectAsStateWithLifecycle()

    val displayUsername = customName.takeIf { it != "User" && it.isNotBlank() }
        ?: SessionManager.username ?: "User"

    // Responsive grid logic
    val columns = when {
        screenWidth < 600.dp -> 2
        screenWidth < 900.dp -> 4
        else -> 6
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════
            // 1. PROFILE HEADER CARD (Adaptive Layout)
            // ═══════════════════════════════════════════════════════════════
            item(span = { GridItemSpan(columns) }) {
                FlatProfileHeader(
                    displayUsername = displayUsername,
                    userLevel = userLevel,
                    customImage = customImage
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // 2. LIFETIME WATCH TIME CARD (Highlight)
            // ═══════════════════════════════════════════════════════════════
            item(span = { GridItemSpan(columns) }) {
                WatchTimeCard(totalWatchTime)
            }

            // ═══════════════════════════════════════════════════════════════
            // 3. STATS TILES (Bento Style but Flat)
            // ═══════════════════════════════════════════════════════════════
            
            // Movies Stat
            item {
                FlatStatCard(
                    label = stringResource(R.string.profile_stats_movies),
                    value = totalMovies.toString(),
                    icon = Icons.Rounded.Movie,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        context.startActivity(Intent(context, HistoryActivity::class.java).apply {
                            putExtra("FILTER_TYPE", "MOVIES")
                        })
                    }
                )
            }

            // Series Stat
            item {
                FlatStatCard(
                    label = stringResource(R.string.profile_stats_series),
                    value = totalSeries.toString(),
                    icon = Icons.Rounded.Tv,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        context.startActivity(Intent(context, HistoryActivity::class.java).apply {
                            putExtra("FILTER_TYPE", "SERIES")
                        })
                    }
                )
            }

            // Live TV Stat (Responsive: visible even on small screens)
            item {
                FlatStatCard(
                    label = stringResource(R.string.tab_live_tv),
                    value = totalLive.toString(),
                    icon = Icons.Rounded.LiveTv,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        context.startActivity(Intent(context, HistoryActivity::class.java).apply {
                            putExtra("FILTER_TYPE", "LIVE")
                        })
                    }
                )
            }

            // Active Stream Stat
            item {
                FlatStatCard(
                    label = "Active",
                    value = unfinished.toString(),
                    icon = Icons.Rounded.PlayCircle,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // 4. ANALYTICS SECTION
            // ═══════════════════════════════════════════════════════════════
            val insightSpan = if (columns > 2) columns / 2 else columns
            
            // Average Session Info
            item(span = { GridItemSpan(insightSpan) }) {
                InsightFlatCard(
                    title = "Avg. Session",
                    value = avgTimePerItem,
                    icon = Icons.Rounded.Schedule
                )
            }

            // Completion Rate Info
            item(span = { GridItemSpan(insightSpan) }) {
                InsightFlatCard(
                    title = "Completion",
                    value = "$completionRate%",
                    icon = Icons.Rounded.CheckCircle
                )
            }

            // Distribution Table (Flat Bar)
            item(span = { GridItemSpan(columns) }) {
                ContentMixBar(
                    movieRatio = movieRatio,
                    seriesRatio = seriesRatio,
                    liveRatio = liveRatio
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // 5. BOTTOM ACTIONS SECTION
            // ═══════════════════════════════════════════════════════════════
            item(span = { GridItemSpan(columns) }) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveShapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Settings", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.logout()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveShapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 6. BOTTOM SPACER (To prevent overlap with floating navigation)
            // ═══════════════════════════════════════════════════════════════
            item(span = { GridItemSpan(columns) }) {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENT: FLAT PROFILE HEADER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FlatProfileHeader(
    displayUsername: String,
    userLevel: String,
    customImage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.ExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar + User Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(screenWidthBasedAvatarSize())
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customImage.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(customImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayUsername.take(1).uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = displayUsername,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = userLevel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENT: WATCH TIME CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun WatchTimeCard(totalMs: Long) {
    val hours = totalMs / (1000 * 60 * 60)
    val minutes = (totalMs / (1000 * 60)) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Column {
                Text(
                    "TOTAL WATCH TIME",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENT: FLAT STAT CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FlatStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENT: INSIGHT FLAT CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun InsightFlatCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENT: CONTENT MIX BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ContentMixBar(movieRatio: Int, seriesRatio: Int, liveRatio: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "CONTENT DISTRIBUTION",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (movieRatio > 0) Box(Modifier.weight(movieRatio.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            if (seriesRatio > 0) Box(Modifier.weight(seriesRatio.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
            if (liveRatio > 0) Box(Modifier.weight(liveRatio.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem("Movies", MaterialTheme.colorScheme.primary)
            LegendItem("Series", MaterialTheme.colorScheme.secondary)
            LegendItem("Live TV", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun screenWidthBasedAvatarSize() = when (LocalConfiguration.current.screenWidthDp) {
    in 0..600 -> 80.dp
    else -> 100.dp
}
