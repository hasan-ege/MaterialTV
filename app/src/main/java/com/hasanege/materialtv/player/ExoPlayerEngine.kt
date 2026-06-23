package com.hasanege.materialtv.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import java.io.File

class ExoPlayerEngine : PlayerEngine {
    private var player: ExoPlayer? = null
    fun getExoPlayer(): ExoPlayer? = player
    private var playbackSpeed: Float = 1.0f
    private var playerView: PlayerView? = null
    private var context: Context? = null
    private var currentContainer: ViewGroup? = null
    private var isAttached: Boolean = false
    
    // Stats
    private var currentBitrate: Long = 0
    private var droppedFrames: Int = 0
    private var videoFormat: String? = null
    
    // Error callback
    private var errorCallback: ((Exception) -> Unit)? = null
    private var playbackStateCallback: ((Boolean) -> Unit)? = null
    private var playbackEndedCallback: (() -> Unit)? = null

    override fun setOnErrorCallback(callback: (Exception) -> Unit) {
        this.errorCallback = callback
    }

    override fun setOnPlaybackStateChanged(callback: (Boolean) -> Unit) {
        this.playbackStateCallback = callback
    }

    override fun setOnPlaybackEndedCallback(callback: () -> Unit) {
        this.playbackEndedCallback = callback
    }

