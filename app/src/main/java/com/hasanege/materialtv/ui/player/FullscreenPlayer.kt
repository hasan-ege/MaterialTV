package com.hasanege.materialtv.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwitchEngine: () -> Unit
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
                        
                        Row {
                             // Audio Track Button
                             IconButton(onClick = { showAudioDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.GraphicEq,
                                     contentDescription = "Audio Track",
                                     tint = Color.White
                                 )
                             }
                             
                             // Subtitle Button
                             IconButton(onClick = { showSubtitleDialog = true }) {
                                 Icon(
                                     imageVector = Icons.Default.Subtitles,
                                     contentDescription = "Subtitles",
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
                                                .height(10.dp)
                                        ) {
                                            LinearWavyProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxSize(),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                waveSpeed = if (isPlaying) 80.dp else 0.dp
                                            )
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
        
        // Track Selection Dialog
        if (showTrackSelectionDialog) {
            TrackSelectionDialog(
                engine = engine,
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
    onSwitchEngine: () -> Unit
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
        
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text(stringResource(R.string.player_subtitles)) },
                text = {
                    Column {
                        val tracks = engine.getSubtitleTracks()
                        val currentTrackId = engine.getCurrentSubtitleTrack()
                        
                        if (tracks.isEmpty()) {
                            Text(stringResource(R.string.player_no_subtitles))
                        } else {
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
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackSelectionDialog(
    engine: PlayerEngine,
    onDismiss: () -> Unit
) {
    val audioTracks = remember(engine) {
        engine.getAudioTracks()
    }

    val subtitleTracks = remember(engine) {
        engine.getSubtitleTracks()
    }
    
    val currentAudioTrack = remember(engine) {
        engine.getCurrentAudioTrack()
    }
    
    val currentSubtitleTrack = remember(engine) {
        engine.getCurrentSubtitleTrack()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_audio_subtitle_selection)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.tracks_audio),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tracks_no_tracks),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(audioTracks) { (trackId, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setAudioTrack(trackId)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .background(
                                            if (trackId == currentAudioTrack) 
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else 
                                                Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (trackId == currentAudioTrack) FontWeight.Bold else FontWeight.Normal,
                                        color = if (trackId == currentAudioTrack) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.tracks_subtitles),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (subtitleTracks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tracks_no_tracks),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(subtitleTracks) { (trackId, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            engine.setSubtitleTrack(trackId)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .background(
                                            if (trackId == currentSubtitleTrack) 
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else 
                                                Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
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
