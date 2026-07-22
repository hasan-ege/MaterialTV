
package com.hasanege.materialtv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.hasanege.materialtv.ui.ExpressiveTabSlider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.first
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.app.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.hasanege.materialtv.navigation.LocalNavController
import com.hasanege.materialtv.navigation.Screen
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.PlayerActivity
import com.hasanege.materialtv.ui.CenteredProgressBar
import com.hasanege.materialtv.ui.ErrorMessage
import com.hasanege.materialtv.ui.NoConnectionScreen
import com.hasanege.materialtv.ui.MaterialTVBottomNavBar
import com.hasanege.materialtv.ui.screens.downloads.DownloadsScreen
import com.hasanege.materialtv.ui.screens.profile.ProfileScreen
import com.hasanege.materialtv.ui.MaterialTVNavRail
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.ui.theme.ExpressiveAnimations
import com.hasanege.materialtv.ui.utils.ImageConfig
import com.hasanege.materialtv.ui.utils.NetworkUtils
import com.hasanege.materialtv.R
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialTVApp(
    homeViewModel: HomeViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isOnline = NetworkUtils.isNetworkAvailable(context)

    val username = SessionManager.username ?: ""
    val password = SessionManager.password ?: ""

    LaunchedEffect(isOnline) {
        if (isOnline) {
            homeViewModel.loadInitialData(username, password)
        }
    }

    // Read start page from settings
    val settingsRepository = remember { com.hasanege.materialtv.data.SettingsRepository.getInstance(context) }
    val initialNavBarStyle = remember {
        kotlinx.coroutines.runBlocking {
            settingsRepository.navBarStyle.first()
        }
    }
    val initialStartPage = remember {
        kotlinx.coroutines.runBlocking {
            settingsRepository.startPage.first()
        }
    }
    val startPage by settingsRepository.startPage.collectAsState(initial = initialStartPage)
    val navBarStyle by settingsRepository.navBarStyle.collectAsState(initial = initialNavBarStyle)
    val initialBottomNavOnlyIcons = remember {
        kotlinx.coroutines.runBlocking {
            settingsRepository.bottomNavOnlyIcons.first()
        }
    }
    val bottomNavOnlyIcons by settingsRepository.bottomNavOnlyIcons.collectAsState(initial = initialBottomNavOnlyIcons)
    
    // Determine initial navigation and tab based on startPage
    val initialNav = remember(startPage) {
        when (startPage) {
            "favorites" -> MainScreen.Favorites.route
            "downloads" -> MainScreen.Downloads.route
            "profile" -> MainScreen.Profile.route
            else -> MainScreen.Home.route // movies, series, live all go to Home
        }
    }
    
    val initialTabIndex = remember(startPage) {
        when (startPage) {
            "movies" -> 0
            "series" -> 1
            "live" -> 2
            else -> 0
        }
    }

    val currentScreen = remember(initialNav) { mutableStateOf(initialNav) }
    val bottomNavItems = listOf(MainScreen.Home, MainScreen.Favorites, MainScreen.Downloads, MainScreen.Profile)
    
    // Search state
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }



