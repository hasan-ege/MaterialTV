package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.*
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, categoryName: String) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(categoryName) })
        }
    ) { paddingValues ->
        androidx.compose.animation.AnimatedVisibility(
            visible = true, // Always visible, but triggers the animation
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            modifier = Modifier.padding(paddingValues)
        ) {
            when (val data = state) {
                null -> {
                    CenteredProgressBar()
                }
                is CategoryData.Movies -> {
                    val lazyPagingItems = data.items.collectAsLazyPagingItems()
                    PagedMoviesList(movies = lazyPagingItems)
                }
                is CategoryData.Series -> {
                    val lazyPagingItems = data.items.collectAsLazyPagingItems()
                    PagedSeriesList(series = lazyPagingItems)
                }
                is CategoryData.LiveStreams -> {
                    val lazyPagingItems = data.items.collectAsLazyPagingItems()
                    PagedLiveTVList(liveStreams = lazyPagingItems)
                }
            }
        }
    }
}