    override fun initialize(context: Context) {
        this.context = context
        
        // Configure RenderersFactory to prefer FFMPEG software audio decoder
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            // EXTENSION_RENDERER_MODE_ON: Use extension renderers only if platform decoder fails (Prioritize HW)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            
            // Enable decoder fallback in case of errors
            setEnableDecoderFallback(true)
        }

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
            )
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Optimized load control for stability and performance
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,    // Min buffer 500ms
                15000,  // Max buffer 15s
                200,    // Buffer for playback (Start after 200ms)
                500     // Buffer for rebuffer
            )
            .setBackBuffer(
                3000,  // Keep 3s back buffer
                true   // Retain back buffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(-1)
            .build()

        player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC) // Use keyframes for seek
            .setLoadControl(loadControl)
            .build()
        
        player?.addAnalyticsListener(object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                // Keep bandwidth estimate as fallback or secondary metric if needed
                // But for "Stats for Nerds", format bitrate is more relevant for the stream quality
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                this@ExoPlayerEngine.droppedFrames += droppedFrames
            }
            
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                if (loadEventInfo.loadDurationMs > 0) {
                    val bitrate = (loadEventInfo.bytesLoaded * 8 * 1000) / loadEventInfo.loadDurationMs
                    currentBitrate = bitrate
                }
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                videoFormat = "${format.width}x${format.height} (${format.sampleMimeType})"
                // Fallback to format bitrate if we haven't calculated one yet
                if (currentBitrate == 0L && format.bitrate != Format.NO_VALUE) {
                    currentBitrate = format.bitrate.toLong()
                }
            }
            
            // Log audio decoder information
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                android.util.Log.d("ExoPlayerEngine", "Audio format changed: ${format.sampleMimeType}, " +
                        "channels: ${format.channelCount}, sampleRate: ${format.sampleRate}")
            }
            
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long
            ) {
                android.util.Log.d("ExoPlayerEngine", "Audio decoder initialized: $decoderName")
            }
        })
        
        // Add error listener
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorCallback?.invoke(Exception(error.message, error))
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackStateCallback?.invoke(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playbackEndedCallback?.invoke()
                }
            }
        })
    }

    override fun attach(container: ViewGroup) {
        android.util.Log.d("ExoPlayerEngine", "attach() called, isAttached=$isAttached, sameContainer=${currentContainer == container}")
        
        // If already attached to the same container, just request layout
        if (isAttached && currentContainer == container && playerView != null) {
            android.util.Log.d("ExoPlayerEngine", "Already attached to same container, requesting layout")
            playerView?.requestLayout()
            return
        }
        
        // If attached to a different container, detach first
        if (isAttached && currentContainer != container) {
            android.util.Log.d("ExoPlayerEngine", "Attached to different container, detaching first")
            detach()
        }
        
        context?.let { ctx ->
            android.util.Log.d("ExoPlayerEngine", "Creating new PlayerView")
            
            playerView = PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                player = this@ExoPlayerEngine.player
                useController = false // Disable default controls
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            
            container.addView(playerView)
            currentContainer = container
            isAttached = true
            android.util.Log.d("ExoPlayerEngine", "PlayerView attached successfully")
        }
    }

    override fun reattach() {
        val container = currentContainer ?: return
        android.util.Log.d("ExoPlayerEngine", "Reattaching view...")
        detach()
        attach(container)
    }

    override fun detach() {
        android.util.Log.d("ExoPlayerEngine", "detach() called, isAttached=$isAttached")
        
        if (!isAttached) {
            return
        }
        
        playerView?.let {
            try {
                (it.parent as? ViewGroup)?.removeView(it)
                it.player = null
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerEngine", "Error detaching: ${e.message}")
            }
        }
        
        playerView = null
        currentContainer = null
        isAttached = false
    }

    override fun prepare(url: String, startPosition: Long) {
        // Lokal dosyalar ve HTTP URL'leri ayır
        val isNetworkUrl = url.startsWith("http://") || url.startsWith("https://")

        if (isNetworkUrl) {
            // Aggressive HTTP settings for FAST ZAPPING
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(5000)  // Reduced from 60s to 5s for fast zapping
                .setReadTimeoutMs(8000)     // Reduced from 60s to 8s
                .setKeepPostFor302Redirects(true)
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to "*/*",
                        "Accept-Language" to "en-US,en;q=0.9,tr;q=0.8",
                        "Accept-Encoding" to "gzip, deflate, br",
                        "Connection" to "keep-alive",
                        "Referer" to url.substringBefore("?").substringBeforeLast("/") + "/",
                        "Origin" to url.substringBefore("://") + "://" + url.substringAfter("://").substringBefore("/"),
                        "Sec-Fetch-Dest" to "video",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Site" to "cross-site"
                    )
                )

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
                context!!,
                httpDataSourceFactory
            )

            // Optimized media source factory for fast loading
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context!!)
                .setDataSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(
                    androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(2) // Reduced retries from 3 to 2
                )

            val mediaItem = MediaItem.fromUri(url)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            player?.apply {
                setMediaSource(mediaSource)
                prepare()
                if (startPosition > 0) seekTo(startPosition)
                // Instant playback for fast zapping
                playWhenReady = true
            }
        } else {
            // LOCAL FILES: Optimized for instant playback
            val file = File(url)
            val fileUri = Uri.fromFile(file)
            val mediaItem = MediaItem.fromUri(fileUri)

            // Use FileDataSource for local files - much faster
            val fileDataSourceFactory = androidx.media3.datasource.FileDataSource.Factory()
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context!!, fileDataSourceFactory)
            
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context!!)
                .setDataSourceFactory(dataSourceFactory)

            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            player?.apply {
                setMediaSource(mediaSource)
                prepare()
                if (startPosition > 0) seekTo(startPosition)
                playWhenReady = true
            }
        }
    }

    override fun play() {
        player?.play()
    }

    override fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    override fun isBuffering(): Boolean {
        return player?.playbackState == Player.STATE_BUFFERING
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.stop()
    }

    override fun release() {
        try {
            // Cancel pending operations
            seekHandler.removeCallbacksAndMessages(null)
            pendingSeekPosition = null
            
            player?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error releasing: ${e.message}")
        } finally {
            player = null
            playerView = null
            context = null
        }
    }

    // Seek optimize
    private var pendingSeekPosition: Long? = null
    private val seekHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val performSeekRunnable = Runnable {
        pendingSeekPosition?.let { pos ->
            try {
                // Ensure position is within bounds
                val duration = player?.duration ?: 0L
                val safePos = if (duration > 0) pos.coerceIn(0, duration) else pos.coerceAtLeast(0)
                
                android.util.Log.d("ExoPlayerEngine", "Performing buffered seek to: $safePos")
                player?.seekTo(safePos)
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerEngine", "Error performing seek: ${e.message}")
            } finally {
                pendingSeekPosition = null
            }
        }
    }

    override fun seekTo(position: Long) {
        // Direct seek (scrollbar usage) - immediate but cancels setting pending seeks
        seekHandler.removeCallbacks(performSeekRunnable)
        pendingSeekPosition = null
        player?.seekTo(position)
    }

    override fun seekBack() {
        val current = pendingSeekPosition ?: player?.currentPosition ?: 0L
        // Subtract 10s
        val target = (current - 10000).coerceAtLeast(0)
        
        scheduleSeek(target)
    }

    override fun seekForward() {
        val current = pendingSeekPosition ?: player?.currentPosition ?: 0L
        val duration = player?.duration ?: Long.MAX_VALUE
        // Add 10s
        val target = (current + 10000)
        // Check upper bound if duration is known
        val safeTarget = if (duration > 0) target.coerceAtMost(duration) else target
        
        scheduleSeek(safeTarget)
    }
    
    private fun scheduleSeek(targetPosition: Long) {
        pendingSeekPosition = targetPosition
        // Cancel previous request
        seekHandler.removeCallbacks(performSeekRunnable)
        // Schedule new request in 400ms (debounce time)
        // 400ms gives user enough time to tap multiple times
        seekHandler.postDelayed(performSeekRunnable, 400)
    }

    override fun getDuration(): Long {
        return player?.duration ?: 0L
    }

    override fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    override fun getBitrate(): Long = currentBitrate
    override fun getDroppedFrames(): Int = droppedFrames
    
    override fun getVideoFormat(): String? {
        return try {
            player?.videoFormat?.let { format ->
                "${format.width}x${format.height} ${format.codecs ?: "Unknown"}"
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Set resize mode for aspect ratio handling
     * @param mode: RESIZE_MODE_FIT, RESIZE_MODE_FILL, RESIZE_MODE_ZOOM, RESIZE_MODE_FIXED_WIDTH, RESIZE_MODE_FIXED_HEIGHT
     */
    fun setResizeMode(mode: Int) {
        playerView?.resizeMode = mode
    }
    
    // Track selection implementation
    override fun getSubtitleTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            tracks.add(Pair(-1, "Disabled"))
            
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val language = format.language ?: "Unknown"
                        val label = format.label ?: language
                        // Use unique ID combining group and track index
                        tracks.add(Pair(groupIndex * 1000 + i, label))
                    }
                }
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error getting subtitle tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun getAudioTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val language = format.language ?: "Unknown"
                        val label = format.label ?: language
                        // Use unique ID combining group and track index
                        tracks.add(Pair(groupIndex * 1000 + i, label))
                    }
                }
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error getting audio tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun setSubtitleTrack(trackId: Int) {
        try {
            val params = player?.trackSelectionParameters?.buildUpon() ?: return
            
            if (trackId == -1) {
                // Disable subtitles
                params.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            } else {
                // Enable subtitles and select specific track
                params.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                
                // Find the track group and index from trackId
                val groupIndex = trackId / 1000
                val trackIndex = trackId % 1000
                
                player?.currentTracks?.groups?.getOrNull(groupIndex)?.let { group ->
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT && trackIndex < group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val override = androidx.media3.common.TrackSelectionOverride(
                            group.mediaTrackGroup,
                            listOf(trackIndex)
                        )
                        params.addOverride(override)
                    }
                }
            }
            
            player?.trackSelectionParameters = params.build()
            android.util.Log.d("ExoPlayerEngine", "Subtitle track set to: $trackId")
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error setting subtitle track: ${e.message}", e)
        }
    }
    
    override fun setAudioTrack(trackId: Int) {
        try {
            val params = player?.trackSelectionParameters?.buildUpon() ?: return
            
            // Find the track group and index from trackId
            val groupIndex = trackId / 1000
            val trackIndex = trackId % 1000
            
            player?.currentTracks?.groups?.getOrNull(groupIndex)?.let { group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && trackIndex < group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val override = androidx.media3.common.TrackSelectionOverride(
                        group.mediaTrackGroup,
                        listOf(trackIndex)
                    )
                    params.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_AUDIO)
                    params.addOverride(override)
                    player?.trackSelectionParameters = params.build()
                    android.util.Log.d("ExoPlayerEngine", "Audio track set to: $trackId (${format.language})")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error setting audio track: ${e.message}", e)
        }
    }
    
    override fun getCurrentSubtitleTrack(): Int {
        return try {
            val params = player?.trackSelectionParameters
            val isDisabled = params?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) ?: true
            
            if (isDisabled) {
                -1
            } else {
                // Find currently selected subtitle track
                player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                return groupIndex * 1000 + i
                            }
                        }
                    }
                }
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
    
    override fun getCurrentAudioTrack(): Int {
        return try {
            // Find currently selected audio track
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && group.isSelected) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            return groupIndex * 1000 + i
                        }
                    }
                }
            }
            0 // Return 0 as default if nothing found
        } catch (e: Exception) {
            -1
        }
    }

    override fun onResume() {
        playerView?.onResume()
    }

    override fun setSubtitleSize(size: String) {
        val sizePx = when(size) {
            "Small" -> 14f
            "Normal" -> 18f
            "Large" -> 24f
            else -> 18f
        }
        playerView?.subtitleView?.apply {
            setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizePx)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        player?.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
    }

    override fun getPlaybackSpeed(): Float {
        return player?.playbackParameters?.speed ?: playbackSpeed
    }
}
