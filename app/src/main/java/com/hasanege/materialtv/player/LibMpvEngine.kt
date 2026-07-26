package com.hasanege.materialtv.player

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hasanege.materialtv.utils.StringUtils
import dev.jdtech.mpv.MPVLib
import java.io.File

class LibMpvEngine : PlayerEngine {
    private var mpv: MPVLib? = null
    private var context: Context? = null
    private var surfaceView: SurfaceView? = null
    private var surfaceHolderCallback: SurfaceHolder.Callback? = null
    private var currentContainer: ViewGroup? = null
    private var isAttached: Boolean = false
    private var isBufferingState: Boolean = false
    private var isPlayingState: Boolean = false
    private var currentUrl: String? = null
    private var pendingStartPosition: Long = -1L

    private var errorCallback: ((Exception) -> Unit)? = null
    private var playbackStateCallback: ((Boolean) -> Unit)? = null
    private var playbackEndedCallback: (() -> Unit)? = null

    override fun initialize(context: Context) {
        this.context = context
        try {
            val mpvInstance = MPVLib.create(context) ?: throw IllegalStateException("Failed to create MPVLib instance")
            mpvInstance.setOptionString("vo", "gpu")
            mpvInstance.setOptionString("hwdec", "auto")
            mpvInstance.setOptionString("sub-font-size", "45")
            mpvInstance.setOptionString("tls-verify", "no")
            mpvInstance.setOptionString(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            )

            mpvInstance.init()

            mpvInstance.addObserver(object : MPVLib.EventObserver {
                override fun eventProperty(property: String) {}
                override fun eventProperty(property: String, value: Long) {}
                override fun eventProperty(property: String, value: Double) {}
                override fun eventProperty(property: String, value: String) {}

                override fun eventProperty(property: String, value: Boolean) {
                    when (property) {
                        "pause" -> {
                            isPlayingState = !value
                            playbackStateCallback?.invoke(isPlayingState)
                        }
                        "paused-for-cache" -> {
                            isBufferingState = value
                        }
                    }
                }

                override fun event(eventId: Int) {
                    when (eventId) {
                        MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                            Log.d("LibMpvEngine", "File loaded successfully")
                            if (pendingStartPosition > 0L) {
                                val seekSec = pendingStartPosition / 1000.0
                                mpvInstance.setPropertyDouble("time-pos", seekSec)
                                pendingStartPosition = -1L
                            }
                            isPlayingState = true
                            playbackStateCallback?.invoke(true)
                        }
                        MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                            Log.d("LibMpvEngine", "Playback end reached")
                            isPlayingState = false
                            playbackStateCallback?.invoke(false)
                            playbackEndedCallback?.invoke()
                        }
                        MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                            if (pendingStartPosition > 0L) {
                                val posSec = pendingStartPosition / 1000.0
                                pendingStartPosition = 0L
                                mpvInstance.setPropertyDouble("time-pos", posSec)
                                Log.d("LibMpvEngine", "MPV RESTART event: Seek applied to $posSec sec")
                            }
                            isPlayingState = !(mpvInstance.getPropertyBoolean("pause") ?: false)
                            playbackStateCallback?.invoke(isPlayingState)
                        }
                        MPVLib.MpvEvent.MPV_EVENT_SHUTDOWN -> {
                            Log.d("LibMpvEngine", "MPV Shutdown")
                            isPlayingState = false
                        }
                    }
                }
            })

            mpvInstance.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            mpvInstance.observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)

            mpv = mpvInstance
        } catch (t: Throwable) {
            Log.e("LibMpvEngine", "Error initializing libmpv: ${t.message}", t)
            throw Exception("MPV native library load failed on this device (${t.localizedMessage}).", t)
        }
    }

    override fun attach(container: ViewGroup) {
        val ctx = context ?: container.context
        currentContainer = container

        if (surfaceView == null) {
            val sv = SurfaceView(ctx)
            val callback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.d("LibMpvEngine", "Surface created, attaching surface to mpv")
                    mpv?.attachSurface(holder.surface)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.d("LibMpvEngine", "Surface destroyed, detaching surface from mpv")
                    mpv?.detachSurface()
                }
            }
            surfaceHolderCallback = callback
            sv.holder.addCallback(callback)
            surfaceView = sv
        }

        surfaceView?.let { sv ->
            if (sv.parent != null && sv.parent != container) {
                (sv.parent as? ViewGroup)?.removeView(sv)
            }
            if (sv.parent == null) {
                container.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            if (sv.holder.surface?.isValid == true) {
                mpv?.attachSurface(sv.holder.surface)
            }
        }
        isAttached = true
    }

    override fun detach() {
        mpv?.detachSurface()
        surfaceView?.let { sv ->
            (sv.parent as? ViewGroup)?.removeView(sv)
        }
        isAttached = false
    }

    override fun reattach() {
        val container = currentContainer ?: return
        detach()
        attach(container)
    }

    override fun prepare(url: String, startPosition: Long) {
        currentUrl = url
        pendingStartPosition = startPosition
        Log.d("LibMpvEngine", "prepare: url=$url, startPosition=$startPosition")
        if (startPosition > 0) {
            val posSec = startPosition / 1000.0
            mpv?.command(arrayOf("loadfile", url, "replace", "start=$posSec"))
        } else {
            mpv?.command(arrayOf("loadfile", url))
        }
    }

    override fun play() {
        mpv?.setPropertyBoolean("pause", false)
        isPlayingState = true
        playbackStateCallback?.invoke(true)
    }

    override fun pause() {
        mpv?.setPropertyBoolean("pause", true)
        isPlayingState = false
        playbackStateCallback?.invoke(false)
    }

    override fun stop() {
        mpv?.command(arrayOf("stop"))
        isPlayingState = false
        playbackStateCallback?.invoke(false)
    }

    override fun release() {
        detach()
        mpv?.destroy()
        mpv = null
        surfaceView = null
        currentContainer = null
    }

    override fun seekTo(position: Long) {
        pendingStartPosition = 0L
        val posSec = position / 1000.0
        mpv?.setPropertyDouble("time-pos", posSec)
    }

    override fun seekBack() {
        val current = getCurrentPosition()
        seekTo((current - 10000).coerceAtLeast(0))
    }

    override fun seekForward() {
        val current = getCurrentPosition()
        val duration = getDuration()
        seekTo((current + 10000).coerceAtMost(duration))
    }

    override fun isPlaying(): Boolean {
        val paused = mpv?.getPropertyBoolean("pause") ?: true
        return !paused
    }

    override fun isBuffering(): Boolean {
        return isBufferingState
    }

    override fun getDuration(): Long {
        val durSec = mpv?.getPropertyDouble("duration") ?: 0.0
        return (durSec * 1000).toLong().coerceAtLeast(0L)
    }

    override fun getCurrentPosition(): Long {
        val posSec = mpv?.getPropertyDouble("time-pos") ?: 0.0
        return (posSec * 1000).toLong().coerceAtLeast(0L)
    }

    override fun setVolume(volume: Float) {
        val mpvVol = (volume * 100).toInt().coerceIn(0, 100)
        mpv?.setPropertyInt("volume", mpvVol)
    }

    override fun getVideoFormat(): String? {
        val codec = mpv?.getPropertyString("video-format") ?: "mpv (FFmpeg)"
        val w = mpv?.getPropertyInt("video-params/w") ?: 0
        val h = mpv?.getPropertyInt("video-params/h") ?: 0
        return if (w > 0 && h > 0) "$codec (${w}x${h})" else codec
    }

    override fun getBitrate(): Long {
        val bps = mpv?.getPropertyInt("video-bitrate") ?: 0
        return bps.toLong()
    }

    override fun getDroppedFrames(): Int {
        return mpv?.getPropertyInt("frame-drop-count") ?: 0
    }

    override fun getSubtitleTracks(): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        list.add(Pair(-1, "Off"))
        val mpvInstance = mpv ?: return list

        val count = mpvInstance.getPropertyInt("track-list/count") ?: 0
        for (i in 0 until count) {
            val type = mpvInstance.getPropertyString("track-list/$i/type")
            if (type == "sub") {
                val id = mpvInstance.getPropertyInt("track-list/$i/id") ?: (i + 1)
                val lang = mpvInstance.getPropertyString("track-list/$i/lang") ?: ""
                val title = mpvInstance.getPropertyString("track-list/$i/title") ?: ""
                val name = listOf(title, lang).filter { it.isNotBlank() }.joinToString(" - ")
                list.add(Pair(id, if (name.isNotBlank()) name else "Subtitle #$id"))
            }
        }
        return list
    }

    override fun getAudioTracks(): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        val mpvInstance = mpv ?: return list

        val count = mpvInstance.getPropertyInt("track-list/count") ?: 0
        for (i in 0 until count) {
            val type = mpvInstance.getPropertyString("track-list/$i/type")
            if (type == "audio") {
                val id = mpvInstance.getPropertyInt("track-list/$i/id") ?: (i + 1)
                val lang = mpvInstance.getPropertyString("track-list/$i/lang") ?: ""
                val title = mpvInstance.getPropertyString("track-list/$i/title") ?: ""
                val name = listOf(title, lang).filter { it.isNotBlank() }.joinToString(" - ")
                list.add(Pair(id, if (name.isNotBlank()) name else "Audio #$id"))
            }
        }
        return list
    }

    override fun setSubtitleTrack(trackId: Int) {
        if (trackId == -1) {
            mpv?.setPropertyString("sid", "no")
        } else {
            mpv?.setPropertyString("sid", trackId.toString())
        }
    }

    override fun setAudioTrack(trackId: Int) {
        if (trackId == -1) {
            mpv?.setPropertyString("aid", "no")
        } else {
            mpv?.setPropertyString("aid", trackId.toString())
        }
    }

    override fun getCurrentSubtitleTrack(): Int {
        val sid = mpv?.getPropertyString("sid") ?: return -1
        if (sid == "no" || sid.isBlank()) return -1
        return sid.toIntOrNull() ?: -1
    }

    override fun getCurrentAudioTrack(): Int {
        val aid = mpv?.getPropertyString("aid") ?: return -1
        if (aid == "no" || aid.isBlank()) return -1
        return aid.toIntOrNull() ?: -1
    }

    override fun addExternalSubtitle(filePath: String, language: String, label: String) {
        Log.d("LibMpvEngine", "Adding external subtitle: $filePath")
        mpv?.command(arrayOf("sub-add", filePath, "select"))
    }

    override fun setOnErrorCallback(callback: (Exception) -> Unit) {
        errorCallback = callback
    }

    override fun setOnPlaybackStateChanged(callback: (Boolean) -> Unit) {
        playbackStateCallback = callback
    }

    override fun setOnPlaybackEndedCallback(callback: () -> Unit) {
        playbackEndedCallback = callback
    }

    override fun setSubtitleSize(size: String) {
        val fontSize = when (size.lowercase()) {
            "small", "küçük" -> "35"
            "large", "büyük" -> "55"
            else -> "45"
        }
        mpv?.setPropertyString("sub-font-size", fontSize)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        val delaySec = delayMs / 1000.0
        mpv?.setPropertyDouble("sub-delay", delaySec)
    }

    override fun getSubtitleDelay(): Long {
        val delaySec = mpv?.getPropertyDouble("sub-delay") ?: 0.0
        return (delaySec * 1000).toLong()
    }

    override fun setPlaybackSpeed(speed: Float) {
        mpv?.setPropertyDouble("speed", speed.toDouble())
    }

    override fun getPlaybackSpeed(): Float {
        val speed = mpv?.getPropertyDouble("speed") ?: 1.0
        return speed.toFloat()
    }
}
