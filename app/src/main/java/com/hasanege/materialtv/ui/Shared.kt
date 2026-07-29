package com.hasanege.materialtv.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
 
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.hasanege.materialtv.ui.theme.animateStaggeredEntry
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ripple
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.basicMarquee
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.FavoritesManager
import com.hasanege.materialtv.MainScreen
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.R
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.utils.ImageConfig
import kotlinx.coroutines.launch


// M3 Expressive Pill-Style Tab Slider with Animated Indicator - Floating Island Style
@Composable

fun ExpressiveTabSlider(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabLongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Detect narrow screen (<360dp)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 480
    
    // Track actual tab item positions and sizes (measured from the Box containing the text)
    var tabItemBounds by remember(tabs) { 
        mutableStateOf(List(tabs.size) { androidx.compose.ui.geometry.Rect.Zero }) 
    }
    
    // Get the selected tab bounds
    val selectedBounds = tabItemBounds.getOrNull(selectedIndex) ?: androidx.compose.ui.geometry.Rect.Zero
    
    // Animated indicator offset (X position relative to Row)
    val indicatorOffsetX by androidx.compose.animation.core.animateFloatAsState(
        targetValue = selectedBounds.left,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_offset_x"
    )
    
    // Animated indicator width
    val indicatorWidth by androidx.compose.animation.core.animateFloatAsState(
        targetValue = selectedBounds.width,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_width"
    )
    
    // Animated indicator height (use max height for consistency)
    val maxHeight = tabItemBounds.maxOfOrNull { it.height } ?: 0f
    val indicatorHeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = maxHeight,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "indicator_height"
    )
    
    // Outer container
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner floating pill
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 4.dp,
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                    ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
                )
                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge
                )
                .height(56.dp)
                .padding(4.dp)
        ) {
            // Scrollable container for tabs
            val scrollState = androidx.compose.foundation.rememberScrollState()
            
            // Auto-scroll to selected tab when it changes
            if (scrollable) {
                androidx.compose.runtime.LaunchedEffect(selectedIndex, tabItemBounds) {
                    if (selectedBounds != androidx.compose.ui.geometry.Rect.Zero) {
                        val scrollTarget = when (selectedIndex) {
                            0 -> 0
                            tabs.size - 1 -> scrollState.maxValue
                            else -> {
                                (selectedBounds.left - (scrollState.maxValue - selectedBounds.width) / 2)
                                    .toInt().coerceIn(0, scrollState.maxValue)
                            }
                        }
                        
                        scrollState.animateScrollTo(
                            value = scrollTarget,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
            
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                // Animated sliding indicator behind tabs
                if (indicatorWidth > 0f && indicatorHeight > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = indicatorOffsetX - scrollState.value
                            }
                            .size(
                                width = with(density) { indicatorWidth.toDp() },
                                height = with(density) { indicatorHeight.toDp() }
                            )
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
                
                // Horizontal scrollable row
                Row(
                    modifier = if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedIndex == index
                        
                        Box(
                            modifier = Modifier
                                .then(if (scrollable) Modifier else Modifier.weight(1f))
                                .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge)
                                .onGloballyPositioned { coordinates ->
                                    // Measure the entire Box bounds (including padding), not just the text
                                    val position = coordinates.positionInParent()
                                    val size = coordinates.size
                                    val newBounds = androidx.compose.ui.geometry.Rect(
                                        left = position.x,
                                        top = position.y,
                                        right = position.x + size.width.toFloat(),
                                        bottom = position.y + size.height.toFloat()
                                    )
                                    if (tabItemBounds.getOrNull(index) != newBounds) {
                                        tabItemBounds = tabItemBounds.toMutableList().also {
                                            if (it.size > index) {
                                                it[index] = newBounds
                                            }
                                        }
                                    }
                                }
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onTabSelected(index)
                                    },
                                    onLongClick = if (onTabLongClick != null) {
                                        {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onTabLongClick(index)
                                        }
                                    } else null
                                )
                                .padding(horizontal = if (isNarrow) 8.dp else 16.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = if (!scrollable) {
                                    if (isNarrow) {
                                        if (title.length > 8) 10.sp else 11.sp
                                    } else {
                                        if (title.length > 10) 11.sp else 13.sp
                                    }
                                } else 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultBottomNavBar(items: List<MainScreen>, currentItemRoute: String, onItemClick: (MainScreen) -> Unit, modifier: Modifier = Modifier, onlyIcons: Boolean = false) {
    androidx.compose.material3.NavigationBar(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        for (screen in items) {
            NavigationBarItem(
                selected = currentItemRoute == screen.route,
                onClick = { onItemClick(screen) },
                icon = { androidx.compose.material3.Icon(screen.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                label = if (onlyIcons) null else {
                    {
                        androidx.compose.material3.Text(
                            text = androidx.compose.ui.res.stringResource(screen.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        ) 
                    }
                }
            )
        }
    }
}

@Composable
fun MaterialTVBottomNavBar(items: List<MainScreen>, currentItemRoute: String, onItemClick: (MainScreen) -> Unit, modifier: Modifier = Modifier, onlyIcons: Boolean = false) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(6.dp, ExpressiveShapes.Full,
                    ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f),
                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f))
                .clip(ExpressiveShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .height(64.dp)
                .padding(horizontal = if (onlyIcons) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (onlyIcons) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentItemRoute == screen.route
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                
                val iconScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "icon_scale"
                )
                
                if (onlyIcons) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onItemClick(screen)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = stringResource(screen.labelRes),
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(ExpressiveShapes.Full)
                            .clickable { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onItemClick(screen) 
                            }
                            .padding(horizontal = if (isNarrow) 8.dp else 12.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(24.dp)
                                .clip(ExpressiveShapes.Full)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else androidx.compose.ui.graphics.Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.labelRes),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(screen.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvNavigationRail(
    items: List<MainScreen>,
    currentItemRoute: String,
    onItemClick: (MainScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        for (screen in items) {
            val isSelected = currentItemRoute == screen.route
            var isFocused by remember { mutableStateOf(false) }
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFocused) 1.2f else 1.0f,
                label = "tv_rail_scale"
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ExpressiveShapes.Full)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isFocused -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        shape = ExpressiveShapes.Full
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onItemClick(screen) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = stringResource(screen.labelRes),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
    }
}



@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CenteredProgressBar() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularWavyProgressIndicator()
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}

@Composable
fun NoResultsFound(
    title: String = stringResource(R.string.search_no_results),
    subtitle: String = stringResource(R.string.search_try_different)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MoviesList(movies: List<VodItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        itemsIndexed(
            items = movies,
            key = { index, movie -> movie.streamId ?: movie.hashCode() },
            contentType = { index, movie -> "movie_card" }
        ) { index, movie ->
            val navController = com.hasanege.materialtv.navigation.LocalNavController.current
            val interactionSource = remember(movie.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
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
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
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
                            fontWeight = FontWeight.ExtraBold,
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SeriesList(series: List<SeriesItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        itemsIndexed(
            items = series,
            key = { index, seriesItem -> seriesItem.seriesId ?: seriesItem.hashCode() },
            contentType = { index, seriesItem -> "series_card" }
        ) { index, seriesItem ->
            val navController = com.hasanege.materialtv.navigation.LocalNavController.current
            val interactionSource = remember(seriesItem.seriesId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
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
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LiveTVList(liveStreams: List<LiveStream>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = com.hasanege.materialtv.navigation.LocalNavController.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        flingBehavior = ScrollableDefaults.flingBehavior()
    ) {
        itemsIndexed(
            items = liveStreams,
            key = { index, liveStream -> liveStream.streamId ?: liveStream.hashCode() },
            contentType = { index, liveStream -> "live_card" }
        ) { index, liveStream ->
            val interactionSource = remember(liveStream.streamId) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            // Spring physics animation like bottom nav
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "scale"
            )

            androidx.compose.material3.ElevatedCard(
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
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
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
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.ic_placeholder),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(1f)
                            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                            .shadow(4.dp, com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = liveStream.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_live_tv),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoConnectionScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.error_no_connection),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Persistent Expressive Mini-Player Floating Bar (Matching Reference Screenshots)
@Composable
fun PersistentFloatingMiniPlayer(
    title: String,
    subtitle: String,
    imageUrl: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 12.dp,
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Large,
                ambientColor = androidx.compose.ui.graphics.Color(0xFF5C243E).copy(alpha = 0.6f),
                spotColor = androidx.compose.ui.graphics.Color(0xFF5C243E).copy(alpha = 0.4f)
            )
            .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Large)
            .background(androidx.compose.ui.graphics.Color(0xFF5C243E))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular / Rounded Art
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_placeholder),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium)
                )
                
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFFE8B8CD),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Soft Pill Play/Pause Action Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFF3C6D7))
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Default.Cast
                                     else androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = androidx.compose.ui.graphics.Color(0xFF4A1E31),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onCloseClick != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                            contentDescription = "Close",
                            tint = androidx.compose.ui.graphics.Color(0xFFE8B8CD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
