package com.hasanege.materialtv.ui.screens.favorites

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
 
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.FavoritesViewModel
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.R
import com.hasanege.materialtv.UiState
import com.hasanege.materialtv.model.FavoriteItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.CenteredProgressBar
import com.hasanege.materialtv.ui.ErrorMessage
import com.hasanege.materialtv.ui.utils.ImageConfig
import com.hasanege.materialtv.ui.components.ExpressiveDialogOption
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import com.hasanege.materialtv.ui.theme.ExpressiveAnimations
import com.hasanege.materialtv.ui.theme.animateStaggeredEntry
import com.hasanege.materialtv.ui.NoConnectionScreen
import com.hasanege.materialtv.ui.utils.NetworkUtils
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel) {
    val context = LocalContext.current
    val navController = com.hasanege.materialtv.navigation.LocalNavController.current
    val favoritesState by viewModel.favoritesState.collectAsState()
    val listsState by viewModel.listsState.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var selectedFavorite by remember { mutableStateOf<FavoriteItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoveToListDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<com.hasanege.materialtv.model.FavoriteList?>(null) }

    // Create tabs from lists
    val allLists = if (listsState is UiState.Success) (listsState as UiState.Success).data else emptyList()
    val tabs = listOf(stringResource(R.string.favorites_all_favorites)) + allLists.map { it.listName }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    
    // Pager state for swipe navigation
    val pagerState = rememberPagerState(
        initialPage = if (viewModel.selectedListId.value == null) 0 
                     else allLists.indexOfFirst { it.listId == viewModel.selectedListId.value } + 1,
        pageCount = { tabs.size.coerceAtLeast(1) }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Sync tab selection with pager when tab is clicked
    val selectedTabIndex = pagerState.currentPage
    
    // Sync pager with viewModel filter
    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        if (index == 0) {
            viewModel.setFilter(listId = null)
        } else {
            val listId = allLists.getOrNull(index - 1)?.listId
            viewModel.setFilter(listId = listId)
        }
    }

    // Main content with floating tab slider - matching Home screen style
    Box(modifier = Modifier.fillMaxSize()) {
        val isOnline = NetworkUtils.isNetworkAvailable(context)
        
        if (!isOnline) {
            NoConnectionScreen()
        } else {
            // HorizontalPager for swipe navigation between categories
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true, // Usually true, but we could make it conditional if needed
            beyondViewportPageCount = 1
        ) { page ->
            // Smooth page transition with scale and alpha - matching Home screen style
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val scale = 1f - (kotlin.math.abs(pageOffset) * 0.1f).coerceIn(0f, 0.1f)
            val alpha = 1f
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                when (val state = favoritesState) {
                    is UiState.Loading -> CenteredProgressBar()
                    is UiState.Error -> ErrorMessage(state.message)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyFavoritesView()
                        } else {
                            // Local filtering per page to prevent content jumping during swipe
                            val currentListId = if (page == 0) null else allLists.getOrNull(page - 1)?.listId
                            val pageData = if (currentListId == null) {
                                state.data
                            } else {
                                state.data.filter { it.listId == currentListId }
                            }
                            
                            if (pageData.isEmpty()) {
                                EmptyFavoritesView() // Or a specific "Empty List" view
                            } else {
                                FavoritesGrid(
                                    favorites = pageData,
                                 onItemClick = { favorite ->
                                     when (favorite.contentType) {
                                         "series" -> navController.navigate(com.hasanege.materialtv.navigation.Screen.SeriesDetail.createRoute(favorite.seriesId ?: -1, favorite.name))
                                         "movie" -> {
                                             val intent = Intent(context, PlayerActivity::class.java).apply {
                                                 putExtra("STREAM_ID", favorite.contentId)
                                                 putExtra("TITLE", favorite.name)
                                                 putExtra("AUTO_PLAY", true)
                                             }
                                             context.startActivity(intent)
                                         }
                                         "live" -> {
                                             val streamUrl = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                                 com.hasanege.materialtv.data.M3uRepository.getStreamUrl(favorite.contentId)
                                             } else {
                                                 "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${favorite.contentId}.ts"
                                             }
                                             if (!streamUrl.isNullOrEmpty()) {
                                                 val intent = Intent(context, PlayerActivity::class.java).apply {
                                                     putExtra("url", streamUrl)
                                                     putExtra("TITLE", favorite.name)
                                                     putExtra("LIVE_STREAM_ID", favorite.contentId)
                                                 }
                                                 context.startActivity(intent)
                                             }
                                         }
                                         else -> return@FavoritesGrid
                                     }
                                 },
                                onItemLongClick = { favorite ->
                                    selectedFavorite = favorite
                                    showEditDialog = true
                                },
                                    topPadding = 80.dp // Space for floating tab slider
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating tab slider on top - matching Home screen style
        if (tabs.isNotEmpty()) {
            FavoritesTabSlider(
                tabs = tabs,
                selectedIndex = selectedTabIndex.coerceAtLeast(0),
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = index,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onTabLongClick = { index ->
                    // index 0 is "All Favorites", can't delete that
                    if (index > 0) {
                        val listToDeleteItem = allLists.getOrNull(index - 1)
                        if (listToDeleteItem != null) {
                            listToDelete = listToDeleteItem
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Animated offset for FAB buttons - move down when tab slider is present
        val fabOffset by animateFloatAsState(
            targetValue = if (tabs.isNotEmpty()) 72f else 16f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "fab_offset"
        )
        
        // Floating action buttons for filter, sort, add
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    translationY = fabOffset * density
                }
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showFilterDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.FilterList, stringResource(R.string.favorites_filter_desc))
            }
            SmallFloatingActionButton(
                onClick = { showSortDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Sort, stringResource(R.string.favorites_sort_desc))
            }
            SmallFloatingActionButton(
                onClick = { showAddListDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.favorites_create_list_desc))
        }
    }

    // Dialogs
    if (showFilterDialog) {
        FilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
            viewModel = viewModel,
            onDismiss = { showSortDialog = false }
        )
    }

    if (showAddListDialog) {
        CreateListDialog(
            viewModel = viewModel,
            onDismiss = { showAddListDialog = false }
        )
    }

    if (showEditDialog && selectedFavorite != null) {
        EditFavoriteDialog(
            favorite = selectedFavorite!!,
            viewModel = viewModel,
            listsState = listsState,
            onDismiss = {
                showEditDialog = false
                selectedFavorite = null
            },
            onMoveToList = {
                showEditDialog = false
                showMoveToListDialog = true
            }
        )
    }
    
    if (showMoveToListDialog && selectedFavorite != null && listsState is UiState.Success) {
        MoveToListDialog(
            lists = (listsState as UiState.Success).data,
            onDismiss = {
                showMoveToListDialog = false
                selectedFavorite = null
            },
            onListSelected = { listId ->
                selectedFavorite?.let { favorite ->
                    viewModel.updateFavorite(favorite.copy(listId = listId ?: 0L))
                }
                showMoveToListDialog = false
                selectedFavorite = null
            }
        )
    }


    if (listToDelete != null) {
        DeleteListDialog(
            list = listToDelete!!,
            onConfirm = {
                viewModel.deleteList(listToDelete!!)
                // If we deleted the currently selected list, switch to "All Favorites"
                if (viewModel.selectedListId.value == listToDelete!!.listId) {
                    viewModel.setFilter(listId = null)
                }
                listToDelete = null
            },
            onDismiss = { listToDelete = null }
        )
    }
    }
}
}

@Composable
fun EmptyFavoritesView() {
    // Subtle pulse animation instead of floating
    val infiniteTransition = rememberInfiniteTransition(label = "emptyFavorites")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
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
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = stringResource(R.string.favorites_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = stringResource(R.string.favorites_empty_desc),
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
                    // Movie Chip
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
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.content_type_movie),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Series Chip
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
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stringResource(R.string.content_type_series),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Live TV Chip
                    Surface(
                        shape = ExpressiveShapes.Full,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.content_type_live_tv),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

// Favorites Tab Slider - Floating Island with animated sliding indicator
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesTabSlider(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabLongClick: ((Int) -> Unit)? = null, // For deleting custom lists
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Padding values for calculation
    val horizontalPadding = with(density) { 16.dp.toPx() }
    val verticalPadding = with(density) { 8.dp.toPx() }
    val spacing = with(density) { 6.dp.toPx() }
    
    // Track tab text dimensions
    var tabTextSizes by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    
    // Calculate full tab dimensions including padding
    val fullTabWidths = tabTextSizes.map { it.first + (horizontalPadding * 2) }
    val fullTabHeights = tabTextSizes.map { it.second + (verticalPadding * 2) }
    
    // Animated indicator offset
    val indicatorOffset by animateFloatAsState(
        targetValue = if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
            fullTabWidths.take(selectedIndex).sum() + (selectedIndex * spacing)
        } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator_offset"
    )
    
    // Animated indicator width
    val indicatorWidth by animateFloatAsState(
        targetValue = if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
            fullTabWidths[selectedIndex]
        } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator_width"
    )
    
    // Animated indicator height (use max height for consistency)
    val maxHeight = fullTabHeights.maxOrNull() ?: 0f
    val indicatorHeight by animateFloatAsState(
        targetValue = maxHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator_height"
    )
    
    // Outer container - just for centering, transparent background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner floating pill
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = ExpressiveShapes.ExtraLarge,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(ExpressiveShapes.ExtraLarge)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = ExpressiveShapes.ExtraLarge
                )
                .padding(6.dp)
        ) {
            // Scrollable container for tabs
            val scrollState = androidx.compose.foundation.rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            
            // Auto-scroll to selected tab when it changes
            LaunchedEffect(selectedIndex, fullTabWidths) {
                if (fullTabWidths.isNotEmpty() && selectedIndex < fullTabWidths.size) {
                    // For first tab, scroll to start (0)
                    // For last tab, scroll to end
                    // For others, try to center
                    val scrollTarget = when (selectedIndex) {
                        0 -> 0
                        tabs.size - 1 -> scrollState.maxValue
                        else -> {
                            val targetOffset = fullTabWidths.take(selectedIndex).sum() + (selectedIndex * spacing)
                            val tabWidth = fullTabWidths.getOrNull(selectedIndex) ?: 0f
                            (targetOffset - (scrollState.maxValue - tabWidth) / 2).toInt().coerceIn(0, scrollState.maxValue)
                        }
                    }
                    
                    scrollState.animateScrollTo(
                        value = scrollTarget,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            
            Box {
                // Animated sliding indicator behind tabs
                if (indicatorWidth > 0f && indicatorHeight > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = indicatorOffset - scrollState.value
                            }
                            .size(
                                width = with(density) { indicatorWidth.toDp() },
                                height = with(density) { indicatorHeight.toDp() }
                            )
                            .clip(ExpressiveShapes.ExtraLarge)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                
                // Horizontal scrollable row
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedIndex == index
                        val canDelete = index > 0 // Can't delete "All Favorites" (index 0)
                        
                        Box(
                            modifier = Modifier
                                .clip(ExpressiveShapes.ExtraLarge)
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onTabSelected(index)
                                    },
                                    onLongClick = if (canDelete && onTabLongClick != null) {
                                        {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onTabLongClick(index)
                                        }
                                    } else null
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val width = coordinates.size.width.toFloat()
                                    val height = coordinates.size.height.toFloat()
                                    if (tabTextSizes.size != tabs.size) {
                                        tabTextSizes = List(tabs.size) { Pair(0f, 0f) }
                                    }
                                    if (tabTextSizes.getOrNull(index) != Pair(width, height)) {
                                        tabTextSizes = tabTextSizes.toMutableList().also { 
                                            it[index] = Pair(width, height) 
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesGrid(
    favorites: List<FavoriteItem>,
    onItemClick: (FavoriteItem) -> Unit,
    onItemLongClick: (FavoriteItem) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 8.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = favorites,
            key = { index, favorite -> favorite.id }
        ) { index, favorite ->
            FavoriteCard(
                favorite = favorite,
                onClick = { onItemClick(favorite) },
                onLongClick = { onItemLongClick(favorite) },
                modifier = Modifier.animateStaggeredEntry(index)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteCard(
    favorite: FavoriteItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    ElevatedCard(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == PointerEventType.Press
                    }
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box {
            // Thumbnail with consistent 16:9 aspect ratio
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(favorite.customThumbnail ?: favorite.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = ImageConfig.getImageLoader(context),
                contentDescription = favorite.name,
                contentScale = if (favorite.contentType == "live") ContentScale.Fit else ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(ExpressiveShapes.Medium)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            
            // Content type badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        color = when (favorite.contentType) {
                            "live" -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                            "series" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        },
                        shape = ExpressiveShapes.ExtraSmall
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (favorite.contentType) {
                        "live" -> stringResource(R.string.content_type_live_upper)
                        "series" -> stringResource(R.string.content_type_series_upper)
                        "movie" -> stringResource(R.string.content_type_movie_upper)
                        else -> favorite.contentType.uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }

            // Rating badge
            if (favorite.rating > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = ExpressiveShapes.ExtraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format("%.1f", favorite.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Watched indicator
            if (favorite.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.favorites_watched),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // Title section
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = favorite.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun FilterDialog(
    viewModel: FavoritesViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.favorites_filter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Content Type Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.favorites_type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpressiveDialogOption(
                            icon = Icons.Default.AllInclusive,
                            label = stringResource(R.string.favorites_all),
                            isSelected = viewModel.selectedType.value == null,
                            onClick = { viewModel.setFilter(type = null) }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.Movie,
                            label = stringResource(R.string.tab_movies),
                            isSelected = viewModel.selectedType.value == "movie",
                            onClick = { viewModel.setFilter(type = "movie") }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.Tv,
                            label = stringResource(R.string.tab_series),
                            isSelected = viewModel.selectedType.value == "series",
                            onClick = { viewModel.setFilter(type = "series") }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.LiveTv,
                            label = stringResource(R.string.live),
                            isSelected = viewModel.selectedType.value == "live",
                            onClick = { viewModel.setFilter(type = "live") }
                        )
                    }
                }

                // Status Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.favorites_status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpressiveDialogOption(
                            icon = Icons.Default.DoneAll,
                            label = stringResource(R.string.favorites_all),
                            isSelected = viewModel.showWatchedOnly.value == null,
                            onClick = { viewModel.setFilter(watchedOnly = null) }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.Visibility,
                            label = stringResource(R.string.favorites_watched),
                            isSelected = viewModel.showWatchedOnly.value == true,
                            onClick = { viewModel.setFilter(watchedOnly = true) }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.VisibilityOff,
                            label = stringResource(R.string.favorites_unwatched),
                            isSelected = viewModel.showWatchedOnly.value == false,
                            onClick = { viewModel.setFilter(watchedOnly = false) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.favorites_done), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearFilters()
                onDismiss()
            }) {
                Text(stringResource(R.string.favorites_clear))
            }
        },
        shape = ExpressiveShapes.Large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun SortDialog(
    viewModel: FavoritesViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.favorites_sort_by),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sort By Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple(FavoritesViewModel.SortOption.DATE_ADDED, stringResource(R.string.favorites_sort_date_added), Icons.Default.Event),
                        Triple(FavoritesViewModel.SortOption.NAME, stringResource(R.string.favorites_sort_name), Icons.Default.SortByAlpha),
                        Triple(FavoritesViewModel.SortOption.RATING, stringResource(R.string.favorites_sort_rating), Icons.Default.Star)
                    ).forEach { (option, label, icon) ->
                        ExpressiveDialogOption(
                            icon = icon,
                            label = label,
                            isSelected = viewModel.sortBy.value == option,
                            onClick = { viewModel.setSortOption(option, viewModel.sortAscending.value) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // Sort Direction Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.favorites_sort_direction),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpressiveDialogOption(
                            icon = Icons.Default.ArrowUpward,
                            label = stringResource(R.string.favorites_sort_ascending),
                            isSelected = viewModel.sortAscending.value,
                            onClick = { viewModel.setSortOption(viewModel.sortBy.value, true) }
                        )
                        ExpressiveDialogOption(
                            icon = Icons.Default.ArrowDownward,
                            label = stringResource(R.string.favorites_sort_descending),
                            isSelected = !viewModel.sortAscending.value,
                            onClick = { viewModel.setSortOption(viewModel.sortBy.value, false) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.favorites_done),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = ExpressiveShapes.Large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListDialog(
    viewModel: FavoritesViewModel,
    onDismiss: () -> Unit
) {
    var listName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_create_list)) },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text(stringResource(R.string.favorites_list_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (listName.isNotEmpty()) {
                        scope.launch {
                            viewModel.createList(listName)
                            onDismiss()
                        }
                    }
                },
                enabled = listName.isNotEmpty()
            ) {
                Text(stringResource(R.string.favorites_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFavoriteDialog(
    favorite: FavoriteItem,
    viewModel: FavoritesViewModel,
    listsState: UiState<List<com.hasanege.materialtv.model.FavoriteList>>,
    onDismiss: () -> Unit,
    onMoveToList: () -> Unit
) {
    var rating by remember { mutableStateOf(favorite.rating) }
    var notes by remember { mutableStateOf(favorite.notes) }
    var isWatched by remember { mutableStateOf(favorite.isWatched) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Rating
                Text(stringResource(R.string.favorites_rating), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        val starRating = star.toFloat()
                        IconButton(
                            onClick = { 
                                rating = if (rating == starRating) 0f else starRating
                            }
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.favorites_stars_desc, star),
                                tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.favorites_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Watched status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.favorites_mark_watched))
                    Switch(
                        checked = isWatched,
                        onCheckedChange = { isWatched = it }
                    )
                }
                
                // Move to List button
                if (listsState is UiState.Success && listsState.data.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onMoveToList,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.favorites_move_to_list))
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.removeFavorite(favorite)
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.favorites_remove), color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.updateFavorite(
                                favorite.copy(
                                    rating = rating,
                                    notes = notes,
                                    isWatched = isWatched
                                )
                            )
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.favorites_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ListSelector(
    lists: List<com.hasanege.materialtv.model.FavoriteList>,
    selectedListId: Long?,
    onListSelected: (Long?) -> Unit,
    onListDelete: (com.hasanege.materialtv.model.FavoriteList) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedListId == null,
                onClick = { onListSelected(null) },
                label = { Text(stringResource(R.string.favorites_all_favorites)) },
                leadingIcon = {
                    if (selectedListId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
        items(count = lists.size) { index ->
            val list = lists[index]
            val isSelected = selectedListId == list.listId
            FilterChip(
                selected = isSelected,
                onClick = { onListSelected(list.listId) },
                label = { Text(list.listName) },
                leadingIcon = {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                trailingIcon = {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onListDelete(list) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun DeleteListDialog(
    list: com.hasanege.materialtv.model.FavoriteList,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_delete_list)) },
        text = { Text(stringResource(R.string.favorites_delete_list_confirm, list.listName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun MoveToListDialog(
    lists: List<com.hasanege.materialtv.model.FavoriteList>,
    onDismiss: () -> Unit,
    onListSelected: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_move_to_list)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onListSelected(0L) }, // 0 for default list
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(stringResource(R.string.favorites_all_favorites_default))
                    }
                }
                lists.forEach { list ->
                    TextButton(
                        onClick = { onListSelected(list.listId) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(list.listName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
