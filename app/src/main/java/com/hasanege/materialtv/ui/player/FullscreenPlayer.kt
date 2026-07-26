package com.hasanege.materialtv.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.hasanege.materialtv.R
import com.hasanege.materialtv.player.PlayerEngine
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@UnstableApi
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun FullscreenPlayer(
    engine: PlayerEngine,
    title: String?,
    showStats: Boolean,
    inPipMode: Boolean = false,
    nextEpisodeThresholdMinutes: Int = 5,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwitchEngine: () -> Unit,
    isLiveStream: Boolean = false,
    onShowEpg: (() -> Unit)? = null,
    skipDbSegments: com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer? = null,
    imdbId: String? = null,
    tmdbId: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    openSubtitlesRepository: com.hasanege.materialtv.repository.OpenSubtitlesRepository? = null,
    episodes: List<com.hasanege.materialtv.model.Episode>? = null,
    currentEpisode: com.hasanege.materialtv.model.Episode? = null,
    onSelectEpisode: ((com.hasanege.materialtv.model.Episode) -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    contentType: com.hasanege.materialtv.model.PlaybackContentType = when {
        isLiveStream -> com.hasanege.materialtv.model.PlaybackContentType.LIVE_TV
        currentEpisode != null || !episodes.isNullOrEmpty() || onSelectEpisode != null -> com.hasanege.materialtv.model.PlaybackContentType.SERIES
        else -> com.hasanege.materialtv.model.PlaybackContentType.MOVIE
    }
) {
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window

    // NORMAL MODE: Full UI with controls
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showTrackSelectionDialog by remember { mutableStateOf(false) }
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    // New Features State
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLocked by remember { mutableStateOf(false) }
    var doubleTapState by remember { mutableStateOf<DoubleTapState?>(null) } // Left or Right
    var isPlaybackSpeedActive by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Slider state
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var currentManualSpeed by remember { mutableStateOf(1.0f) }

    // Seeking optimization state
    var seekTargetAccumulated by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    // Gesture control states
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var isVolumeGesture by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(0f) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(engine) {
        while(true) {
            isPlaying = engine.isPlaying()
            duration = engine.getDuration()
            currentPosition = engine.getCurrentPosition()
            
            // Only update slider if not seeking
            if (!isSeeking) {
                sliderValue = currentPosition.toFloat()
            }
            
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000L)
            controlsVisible = false
        }
    }

    // Hide indicator after a delay
    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1000L)
            showGestureIndicator = false
        }
    }
    
    // Hide double tap animation
    LaunchedEffect(doubleTapState, lastSeekTime) {
        if (doubleTapState != null) {
            delay(600L)
            doubleTapState = null
        }
    }

    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        if (!isLocked) controlsVisible = !controlsVisible 
                        else if (controlsVisible) controlsVisible = false // Allow hiding if stuck
                        else {
                             controlsVisible = true
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            val currentTime = System.currentTimeMillis()
                            
                            // Reset accumulation if it's been a while since last double tap
                            if (currentTime - lastSeekTime > 800) {
                                seekTargetAccumulated = engine.getCurrentPosition()
                            }
                            
                            if (offset.x < screenWidth / 2) {
                                // Rewind
                                seekTargetAccumulated = (seekTargetAccumulated - 10000).coerceAtLeast(0)
                                engine.seekTo(seekTargetAccumulated)
                                doubleTapState = DoubleTapState.Rewind
                            } else {
                                // Forward
                                seekTargetAccumulated = (seekTargetAccumulated + 10000).coerceAtMost(engine.getDuration())
                                engine.seekTo(seekTargetAccumulated)
                                doubleTapState = DoubleTapState.Forward
                            }
                            lastSeekTime = currentTime
                        }
                    },
                    onLongPress = {
                        if (!isLocked) {
                            isPlaybackSpeedActive = true
                            engine.setPlaybackSpeed(2.0f)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
            .pointerInput(isPlaybackSpeedActive) {
                if (isPlaybackSpeedActive) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                isPlaybackSpeedActive = false
                                engine.setPlaybackSpeed(currentManualSpeed)
                                break
                            }
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            val screenWidth = size.width
                            isVolumeGesture = it.x > screenWidth / 2
                            showGestureIndicator = true
                            if (isVolumeGesture) {
                                currentVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            } else {
                                currentBrightness =
                                    window.attributes.screenBrightness.takeIf { br -> br > 0 } ?: 0.5f
                            }
                        },
                        onDragEnd = {
                            showGestureIndicator = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        val screenHeight = size.height

                        if (abs(x) > abs(y)) return@detectDragGestures // Ignore horizontal drags

                        if (isVolumeGesture) {
                            val delta = (-y / screenHeight) * maxVolume
                            currentVolume = (currentVolume + delta).coerceIn(0f, maxVolume.toFloat())
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                currentVolume.toInt(),
                                0
                            )

                            gestureIcon = Icons.AutoMirrored.Filled.VolumeUp
                            gestureValue = (currentVolume / maxVolume) * 100
                        } else { // Brightness
                            val delta = -y / screenHeight
                            currentBrightness = (currentBrightness + delta).coerceIn(0f, 1f)

                            window.attributes =
                                window.attributes.apply { screenBrightness = currentBrightness }

                            gestureIcon = Icons.Default.BrightnessMedium
                            gestureValue = currentBrightness * 100
                        }
                    }
                }
            })
        {
            key(inPipMode) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            engine.attach(this)
                        }
                    },
                    update = { view ->
                        view.requestLayout()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Double Tap Animation Overlay
            doubleTapState?.let { state ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = if (state == DoubleTapState.Rewind) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                     Column(
                         modifier = Modifier.padding(50.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                         Icon(
                             imageVector = if (state == DoubleTapState.Rewind) Icons.Default.Replay10 else Icons.Default.Forward10,
                             contentDescription = null,
                             tint = Color.White,
                             modifier = Modifier.size(50.dp)
                         )
                         Text(stringResource(R.string.player_10s), color = Color.White, fontWeight = FontWeight.Bold)
                     }
                }
            }
            
            // 2x Speed Indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = isPlaybackSpeedActive,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                androidx.compose.material3.Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                             imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "2X",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !inPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.4f))) {
                    
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         title?.let {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             // Audio Track Button
                             IconButton(onClick = { showAudioDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.GraphicEq,
                                     contentDescription = "Audio Track",
                                     tint = Color.White
                                 )
                             }
                             
                             // Subtitle Button
                             IconButton(onClick = { showTrackSelectionDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.Subtitles,
                                     contentDescription = "Subtitles",
                                     tint = Color.White
                                 )
                             }
                             
                              // EPG / Episode Selection Button
                              android.util.Log.d("EpisodesBtn", "contentType=$contentType, episodes=${episodes?.size}, currentEpisode=${currentEpisode?.id}, onSelectEpisode=${onSelectEpisode != null}")
                              when (contentType) {
                                  com.hasanege.materialtv.model.PlaybackContentType.LIVE_TV -> {
                                      IconButton(onClick = { onShowEpg?.invoke() }) {
                                          Icon(
                                              imageVector = Icons.AutoMirrored.Filled.List,
                                              contentDescription = "Yayın Akışı (EPG)",
                                              tint = Color.White
                                          )
                                      }
                                  }
                                  com.hasanege.materialtv.model.PlaybackContentType.SERIES -> {
                                      IconButton(onClick = { 
                                          android.util.Log.d("EpisodesBtn", "Button clicked! showEpisodesDialog = true")
                                          showEpisodesDialog = true 
                                      }) {
                                          Icon(
                                              imageVector = Icons.AutoMirrored.Filled.List,
                                              contentDescription = "Bölüm Listesi",
                                              tint = Color.White
                                          )
                                      }
                                  }
                                  com.hasanege.materialtv.model.PlaybackContentType.MOVIE -> {
                                      // Hidden
                                  }
                              }

                             // PiP Button
                             IconButton(onClick = { onEnterPip?.invoke() }) {
                                 Icon(
                                     imageVector = Icons.Default.PictureInPictureAlt,
                                     contentDescription = "Resim İçinde Resim (PiP)",
                                     tint = Color.White
                                 )
                             }

                             // Screen Rotation Button
                             IconButton(onClick = {
                                 val currentOrientation = activity.requestedOrientation
                                 activity.requestedOrientation = if (
                                     currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                     currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                                     currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                 ) {
                                     android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                 } else {
                                     android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                 }
                             }) {
                                 Icon(
                                     imageVector = Icons.Default.ScreenRotation,
                                     contentDescription = "Ekranı Döndür",
                                     tint = Color.White
                                 )
                             }

                             // Lock Button
                             IconButton(onClick = { isLocked = !isLocked }) {
                                 Icon(
                                     imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                     contentDescription = "Lock",
                                     tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White
                                 )
                             }
                        }

                        }

                    if (!isLocked) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { engine.seekBack() }) {
                                Icon(
                                    modifier = Modifier.size(48.dp),
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Replay 10 seconds",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = { if (isPlaying) engine.pause() else engine.play() }) {
                                Icon(
                                    modifier = Modifier.size(64.dp),
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = { engine.seekForward() }) {
                                Icon(
                                    modifier = Modifier.size(48.dp),
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.width(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = formatDuration(sliderValue.toLong()), color = Color.White)
                                }
                                if (isBuffering) {
                                    LinearWavyProgressIndicator(
                                        modifier = Modifier.weight(1f).height(10.dp),
                                        waveSpeed = 100.dp
                                    )
                                } else {
                                    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                        val sliderWidth = maxWidth
                                        val progress = if (duration > 0) sliderValue / duration.toFloat() else 0f
                                        
                                        // Wavy Progress Track
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .fillMaxWidth()
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                        ) {
                                            LinearWavyProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                waveSpeed = if (isPlaying) 80.dp else 0.dp
                                            )
                                            
                                            // SkipDB Segments Overlay
                                            if (skipDbSegments != null && duration > 0) {
                                                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                                val secondaryColor = MaterialTheme.colorScheme.secondary
                                                val errorColor = MaterialTheme.colorScheme.error
                                                
                                                Canvas(modifier = Modifier.matchParentSize()) {
                                                    val w = size.width
                                                    val drawSegment = { startMs: Long, endMs: Long, color: Color ->
                                                        val startX = (startMs.toFloat() / duration.toFloat()) * w
                                                        val endX = (endMs.toFloat() / duration.toFloat()) * w
                                                        drawRect(
                                                            color = color,
                                                            topLeft = androidx.compose.ui.geometry.Offset(x = startX, y = 0f),
                                                            size = androidx.compose.ui.geometry.Size(width = (endX - startX).coerceAtLeast(2f), height = size.height),
                                                            blendMode = BlendMode.SrcAtop
                                                        )
                                                    }
                                                    
                                                    skipDbSegments.intro?.let { drawSegment(it.startMs, it.endMs, tertiaryColor) }
                                                    skipDbSegments.recap?.let { drawSegment(it.startMs, it.endMs, secondaryColor) }
                                                    skipDbSegments.outro?.let { drawSegment(it.startMs, it.endMs, errorColor) }
                                                }
                                            }
                                        }

                                        // Interactive Transparent Slider Overlay
                                        Slider(
                                            value = sliderValue,
                                            onValueChange = {
                                                isSeeking = true
                                                sliderValue = it
                                            },
                                            onValueChangeFinished = {
                                                engine.seekTo(sliderValue.toLong())
                                                kotlinx.coroutines.GlobalScope.launch {
                                                    kotlinx.coroutines.delay(50)
                                                    isSeeking = false
                                                }
                                            },
                                            valueRange = 0f..duration.toFloat().coerceAtLeast(0f),
                                            modifier = Modifier.fillMaxWidth().offset(y = (-1).dp),
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = Color.Transparent,
                                                inactiveTrackColor = Color.Transparent,
                                                thumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        
                                        // Seek Preview Bubble
                                        if (isSeeking) {
                                            val progress = sliderValue / duration.toFloat().coerceAtLeast(1f)
                                            val offsetX = sliderWidth * progress
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(bottom = 30.dp)
                                                    .offset(x = offsetX - 20.dp)
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = formatDuration(sliderValue.toLong()),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier.width(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = formatDuration(duration), color = Color.White)
                                }
                            }
                        }
                    } else {
                        // Locked State Indicator
                        Box(modifier = Modifier.align(Alignment.Center)) {
                             Icon(
                                 imageVector = Icons.Filled.Lock,
                                 contentDescription = "Locked",
                                 tint = Color.White.copy(alpha = 0.5f),
                                 modifier = Modifier.size(64.dp)
                             )
                        }
                    }
                }
            }
            
            // SkipDB Segments (Intro, Recap, Outro/Credits) & Next Episode Button
            val introSeg = skipDbSegments?.intro
            val recapSeg = skipDbSegments?.recap
            val outroSeg = skipDbSegments?.outro

            val isNearEnd = if (outroSeg != null) {
                false
            } else {
                val thresholdMs = nextEpisodeThresholdMinutes * 60 * 1000L
                duration > 0 && currentPosition >= duration - thresholdMs
            }

            val inIntroWindow = introSeg != null && currentPosition >= (introSeg.startMs - 1000).coerceAtLeast(0) && currentPosition <= introSeg.endMs
            val inRecapWindow = recapSeg != null && currentPosition >= (recapSeg.startMs - 1000).coerceAtLeast(0) && currentPosition <= recapSeg.endMs
            val inOutroWindow = outroSeg != null && currentPosition >= (outroSeg.startMs - 1000).coerceAtLeast(0) && currentPosition <= outroSeg.endMs

            if (!inPipMode && (inIntroWindow || inRecapWindow || inOutroWindow)) {
                val (labelText, onClickAction) = when {
                    inIntroWindow -> Pair(stringResource(R.string.player_skip_intro), {
                        if (duration > 0 && duration - introSeg!!.endMs < 10000) {
                            onNext()
                        } else {
                            engine.seekTo(introSeg!!.endMs)
                        }
                    })
                    inRecapWindow -> Pair(stringResource(R.string.player_skip_recap), {
                        if (duration > 0 && duration - recapSeg!!.endMs < 10000) {
                            onNext()
                        } else {
                            engine.seekTo(recapSeg!!.endMs)
                        }
                    })
                    else -> Pair(stringResource(R.string.player_skip_outro), { onNext() })
                }

                Button(
                    onClick = { onClickAction() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.85f), contentColor = Color.Black)
                ) {
                    Text(text = labelText, fontWeight = FontWeight.Bold)
                }
            } else if (!inPipMode && isNearEnd) {
                Button(
                    onClick = { onNext() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.8f), contentColor = Color.Black)
                ) {
                    Text("Play Next Episode", fontWeight = FontWeight.Bold)
                }
            }
            
            // Stats Overlay
            if (showStats) {
                StatsOverlay(engine)
            }

            // Gesture Indicator
            AnimatedVisibility(
                visible = showGestureIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        gestureIcon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${gestureValue.toInt()}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Episode Selection Dialog
        if (showEpisodesDialog) {
            EpisodesSelectionDialog(
                episodes = episodes,
                currentEpisode = currentEpisode,
                onSelectEpisode = { ep ->
                    onSelectEpisode?.invoke(ep)
                    showEpisodesDialog = false
                },
                onDismiss = { showEpisodesDialog = false }
            )
        }

        // Track Selection Dialog
        if (showTrackSelectionDialog) {
            TrackSelectionDialog(
                engine = engine,
                imdbId = imdbId,
                tmdbId = tmdbId,
                title = title,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                openSubtitlesRepository = openSubtitlesRepository,
                onDismiss = { showTrackSelectionDialog = false }
            )
        }
        
        // Error message overlay
        errorMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Audio Track Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text(stringResource(R.string.player_audio_tracks)) },
                text = {
                    Column {
                        val tracks = engine.getAudioTracks()
                        val currentTrackId = engine.getCurrentAudioTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_audio_tracks))
                        } else {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }
        
        // Subtitle Dialog  
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text(stringResource(R.string.player_subtitles)) },
                text = {
                    Column {
                        val tracks = engine.getSubtitleTracks()
                        val currentTrackId = engine.getCurrentSubtitleTrack()
                        
                        // Add "None" option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    engine.setSubtitleTrack(-1)
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTrackId == -1),
                                onClick = {
                                    engine.setSubtitleTrack(-1)
                                    showSubtitleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.player_none))
                        }
                        
                        if (tracks.isNotEmpty()) {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setSubtitleTrack(id)
                                            showSubtitleDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }

        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = currentManualSpeed,
                onSpeedSelected = { speed ->
                    currentManualSpeed = speed
                    engine.setPlaybackSpeed(speed)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }
    }
}

