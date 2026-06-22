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
    object Detail : Screen("detail/{streamId}/{title}") {
        fun createRoute(streamId: Int, title: String) = "detail/$streamId/$title"
    }
    object SeriesDetail : Screen("seriesDetail/{seriesId}/{title}") {
        fun createRoute(seriesId: Int, title: String) = "seriesDetail/$seriesId/$title"
    }
    object Player : Screen("player/{streamId}/{streamType}") {
        fun createRoute(streamId: Int, streamType: String) = "player/$streamId/$streamType"
    }
    object Search : Screen("search")
    object Favorites : Screen("favorites")
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
        composable(Screen.Detail.route) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId")?.toIntOrNull() ?: -1
            val title = backStackEntry.arguments?.getString("title") ?: ""
            com.hasanege.materialtv.MovieDetailScreenRoute(
                streamId = streamId,
                initialTitle = title,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { url, t, sId, _, pos ->
                    android.util.Log.d("AppNavigation", "Navigate to Player: $url")
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("TITLE", t)
                        putExtra("STREAM_ID", sId)
                        putExtra("AUTO_PLAY", true)
                        putExtra("position", pos)
                    }
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.SeriesDetail.route) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId")?.toIntOrNull() ?: -1
            val title = backStackEntry.arguments?.getString("title") ?: ""
            com.hasanege.materialtv.SeriesDetailScreenRoute(
                seriesId = seriesId,
                initialTitle = title,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { url, t, eId, sId, pos ->
                    android.util.Log.d("AppNavigation", "Navigate to Player: $url")
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("TITLE", t)
                        putExtra("STREAM_ID", eId)
                        putExtra("SERIES_ID", sId)
                        putExtra("AUTO_PLAY", true)
                        putExtra("position", pos)
                    }
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.Player.route) { backStackEntry ->
            // Temporary stub until we implement PlayerScreen
            androidx.compose.material3.Text("Player Screen stub")
        }
    }
    }
}
