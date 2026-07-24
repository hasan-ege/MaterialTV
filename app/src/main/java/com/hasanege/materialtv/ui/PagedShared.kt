package com.hasanege.materialtv.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.basicMarquee
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.FavoritesManager
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.R
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.animateStaggeredEntry
import com.hasanege.materialtv.ui.utils.ImageConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PagedMoviesList(movies: LazyPagingItems<VodItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            count = movies.itemCount,
            key = movies.itemKey { it.streamId ?: it.hashCode() },
            contentType = movies.itemContentType { "movie_card" }
        ) { index ->
            val movie = movies[index]
            if (movie != null) {
                val navController = com.hasanege.materialtv.navigation.LocalNavController.current
                val interactionSource = remember(movie.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "scale"
                )

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateStaggeredEntry(index)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                navController.navigate(com.hasanege.materialtv.navigation.Screen.Detail.createRoute(movie.streamId ?: 0, movie.name ?: ""))
                            },
                            onLongClick = {
                                scope.launch {
                                    val added = FavoritesManager.toggleFavorite(
                                        contentId = movie.streamId ?: 0,
                                        contentType = "movie",
                                        name = movie.name ?: "",
                                        thumbnailUrl = movie.streamIcon,
                                        year = movie.year,
                                        categoryId = movie.categoryId
                                    )
                                    Toast.makeText(
                                        context,
                                        if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ),
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(movie.streamIcon)
                                .crossfade(true)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = movie.name ?: "",
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_placeholder),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(2f / 3f)
                                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                                .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = movie.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            movie.rating5Based?.let { rating ->
                                if (rating > 0) {
                                    val displayRating = "%.1f".format(rating)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = " $displayRating",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            movie.year?.let { year ->
                                 Text(
                                    text = year,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PagedSeriesList(series: LazyPagingItems<SeriesItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            count = series.itemCount,
            key = series.itemKey { it.seriesId ?: it.hashCode() },
            contentType = series.itemContentType { "series_card" }
        ) { index ->
            val seriesItem = series[index]
            if (seriesItem != null) {
                val navController = com.hasanege.materialtv.navigation.LocalNavController.current
                val interactionSource = remember(seriesItem.seriesId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "scale"
                )

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateStaggeredEntry(index)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                navController.navigate(com.hasanege.materialtv.navigation.Screen.SeriesDetail.createRoute(seriesItem.seriesId ?: 0, seriesItem.name ?: ""))
                            },
                            onLongClick = {
                                scope.launch {
                                    val added = FavoritesManager.toggleFavorite(
                                        contentId = seriesItem.seriesId ?: 0,
                                        contentType = "series",
                                        name = seriesItem.name ?: "",
                                        thumbnailUrl = seriesItem.cover,
                                        genre = seriesItem.genre,
                                        year = seriesItem.year,
                                        categoryId = seriesItem.categoryId,
                                        seriesId = seriesItem.seriesId,
                                        streamIcon = seriesItem.cover
                                    )
                                    Toast.makeText(
                                        context,
                                        if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ),
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(seriesItem.cover)
                                .crossfade(true)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = seriesItem.name ?: "",
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_placeholder),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(2f / 3f)
                                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                                .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = seriesItem.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!seriesItem.releaseDate.isNullOrEmpty()) {
                                Text(
                                    text = seriesItem.releaseDate ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (!seriesItem.plot.isNullOrEmpty()) {
                                Text(
                                    text = seriesItem.plot ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PagedLiveTVList(liveStreams: LazyPagingItems<LiveStream>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = com.hasanege.materialtv.navigation.LocalNavController.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        items(
            count = liveStreams.itemCount,
            key = liveStreams.itemKey { it.streamId ?: it.hashCode() },
            contentType = liveStreams.itemContentType { "live_card" }
        ) { index ->
            val liveStream = liveStreams[index]
            if (liveStream != null) {
                val interactionSource = remember(liveStream.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "scale"
                )

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateStaggeredEntry(index)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                val streamId = liveStream.streamId ?: return@combinedClickable
                                navController.navigate(
                                    com.hasanege.materialtv.navigation.Screen.LiveDetail.createRoute(
                                        streamId = streamId,
                                        channelName = liveStream.name ?: "",
                                        streamIcon = liveStream.streamIcon
                                    )
                                )
                            },
                            onLongClick = {
                                scope.launch {
                                    val added = FavoritesManager.toggleFavorite(
                                        contentId = liveStream.streamId ?: 0,
                                        contentType = "live",
                                        name = liveStream.name ?: "",
                                        thumbnailUrl = liveStream.streamIcon,
                                        categoryId = liveStream.categoryId,
                                        streamIcon = liveStream.streamIcon
                                    )
                                    Toast.makeText(
                                        context,
                                        if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ),
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(liveStream.streamIcon)
                                .crossfade(true)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(context),
                            contentDescription = liveStream.name ?: "",
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_placeholder),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                                .shadow(2.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                        )
                        Text(
                            text = liveStream.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .weight(1f)
                                .basicMarquee(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
