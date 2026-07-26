
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.graphics.toArgb
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

    // Sync system navigation bar color with app nav bar style.
    val view = androidx.compose.ui.platform.LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            if (navBarStyle == "bottom") {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightNavigationBars = false
            } else {
                // Floating pill or rail: solid black
                window.navigationBarColor = android.graphics.Color.BLACK
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightNavigationBars = false
            }
        }
    }

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
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                    end = paddingValues.calculateRightPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
                )
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
                                        HomeScreen(
                                            homeViewModel = homeViewModel, 
                                            initialTabIndex = initialTabIndex, 
                                            isSearchExpanded = isSearchExpanded,
                                            searchQuery = searchQuery,
                                            onQueryChange = { searchQuery = it },
                                            onSearchExpandedChange = { isSearchExpanded = it }
                                        )
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
                                        HomeScreen(
                                            homeViewModel = homeViewModel, 
                                            initialTabIndex = initialTabIndex, 
                                            isSearchExpanded = isSearchExpanded,
                                            searchQuery = searchQuery,
                                            onQueryChange = { searchQuery = it },
                                            onSearchExpandedChange = { isSearchExpanded = it }
                                        )
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 88.dp, bottom = 16.dp)
            ) {
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
fun HomeScreen(
    homeViewModel: HomeViewModel, 
    initialTabIndex: Int = 0, 
    isSearchExpanded: Boolean = false,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    onSearchExpandedChange: (Boolean) -> Unit = {},
    onCastClick: () -> Unit = {}
) {
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
    
    val animateToTab: (Int) -> Unit = { targetIndex ->
        selectedTabIndex = targetIndex
        scope.launch {
            pagerState.animateScrollToPage(
                page = targetIndex,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            )
        }
    }

    // Sync Pager scroll with Tab selection when swipe completes
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (!pagerState.isScrollInProgress && selectedTabIndex != settledPage) {
                selectedTabIndex = settledPage
            }
        }
    }

    var initialSearchTabIndex by remember { mutableIntStateOf(initialTabIndex) }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            initialSearchTabIndex = selectedTabIndex
        } else {
            animateToTab(initialSearchTabIndex)
        }
    }

    val moviesState = homeViewModel.moviesByCategoriesState
    val seriesState = homeViewModel.seriesByCategoriesState
    val liveState = homeViewModel.liveByCategoriesState

    LaunchedEffect(searchQuery, isSearchExpanded, moviesState, seriesState, liveState) {
        if (isSearchExpanded && searchQuery.isNotBlank()) {
            val hasMovies = (moviesState as? UiState.Success)?.data?.values?.flatten()
                ?.any { it.name?.contains(searchQuery, ignoreCase = true) == true } == true

            val hasSeries = (seriesState as? UiState.Success)?.data?.values?.flatten()
                ?.any { it.name?.contains(searchQuery, ignoreCase = true) == true } == true

            val hasLive = (liveState as? UiState.Success)?.data?.values?.flatten()
                ?.any { it.name?.contains(searchQuery, ignoreCase = true) == true } == true

            val targetTab = when {
                hasMovies -> 0
                hasSeries -> 1
                hasLive -> 2
                else -> initialSearchTabIndex
            }
            if (selectedTabIndex != targetTab) {
                animateToTab(targetTab)
            }
        } else if (isSearchExpanded && searchQuery.isBlank()) {
            if (selectedTabIndex != initialSearchTabIndex) {
                animateToTab(initialSearchTabIndex)
            }
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
                    contentPadding = PaddingValues(top = tabHeightDp, bottom = 100.dp),
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = searchQuery
                )
            }
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 12.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(if (configuration.screenWidthDp < 360) 4.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = isSearchExpanded,
                    label = "TopBarSearchAnim"
                ) { expanded ->
                    if (expanded) {
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 4.dp,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
                                ),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        onQueryChange("")
                                        onSearchExpandedChange(false) 
                                    },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = MaterialTheme.colorScheme.onSurface)
                                }
                                androidx.compose.material3.TextField(
                                    value = searchQuery,
                                    onValueChange = onQueryChange,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.search_field_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                    singleLine = true
                                )
                                androidx.compose.animation.AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .width(56.dp)
                                        .shadow(
                                            elevation = 4.dp,
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                            spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
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
                                onTabSelected = { index -> animateToTab(index) },
                                modifier = Modifier.weight(1f),
                                scrollable = false
                            )
                            
                            FloatingActionIsland(
                                icon = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                onClick = { onSearchExpandedChange(true) }
                            )
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = isSearchExpanded) {
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    ExpressiveTabSlider(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { index -> animateToTab(index) },
                        scrollable = false
                    )
                }
            }
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
                elevation = 4.dp,
                shape = androidx.compose.foundation.shape.CircleShape,
                ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
            )
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isSearchExpanded: Boolean = false,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    val navController = com.hasanege.materialtv.navigation.LocalNavController.current
    val isRefreshing = viewModel.isRefreshing
    val scope = rememberCoroutineScope()

    val onPinContinueItem = { item: ContinueWatchingItem ->
        viewModel.togglePin(item)
    }

    val onFavoriteContinueItem = { item: ContinueWatchingItem ->
        scope.launch {
            val wasAdded = com.hasanege.materialtv.FavoritesManager.toggleFavorite(
                contentId = item.streamId,
                contentType = item.type,
                name = item.name,
                thumbnailUrl = item.streamIcon,
                seriesId = item.seriesId,
                streamIcon = item.streamIcon
            )
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    if (wasAdded) context.getString(R.string.favorites_added) else context.getString(R.string.favorites_removed),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        Unit
    }

    val favoritesList by com.hasanege.materialtv.FavoritesManager.favoritesFlow.collectAsState()
    val isFavoriteContinueItem = { item: ContinueWatchingItem ->
        favoritesList.any { it.contentId == item.streamId && it.contentType == item.type }
    }

    val strMovieUpper = stringResource(R.string.content_type_movie_upper)
    val strSeriesUpper = stringResource(R.string.content_type_series_upper)
    val strLiveUpper = stringResource(R.string.content_type_live_upper)

    val getSubtitleForContinueItem = { item: ContinueWatchingItem ->
        when (item.type) {
            "movie" -> strMovieUpper
            "series" -> strSeriesUpper
            "live" -> strLiveUpper
            else -> item.type?.uppercase()
        }
    }

    val onRemoveContinueItem = { item: ContinueWatchingItem ->
        viewModel.removeFromContinueWatching(item)
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true) },
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 76.dp)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp
            val adaptivePadding = if (isWide) 32.dp else 0.dp
            
            val continueWatchingState by viewModel.continueWatchingState.collectAsState()
            val continueWatchingList = (continueWatchingState as? UiState.Success)?.data ?: emptyList()

            if (isSearchExpanded && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = adaptivePadding)
                        .padding(top = if (isSearchExpanded) 60.dp else 0.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            val state = viewModel.moviesByCategoriesState
                            if (state is UiState.Success) {
                                val searchResults = state.data.values.flatten()
                                    .filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
                                    .distinctBy { it.streamId }
                                if (searchResults.isEmpty()) com.hasanege.materialtv.ui.NoResultsFound()
                                else com.hasanege.materialtv.ui.MoviesList(searchResults)
                            } else {
                                CenteredProgressBar()
                            }
                        }
                        1 -> {
                            val state = viewModel.seriesByCategoriesState
                            if (state is UiState.Success) {
                                val searchResults = state.data.values.flatten()
                                    .filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
                                    .distinctBy { it.seriesId }
                                if (searchResults.isEmpty()) com.hasanege.materialtv.ui.NoResultsFound()
                                else com.hasanege.materialtv.ui.SeriesList(searchResults)
                            } else {
                                CenteredProgressBar()
                            }
                        }
                        2 -> {
                            val state = viewModel.liveByCategoriesState
                            if (state is UiState.Success) {
                                val searchResults = state.data.values.flatten()
                                    .filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
                                    .distinctBy { it.streamId }
                                if (searchResults.isEmpty()) com.hasanege.materialtv.ui.NoResultsFound()
                                else com.hasanege.materialtv.ui.LiveTVList(searchResults)
                            } else {
                                CenteredProgressBar()
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 1200.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = adaptivePadding),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                ) {
                    // 1. Tab Specific Content (Movies, Series, Live TV)
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

                                // Hero Banner: "İzlemeye DEVAM ET" (Only render item if continueWatchingList is not empty)
                                if (continueWatchingList.isNotEmpty()) {
                                    item(key = "hero_carousel_movies") {
                                        HeroCarousel(
                                            items = continueWatchingList,
                                            key = { item -> item.streamId },
                                            imageUrlProvider = { it.streamIcon },
                                            titleProvider = { it.name },
                                            subtitleProvider = getSubtitleForContinueItem,
                                            isRandom = false,
                                            onRerollClick = {
                                                viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true)
                                            },
                                            onItemClick = { item ->
                                                launchContinueWatchingItem(context, navController, item)
                                            },
                                            onPinClick = onPinContinueItem,
                                            onFavoriteClick = onFavoriteContinueItem,
                                            onRemoveClick = onRemoveContinueItem,
                                            isFavoriteProvider = isFavoriteContinueItem
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

                                // Hero Banner: "İzlemeye DEVAM ET" (Only render item if continueWatchingList is not empty)
                                if (continueWatchingList.isNotEmpty()) {
                                    item(key = "hero_carousel_series") {
                                        HeroCarousel(
                                            items = continueWatchingList,
                                            key = { item -> item.streamId },
                                            imageUrlProvider = { it.streamIcon },
                                            titleProvider = { it.name },
                                            subtitleProvider = getSubtitleForContinueItem,
                                            isRandom = false,
                                            onRerollClick = {
                                                viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true)
                                            },
                                            onItemClick = { item ->
                                                launchContinueWatchingItem(context, navController, item)
                                            },
                                            onPinClick = onPinContinueItem,
                                            onFavoriteClick = onFavoriteContinueItem,
                                            onRemoveClick = onRemoveContinueItem,
                                            isFavoriteProvider = isFavoriteContinueItem
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

                                // Hero Banner: "İzlemeye DEVAM ET" for Live TV tab (Only render item if continueWatchingList is not empty)
                                if (continueWatchingList.isNotEmpty()) {
                                    item(key = "hero_carousel_live") {
                                        HeroCarousel(
                                            items = continueWatchingList,
                                            key = { item -> item.streamId },
                                            imageUrlProvider = { it.streamIcon },
                                            titleProvider = { it.name },
                                            subtitleProvider = getSubtitleForContinueItem,
                                            isRandom = false,
                                            onRerollClick = {
                                                viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true)
                                            },
                                            onItemClick = { item ->
                                                launchContinueWatchingItem(context, navController, item)
                                            },
                                            onPinClick = onPinContinueItem,
                                            onFavoriteClick = onFavoriteContinueItem,
                                            onRemoveClick = onRemoveContinueItem,
                                            isFavoriteProvider = isFavoriteContinueItem
                                        )
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
                                        navController.navigate(
                                            com.hasanege.materialtv.navigation.Screen.LiveDetail.createRoute(
                                                liveStream.streamId ?: -1,
                                                liveStream.name ?: "",
                                                liveStream.streamIcon ?: ""
                                            )
                                        )
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
private fun getDynamicCardSpec(index: Int, seed: Int): Pair<androidx.compose.ui.unit.DpSize, androidx.compose.ui.graphics.Shape> {
    val hash = kotlin.math.abs(seed * 31 + index * 17 + 7)
    
    // 4 size variations for dynamic Bento horizontal scroll
    val size = when (hash % 4) {
        0 -> androidx.compose.ui.unit.DpSize(210.dp, 145.dp) // Wide card
        1 -> androidx.compose.ui.unit.DpSize(145.dp, 165.dp) // Tall card
        2 -> androidx.compose.ui.unit.DpSize(160.dp, 150.dp) // Square-ish card
        else -> androidx.compose.ui.unit.DpSize(235.dp, 155.dp) // Extra wide card
    }
    
    // 6 organic Expressive shape variations
    val shape = when (hash % 6) {
        0 -> RoundedCornerShape(topStart = 44.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 44.dp)
        1 -> RoundedCornerShape(topStart = 16.dp, topEnd = 44.dp, bottomStart = 44.dp, bottomEnd = 16.dp)
        2 -> RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        3 -> RoundedCornerShape(24.dp)
        4 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 36.dp, bottomEnd = 36.dp)
        else -> androidx.compose.foundation.shape.CircleShape
    }
    
    return Pair(size, shape)
}

fun launchContinueWatchingItem(
    context: Context,
    navController: androidx.navigation.NavHostController,
    item: ContinueWatchingItem
) {
    if (item.type == "series") {
        navController.navigate(Screen.SeriesDetail.createRoute(item.seriesId ?: item.streamId, item.name, autoPlay = true))
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
                putExtra("STREAM_ICON", item.streamIcon)
                putExtra("TYPE", "live")
            }
            context.startActivity(intent)
        }
    } else {
        // Movies: Navigate to Movie Detail first, then autoPlay from saved position
        navController.navigate(Screen.Detail.createRoute(item.streamId, item.name, autoPlay = true))
    }
}

// Expressive Organic Collage Stage Showcase (Scrollable with dynamic Bento algorithm)
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
    isRandom: Boolean = false,
    onRerollClick: () -> Unit = {},
    onPinClick: ((T) -> Unit)? = null,
    onFavoriteClick: ((T) -> Unit)? = null,
    onRemoveClick: ((T) -> Unit)? = null,
    isFavoriteProvider: ((T) -> Boolean)? = null,
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

    val currentDisplayItems = remember(items, externalSeed, isRandom) {
        if (isRandom) {
            val random = java.util.Random(externalSeed.toLong())
            items.shuffled(random)
        } else {
            items
        }
    }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        // Expressive Top Title + Circular Refresh Icon FAB Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_continue),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.home_watching),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }

            // Circular Refresh Icon Button
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
                    contentDescription = "Yenile",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                )
            }
        }

        // Scrollable Dynamic Bento / Collage Showcase
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
        ) {
            itemsIndexed(
                items = currentDisplayItems,
                key = { index, item -> key?.invoke(item) ?: item.hashCode() }
            ) { index, item ->
                var showMenu by remember { mutableStateOf(false) }
                val posterUrl = imageUrlProvider(item)
                val title = titleProvider(item) ?: ""
                val subtitle = subtitleProvider(item)

                val itemSeed = key?.invoke(item)?.hashCode() ?: item.hashCode()
                val (cardSize, cardShape) = getDynamicCardSpec(index, itemSeed)

                val tiltAngle = remember(index, itemSeed) {
                    val angles = floatArrayOf(-4f, 3f, -2f, 5f, -3f, 4f)
                    angles[kotlin.math.abs(itemSeed + index) % angles.size]
                }

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "bento_card_scale"
                )

                val continueItem = item as? ContinueWatchingItem
                val isPinned = continueItem?.isPinned == true

                Box(
                    modifier = Modifier
                        .width(cardSize.width)
                        .height(cardSize.height)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = tiltAngle
                        }
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { onItemClick(item) },
                            onLongClick = if (onPinClick != null || onFavoriteClick != null || onRemoveClick != null) {
                                { showMenu = true }
                            } else null
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(300)
                            .build(),
                        imageLoader = ImageConfig.getImageLoader(context),
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
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), ExpressiveShapes.Small)
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }

                    // Title & Subtitle Badge Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        if (!subtitle.isNullOrBlank()) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = subtitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onPinClick != null) {
                            DropdownMenuItem(
                                text = { Text(if (isPinned) stringResource(R.string.continue_watching_unpin) else stringResource(R.string.continue_watching_pin)) },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                onClick = {
                                    onPinClick(item)
                                    showMenu = false
                                }
                            )
                        }
                        if (onFavoriteClick != null) {
                            val isFav = isFavoriteProvider?.invoke(item) == true
                            DropdownMenuItem(
                                text = { Text(stringResource(if (isFav) R.string.favorites_remove_action else R.string.favorites_add)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onFavoriteClick(item)
                                    showMenu = false
                                }
                            )
                        }
                        if (onRemoveClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.continue_watching_remove)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    onRemoveClick(item)
                                    showMenu = false
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
fun ContentRow(
    title: String,
    items: List<VodItem>,
    onSeeAllClick: () -> Unit,
    key: ((VodItem) -> Any)? = { it.streamId ?: 0 },
    onItemClick: (VodItem) -> Unit
) {
    if (items.isEmpty()) return
    
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
        Column(modifier = Modifier.padding(vertical = 12.dp)) { 
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
                    modifier = Modifier.weight(1f).basicMarquee(),
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
    if (items.isEmpty()) return
    
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
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
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
                    modifier = Modifier.weight(1f).basicMarquee(),
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
    if (items.isEmpty()) return
    
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
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                        modifier = Modifier.weight(1f).basicMarquee(),
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
                            .width(150.dp)
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
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.manage_categories_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.manage_categories_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { viewModel.resetCategoryPreferences(currentTab) }) {
                    Text(stringResource(R.string.manage_categories_reset), color = MaterialTheme.colorScheme.primary)
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

            // Tab Selection Filter Chips inside BottomSheet
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    stringResource(R.string.manage_categories_movies),
                    stringResource(R.string.manage_categories_series),
                    stringResource(R.string.manage_categories_live)
                ).forEachIndexed { idx, label ->
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
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.Medium)
                            .background(
                                if (isHidden) MaterialTheme.colorScheme.surfaceContainerLowest
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag handle icon for reordering by dragging up/down
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Sürükle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .pointerInput(catId, index) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { dragOffsetY = 0f },
                                        onDragCancel = { dragOffsetY = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount
                                            if (dragOffsetY > 40f && index < sortedCategories.size - 1) {
                                                viewModel.moveCategoryDown(currentTab, sortedCategories, index)
                                                dragOffsetY = 0f
                                            } else if (dragOffsetY < -40f && index > 0) {
                                                viewModel.moveCategoryUp(currentTab, sortedCategories, index)
                                                dragOffsetY = 0f
                                            }
                                        }
                                    )
                                }
                        )

                        Text(
                            text = cat.categoryName ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isHidden) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (isHidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveCategoryUp(currentTab, sortedCategories, index) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Yukarı Taşımak",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.moveCategoryDown(currentTab, sortedCategories, index) },
                                enabled = index < sortedCategories.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Aşağı Taşımak",
                                    tint = if (index < sortedCategories.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            IconButton(onClick = { viewModel.toggleCategoryVisibility(currentTab, catId) }) {
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
