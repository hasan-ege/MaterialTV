package com.hasanege.materialtv.player

import android.content.Context
import android.view.ViewGroup

interface PlayerEngine {
    fun initialize(context: Context)
    fun attach(container: ViewGroup)
    fun detach()
    fun reattach() // Force recreate view
    fun prepare(url: String, startPosition: Long = 0L)
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun seekTo(position: Long)
    fun seekBack()
    fun seekForward()
    fun isPlaying(): Boolean
    fun isBuffering(): Boolean
    fun getDuration(): Long
    fun getCurrentPosition(): Long
    fun setVolume(volume: Float)
    
    // Stats
    fun getVideoFormat(): String?
    fun getBitrate(): Long
    fun getDroppedFrames(): Int
    
    // Track selection
    fun getSubtitleTracks(): List<Pair<Int, String>>
    fun getAudioTracks(): List<Pair<Int, String>>
    fun setSubtitleTrack(trackId: Int)
    fun setAudioTrack(trackId: Int)
    fun getCurrentSubtitleTrack(): Int
    fun getCurrentAudioTrack(): Int
    fun addExternalSubtitle(filePath: String, language: String = "tr", label: String = "External Subtitle") {}

    // Error handling
    fun setOnErrorCallback(callback: (Exception) -> Unit)
    
    // State handling
    fun setOnPlaybackStateChanged(callback: (Boolean) -> Unit)
    fun setOnPlaybackEndedCallback(callback: () -> Unit)
    
    // Lifecycle
    fun onResume() {}
    fun onPauseLifecycle() {} // Rename avoid conflict with pause() command
    fun setSubtitleSize(size: String) {}
    
    // Subtitle delay / calibration (in milliseconds)
    fun setSubtitleDelay(delayMs: Long) {}
    fun getSubtitleDelay(): Long = 0L

    // Speed control
    fun setPlaybackSpeed(speed: Float) {}
    fun getPlaybackSpeed(): Float = 1.0f
}
