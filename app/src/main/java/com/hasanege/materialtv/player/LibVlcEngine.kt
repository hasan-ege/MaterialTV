package com.hasanege.materialtv.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

class LibVlcEngine : PlayerEngine {
    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    private var videoLayout: VLCVideoLayout? = null
    private var surfaceView: SurfaceView? = null
    private var currentContainer: ViewGroup? = null
    private var isAttached: Boolean = false
    private var isBuffering: Boolean = false
    private var subtitleSizeScale: Int = 100

    override fun initialize(context: Context) {
        this.context = context
        
        try {
            // Minimal VLC arguments for stability
            val args = ArrayList<String>().apply {
                // Basic network caching (Ultra low for instant start)
                add("--network-caching=300")
                
                // Hardware acceleration
                add("--codec=mediacodec_ndk,all")
                
                // Audio output
                add("--aout=opensles")
                
                // Disable unnecessary features
                add("--no-stats")
                add("--no-osd")
                add("--no-video-title-show")
            }
            
            libVlc = LibVLC(context, args)
            mediaPlayer = MediaPlayer(libVlc).apply {
                // Set scale mode for proper aspect ratio
                videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                
                // Add event listener for error handling and playback monitoring
                setEventListener { event ->
                    when (event.type) {
                        MediaPlayer.Event.EncounteredError -> {
                            val errorMsg = "VLC playback error: ${event.type}"
                            android.util.Log.e("LibVlcEngine", errorMsg)
                            errorCallback?.invoke(Exception(errorMsg))
                        }
                        MediaPlayer.Event.EndReached -> {
                            android.util.Log.d("LibVlcEngine", "Playback ended")
                            playbackEndedCallback?.invoke()
                        }
                        MediaPlayer.Event.Playing -> {
                            android.util.Log.d("LibVlcEngine", "Playback started")
                            if (startPosition != -1L) {
                                val current = mediaPlayer?.time ?: 0L
                                if (java.lang.Math.abs(current - startPosition) > 1000) {
                                     android.util.Log.d("LibVlcEngine", "Applying pending seek to $startPosition")
                                     mediaPlayer?.time = startPosition
                                }
                                startPosition = -1L
                            }
                            playbackStateCallback?.invoke(true)
                        }
                        MediaPlayer.Event.Paused -> {
                            android.util.Log.d("LibVlcEngine", "Playback paused")
                            playbackStateCallback?.invoke(false)
                        }
                        MediaPlayer.Event.Stopped -> {
                            android.util.Log.d("LibVlcEngine", "Playback stopped")
                        }
                        MediaPlayer.Event.Buffering -> {
                            android.util.Log.d("LibVlcEngine", "Buffering: ${event.buffering}%")
                            isBuffering = event.buffering < 100f
                        }
                        else -> {
                            // Ignore other events
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error initializing VLC: ${e.message}")
            throw e
        }
    }

    override fun attach(container: ViewGroup) {
        android.util.Log.d("LibVlcEngine", "attach() called, isAttached=$isAttached, sameContainer=${currentContainer == container}")
        
        // If already attached to the same container, just request layout
        if (isAttached && currentContainer == container && videoLayout != null) {
            android.util.Log.d("LibVlcEngine", "Already attached to same container, requesting layout")
            videoLayout?.requestLayout()
            return
        }
        
        // If attached to a different container, detach first
        if (isAttached && currentContainer != container) {
            android.util.Log.d("LibVlcEngine", "Attached to different container, detaching first")
            detach()
        }
        
        context?.let { ctx ->
            android.util.Log.d("LibVlcEngine", "Creating new VLCVideoLayout")
            
            // Use VLCVideoLayout for better aspect ratio handling
            val layout = VLCVideoLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            
            container.addView(layout)
            videoLayout = layout
            currentContainer = container
            isAttached = true
            
            // Attach media player to the layout
            mediaPlayer?.attachViews(layout, null, false, false)
            android.util.Log.d("LibVlcEngine", "VLCVideoLayout attached successfully")
            
            // Fix for black screen on resume: Toggle video track to wake up Vout
            try {
                if (mediaPlayer?.isPlaying == true) {
                    val currentTrack = mediaPlayer?.videoTrack
                    if (currentTrack != null && currentTrack != -1) {
                        mediaPlayer?.videoTrack = -1
                        mediaPlayer?.videoTrack = currentTrack
                    }
                    mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                }
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error refreshing video output: ${e.message}")
            }
        }
    }

    override fun reattach() {
        val container = currentContainer ?: return
        detach()
        attach(container)
    }

    override fun detach() {
        android.util.Log.d("LibVlcEngine", "detach() called, isAttached=$isAttached")
        
        if (!isAttached) {
            return
        }
        
        try {
            mediaPlayer?.detachViews()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error detaching views: ${e.message}")
        }
        
        videoLayout?.let { layout ->
            try {
                (layout.parent as? ViewGroup)?.removeView(layout)
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error removing layout: ${e.message}")
            }
        }
        
        videoLayout = null
        surfaceView = null
        currentContainer = null
        isAttached = false
    }

    override fun prepare(url: String, startPosition: Long) {
        this.startPosition = -1L
        libVlc?.let { vlc ->
            try {
                val isLocalFile = !url.startsWith("http://") && !url.startsWith("https://")
                
                val media = when {
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        Media(vlc, Uri.parse(url))
                    }
                    url.startsWith("content://") -> {
                        Media(vlc, Uri.parse(url))
                    }
                    url.startsWith("file://") -> {
                        Media(vlc, Uri.parse(url))
                    }
                    else -> {
                        // Assume it's a raw file path
                        val file = File(url)
                        val fileUri = Uri.fromFile(file)
                        Media(vlc, fileUri)
                    }
                }.apply {
                    if (startPosition > 0) {
                        addOption(":start-time=${startPosition / 1000f}")
                    }
                    // Enable hardware decoding
                    setHWDecoderEnabled(true, false)
                    
                    if (isLocalFile) {
                        // LOCAL FILES: Minimal caching for instant playback
                        addOption(":file-caching=150")        // Very low for local files
                        addOption(":network-caching=0")       // No network caching needed
                        addOption(":live-caching=0")          // No live caching needed
                        addOption(":clock-jitter=0")          // Instant playback
                        addOption(":clock-synchro=0")         // No sync delay
                        addOption(":avcodec-fast")            // Fast decoding
                        addOption(":avcodec-threads=0")       // Auto-detect threads
                    } else {
                        // NETWORK STREAMS: Ultra optimized
                        addOption(":network-caching=300")     // 300ms
                        addOption(":live-caching=300")        // 300ms
                        addOption(":file-caching=100")        
                        addOption(":clock-jitter=0")          
                        addOption(":clock-synchro=0")
                        addOption(":avcodec-fast")            // Enable fast decoding
                        addOption(":avcodec-threads=0")       // Auto threads         
                        addOption(":sub-text-scale=$subtitleSizeScale")
                    }
                }
                
                mediaPlayer?.media = media
                media.release()
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error preparing media: ${e.message}")
            }
        }
    }

    override fun play() {
        try {
            mediaPlayer?.play()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error playing: ${e.message}")
        }
    }

    override fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error pausing: ${e.message}")
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error stopping: ${e.message}")
        }
    }

    override fun release() {
        try {
            // Proper cleanup sequence
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.setEventListener(null)
                player.detachViews()
                player.release()
            }
            
            libVlc?.release()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error releasing: ${e.message}")
        } finally {
            mediaPlayer = null
            libVlc = null
            videoLayout = null
            surfaceView = null
            context = null
        }
    }