// ... (existing imports)

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth > 600.dp
            
            if (isWideScreen || navBarStyle == "rail") {
                Row(modifier = Modifier.fillMaxSize()) {
                    MaterialTVNavRail(
                        items = bottomNavItems,
                        currentItemRoute = currentScreen.value,
                        onItemClick = { currentScreen.value = it.route }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = currentScreen.value,
                            transitionSpec = {
                                com.hasanege.materialtv.ui.theme.ExpressiveAnimations.enterTransition.togetherWith(
                                    com.hasanege.materialtv.ui.theme.ExpressiveAnimations.exitTransition
                                )
                            },
                            label = "WideScreenNavigation"
                        ) { targetState ->
                            when (targetState) {
                                MainScreen.Home.route -> {
                                    if (isOnline) {
                                        HomeScreen(homeViewModel, initialTabIndex, onSearchClick = { isSearchExpanded = true })
                                    } else {
                                        NoConnectionScreen()
                                    }
                                }
                                MainScreen.Favorites.route -> com.hasanege.materialtv.ui.screens.favorites.FavoritesScreen(favoritesViewModel)
                                MainScreen.Downloads.route -> DownloadsScreen(downloadsViewModel)
                                MainScreen.Profile.route -> ProfileScreen(profileViewModel)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) { // Content extends behind nav
                        androidx.compose.animation.AnimatedContent(
                            targetState = currentScreen.value,
                            transitionSpec = {
                                com.hasanege.materialtv.ui.theme.ExpressiveAnimations.enterTransition.togetherWith(
                                    com.hasanege.materialtv.ui.theme.ExpressiveAnimations.exitTransition
                                )
                            },
                            label = "NarrowScreenNavigation"
                        ) { targetState ->
                            when (targetState) {
                                MainScreen.Home.route -> {
                                    if (isOnline) {
                                        HomeScreen(homeViewModel, initialTabIndex, onSearchClick = { isSearchExpanded = true })
                                    } else {
                                        NoConnectionScreen()
                                    }
                                }
                                MainScreen.Favorites.route -> com.hasanege.materialtv.ui.screens.favorites.FavoritesScreen(favoritesViewModel)
                                MainScreen.Downloads.route -> DownloadsScreen(downloadsViewModel)
                                MainScreen.Profile.route -> ProfileScreen(profileViewModel)
                            }
                        }
                    }
                    val continueWatchingState by homeViewModel.continueWatchingState.collectAsState()
                    val latestContinueItem = (continueWatchingState as? UiState.Success)?.data?.firstOrNull()

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(3f)
                    ) {
                        if (latestContinueItem != null) {
                            com.hasanege.materialtv.ui.PersistentFloatingMiniPlayer(
                                title = latestContinueItem.name ?: "",
                                subtitle = latestContinueItem.type?.uppercase() ?: "IPTV",
                                imageUrl = latestContinueItem.streamIcon,
                                isPlaying = false,
                                onPlayPauseClick = {
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("STREAM_ID", latestContinueItem.streamId)
                                        putExtra("TITLE", latestContinueItem.name)
                                        putExtra("AUTO_PLAY", true)
                                        putExtra("position", latestContinueItem.position)
                                    }
                                    context.startActivity(intent)
                                },
                                onClick = {
                                    if (latestContinueItem.type == "series") {
                                        navController.navigate(Screen.SeriesDetail.createRoute(latestContinueItem.seriesId ?: -1, latestContinueItem.name ?: ""))
                                    } else {
                                        val intent = Intent(context, PlayerActivity::class.java).apply {
                                            putExtra("STREAM_ID", latestContinueItem.streamId)
                                            putExtra("TITLE", latestContinueItem.name)
                                            putExtra("AUTO_PLAY", true)
                                            putExtra("position", latestContinueItem.position)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                onCloseClick = {
                                    homeViewModel.removeFromContinueWatching(latestContinueItem)
                                }
                            )
                        }

                        if (navBarStyle == "floating") {
                            com.hasanege.materialtv.ui.MaterialTVBottomNavBar(
                                items = bottomNavItems,
                                currentItemRoute = currentScreen.value,
                                onItemClick = { currentScreen.value = it.route },
                                onlyIcons = bottomNavOnlyIcons
                            )
                        } else {
                            com.hasanege.materialtv.ui.DefaultBottomNavBar(
                                items = bottomNavItems,
                                currentItemRoute = currentScreen.value,
                                onItemClick = { currentScreen.value = it.route },
                                onlyIcons = bottomNavOnlyIcons
                            )
                        }
                    }
                }
            }
            
            // Expanding Search Overlay - Always render so AnimatedVisibility can animate
            ExpandingSearchBar(
                isExpanded = isSearchExpanded,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { searchViewModel.search(it) },
                onExpandedChange = { isSearchExpanded = it },
                searchViewModel = searchViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandingSearchBar(
    isExpanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    searchViewModel: SearchViewModel
) {
    val context = LocalContext.current
    
    // Debounced search
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(300)
        onSearch(query)
    }

    // Back Handler - when search is open, back button closes it
    androidx.activity.compose.BackHandler(enabled = isExpanded) {
        onQueryChange("")
        onExpandedChange(false)
    }
    
    // M3 Expressive Spring Animations - Extra bouncy for search menu
    val springSpec = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )
    val springSpecInt = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )
    
    AnimatedVisibility(
        visible = isExpanded,
        enter = slideInVertically(
            initialOffsetY = { -it }, // From top - prevent edge glitch
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) + scaleIn(
            initialScale = 0.85f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it }, // To top - prevent edge glitch
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) + fadeOut(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) + scaleOut(
            targetScale = 0.85f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    ) {
        // Full screen overlay with glassmorphism background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Modern Search Card with elevation and rounded corners
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = ExpressiveShapes.Medium,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    shape = ExpressiveShapes.Medium,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button with ripple effect
                        IconButton(
                            onClick = { 
                                onQueryChange("")
                                onExpandedChange(false) 
                            },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Search TextField with custom styling
                        androidx.compose.material3.TextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            placeholder = { 
                                Text(
                                    stringResource(R.string.search_field_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            singleLine = true
                        )
                        
                        // Clear button with animation
                        androidx.compose.animation.AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + 
                                   androidx.compose.animation.scaleIn(animationSpec = ExpressiveAnimations.enter()),
                            exit = fadeOut(animationSpec = ExpressiveAnimations.exit()) + 
                                  androidx.compose.animation.scaleOut(animationSpec = ExpressiveAnimations.exit())
                        ) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                
                // Search Results with modern card design
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 4.dp,
                            shape = ExpressiveShapes.Medium
                        ),
                    shape = ExpressiveShapes.Medium,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    SearchResultsOverlay(
                        searchViewModel = searchViewModel,
                        onDismiss = { onExpandedChange(false) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsOverlay(
    searchViewModel: SearchViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_movies),
        stringResource(R.string.tab_series),
        stringResource(R.string.tab_live_tv)
    )
    
    // Auto-switch logic: Find first tab with results if current is empty
    val moviesState by searchViewModel.movies
    val seriesState by searchViewModel.series
    val liveStreamsState by searchViewModel.liveStreams

    androidx.compose.runtime.LaunchedEffect(moviesState, seriesState, liveStreamsState) {
        if (!searchViewModel.isLoading.value) {
            val hasMovies = (moviesState as? UiState.Success)?.data?.isNotEmpty() == true
            val hasSeries = (seriesState as? UiState.Success)?.data?.isNotEmpty() == true
            val hasLive = (liveStreamsState as? UiState.Success)?.data?.isNotEmpty() == true

            val currentTabHasResults = when (selectedTab) {
                0 -> hasMovies
                1 -> hasSeries
                2 -> hasLive
                else -> false
            }

            if (!currentTabHasResults) {
                if (hasMovies) {
                    selectedTab = 0
                } else if (hasSeries) {
                    selectedTab = 1
                } else if (hasLive) {
                    selectedTab = 2
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExpressiveTabSlider(
            tabs = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { index -> selectedTab = index },
            modifier = Modifier.fillMaxWidth(),
            scrollable = false
        )
        
        if (searchViewModel.isLoading.value) {
            CenteredProgressBar()
        } else {
            when (selectedTab) {
                0 -> {
                    when (val moviesState = searchViewModel.movies.value) {
                        is UiState.Loading -> CenteredProgressBar()
                        is UiState.Success -> {
                            if (moviesState.data.isEmpty()) {
                                com.hasanege.materialtv.ui.NoResultsFound()
                            } else {
                                com.hasanege.materialtv.ui.MoviesList(moviesState.data)
                            }
                        }
                        is UiState.Error -> ErrorMessage(moviesState.message)
                    }
                }
                1 -> {
                    when (val seriesState = searchViewModel.series.value) {
                        is UiState.Loading -> CenteredProgressBar()
                        is UiState.Success -> {
                            if (seriesState.data.isEmpty()) {
                                com.hasanege.materialtv.ui.NoResultsFound()
                            } else {
                                com.hasanege.materialtv.ui.SeriesList(seriesState.data)
                            }
                        }
                        is UiState.Error -> ErrorMessage(seriesState.message)
                    }
                }
                2 -> {
                    when (val liveState = searchViewModel.liveStreams.value) {
                        is UiState.Loading -> CenteredProgressBar()
                        is UiState.Success -> {
                            if (liveState.data.isEmpty()) {
                                com.hasanege.materialtv.ui.NoResultsFound()
                            } else {
                                com.hasanege.materialtv.ui.LiveTVList(liveState.data)
                            }
                        }
                        is UiState.Error -> ErrorMessage(liveState.message)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun HomeScreen(homeViewModel: HomeViewModel, initialTabIndex: Int = 0, onSearchClick: () -> Unit = {}, onCastClick: () -> Unit = {}) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val selectedTabIndexState = remember { mutableIntStateOf(initialTabIndex) }
    var selectedTabIndex by selectedTabIndexState
    val tabs = listOf(
        stringResource(R.string.tab_movies),
        stringResource(R.string.tab_series),
        stringResource(R.string.tab_live_tv)
    )
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialTabIndex,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()
    
    // Status bar padding for floating menu
    val safeTopPadding = WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding()
    
    val context = LocalContext.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.loadContinueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    
    // Sync Tab selection with Pager when tab is clicked
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(
                page = selectedTabIndex,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
    }
    
    // Sync Pager scroll with Tab selection
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTabIndex != pagerState.currentPage) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    var showManageCategoriesBottomSheet by remember { mutableStateOf(false) }

    // Box layout
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Content Pager (Bottom Layer)
        val tabHeightDp = 72.dp
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            beyondViewportPageCount = 1 
        ) { page ->
            // Smooth page transition
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val scale = 1f - (kotlin.math.abs(pageOffset) * 0.1f).coerceIn(0f, 0.1f)
            val alpha = 1f - (kotlin.math.abs(pageOffset) * 0.3f).coerceIn(0f, 0.3f)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                CategoryScreen(
                    viewModel = homeViewModel, 
                    selectedTab = page,
                    contentPadding = PaddingValues(top = tabHeightDp, bottom = 100.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f) 
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(if (configuration.screenWidthDp < 360) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showMoreMenu by remember { mutableStateOf(false) }
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.TopCenter, unbounded = true)
                    .zIndex(10f)
            ) {
                Column(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .animateContentSize(alignment = Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showMoreMenu = !showMoreMenu
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Daha Fazla",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (showMoreMenu) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showMoreMenu = false
                                    context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showMoreMenu = false
                                    showManageCategoriesBottomSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Kategorileri Düzenle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            ExpressiveTabSlider(
                tabs = tabs,
                selectedIndex = selectedTabIndex,
                onTabSelected = { index -> selectedTabIndex = index },
                modifier = Modifier.weight(1f),
                scrollable = false
            )
            
            FloatingActionIsland(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.action_search),
                onClick = onSearchClick
            )
        }

        if (showManageCategoriesBottomSheet) {
            ManageCategoriesBottomSheet(
                viewModel = homeViewModel,
                selectedTab = selectedTabIndex,
                onDismiss = { showManageCategoriesBottomSheet = false }
            )
        }
    }
}

// Small floating action island button
@Composable
fun FloatingActionIsland(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = androidx.compose.foundation.shape.CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun CategoryScreen(
    viewModel: HomeViewModel,
    selectedTab: Int,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val navController = com.hasanege.materialtv.navigation.LocalNavController.current
    val isRefreshing = viewModel.isRefreshing

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true) },
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp
            val adaptivePadding = if (isWide) 32.dp else 0.dp
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 1200.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = adaptivePadding),
                contentPadding = contentPadding
            ) {
                // 1. Continue Watching Section
                item(key = "continue_watching") {
                    val continueWatchingState by viewModel.continueWatchingState.collectAsState()
                    
                    when (val state = continueWatchingState) {
                        is UiState.Success -> {
                            if (state.data.isNotEmpty()) {
                                ContinueWatchingRow(
                                    items = state.data,
                                    onItemClick = { item ->
                                        if (item.type == "series") {
                                            navController.navigate(Screen.SeriesDetail.createRoute(item.seriesId ?: -1, item.name ?: ""))
                                        } else if (item.type == "live") {
                                            val streamUrl = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                                com.hasanege.materialtv.data.M3uRepository.getStreamUrl(item.streamId)
                                            } else {
                                                "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${item.streamId}.ts"
                                            }
                                            if (!streamUrl.isNullOrEmpty()) {
                                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                                    putExtra("url", streamUrl)
                                                    putExtra("TITLE", item.name)
                                                    putExtra("LIVE_STREAM_ID", item.streamId)
                                                }
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                                putExtra("STREAM_ID", item.streamId)
                                                putExtra("TITLE", item.name)
                                                putExtra("AUTO_PLAY", true)
                                                putExtra("position", item.position)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    onPin = { item ->
                                        val updatedItems = state.data.map {
                                            if (it.streamId == item.streamId) {
                                                it.copy(isPinned = !it.isPinned)
                                            } else {
                                                it
                                            }
                                        }
                                        viewModel.updateContinueWatchingItems(updatedItems)
                                    },
                                    onRemove = { item ->
                                        viewModel.removeFromContinueWatching(item)
                                    }
                                )
                            }
                        }
                        else -> {}
                    }
                }

                // 2. Tab Specific Content (Movies, Series, Live TV)
                when (selectedTab) {
                    0 -> {
                        when (val moviesByCategoriesState = viewModel.moviesByCategoriesState) {
                            is UiState.Loading -> item { CenteredProgressBar() }
                            is UiState.Success -> {
                                val allMovies = moviesByCategoriesState.data.values.flatten()
                                val hiddenSet = viewModel.hiddenCategoryIdsMovies
                                val customOrder = viewModel.orderedCategoryIdsMovies
                                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                                
                                val filteredData = moviesByCategoriesState.data.entries
                                    .filter { it.value.isNotEmpty() && !hiddenSet.contains(it.key.categoryId ?: "") }
                                    .sortedWith { a, b ->
                                        val posA = orderMap[a.key.categoryId ?: ""]
                                        val posB = orderMap[b.key.categoryId ?: ""]
                                        if (posA != null && posB != null) {
                                            posA.compareTo(posB)
                                        } else if (posA != null) {
                                            -1
                                        } else if (posB != null) {
                                            1
                                        } else {
                                            (a.key.categoryName ?: "").compareTo(b.key.categoryName ?: "", ignoreCase = true)
                                        }
                                    }

                                // Hero Spotlight Banner (Shuffled top movies)
                                if (allMovies.isNotEmpty()) {
                                    item(key = "hero_carousel_movies") {
                                        HeroCarousel(
                                            items = allMovies,
                                            key = { item -> item.streamId ?: 0 },
                                            imageUrlProvider = { it.streamIcon },
                                            titleProvider = { it.name },
                                            subtitleProvider = { it.year },
                                            externalSeed = viewModel.featuredSeedMovies,
                                            onRerollClick = { viewModel.rerollFeaturedItems(0) },
                                            onItemClick = { vodItem ->
                                                navController.navigate(Screen.Detail.createRoute(vodItem.streamId ?: -1, vodItem.name ?: ""))
                                            }
                                        )
                                    }
                                }

                                items(
                                    items = filteredData,
                                    key = { (category, _) -> category.categoryId }
                                ) { (category, movies) ->
                                    ContentRow(title = category.categoryName, items = movies, onSeeAllClick = {
                                        navController.navigate(Screen.Category.createRoute(category.categoryId, "movie", category.categoryName))
                                    }) { vodItem ->
                                        navController.navigate(Screen.Detail.createRoute(vodItem.streamId ?: -1, vodItem.name ?: ""))
                                    }
                                }
                            }
                            is UiState.Error -> item { ErrorMessage(moviesByCategoriesState.message) }
                        }
                    }
                    1 -> {
                        when (val seriesByCategoriesState = viewModel.seriesByCategoriesState) {
                            is UiState.Loading -> item { CenteredProgressBar() }
                            is UiState.Success -> {
                                val allSeries = seriesByCategoriesState.data.values.flatten()
                                val hiddenSet = viewModel.hiddenCategoryIdsSeries
                                val customOrder = viewModel.orderedCategoryIdsSeries
                                val orderMap = customOrder.withIndex().associate { it.value to it.index }

                                val filteredData = seriesByCategoriesState.data.entries
                                    .filter { it.value.isNotEmpty() && !hiddenSet.contains(it.key.categoryId ?: "") }
                                    .sortedWith { a, b ->
                                        val posA = orderMap[a.key.categoryId ?: ""]
                                        val posB = orderMap[b.key.categoryId ?: ""]
                                        if (posA != null && posB != null) {
                                            posA.compareTo(posB)
                                        } else if (posA != null) {
                                            -1
                                        } else if (posB != null) {
                                            1
                                        } else {
                                            (a.key.categoryName ?: "").compareTo(b.key.categoryName ?: "", ignoreCase = true)
                                        }
                                    }

                                // Hero Spotlight Banner
                                if (allSeries.isNotEmpty()) {
                                    item(key = "hero_carousel_series") {
                                        HeroCarousel(
                                            items = allSeries,
                                            key = { item -> item.seriesId ?: 0 },
                                            imageUrlProvider = { it.cover },
                                            titleProvider = { it.name },
                                            subtitleProvider = { it.year },
                                            externalSeed = viewModel.featuredSeedSeries,
                                            onRerollClick = { viewModel.rerollFeaturedItems(1) },
                                            onItemClick = { seriesItem ->
                                                navController.navigate(Screen.SeriesDetail.createRoute(seriesItem.seriesId ?: -1, seriesItem.name ?: ""))
                                            }
                                        )
                                    }
                                }

                                items(
                                    items = filteredData,
                                    key = { (category, _) -> category.categoryId }
                                ) { (category, series) ->
                                    SeriesContentRow(
                                        title = category.categoryName,
                                        items = series,
                                        key = { it.seriesId ?: 0 },
                                        onSeeAllClick = {
                                            navController.navigate(Screen.Category.createRoute(category.categoryId, "series", category.categoryName))
                                        }
                                    ) { seriesItem ->
                                        navController.navigate(Screen.SeriesDetail.createRoute(seriesItem.seriesId ?: -1, seriesItem.name ?: ""))
                                    }
                                }
                            }
                            is UiState.Error -> item { ErrorMessage(seriesByCategoriesState.message) }
                        }
                    }
                    2 -> {
                        when (val liveByCategoriesState = viewModel.liveByCategoriesState) {
                            is UiState.Loading -> item { CenteredProgressBar() }
                            is UiState.Success -> {
                                val hiddenSet = viewModel.hiddenCategoryIdsLive
                                val customOrder = viewModel.orderedCategoryIdsLive
                                val orderMap = customOrder.withIndex().associate { it.value to it.index }

                                val filteredData = liveByCategoriesState.data.entries
                                    .filter { it.value.isNotEmpty() && !hiddenSet.contains(it.key.categoryId ?: "") }
                                    .sortedWith { a, b ->
                                        val posA = orderMap[a.key.categoryId ?: ""]
                                        val posB = orderMap[b.key.categoryId ?: ""]
                                        if (posA != null && posB != null) {
                                            posA.compareTo(posB)
                                        } else if (posA != null) {
                                            -1
                                        } else if (posB != null) {
                                            1
                                        } else {
                                            (a.key.categoryName ?: "").compareTo(b.key.categoryName ?: "", ignoreCase = true)
                                        }
                                    }

                                items(
                                    items = filteredData,
                                    key = { (category, _) -> category.categoryId }
                                ) { (category, liveStreams) ->
                                    LiveStreamContentRow(
                                        title = category.categoryName,
                                        items = liveStreams,
                                        onSeeAllClick = {
                                            navController.navigate(Screen.Category.createRoute(category.categoryId, "live", category.categoryName))
                                        },
                                        key = { it.streamId ?: 0 }
                                    ) { liveStream ->
                                        val streamUrl = if (SessionManager.loginType == SessionManager.LoginType.M3U) {
                                            com.hasanege.materialtv.data.M3uRepository.getStreamUrl(liveStream.streamId ?: 0)
                                        } else {
                                            "${SessionManager.serverUrl}/live/${SessionManager.username}/${SessionManager.password}/${liveStream.streamId}.ts"
                                        }
                                        if (!streamUrl.isNullOrEmpty()) {
                                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                                putExtra("url", streamUrl)
                                                putExtra("TITLE", liveStream.name)
                                                putExtra("LIVE_STREAM_ID", liveStream.streamId ?: -1)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> item { ErrorMessage(liveByCategoriesState.message) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChips(viewModel: HomeViewModel, selectedTab: Int) {
    val (categories, selectedCategoryId, onCategorySelected) = when (selectedTab) {
        0 -> Triple(viewModel.movieCategories, viewModel.selectedMovieCategoryId, viewModel::onMovieCategorySelected)
        1 -> Triple(viewModel.seriesCategories, viewModel.selectedSeriesCategoryId, viewModel::onSeriesCategorySelected)
        else -> Triple(viewModel.liveCategories, viewModel.selectedLiveCategoryId, viewModel::onLiveCategorySelected)
    }

    if (categories.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        item {
            val isSelected = selectedCategoryId == null
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(null) },
                label = { 
                    Text(
                        text = stringResource(R.string.category_all),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                shape = ExpressiveShapes.Full,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null
            )
        }
        items(categories, key = { it.categoryId }) { category ->
            val isSelected = category.categoryId == selectedCategoryId
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category.categoryId) },
                label = { 
                    Text(
                        text = category.categoryName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                shape = ExpressiveShapes.Full,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null
            )
        }
    }
}


// Expressive Organic Collage Stage Showcase (Matching Reference Screenshot 1)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> HeroCarousel(
    items: List<T>,
    onItemClick: (T) -> Unit,
    key: ((T) -> Any)? = null,
    imageUrlProvider: (T) -> String?,
    titleProvider: (T) -> String?,
    subtitleProvider: (T) -> String? = { null },
    externalSeed: Int = 1,
    onRerollClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    
    val context = LocalContext.current
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "reroll_spin"
    )

    // Deterministic shuffle using externalSeed: 100% persistent across navigation/recompositions!
    val currentDisplayItems = remember(items, externalSeed) {
        val random = java.util.Random(externalSeed.toLong())
        items.shuffled(random).take(3)
    }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        // Expressive Top Title + Circular Reroll Icon FAB Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "WATCH",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(y = (-14).dp)
                )
            }

            // Circular Reroll Icon Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .clickable { 
                        rotationAngle += 360f
                        onRerollClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reroll",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                )
            }
        }

        // Asymmetric Organic Bubble Collage Stage (Matching Screenshot 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val leftItem = currentDisplayItems.getOrNull(0)
            val centerItem = currentDisplayItems.getOrNull(1) ?: currentDisplayItems.firstOrNull()
            val rightItem = currentDisplayItems.getOrNull(2)

            // 1. LEFT SATELLITE BUBBLE (Small Circle/Petal)
            leftItem?.let { item ->
                val posterUrl = imageUrlProvider(item)
                val title = titleProvider(item) ?: ""
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 12.dp, y = (-28).dp)
                        .size(86.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onItemClick(item) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(posterUrl).crossfade(400).build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. RIGHT SATELLITE BUBBLE (Small Circle/Petal)
            rightItem?.let { item ->
                val posterUrl = imageUrlProvider(item)
                val title = titleProvider(item) ?: ""
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-12).dp, y = 32.dp)
                        .size(82.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onItemClick(item) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(posterUrl).crossfade(400).build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. CENTER HIGHLIGHT (BIG Rotated Organic Capsule - Matching Screenshot 1)
            centerItem?.let { item ->
                val posterUrl = imageUrlProvider(item)
                val title = titleProvider(item) ?: ""

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "center_card_scale"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(220.dp)
                        .height(145.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = -14f
                        }
                        .clip(
                            RoundedCornerShape(
                                topStart = 64.dp,
                                topEnd = 24.dp,
                                bottomStart = 24.dp,
                                bottomEnd = 64.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { onItemClick(item) }
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(posterUrl).crossfade(400).build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark Gradient Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    )

                    // Title Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentRow(
    title: String,
    items: List<VodItem>,
    onSeeAllClick: () -> Unit,
    key: ((VodItem) -> Any)? = { it.streamId ?: 0 },
    onItemClick: (VodItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AnimatedVisibility(
        visible = true, 
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 }, 
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) { 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        stringResource(R.string.action_see_all),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.streamId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(156.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                ExpressiveShapes.Medium
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = { onItemClick(item) },
                                onLongClick = {
                                    scope.launch {
                                        val added = FavoritesManager.toggleFavorite(
                                            contentId = item.streamId ?: 0,
                                            contentType = "movie",
                                            name = item.name ?: "",
                                            thumbnailUrl = item.streamIcon,
                                            year = item.year,
                                            categoryId = item.categoryId
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ),
                        shape = ExpressiveShapes.Medium,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.streamIcon)
                                    .crossfade(300)
                                    .build(),
                                imageLoader = ImageConfig.getImageLoader(context),
                                contentDescription = item.name ?: "",
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                modifier = Modifier.fillMaxSize()
                            )
                            

                            // Top Rating Badge
                            item.rating5Based?.let { rating ->
                                if (rating > 0) {
                                    val displayRating = "%.1f".format(rating)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(ExpressiveShapes.Full)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = " $displayRating",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Title & Info at Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item.name ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.basicMarquee()
                                )
                                item.year?.let { yr ->
                                    Text(
                                        text = yr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesContentRow(
    title: String,
    items: List<SeriesItem>,
    onSeeAllClick: () -> Unit,
    key: ((SeriesItem) -> Any)? = { it.seriesId ?: 0 },
    onItemClick: (SeriesItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        stringResource(R.string.action_see_all),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.seriesId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(156.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                ExpressiveShapes.Medium
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = { onItemClick(item) },
                                onLongClick = {
                                    scope.launch {
                                        val added = FavoritesManager.toggleFavorite(
                                            contentId = item.seriesId ?: 0,
                                            contentType = "series",
                                            name = item.name ?: "",
                                            thumbnailUrl = item.cover,
                                            genre = item.genre,
                                            year = item.year,
                                            categoryId = item.categoryId,
                                            seriesId = item.seriesId,
                                            streamIcon = item.cover
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ),
                        shape = ExpressiveShapes.Medium,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.cover)
                                    .crossfade(300)
                                    .build(),
                                imageLoader = ImageConfig.getImageLoader(context),
                                contentDescription = item.name ?: "",
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                modifier = Modifier.fillMaxSize()
                            )
                            

                            // Top Series Pill Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(ExpressiveShapes.Full)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SERIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            // Title & Info at Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item.name ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.basicMarquee()
                                )
                                item.year?.let { yr ->
                                    Text(
                                        text = yr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveStreamContentRow(
    title: String,
    items: List<LiveStream>,
    onSeeAllClick: () -> Unit,
    key: ((LiveStream) -> Any)? = { it.streamId ?: 0 },
    onItemClick: (LiveStream) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = ExpressiveAnimations.enter()) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = ExpressiveAnimations.enter()
        ),
        exit = fadeOut(animationSpec = ExpressiveAnimations.exit())
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(androidx.compose.ui.graphics.Color.Red)
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onSeeAllClick) { 
                    Text(
                        stringResource(R.string.action_see_all),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.streamId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(200.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                ExpressiveShapes.Medium
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = { onItemClick(item) },
                                onLongClick = {
                                    scope.launch {
                                        val added = FavoritesManager.toggleFavorite(
                                            contentId = item.streamId ?: 0,
                                            contentType = "live",
                                            name = item.name ?: "",
                                            thumbnailUrl = item.streamIcon,
                                            categoryId = item.categoryId,
                                            streamIcon = item.streamIcon
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            if (added) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ),
                        shape = ExpressiveShapes.Medium,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.streamIcon)
                                    .crossfade(300)
                                    .build(),
                                imageLoader = ImageConfig.getImageLoader(context),
                                contentDescription = item.name ?: "",
                                contentScale = ContentScale.Fit,
                                error = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_placeholder),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(12.dp)
                            )
                            
                            // Top LIVE Badge Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(ExpressiveShapes.Full)
                                    .background(androidx.compose.ui.graphics.Color(0xFFD32F2F))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(androidx.compose.ui.graphics.Color.White)
                                    )
                                    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                            
                            // Bottom Title Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.name ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.basicMarquee()
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
fun ManageCategoriesBottomSheet(
    viewModel: HomeViewModel,
    selectedTab: Int = 0,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTab by remember { mutableIntStateOf(selectedTab) }
    
    val categoryList = remember(currentTab, viewModel.moviesByCategoriesState, viewModel.seriesByCategoriesState, viewModel.liveByCategoriesState) {
        val activeCategories = when (currentTab) {
            0 -> (viewModel.moviesByCategoriesState as? UiState.Success)?.data?.keys?.toList()
            1 -> (viewModel.seriesByCategoriesState as? UiState.Success)?.data?.keys?.toList()
            else -> (viewModel.liveByCategoriesState as? UiState.Success)?.data?.keys?.toList()
        }
        if (!activeCategories.isNullOrEmpty()) {
            activeCategories
        } else {
            when (currentTab) {
                0 -> viewModel.movieCategories
                1 -> viewModel.seriesCategories
                else -> viewModel.liveCategories
            }
        }
    }

    val hiddenSet = when (currentTab) {
        0 -> viewModel.hiddenCategoryIdsMovies
        1 -> viewModel.hiddenCategoryIdsSeries
        else -> viewModel.hiddenCategoryIdsLive
    }

    val customOrder = when (currentTab) {
        0 -> viewModel.orderedCategoryIdsMovies
        1 -> viewModel.orderedCategoryIdsSeries
        else -> viewModel.orderedCategoryIdsLive
    }

    val sortedCategories = remember(categoryList, customOrder) {
        if (customOrder.isEmpty()) {
            categoryList.sortedBy { it.categoryName?.lowercase() ?: "" }
        } else {
            val orderMap = customOrder.withIndex().associate { it.value to it.index }
            categoryList.sortedWith { a, b ->
                val posA = orderMap[a.categoryId ?: ""]
                val posB = orderMap[b.categoryId ?: ""]
                if (posA != null && posB != null) {
                    posA.compareTo(posB)
                } else if (posA != null) {
                    -1
                } else if (posB != null) {
                    1
                } else {
                    (a.categoryName ?: "").compareTo(b.categoryName ?: "", ignoreCase = true)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kategorileri Düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sıralamayı değiştirin veya gizleyin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { viewModel.resetCategoryPreferences(currentTab) }) {
                    Text("Sıfırla", color = MaterialTheme.colorScheme.primary)
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

            // Tab Selection Filter Chips inside BottomSheet
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Filmler", "Diziler", "Canlı TV").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = currentTab == idx,
                        onClick = { currentTab = idx },
                        label = { Text(label, fontWeight = if (currentTab == idx) FontWeight.Bold else FontWeight.Medium) },
                        shape = ExpressiveShapes.Full,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                itemsIndexed(sortedCategories, key = { _, cat -> cat.categoryId ?: "" }) { index, cat ->
                    val catId = cat.categoryId ?: ""
                    val isHidden = hiddenSet.contains(catId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.Medium)
                            .background(
                                if (isHidden) MaterialTheme.colorScheme.surfaceContainerLowest
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat.categoryName ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isHidden) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (isHidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveCategoryUp(selectedTab, sortedCategories, index) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Yukarı Taşımak",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.moveCategoryDown(selectedTab, sortedCategories, index) },
                                enabled = index < sortedCategories.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Aşağı Taşımak",
                                    tint = if (index < sortedCategories.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            IconButton(onClick = { viewModel.toggleCategoryVisibility(selectedTab, catId) }) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Gizle/Göster",
                                    tint = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        }
    }
}
