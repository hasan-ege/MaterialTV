package com.hasanege.materialtv

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MainScreen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    object Home : MainScreen("home", R.string.nav_home, Icons.Default.Home)
    object Favorites : MainScreen("favorites", R.string.nav_favorites, Icons.Default.Favorite)
    object Downloads : MainScreen("downloads", R.string.nav_downloads, Icons.Default.Download)
    object Profile : MainScreen("profile", R.string.nav_profile, Icons.Default.Person)
}
