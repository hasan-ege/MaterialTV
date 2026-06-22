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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, categoryName: String) {
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
            when (val state = viewModel.uiState.value) {
                is UiState.Loading -> {
                    CenteredProgressBar()
                }
                is UiState.Success -> {
                    when (state.data) {
                        is CategoryData.Movies -> MoviesList(
                            movies = state.data.items
                        )
                        is CategoryData.Series -> SeriesList(
                            series = state.data.items
                        )
                        is CategoryData.LiveStreams -> LiveTVList(
                            liveStreams = state.data.items
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorMessage(message = state.message)
                }
            }
        }
    }
}