    private var startPosition: Long = -1L

    override fun seekTo(position: Long) {
        try {
            startPosition = position
            mediaPlayer?.time = position
            android.util.Log.d("LibVlcEngine", "Seeking to $position (pending: $startPosition)")
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking: ${e.message}")
        }
    }

    override fun seekBack() {
        try {
            mediaPlayer?.let {
                it.time = (it.time - 10000).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking back: ${e.message}")
        }
    }

    override fun seekForward() {
        try {
            mediaPlayer?.let {
                val newTime = it.time + 10000
                val maxTime = it.length
                it.time = if (maxTime > 0) newTime.coerceAtMost(maxTime) else newTime
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking forward: ${e.message}")
        }
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    override fun isBuffering(): Boolean {
        return isBuffering
    }

    override fun getDuration(): Long {
        return try {
            mediaPlayer?.length ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun setVolume(volume: Float) {
        try {
            mediaPlayer?.volume = (volume * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting volume: ${e.message}")
        }
    }

    override fun getVideoFormat(): String? {
        return try {
            val track = mediaPlayer?.currentVideoTrack
            if (track != null) {
                "${track.width}x${track.height}"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getBitrate(): Long {
        return try {
            val media = mediaPlayer?.media
            val stats = media?.stats
            val bitrate = stats?.inputBitrate?.toLong() ?: 0L
            media?.release()
            bitrate
        } catch (e: Exception) {
            0L
        }
    }

    override fun getDroppedFrames(): Int {
        return try {
            val media = mediaPlayer?.media
            val stats = media?.stats
            val dropped = stats?.lostPictures ?: 0
            media?.release()
            dropped
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Set aspect ratio mode
     * @param mode: "best_fit", "fill", "16:9", "4:3", etc.
     */
    fun setAspectRatio(mode: String) {
        try {
            when (mode.lowercase()) {
                "best_fit" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                "fill" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                "fit_screen" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
                "16:9" -> mediaPlayer?.aspectRatio = "16:9"
                "4:3" -> mediaPlayer?.aspectRatio = "4:3"
                else -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting aspect ratio: ${e.message}")
        }
    }
    
    // Track selection implementation
    override fun getSubtitleTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            tracks.add(Pair(-1, "Disabled"))
            
            mediaPlayer?.spuTracks?.forEach { track ->
                val name = track.name ?: "Track ${track.id}"
                tracks.add(Pair(track.id, name))
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error getting subtitle tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun getAudioTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            
            mediaPlayer?.audioTracks?.forEach { track ->
                val name = track.name ?: "Track ${track.id}"
                tracks.add(Pair(track.id, name))
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error getting audio tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun setSubtitleTrack(trackId: Int) {
        try {
            mediaPlayer?.spuTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting subtitle track: ${e.message}")
        }
    }
    
    override fun setAudioTrack(trackId: Int) {
        try {
            mediaPlayer?.audioTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting audio track: ${e.message}")
        }
    }

    override fun getCurrentSubtitleTrack(): Int {
        return try {
            mediaPlayer?.spuTrack ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    override fun getCurrentAudioTrack(): Int {
        return try {
            mediaPlayer?.audioTrack ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    private var errorCallback: ((Exception) -> Unit)? = null
    private var playbackStateCallback: ((Boolean) -> Unit)? = null
    private var playbackEndedCallback: (() -> Unit)? = null

    override fun setOnErrorCallback(callback: (Exception) -> Unit) {
        errorCallback = callback
    }

    override fun setOnPlaybackStateChanged(callback: (Boolean) -> Unit) {
        playbackStateCallback = callback
    }

    override fun setOnPlaybackEndedCallback(callback: () -> Unit) {
        playbackEndedCallback = callback
    }

    override fun onResume() {
        // VLCVideoLayout usually handles lifecycle automatically
    }

    override fun onPauseLifecycle() {
        // No specific action needed for LibVLC
    }

    override fun setSubtitleSize(size: String) {
        subtitleSizeScale = when(size) {
            "Small" -> 80
            "Normal" -> 100
            "Large" -> 150
            else -> 100
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.rate = speed
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting playback speed: ${e.message}")
        }
    }

    override fun getPlaybackSpeed(): Float {
        return mediaPlayer?.rate ?: 1.0f
    }
}
