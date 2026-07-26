package com.hasanege.materialtv.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hasanege.materialtv.MainViewModel
import com.hasanege.materialtv.LoginScreen
import android.content.Intent
import com.hasanege.materialtv.PlayerActivity
import androidx.compose.ui.platform.LocalContext

val LocalNavController = androidx.compose.runtime.compositionLocalOf<NavHostController> {
    error("No NavController provided")
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Main : Screen("main")
    object Category : Screen("category/{categoryId}/{categoryType}/{categoryName}") {
        fun createRoute(categoryId: String, categoryType: String, categoryName: String) = "category/$categoryId/$categoryType/$categoryName"
    }
    object Detail : Screen("detail/{streamId}/{title}?autoPlay={autoPlay}") {
        fun createRoute(streamId: Int, title: String, autoPlay: Boolean = false) =
            "detail/$streamId/${java.net.URLEncoder.encode(title, "UTF-8")}?autoPlay=$autoPlay"
    }
    object SeriesDetail : Screen("seriesDetail/{seriesId}/{title}?autoPlay={autoPlay}") {
        fun createRoute(seriesId: Int, title: String, autoPlay: Boolean = false) =
            "seriesDetail/$seriesId/${java.net.URLEncoder.encode(title, "UTF-8")}?autoPlay=$autoPlay"
    }
    object Player : Screen("player/{streamId}/{streamType}") {
        fun createRoute(streamId: Int, streamType: String) = "player/$streamId/$streamType"
    }
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Levels : Screen("levels")
    object LiveDetail : Screen("liveDetail/{streamId}/{channelName}/{streamIcon}") {
        fun createRoute(streamId: Int, channelName: String, streamIcon: String?) =
            "liveDetail/$streamId/${java.net.URLEncoder.encode(channelName, "UTF-8")}/${java.net.URLEncoder.encode(streamIcon ?: "", "UTF-8")}"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    mainViewModel: MainViewModel,
    customName: String?,
    customAvatar: String?
) {
    val context = LocalContext.current
    androidx.compose.runtime.CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Splash.route) {
            // Placeholder or empty for now
        }
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = mainViewModel,
                customName = customName,
                customAvatar = customAvatar,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            com.hasanege.materialtv.MaterialTVApp()
        }
        composable(Screen.Category.route) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryType = backStackEntry.arguments?.getString("categoryType") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val viewModel: com.hasanege.materialtv.CategoryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            
            androidx.compose.runtime.LaunchedEffect(categoryId, categoryType) {
                if (categoryId.isNotEmpty() && categoryType.isNotEmpty()) {
                    viewModel.loadCategoryItems(categoryId, categoryType)
                }
            }
            com.hasanege.materialtv.CategoryScreen(viewModel = viewModel, categoryName = categoryName)
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                androidx.navigation.navArgument("streamId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("autoPlay") { type = androidx.navigation.NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId")?.toIntOrNull() ?: -1
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            com.hasanege.materialtv.MovieDetailScreenRoute(
                streamId = streamId,
                initialTitle = title,
                autoPlay = autoPlay,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { url, t, sId, _, pos, icon, imdbId, tmdbId ->
                    android.util.Log.d("AppNavigation", "Navigate to Player: ${com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(url)}")
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("TITLE", t)
                        putExtra("STREAM_ID", sId)
                        putExtra("STREAM_ICON", icon)
                        putExtra("AUTO_PLAY", true)
                        putExtra("position", pos)
                        if (!imdbId.isNullOrBlank()) putExtra("IMDB_ID", imdbId)
                        if (!tmdbId.isNullOrBlank()) putExtra("TMDB_ID", tmdbId)
                    }
                    context.startActivity(intent)
                }
            )
        }
        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("seriesId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("autoPlay") { type = androidx.navigation.NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId")?.toIntOrNull() ?: -1
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            com.hasanege.materialtv.SeriesDetailScreenRoute(
                seriesId = seriesId,
                initialTitle = title,
                autoPlay = autoPlay,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { url, t, eId, sId, pos, icon, imdbId, tmdbId, season, episode ->
                    android.util.Log.d("AppNavigation", "Navigate to Player for Series: $t, seriesId: $sId, episodeId: $eId")
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("TITLE", t)
                        putExtra("EPISODE_ID", eId.toString())
                        putExtra("SERIES_ID", sId)
                        putExtra("STREAM_ICON", icon)
                        putExtra("AUTO_PLAY", true)
                        putExtra("position", pos)
                        if (!imdbId.isNullOrBlank()) putExtra("IMDB_ID", imdbId)
                        if (!tmdbId.isNullOrBlank()) putExtra("TMDB_ID", tmdbId)
                        if (season != null && season > 0) putExtra("SEASON", season)
                        if (episode != null && episode > 0) putExtra("EPISODE", episode)
                    }
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.Player.route) { backStackEntry ->
            // Temporary stub until we implement PlayerScreen
            androidx.compose.material3.Text("Player Screen stub")
        }
        composable(Screen.Levels.route) {
            com.hasanege.materialtv.ui.screens.levels.LevelsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LiveDetail.route) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId")?.toIntOrNull() ?: -1
            val channelName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("channelName") ?: "", "UTF-8"
            )
            val streamIcon = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("streamIcon") ?: "", "UTF-8"
            ).takeIf { it.isNotBlank() }
            com.hasanege.materialtv.LiveDetailScreenRoute(
                streamId = streamId,
                channelName = channelName,
                streamIcon = streamIcon,
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}
