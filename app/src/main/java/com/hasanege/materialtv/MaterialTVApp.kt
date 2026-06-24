
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.hasanege.materialtv.ui.ExpressiveTabSlider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.FilterChip
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


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
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

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name_material),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_name_tv),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } 
                },
                actions = {
                    // Icons moved to floating islands in HomeScreen
                },
                scrollBehavior = scrollBehavior,
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.padding(paddingValues)
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
                    if (navBarStyle == "floating") {
                        com.hasanege.materialtv.ui.MaterialTVBottomNavBar(
                            items = bottomNavItems,
                            currentItemRoute = currentScreen.value,
                            onItemClick = { currentScreen.value = it.route },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    } else {
                        com.hasanege.materialtv.ui.DefaultBottomNavBar(
                            items = bottomNavItems,
                            currentItemRoute = currentScreen.value,
                            onItemClick = { currentScreen.value = it.route },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
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
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
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
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(if (configuration.screenWidthDp < 360) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionIsland(
                icon = Icons.Default.Cast,
                contentDescription = stringResource(R.string.action_cast),
                onClick = { 
                    context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                }
            )
            
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
            .padding(horizontal = 4.dp)
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
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.loadInitialData(SessionManager.username ?: "", SessionManager.password ?: "", true) })

    Box(modifier = Modifier.pullRefresh(pullRefreshState), contentAlignment = Alignment.TopCenter) {
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
                // Continue Watching Section (Standard list item)
                item {
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

                // Categories based on selected tab
                when (selectedTab) {
                    0 -> {
                        when (val moviesByCategoriesState = viewModel.moviesByCategoriesState) {
                            is UiState.Loading -> item { CenteredProgressBar() }
                            is UiState.Success -> {
                                // Hero Carousel with featured movies
                                val allMovies = moviesByCategoriesState.data.values.flatten()
                                if (allMovies.isNotEmpty()) {
                                    item(key = "hero_carousel") {
                                        HeroCarousel(
                                            items = allMovies.shuffled(),
                                            key = { item -> item.streamId ?: 0 },
                                            imageUrlProvider = { it.streamIcon },
                                            titleProvider = { it.name },
                                            subtitleProvider = { it.year },
                                            onItemClick = { vodItem ->
                                                navController.navigate(Screen.Detail.createRoute(vodItem.streamId ?: -1, vodItem.name ?: ""))
                                            }
                                        )
                                    }
                                }

                                items(
                                    items = moviesByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
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
                                // Hero Carousel with featured series
                                val allSeries = seriesByCategoriesState.data.values.flatten()
                                if (allSeries.isNotEmpty()) {
                                    item(key = "hero_carousel_series") {
                                        HeroCarousel(
                                            items = allSeries.shuffled(),
                                            key = { item -> item.seriesId ?: 0 },
                                            imageUrlProvider = { it.cover },
                                            titleProvider = { it.name },
                                            subtitleProvider = { it.year },
                                            onItemClick = { seriesItem ->
                                                navController.navigate(Screen.SeriesDetail.createRoute(seriesItem.seriesId ?: -1, seriesItem.name ?: ""))
                                            }
                                        )
                                    }
                                }

                                items(
                                    items = seriesByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
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
                                items(
                                    items = liveByCategoriesState.data.entries.filter { it.value.isNotEmpty() }.toList(),
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
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CategoryChips(viewModel: HomeViewModel, selectedTab: Int) {
    val (categories, selectedCategoryId, onCategorySelected) = when (selectedTab) {
        0 -> Triple(viewModel.movieCategories, viewModel.selectedMovieCategoryId, viewModel::onMovieCategorySelected)
        1 -> Triple(viewModel.seriesCategories, viewModel.selectedSeriesCategoryId, viewModel::onSeriesCategorySelected)
        else -> Triple(viewModel.liveCategories, viewModel.selectedLiveCategoryId, viewModel::onLiveCategorySelected)
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.category_all)) },
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = category.categoryId == selectedCategoryId,
                onClick = { onCategorySelected(category.categoryId) },
                label = { Text(category.categoryName) },
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium
            )
        }
    }
}


// Netflix-style Hero Carousel for featured content
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T> HeroCarousel(
    items: List<T>,
    onItemClick: (T) -> Unit,
    key: ((T) -> Any)? = null, // Added key provider support
    imageUrlProvider: (T) -> String?,
    titleProvider: (T) -> String?,
    subtitleProvider: (T) -> String? = { null },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    
    // Unlimited items, shuffled
    val heroCarouselState = rememberCarouselState { items.size }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val heroHeight = remember(configuration) { 
        if (configuration.screenWidthDp > 600) 400.dp else 280.dp 
    }
    
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.home_featured),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        HorizontalMultiBrowseCarousel(
            state = heroCarouselState,
            preferredItemWidth = configuration.screenWidthDp.dp - 48.dp,
            itemSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
        ) { index ->
            val item = items[index]
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ExpressiveShapes.ExtraLarge)
                    .clickable { onItemClick(item) }
            ) {
                // Background Image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrlProvider(item))
                        .crossfade(500)
                        .build(),
                    imageLoader = ImageConfig.getImageLoader(LocalContext.current),
                    contentDescription = titleProvider(item),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = titleProvider(item) ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val subtitle = subtitleProvider(item)
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { onItemClick(item) },
                            shape = ExpressiveShapes.Large,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_play))
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
    key: ((VodItem) -> Any)? = { it.streamId ?: 0 }, // Key before action
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
        Column(modifier = Modifier.padding(vertical = 20.dp)) { 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), 
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
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items, key = { it.streamId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(150.dp)
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
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val isTablet = configuration.screenWidthDp > 600
                            val cardAspectRatio = remember(isTablet) { if (isTablet) 3f/4f else 2f/3f }
                            
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(cardAspectRatio)
                            )
                            Text(
                                item.name ?: "",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                minLines = 2
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
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items, key = { it.seriesId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(150.dp)
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
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val isTablet = configuration.screenWidthDp > 600
                            val cardAspectRatio = remember(isTablet) { if (isTablet) 3f/4f else 2f/3f }
                            
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(cardAspectRatio)
                            )
                            Text(
                                item.name ?: "",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                minLines = 2
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
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items, key = { it.streamId ?: 0 }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "card_scale"
                    )

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .width(150.dp)
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
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        ),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column {
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
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            Text(
                                item.name ?: "",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                minLines = 2
                            )
                        }
                    }
                }
            }
            }
        }
    }


