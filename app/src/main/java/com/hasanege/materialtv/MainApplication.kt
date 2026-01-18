package com.hasanege.materialtv

import android.app.Application
import androidx.work.Configuration
import com.hasanege.materialtv.data.SettingsRepository
import com.hasanege.materialtv.data.settingsDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hasanege.materialtv.data.PlaylistManager
import com.hasanege.materialtv.network.CredentialsManager
import com.hasanege.materialtv.utils.LanguageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    lateinit var credentialsManager: CredentialsManager
    lateinit var playlistManager: PlaylistManager

    companion object {
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        credentialsManager = CredentialsManager(this)
        playlistManager = PlaylistManager(this)
        
        // Restore session early to avoid crashes in Activities
        restoreSession()
        
        FavoritesManager.initialize(this)
        WatchHistoryManager.initialize(this)
        applySavedLanguage()
    }

    private fun restoreSession() {
        val serverUrl = credentialsManager.getServerUrl()
        val username = credentialsManager.getUsername()
        val password = credentialsManager.getPassword()
        val m3uUrl = credentialsManager.getM3uUrl()

        if (!m3uUrl.isNullOrBlank()) {
            com.hasanege.materialtv.network.SessionManager.initializeM3u(m3uUrl)
        } else if (!serverUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
            com.hasanege.materialtv.network.SessionManager.initialize(serverUrl, username, password)
        }
    }

    private fun applySavedLanguage() {
        runBlocking {
            val prefs = settingsDataStore.data.first()
            val languageCode = prefs[SettingsRepository.LANGUAGE] ?: "system"
            LanguageManager.applyLanguage(languageCode)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // Increased to 30% for smoother scrolling
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.10) // Increase disk cache to 10% or at least 250MB
                    .build()
            }
            .crossfade(true) // Enable crossfade globally
            .respectCacheHeaders(false) // Ignore cache headers to force caching
            .build()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            // Use default executor - queue management is handled by DownloadQueueProcessor
            .build()
}
