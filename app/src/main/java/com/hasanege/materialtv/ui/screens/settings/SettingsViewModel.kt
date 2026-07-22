package com.hasanege.materialtv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val maxConcurrentDownloads: StateFlow<Int> = repository.maxConcurrentDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val downloadNotificationsEnabled: StateFlow<Boolean> = repository.downloadNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val useVlcForDownloads: StateFlow<Boolean> = repository.useVlcForDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoPlayNextEpisode: StateFlow<Boolean> = repository.autoPlayNextEpisode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val defaultPlayer: StateFlow<com.hasanege.materialtv.data.PlayerPreference> = repository.defaultPlayerPreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.hasanege.materialtv.data.PlayerPreference.HYBRID)
    val statsForNerds: StateFlow<Boolean> = repository.statsForNerds
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val autoRetryFailedDownloads: StateFlow<Boolean> = repository.autoRetryFailedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val bottomNavOnlyIcons: StateFlow<Boolean> = repository.bottomNavOnlyIcons
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setBottomNavOnlyIcons(enabled: Boolean) {
        viewModelScope.launch { repository.setBottomNavOnlyIcons(enabled) }
    }


    val enableDownloadCovers: StateFlow<Boolean> = repository.enableDownloadCovers
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val useFFmpegDownloader: StateFlow<Boolean> = repository.useFFmpegDownloader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch {
            repository.setMaxConcurrentDownloads(value)
        }
    }

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDownloadNotificationsEnabled(enabled) }
    }

    fun setUseVlcForDownloads(enabled: Boolean) {
        viewModelScope.launch { repository.setUseVlcForDownloads(enabled) }
    }
    

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoPlayNextEpisode(enabled) }
    }

    fun setDefaultPlayerPreference(value: com.hasanege.materialtv.data.PlayerPreference) {
        viewModelScope.launch { repository.setDefaultPlayerPreference(value) }
    }

    fun setStatsForNerds(enabled: Boolean) {
        viewModelScope.launch { repository.setStatsForNerds(enabled) }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch { repository.setLanguage(value) }
    }

    fun setAutoRetryFailedDownloads(value: Boolean) {
        viewModelScope.launch {
            repository.setAutoRetryFailedDownloads(value)
        }
    }

    fun setUseFFmpegDownloader(value: Boolean) {
        viewModelScope.launch {
            repository.setUseFFmpegDownloader(value)
        }
    }

    val startPage: StateFlow<String> = repository.startPage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "movies")

    fun setStartPage(value: String) {
        viewModelScope.launch {
            repository.setStartPage(value)
        }
    }


    fun setEnableDownloadCovers(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableDownloadCovers(enabled)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            com.hasanege.materialtv.WatchHistoryManager.clearHistory()
        }
    }
    
    // Auto-restart on speed drop
    val autoRestartOnSpeedDrop: StateFlow<Boolean> = repository.autoRestartOnSpeedDrop
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun setAutoRestartOnSpeedDrop(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRestartOnSpeedDrop(enabled)
        }
    }
    
    // Minimum download speed threshold (KB/s)
    val minDownloadSpeedKbps: StateFlow<Int> = repository.minDownloadSpeedKbps
        .stateIn(viewModelScope, SharingStarted.Eagerly, 100)
    
    fun setMinDownloadSpeedKbps(value: Int) {
        viewModelScope.launch {
            repository.setMinDownloadSpeedKbps(value)
        }
    }
    
    // Speed restart delay (seconds) - how long to wait before restart
    val speedRestartDelaySeconds: StateFlow<Int> = repository.speedRestartDelaySeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    
    fun setSpeedRestartDelaySeconds(value: Int) {
        viewModelScope.launch {
            repository.setSpeedRestartDelaySeconds(value)
        }
    }

    // Continue Watching Threshold
    val nextEpisodeThresholdMinutes: StateFlow<Int> = repository.nextEpisodeThresholdMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    fun setNextEpisodeThresholdMinutes(value: Int) {
        viewModelScope.launch {
            repository.setNextEpisodeThresholdMinutes(value)
        }
    }

    // Theme Mode
    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    // Font Family
    val fontFamily: StateFlow<String> = repository.fontFamily
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    fun setFontFamily(font: String) {
        viewModelScope.launch {
            repository.setFontFamily(font)
        }
    }

    fun handleCustomFontSelection(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fontsDir = java.io.File(context.filesDir, "fonts")
                if (!fontsDir.exists()) fontsDir.mkdirs()
                
                // Clear old custom fonts to save space
                fontsDir.listFiles()?.forEach { it.delete() }
                
                val fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                val destFile = java.io.File(fontsDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                repository.setFontFamily(destFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Last Updated Date
    val lastUpdatedDate: StateFlow<Long> = repository.lastUpdatedDate
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // Custom Accent Color
    val customAccentColor: StateFlow<String> = repository.customAccentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "#6750A4")

    fun setCustomAccentColor(hexColor: String) {
        viewModelScope.launch {
            repository.setCustomAccentColor(hexColor)
        }
    }

    // Custom Background Color
    val customBackgroundColor: StateFlow<String> = repository.customBackgroundColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "#000000")

    fun setCustomBackgroundColor(hexColor: String) {
        viewModelScope.launch {
            repository.setCustomBackgroundColor(hexColor)
        }
    }

    // Custom Text Color
    val customTextColor: StateFlow<String> = repository.customTextColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "#FFFFFF")

    fun setCustomTextColor(hexColor: String) {
        viewModelScope.launch {
            repository.setCustomTextColor(hexColor)
        }
    }

    // Navigation Bar Style
    val navBarStyle: StateFlow<String> = repository.navBarStyle
        .stateIn(viewModelScope, SharingStarted.Eagerly, "bottom")

    fun setNavBarStyle(style: String) {
        viewModelScope.launch {
            repository.setNavBarStyle(style)
        }
    }
}
