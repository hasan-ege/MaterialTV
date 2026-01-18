package com.hasanege.materialtv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hasanege.materialtv.model.ContinueWatchingItem
import com.hasanege.materialtv.ui.utils.ImageConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onItemClick: (ContinueWatchingItem) -> Unit,
    onPin: (ContinueWatchingItem) -> Unit,
    onRemove: (ContinueWatchingItem) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ContinueWatchingItem?>(null) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.continue_watching_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        val carouselState = androidx.compose.material3.carousel.rememberCarouselState { items.size }
        
        androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 180.dp,
            itemSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            val item = items[index]
            // Premium spring physics for Google-like alive feel
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "card_scale"
            )

            androidx.compose.material3.ElevatedCard(
                modifier = Modifier
                    .maskClip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium)
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
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(),
                        onClick = { onItemClick(item) },
                        onLongClick = {
                            selectedItem = item
                            showMenu = true
                        }
                    ),
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Column {
                    Box {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.streamIcon)
                                .crossfade(300)
                                .build(),
                            imageLoader = ImageConfig.getImageLoader(LocalContext.current),
                            contentDescription = item.name,
                            contentScale = if (item.type == "live") ContentScale.Fit else ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                        if (item.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        androidx.compose.material3.LinearWavyProgressIndicator(
                            progress = { item.position.toFloat() / item.duration.toFloat().coerceAtLeast(1f) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(4.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            color = MaterialTheme.colorScheme.primary,
                            amplitude = { 0.5f }
                        )
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            minLines = 2
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (selectedItem?.isPinned == true) stringResource(R.string.continue_watching_unpin) else stringResource(R.string.continue_watching_pin)) },
                onClick = {
                    selectedItem?.let(onPin)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.favorites_add)) },
                onClick = {
                    selectedItem?.let { item ->
                        scope.launch {
                            val added = com.hasanege.materialtv.FavoritesManager.toggleFavorite(
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
                                    if (added) "Added to favorites" else "Removed from favorites",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.continue_watching_remove)) },
                onClick = {
                    selectedItem?.let(onRemove)
                    showMenu = false
                }
            )
        }
    }
}