enum class DoubleTapState {
    Rewind, Forward
}

@UnstableApi
@Composable
fun PlayerControlsOverlay(
    engine: PlayerEngine,
    title: String?,
    showStats: Boolean,
    inPipMode: Boolean = false,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwitchEngine: () -> Unit,
    skipDbSegments: com.hasanege.materialtv.model.skipdb.SkipSegmentsContainer? = null
) {
    val context = LocalContext.current
    val activity = context as Activity
    val window = activity.window

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    // State
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLocked by remember { mutableStateOf(false) }
    var doubleTapState by remember { mutableStateOf<DoubleTapState?>(null) }
    var isPlaybackSpeedActive by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Slider state
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var currentManualSpeed by remember { mutableStateOf(1.0f) }

    // Seeking optimization state
    var seekTargetAccumulated by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    // Gesture control states
    var showGestureIndicator by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var currentVolume by remember { mutableFloatStateOf(0f) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(engine) {
        while(true) {
            isPlaying = engine.isPlaying()
            duration = engine.getDuration()
            currentPosition = engine.getCurrentPosition()
            
            if (!isSeeking) {
                sliderValue = currentPosition.toFloat()
            }
            
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(showGestureIndicator) {
        if (showGestureIndicator) {
            delay(1000L)
            showGestureIndicator = false
        }
    }
    
    LaunchedEffect(doubleTapState, lastSeekTime) {
        if (doubleTapState != null) {
            delay(600L)
            doubleTapState = null
        }
    }

    if (inPipMode) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            val currentTime = System.currentTimeMillis()
                            
                            if (currentTime - lastSeekTime > 800) {
                                seekTargetAccumulated = engine.getCurrentPosition()
                            }
                            
                            if (offset.x < screenWidth / 2) {
                                seekTargetAccumulated = (seekTargetAccumulated - 10000).coerceAtLeast(0)
                                engine.seekTo(seekTargetAccumulated)
                                doubleTapState = DoubleTapState.Rewind
                            } else {
                                seekTargetAccumulated = (seekTargetAccumulated + 10000).coerceAtMost(engine.getDuration())
                                engine.seekTo(seekTargetAccumulated)
                                doubleTapState = DoubleTapState.Forward
                            }
                            lastSeekTime = currentTime
                        },
                        onLongPress = {
                            if (!isLocked) {
                                isPlaybackSpeedActive = true
                                engine.setPlaybackSpeed(2.0f)
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }
            }
            .pointerInput(isPlaybackSpeedActive) {
                if (isPlaybackSpeedActive) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                isPlaybackSpeedActive = false
                                engine.setPlaybackSpeed(currentManualSpeed)
                                break
                            }
                        }
                    }
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val screenWidth = size.width
                        val isLeftSide = change.position.x < screenWidth / 2
                        
                        showGestureIndicator = true
                        
                        if (isLeftSide) {
                            currentBrightness = (currentBrightness - (dragAmount.y / 500f)).coerceIn(0.01f, 1f)
                            window.attributes = window.attributes.apply { screenBrightness = currentBrightness }
                            gestureIcon = Icons.Default.BrightnessMedium
                            gestureValue = currentBrightness * 100
                        } else {
                            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                            currentVolume = (currentVolume - (dragAmount.y / 500f)).coerceIn(0f, 1f)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (currentVolume * maxVolume).toInt(),
                                0
                            )
                            gestureIcon = Icons.AutoMirrored.Filled.VolumeUp
                            gestureValue = currentVolume * 100
                        }
                    }
                }
            }
    ) {
        doubleTapState?.let { state ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = if (state == DoubleTapState.Rewind) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Column(
                    modifier = Modifier.padding(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (state == DoubleTapState.Rewind) Icons.Default.Replay10 else Icons.Default.Forward10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                    Text(stringResource(R.string.player_10s), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isPlaybackSpeedActive,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            androidx.compose.material3.Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "2X",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.4f))) {
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    title?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAudioDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio Track",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { showSpeedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Playback Speed",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { onSwitchEngine() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Switch Engine",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { isLocked = !isLocked }) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock/Unlock",
                                tint = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPrevious() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { engine.seekTo(engine.getCurrentPosition() - 10000) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Replay 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { if (isPlaying) engine.pause() else engine.play() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = { engine.seekTo(engine.getCurrentPosition() + 10000) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { onNext() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (duration > 0) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (skipDbSegments != null) {
                                Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(4.dp)) {
                                    val w = size.width
                                    val drawSegment = { startMs: Long, endMs: Long, color: Color ->
                                        val startX = (startMs.toFloat() / duration.toFloat()) * w
                                        val endX = (endMs.toFloat() / duration.toFloat()) * w
                                        drawRect(
                                            color = color,
                                            topLeft = androidx.compose.ui.geometry.Offset(x = startX, y = 0f),
                                            size = androidx.compose.ui.geometry.Size(width = (endX - startX).coerceAtLeast(2f), height = size.height)
                                        )
                                    }
                                    
                                    skipDbSegments.intro?.let { drawSegment(it.startMs, it.endMs, Color(0x99FFC107)) }
                                    skipDbSegments.recap?.let { drawSegment(it.startMs, it.endMs, Color(0x992196F3)) }
                                    skipDbSegments.outro?.let { drawSegment(it.startMs, it.endMs, Color(0x999C27B0)) }
                                }
                            }
                            Slider(
                                value = sliderValue,
                                onValueChange = { 
                                    isSeeking = true
                                    sliderValue = it 
                                },
                                onValueChangeFinished = {
                                    engine.seekTo(sliderValue.toLong())
                                    isSeeking = false
                                },
                                valueRange = 0f..duration.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDuration(duration),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                if (showGestureIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            gestureIcon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                    text = "${gestureValue.toInt()}%",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                if (showStats) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Text(stringResource(R.string.player_engine_label, engine.javaClass.simpleName), color = Color.White, fontSize = 10.sp)
                        Text(stringResource(R.string.player_position_label, formatDuration(currentPosition), formatDuration(duration)), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
        
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text(stringResource(R.string.player_audio_tracks)) },
                text = {
                    Column {
                        val tracks = engine.getAudioTracks()
                        val currentTrackId = engine.getCurrentAudioTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_audio_tracks))
                        } else {
                            tracks.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (id == currentTrackId),
                                        onClick = {
                                            engine.setAudioTrack(id)
                                            showAudioDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(stringResource(R.string.player_close))
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackSelectionDialog(
    engine: PlayerEngine,
    imdbId: String? = null,
    tmdbId: String? = null,
    title: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    openSubtitlesRepository: com.hasanege.materialtv.repository.OpenSubtitlesRepository? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val subtitleTracks = remember(engine) { engine.getSubtitleTracks() }
    val currentSubtitleTrack = remember(engine) { engine.getCurrentSubtitleTrack() }

    val settingsRepo = remember { com.hasanege.materialtv.data.SettingsRepository.getInstance(context) }
    val openSubtitlesApiKey by settingsRepo.openSubtitlesApiKey.collectAsState(initial = null)
    val preferredLanguage by settingsRepo.preferredSubtitleLanguage.collectAsState(initial = null)

    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<com.hasanege.materialtv.model.opensubtitles.OpenSubtitlesItem>>(emptyList()) }
    var openSubtitlesError by remember { mutableStateOf<String?>(null) }
    var downloadingFileId by remember { mutableStateOf<Int?>(null) }
    var subtitleDelayMs by remember { mutableLongStateOf(engine.getSubtitleDelay()) }

    fun performOpenSubtitlesSearch() {
        val apiKey = openSubtitlesApiKey
        val repo = openSubtitlesRepository ?: return

        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isSearching = true
            openSubtitlesError = null
            try {
                val parsedSeason = seasonNumber ?: title?.let { t ->
                    Regex("""(?i)S(\d+)\s*E(\d+)""").find(t)?.groupValues?.get(1)?.toIntOrNull()
                }
                val parsedEpisode = episodeNumber ?: title?.let { t ->
                    Regex("""(?i)S(\d+)\s*E(\d+)""").find(t)?.groupValues?.get(2)?.toIntOrNull()
                }

                val results = repo.searchSubtitles(
                    apiKey = apiKey,
                    imdbId = imdbId,
                    tmdbId = tmdbId,
                    seasonNumber = parsedSeason,
                    episodeNumber = parsedEpisode,
                    query = title
                )
                // Sort descending by download count
                searchResults = results.sortedByDescending { it.attributes?.downloadCount ?: 0 }
            } catch (e: com.hasanege.materialtv.repository.OpenSubtitlesQuotaException) {
                openSubtitlesError = e.message
            } catch (e: Exception) {
                openSubtitlesError = e.message ?: context.getString(R.string.network_error)
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(imdbId, tmdbId, title, seasonNumber, episodeNumber, openSubtitlesApiKey) {
        if (searchResults.isEmpty() && !isSearching) {
            performOpenSubtitlesSearch()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Shared subtitle list composable (used in both layouts)
    @Composable
    fun SubtitleList(modifier: Modifier = Modifier) {
        LazyColumn(modifier = modifier) {
            // EMBEDDED SUBTITLES SECTION
            item {
                Text(
                    text = stringResource(R.string.player_tab_embed),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (subtitleTracks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.tracks_no_tracks),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            } else {
                items(subtitleTracks) { (trackId, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                engine.setSubtitleTrack(trackId)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .background(
                                if (trackId == currentSubtitleTrack)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayLabel = if (trackId == -1 || label.equals("Disabled", ignoreCase = true)) {
                            stringResource(R.string.tracks_disabled)
                        } else {
                            label
                        }
                        Text(
                            text = displayLabel,
                            fontSize = 13.sp,
                            fontWeight = if (trackId == currentSubtitleTrack) FontWeight.Bold else FontWeight.Normal,
                            color = if (trackId == currentSubtitleTrack)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                }
            }

            item {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // OPENSUBTITLES SECTION
            item {
                Text(
                    text = stringResource(R.string.player_tab_opensubtitles),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (openSubtitlesError != null) {
                item {
                    Text(
                        text = openSubtitlesError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else if (isSearching) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.opensubtitles_searching),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    val noResultsMsg = if (imdbId.isNullOrBlank() && tmdbId.isNullOrBlank()) {
                        "Bu içerik için IMDb ID bulunamadı. Altyazı veya Skip verisi mevcut değil."
                    } else {
                        stringResource(R.string.opensubtitles_search_no_results)
                    }
                    Text(
                        text = noResultsMsg,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(searchResults) { item ->
                    val attr = item.attributes
                    val fileObj = attr?.files?.firstOrNull()
                    val fileId = fileObj?.fileId
                    val lang = attr?.language ?: "unk"
                    val release = attr?.release ?: fileObj?.fileName ?: "Subtitle"
                    val downloads = attr?.downloadCount ?: 0
                    val rating = attr?.ratings ?: 0f

                    val isDownloading = downloadingFileId == fileId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = fileId != null && !isDownloading) {
                                if (fileId != null) {
                                    downloadingFileId = fileId
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val repo = openSubtitlesRepository ?: return@launch
                                            val downloadedFile = repo.downloadSubtitle(
                                                openSubtitlesApiKey,
                                                fileId,
                                                context
                                            )
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                engine.addExternalSubtitle(
                                                    downloadedFile.absolutePath,
                                                    lang,
                                                    "$lang - $release"
                                                )
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.opensubtitles_applied, lang),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                onDismiss()
                                            }
                                        } catch (e: com.hasanege.materialtv.repository.OpenSubtitlesQuotaException) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                openSubtitlesError = e.message
                                                downloadingFileId = null
                                            }
                                        } catch (e: Exception) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                openSubtitlesError = e.message ?: context.getString(R.string.download_error)
                                                downloadingFileId = null
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flagEmoji = getLanguageFlagEmoji(lang)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = flagEmoji,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = release,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "⭐ ${String.format(Locale.getDefault(), "%.1f", rating)} • ⬇ $downloads",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Shared calibration panel composable
    @Composable
    fun CalibrationPanel(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.subtitle_calibration),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                val delaySec = subtitleDelayMs / 1000f
                val normalStr = stringResource(R.string.subtitle_normal)
                val delayText = when {
                    delaySec > 0f -> String.format(Locale.getDefault(), "+%.1fs", delaySec)
                    delaySec < 0f -> String.format(Locale.getDefault(), "%.1fs", delaySec)
                    else -> String.format(Locale.getDefault(), "0.0s (%s)", normalStr)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = delayText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (subtitleDelayMs != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subtitleDelayMs != 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = {
                                subtitleDelayMs = 0L
                                engine.setSubtitleDelay(0L)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(stringResource(R.string.subtitle_reset), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val sliderValueSec = (subtitleDelayMs / 1000f).coerceIn(-10f, 10f)

            // Fine-tune step buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val newDelay = (subtitleDelayMs - 100L).coerceIn(-10000L, 10000L)
                        subtitleDelayMs = newDelay
                        engine.setSubtitleDelay(newDelay)
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("-0.1s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = sliderValueSec,
                    onValueChange = { newValue ->
                        val roundedMs = (Math.round(newValue * 10f) * 100L)
                        subtitleDelayMs = roundedMs
                        engine.setSubtitleDelay(roundedMs)
                    },
                    valueRange = -10f..10f,
                    steps = 199,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = {
                        val newDelay = (subtitleDelayMs + 100L).coerceIn(-10000L, 10000L)
                        subtitleDelayMs = newDelay
                        engine.setSubtitleDelay(newDelay)
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("+0.1s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Scale labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-10s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+10s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isLandscape) {
                Spacer(modifier = Modifier.height(12.dp))
                // Quick jump buttons for landscape (easier to tap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2000L, -1000L, -500L).forEach { ms ->
                        OutlinedButton(
                            onClick = {
                                val newDelay = (subtitleDelayMs + ms).coerceIn(-10000L, 10000L)
                                subtitleDelayMs = newDelay
                                engine.setSubtitleDelay(newDelay)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("${ms/1000}s", fontSize = 10.sp)
                        }
                    }
                    listOf(500L, 1000L, 2000L).forEach { ms ->
                        OutlinedButton(
                            onClick = {
                                val newDelay = (subtitleDelayMs + ms).coerceIn(-10000L, 10000L)
                                subtitleDelayMs = newDelay
                                engine.setSubtitleDelay(newDelay)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+${ms/1000}s", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(if (isLandscape) 0.85f else 0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.player_subtitles),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (isLandscape) {
                    val delaySec = subtitleDelayMs / 1000f
                    val delayText = when {
                        delaySec > 0f -> String.format(Locale.getDefault(), "+%.1fs", delaySec)
                        delaySec < 0f -> String.format(Locale.getDefault(), "%.1fs", delaySec)
                        else -> null
                    }
                    if (delayText != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⏱ $delayText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            if (isLandscape) {
                // LANDSCAPE: side-by-side layout (85% dialog width)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT: Calibration panel
                    CalibrationPanel(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight()
                    )

                    // Vertical divider
                    androidx.compose.material3.VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // RIGHT: Subtitle list
                    SubtitleList(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // PORTRAIT: vertical scroll layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // 0. SUBTITLE CALIBRATION SLIDER SECTION
                    item {
                        CalibrationPanel(modifier = Modifier.fillMaxWidth())
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // EMBEDDED SUBTITLES
                    item {
                        Text(
                            text = stringResource(R.string.player_tab_embed),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    if (subtitleTracks.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.tracks_no_tracks),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        items(subtitleTracks) { (trackId, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        engine.setSubtitleTrack(trackId)
                                        onDismiss()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .background(
                                        if (trackId == currentSubtitleTrack)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else
                                            Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayLabel = if (trackId == -1 || label.equals("Disabled", ignoreCase = true)) {
                                    stringResource(R.string.tracks_disabled)
                                } else {
                                    label
                                }
                                Text(
                                    text = displayLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (trackId == currentSubtitleTrack) FontWeight.Bold else FontWeight.Normal,
                                    color = if (trackId == currentSubtitleTrack)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    item {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // OPENSUBTITLES SECTION
                    item {
                        Text(
                            text = stringResource(R.string.player_tab_opensubtitles),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    if (openSubtitlesError != null) {
                        item {
                            Text(
                                text = openSubtitlesError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else if (isSearching) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text(
                                    text = stringResource(R.string.opensubtitles_searching),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.opensubtitles_search_no_results),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(searchResults) { item ->
                            val attr = item.attributes
                            val fileObj = attr?.files?.firstOrNull()
                            val fileId = fileObj?.fileId
                            val lang = attr?.language ?: "unk"
                            val release = attr?.release ?: fileObj?.fileName ?: "Subtitle"
                            val downloads = attr?.downloadCount ?: 0
                            val rating = attr?.ratings ?: 0f

                            val isDownloading = downloadingFileId == fileId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = fileId != null && !isDownloading) {
                                        if (fileId != null) {
                                            downloadingFileId = fileId
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val repo = openSubtitlesRepository ?: return@launch
                                                    val downloadedFile = repo.downloadSubtitle(
                                                        openSubtitlesApiKey,
                                                        fileId,
                                                        context
                                                    )
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        engine.addExternalSubtitle(
                                                            downloadedFile.absolutePath,
                                                            lang,
                                                            "$lang - $release"
                                                        )
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            context.getString(R.string.opensubtitles_applied, lang),
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                        onDismiss()
                                                    }
                                                } catch (e: com.hasanege.materialtv.repository.OpenSubtitlesQuotaException) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        openSubtitlesError = e.message
                                                        downloadingFileId = null
                                                    }
                                                } catch (e: Exception) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        openSubtitlesError = e.message ?: context.getString(R.string.download_error)
                                                        downloadingFileId = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val flagEmoji = getLanguageFlagEmoji(lang)

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = flagEmoji,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = release,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "⭐ ${String.format(Locale.getDefault(), "%.1f", rating)} • ⬇ $downloads",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.player_close))
            }
        }
    )
}



@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_playback_speed)) },
        text = {
            LazyColumn(modifier = Modifier.height(240.dp)) {
                items(speeds) { speed ->
                    val label = if (speed == 1.0f) stringResource(R.string.player_speed_normal) else "${speed}x"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 8.dp)
                            .background(
                                if (speed == currentSpeed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (speed == currentSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.player_close))
            }
        }
    )
}

@Composable
fun StatsOverlay(engine: PlayerEngine) {
    var bitrate by remember { mutableStateOf("N/A") }
    var droppedFrames by remember { mutableStateOf("N/A") }
    var videoFormat by remember { mutableStateOf("N/A") }

    LaunchedEffect(engine) {
        while (true) {
            try {
                val bitrateValue = engine.getBitrate()
                bitrate = if (bitrateValue > 0) "${bitrateValue / 1000} kbps" else "N/A"
                
                val droppedValue = engine.getDroppedFrames()
                droppedFrames = droppedValue.toString()
                
                videoFormat = engine.getVideoFormat() ?: "N/A"
            } catch (e: Exception) {
                android.util.Log.e("StatsOverlay", "Error getting stats: ${e.message}")
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Bitrate: $bitrate",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Dropped: $droppedFrames",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Format: $videoFormat",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getLanguageFlagEmoji(langCode: String?): String {
    if (langCode.isNullOrBlank()) return "🌐"
    return when (langCode.lowercase(Locale.getDefault())) {
        "tr", "tur" -> "🇹🇷"
        "en", "eng" -> "🇬🇧"
        "de", "ger", "deu" -> "🇩🇪"
        "fr", "fre", "fra" -> "🇫🇷"
        "es", "spa" -> "🇪🇸"
        "it", "ita" -> "🇮🇹"
        "ru", "rus" -> "🇷🇺"
        "pt", "por" -> "🇵🇹"
        "pb", "pt-br" -> "🇧🇷"
        "ar", "ara" -> "🇸🇦"
        "nl", "dut", "nld" -> "🇳🇱"
        "pl", "pol" -> "🇵🇱"
        "sv", "swe" -> "🇸🇪"
        "no", "nor" -> "🇳🇴"
        "fi", "fin" -> "🇫🇮"
        "da", "dan" -> "🇩🇰"
        "el", "gre", "ell" -> "🇬🇷"
        "zh", "chi", "zho", "zh-cn" -> "🇨🇳"
        "zh-tw" -> "🇹🇼"
        "ja", "jpn" -> "🇯🇵"
        "ko", "kor" -> "🇰🇷"
        "hi", "hin" -> "🇮🇳"
        "fa", "per", "fas" -> "🇮🇷"
        "he", "heb" -> "🇮🇱"
        "uk", "ukr" -> "🇺🇦"
        "cs", "cze", "ces" -> "🇨🇿"
        "ro", "rum", "ron" -> "🇷🇴"
        "hu", "hun" -> "🇭🇺"
        "bg", "bul" -> "🇧🇬"
        "sr", "srp" -> "🇷🇸"
        "hr", "hrv" -> "🇭🇷"
        "sk", "slo", "slk" -> "🇸🇰"
        "az", "aze" -> "🇦🇿"
        "kk", "kaz" -> "🇰🇿"
        "uz", "uzb" -> "🇺🇿"
        "id", "ind" -> "🇮🇩"
        "th", "tha" -> "🇹🇭"
        "vi", "vie" -> "🇻🇳"
        else -> {
            if (langCode.length == 2) {
                try {
                    val firstChar = Character.codePointAt(langCode.uppercase(Locale.getDefault()), 0) - 0x41 + 0x1F1E6
                    val secondChar = Character.codePointAt(langCode.uppercase(Locale.getDefault()), 1) - 0x41 + 0x1F1E6
                    String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
                } catch (e: Exception) {
                    "🌐"
                }
            } else {
                "🌐"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodesSelectionDialog(
    episodes: List<com.hasanege.materialtv.model.Episode>?,
    currentEpisode: com.hasanege.materialtv.model.Episode? = null,
    onSelectEpisode: (com.hasanege.materialtv.model.Episode) -> Unit,
    onDismiss: () -> Unit
) {
    android.util.Log.d("EpisodesDialog", "Dialog opened. episodes=${episodes?.size}, currentEpisode=${currentEpisode?.id}")

    val safeEpisodes = episodes ?: emptyList()
    val seasons = remember(safeEpisodes) {
        safeEpisodes.mapNotNull { it.season }.distinct().sorted()
    }

    var selectedSeason by remember(currentEpisode, seasons) {
        mutableStateOf(currentEpisode?.season ?: seasons.firstOrNull() ?: 1)
    }

    val filteredEpisodes = remember(safeEpisodes, selectedSeason) {
        safeEpisodes.filter { it.season == selectedSeason }
            .sortedBy { it.episodeNum?.toIntOrNull() ?: 0 }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(if (isLandscape) 0.5f else 0.95f)
            .fillMaxHeight(if (isLandscape) 0.9f else 0.7f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.detail_episodes),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            if (safeEpisodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bölümler yükleniyor...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (isLandscape) {
                // LANDSCAPE: Seasons left, episodes right
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (seasons.size > 1) {
                        LazyColumn(
                            modifier = Modifier
                                .width(115.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(seasons) { seasonNum ->
                                val isSelected = seasonNum == selectedSeason
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSeason = seasonNum },
                                    label = {
                                        Text(
                                            text = "$seasonNum. Sezon",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        androidx.compose.material3.VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredEpisodes) { episode ->
                            val isPlaying = episode.id == currentEpisode?.id
                            EpisodeListItem(
                                episode = episode,
                                isPlaying = isPlaying,
                                onClick = {
                                    onSelectEpisode(episode)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            } else {
                // PORTRAIT: Season chips top, episodes list below
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (seasons.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            items(seasons) { seasonNum ->
                                val isSelected = seasonNum == selectedSeason
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSeason = seasonNum },
                                    label = {
                                        Text(
                                            text = "$seasonNum. Sezon",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredEpisodes) { episode ->
                            val isPlaying = episode.id == currentEpisode?.id
                            EpisodeListItem(
                                episode = episode,
                                isPlaying = isPlaying,
                                onClick = {
                                    onSelectEpisode(episode)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun EpisodeListItem(
    episode: com.hasanege.materialtv.model.Episode,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val epNum = episode.episodeNum?.let { "$it. Bölüm" } ?: ""
    val epTitle = episode.title?.takeIf { it.isNotBlank() } ?: epNum

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isPlaying)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Oynatılıyor",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (epTitle != epNum && epNum.isNotEmpty()) "$epNum - $epTitle" else epTitle,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!episode.duration.isNullOrBlank()) {
                Text(
                    text = "⏱ ${episode.duration}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
