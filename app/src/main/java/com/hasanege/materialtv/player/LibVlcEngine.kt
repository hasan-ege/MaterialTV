package com.hasanege.materialtv.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hasanege.materialtv.utils.StringUtils
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
            // Standard VLC arguments for IPTV stability
            val args = ArrayList<String>().apply {
                add("--network-caching=1000")
                add("--http-reconnect")
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
                        MediaPlayer.Event.Playing,
                        MediaPlayer.Event.TimeChanged,
                        MediaPlayer.Event.PositionChanged,
                        MediaPlayer.Event.LengthChanged -> {
                            if (pendingStartPosition != -1L) {
                                val targetPos = pendingStartPosition
                                val isSeekable = mediaPlayer?.isSeekable == true
                                val length = mediaPlayer?.length ?: 0L
                                if (isSeekable || length > 0L) {
                                    pendingStartPosition = -1L
                                    android.util.Log.d("LibVlcEngine", "Content loaded! Seeking to $targetPos ms (length=$length, isSeekable=$isSeekable)")
                                    mediaPlayer?.time = targetPos
                                    if (length > 0L) {
                                        val pct = (targetPos.toDouble() / length.toDouble()).coerceIn(0.0, 1.0).toFloat()
                                        mediaPlayer?.position = pct
                                    }
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        try {
                                            if (mediaPlayer?.isPlaying == true) {
                                                mediaPlayer?.time = targetPos
                                                val len = mediaPlayer?.length ?: 0L
                                                if (len > 0L) {
                                                    val pct = (targetPos.toDouble() / len.toDouble()).coerceIn(0.0, 1.0).toFloat()
                                                    mediaPlayer?.position = pct
                                                }
                                                android.util.Log.d("LibVlcEngine", "VLC Fallback seek (200ms) applied to $targetPos ms")
                                            }
                                        } catch (e: Exception) {}
                                    }, 200L)
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        try {
                                            if (mediaPlayer?.isPlaying == true) {
                                                mediaPlayer?.time = targetPos
                                                val len = mediaPlayer?.length ?: 0L
                                                if (len > 0L) {
                                                    val pct = (targetPos.toDouble() / len.toDouble()).coerceIn(0.0, 1.0).toFloat()
                                                    mediaPlayer?.position = pct
                                                }
                                                android.util.Log.d("LibVlcEngine", "VLC Fallback seek (600ms) applied to $targetPos ms")
                                            }
                                        } catch (e: Exception) {}
                                    }, 600L)
                                }
                            }
                            if (event.type == MediaPlayer.Event.Playing) {
                                playbackStateCallback?.invoke(true)
                            }
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
            android.util.Log.e("LibVlcEngine", "Error initializing VLC: ${StringUtils.sanitizeUrl(e.message)}")
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
                android.util.Log.e("LibVlcEngine", "Error refreshing video output: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error detaching views: ${StringUtils.sanitizeUrl(e.message)}")
        }
        
        videoLayout?.let { layout ->
            try {
                (layout.parent as? ViewGroup)?.removeView(layout)
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error removing layout: ${StringUtils.sanitizeUrl(e.message)}")
            }
        }
        
        videoLayout = null
        surfaceView = null
        currentContainer = null
        isAttached = false
    }

    private var pendingStartPosition: Long = -1L

    override fun prepare(url: String, startPosition: Long) {
        this.pendingStartPosition = if (startPosition > 0) startPosition else -1L
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
                        val file = File(url)
                        val fileUri = Uri.fromFile(file)
                        Media(vlc, fileUri)
                    }
                }.apply {
                    if (startPosition > 0) {
                        addOption(":start-time=${startPosition / 1000L}")
                    }
                    setHWDecoderEnabled(true, false)
                    
                    if (isLocalFile) {
                        addOption(":file-caching=300")
                    } else {
                        addOption(":network-caching=1000")
                        addOption(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                        addOption(":sub-text-scale=$subtitleSizeScale")
                    }
                }
                
                mediaPlayer?.media = media
                media.release()
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error preparing media: ${StringUtils.sanitizeUrl(e.message)}")
            }
        }
    }

    override fun seekTo(position: Long) {
        try {
            pendingStartPosition = -1L
            mediaPlayer?.time = position
            val len = mediaPlayer?.length ?: 0L
            if (len > 0L) {
                val pct = (position.toDouble() / len.toDouble()).coerceIn(0.0, 1.0).toFloat()
                mediaPlayer?.position = pct
            }
            android.util.Log.d("LibVlcEngine", "Manual seekTo called: $position ms")
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }

    override fun play() {
        try {
            mediaPlayer?.play()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error playing: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }

    override fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error pausing: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error stopping: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error releasing: ${StringUtils.sanitizeUrl(e.message)}")
        } finally {
            mediaPlayer = null
            libVlc = null
            videoLayout = null
            surfaceView = null
            context = null
        }
    }

    override fun seekBack() {
        try {
            mediaPlayer?.let {
                it.time = (it.time - 10000).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking back: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error seeking forward: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error setting volume: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error setting aspect ratio: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error getting subtitle tracks: ${StringUtils.sanitizeUrl(e.message)}")
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
            android.util.Log.e("LibVlcEngine", "Error getting audio tracks: ${StringUtils.sanitizeUrl(e.message)}")
            emptyList()
        }
    }
    
    override fun setSubtitleTrack(trackId: Int) {
        try {
            mediaPlayer?.spuTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting subtitle track: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }
    
    override fun setAudioTrack(trackId: Int) {
        try {
            mediaPlayer?.audioTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting audio track: ${StringUtils.sanitizeUrl(e.message)}")
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

    override fun addExternalSubtitle(filePath: String, language: String, label: String) {
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                val uri = android.net.Uri.fromFile(file)
                // In LibVLC, 0 represents Media.Slave.Type.Subtitle
                val success = mediaPlayer?.addSlave(0, uri, true) ?: false
            } else {
                android.util.Log.e("LibVlcEngine", "External subtitle file not found: $filePath")
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error adding external subtitle: ${StringUtils.sanitizeUrl(e.message)}", e)
        }
    }

    private var vlcSubtitleDelayMs: Long = 0L

    override fun setSubtitleDelay(delayMs: Long) {
        vlcSubtitleDelayMs = delayMs
        try {
            mediaPlayer?.setSpuDelay(delayMs * 1000L)
            android.util.Log.d("LibVlcEngine", "Subtitle delay set to $delayMs ms")
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting SPU delay: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }

    override fun getSubtitleDelay(): Long {
        return try {
            val delayUs = mediaPlayer?.spuDelay ?: (vlcSubtitleDelayMs * 1000L)
            delayUs / 1000L
        } catch (e: Exception) {
            vlcSubtitleDelayMs
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
            android.util.Log.e("LibVlcEngine", "Error setting playback speed: ${StringUtils.sanitizeUrl(e.message)}")
        }
    }

    override fun getPlaybackSpeed(): Float {
        return mediaPlayer?.rate ?: 1.0f
    }
}
