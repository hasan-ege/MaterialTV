package com.hasanege.materialtv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import com.hasanege.materialtv.utils.LanguageManager
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject
import javax.inject.Singleton

// Singleton DataStore instance
val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) {

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    // Expose default player preference as enum
    val defaultPlayerPreference: kotlinx.coroutines.flow.StateFlow<com.hasanege.materialtv.data.PlayerPreference> =
        context.settingsDataStore.data.map { prefs ->
            val prefString = prefs[DEFAULT_PLAYER] ?: "VLC"
            try {
                com.hasanege.materialtv.data.PlayerPreference.valueOf(prefString.uppercase())
            } catch (e: IllegalArgumentException) {
                com.hasanege.materialtv.data.PlayerPreference.VLC
            }
        }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, com.hasanege.materialtv.data.PlayerPreference.VLC)

    suspend fun setDefaultPlayerPreference(value: com.hasanege.materialtv.data.PlayerPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[DEFAULT_PLAYER] = value.name
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val DOWNLOAD_NOTIFICATIONS_ENABLED = booleanPreferencesKey("download_notifications_enabled")
        val USE_VLC_FOR_DOWNLOADS = booleanPreferencesKey("use_vlc_for_downloads")
        val AUTO_PLAY_NEXT_EPISODE = booleanPreferencesKey("auto_play_next_episode")
        val DEFAULT_PLAYER = stringPreferencesKey("default_player")
        val STATS_FOR_NERDS = booleanPreferencesKey("stats_for_nerds")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_RETRY_FAILED_DOWNLOADS = booleanPreferencesKey("auto_retry_failed_downloads")
        val START_PAGE = stringPreferencesKey("start_page")
        val AUTO_RESTART_ON_SPEED_DROP = booleanPreferencesKey("auto_restart_on_speed_drop")
        val MIN_DOWNLOAD_SPEED_KBPS = intPreferencesKey("min_download_speed_kbps")
        val SPEED_RESTART_DELAY_SECONDS = intPreferencesKey("speed_restart_delay_seconds")
        val NEXT_EPISODE_THRESHOLD = intPreferencesKey("next_episode_threshold")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")
        val ENABLE_DOWNLOAD_COVERS = booleanPreferencesKey("enable_download_covers")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LAST_UPDATED_DATE = androidx.datastore.preferences.core.longPreferencesKey("last_updated_date")
        val CUSTOM_ACCENT_COLOR = stringPreferencesKey("custom_accent_color")
        val CUSTOM_BACKGROUND_COLOR = stringPreferencesKey("custom_background_color")
        val CUSTOM_TEXT_COLOR = stringPreferencesKey("custom_text_color")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val BOTTOM_NAV_ONLY_ICONS = booleanPreferencesKey("bottom_nav_only_icons")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
        val OPENSUBTITLES_API_KEY = stringPreferencesKey("opensubtitles_api_key")
        val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("preferred_subtitle_language")
    }

    val bottomNavOnlyIcons: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[BOTTOM_NAV_ONLY_ICONS] ?: false
    }

    suspend fun setBottomNavOnlyIcons(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[BOTTOM_NAV_ONLY_ICONS] = enabled
        }
    }

    val maxConcurrentDownloads: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[MAX_CONCURRENT_DOWNLOADS] ?: 3
    }

    val downloadAlgorithm: Flow<DownloadAlgorithm> = flowOf(DownloadAlgorithm.OKHTTP)

    val downloadNotificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[DOWNLOAD_NOTIFICATIONS_ENABLED] ?: true
    }

    val useVlcForDownloads: kotlinx.coroutines.flow.StateFlow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[USE_VLC_FOR_DOWNLOADS] ?: true
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, true)

    val autoPlayNextEpisode: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AUTO_PLAY_NEXT_EPISODE] ?: true
    }

    val defaultPlayer: kotlinx.coroutines.flow.StateFlow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[DEFAULT_PLAYER] ?: "VLC"
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, "VLC")

    val language: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "system"
    }


    val statsForNerds: kotlinx.coroutines.flow.StateFlow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[STATS_FOR_NERDS] ?: false
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    val autoRetryFailedDownloads: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AUTO_RETRY_FAILED_DOWNLOADS] ?: true
    }

    suspend fun setMaxConcurrentDownloads(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[MAX_CONCURRENT_DOWNLOADS] = value
        }
    }

    suspend fun setDownloadAlgorithm(value: DownloadAlgorithm) {
        // No-op, only OKHTTP supported
    }

    suspend fun setDownloadNotificationsEnabled(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DOWNLOAD_NOTIFICATIONS_ENABLED] = value
        }
    }

    suspend fun setUseVlcForDownloads(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[USE_VLC_FOR_DOWNLOADS] = value
        }
    }

    suspend fun setAutoPlayNextEpisode(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_PLAY_NEXT_EPISODE] = value
        }
    }

    suspend fun setDefaultPlayer(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[DEFAULT_PLAYER] = value
        }
    }

    suspend fun setLanguage(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[LANGUAGE] = value
        }
        LanguageManager.applyLanguage(value)
    }

    suspend fun setStatsForNerds(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[STATS_FOR_NERDS] = value
        }
    }

    suspend fun setAutoRetryFailedDownloads(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_RETRY_FAILED_DOWNLOADS] = value
        }
    }

    val useFFmpegDownloader: Flow<Boolean> = flowOf(false) // Deprecated

    suspend fun setUseFFmpegDownloader(value: Boolean) {
        // No-op
    }

    val startPage: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[START_PAGE] ?: "movies"
    }

    suspend fun setStartPage(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[START_PAGE] = value
        }
    }
    
    // Auto-restart on speed drop
    val autoRestartOnSpeedDrop: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AUTO_RESTART_ON_SPEED_DROP] ?: false
    }
    
    suspend fun setAutoRestartOnSpeedDrop(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_RESTART_ON_SPEED_DROP] = value
        }
    }
    
    // Minimum download speed (KB/s) before restart
    val minDownloadSpeedKbps: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[MIN_DOWNLOAD_SPEED_KBPS] ?: 100 // Default 100 KB/s
    }
    
    suspend fun setMinDownloadSpeedKbps(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[MIN_DOWNLOAD_SPEED_KBPS] = value
        }
    }
    
    // Speed restart delay (seconds) - default 10, range 0-60
    val speedRestartDelaySeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[SPEED_RESTART_DELAY_SECONDS] ?: 10
    }
    
    suspend fun setSpeedRestartDelaySeconds(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[SPEED_RESTART_DELAY_SECONDS] = value.coerceIn(0, 60)
        }
    }
    
    // Continue Watching Threshold (Minutes)
    val nextEpisodeThresholdMinutes: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[NEXT_EPISODE_THRESHOLD] ?: 5 // Default 5 minutes
    }
    
    suspend fun setNextEpisodeThresholdMinutes(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[NEXT_EPISODE_THRESHOLD] = value
        }
    }

    // First Launch
    val isFirstLaunch: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH] = !completed
        }
    }

    // Disclaimer Acceptance
    val isDisclaimerAccepted: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[DISCLAIMER_ACCEPTED] ?: false
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DISCLAIMER_ACCEPTED] = accepted
        }
    }

    // User Profile
    val userName: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        val encrypted = prefs[USER_NAME]
        if (!encrypted.isNullOrEmpty()) com.hasanege.materialtv.utils.CryptoManager.decrypt(encrypted) else null
    }

    suspend fun setUserName(name: String) {
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(name)
        context.settingsDataStore.edit { prefs ->
            prefs[USER_NAME] = encrypted
        }
    }

    val userAvatarPath: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        val encrypted = prefs[USER_AVATAR_PATH]
        if (!encrypted.isNullOrEmpty()) com.hasanege.materialtv.utils.CryptoManager.decrypt(encrypted) else null
    }

    suspend fun setUserAvatarPath(path: String) {
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(path)
        context.settingsDataStore.edit { prefs ->
            prefs[USER_AVATAR_PATH] = encrypted
        }
    }


    // Download Covers
    val enableDownloadCovers: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[ENABLE_DOWNLOAD_COVERS] ?: true
    }

    suspend fun setEnableDownloadCovers(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[ENABLE_DOWNLOAD_COVERS] = enabled
        }
    }

    // Theme Mode
    val themeMode: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    // Font Family
    val fontFamily: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[FONT_FAMILY] ?: "default"
    }

    suspend fun setFontFamily(font: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[FONT_FAMILY] = font
        }
    }

    // Last Updated Date
    val lastUpdatedDate: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[LAST_UPDATED_DATE] ?: 0L
    }

    suspend fun setLastUpdatedDate(timeInMillis: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_UPDATED_DATE] = timeInMillis
        }
    }

    // Custom Accent Color
    val customAccentColor: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[CUSTOM_ACCENT_COLOR] ?: "#6750A4"
    }

    suspend fun setCustomAccentColor(hexColor: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[CUSTOM_ACCENT_COLOR] = hexColor
        }
    }

    // Custom Background Color
    val customBackgroundColor: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[CUSTOM_BACKGROUND_COLOR] ?: "#000000"
    }

    suspend fun setCustomBackgroundColor(hexColor: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[CUSTOM_BACKGROUND_COLOR] = hexColor
        }
    }

    // Custom Text Color
    val customTextColor: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[CUSTOM_TEXT_COLOR] ?: "#FFFFFF"
    }

    suspend fun setCustomTextColor(hexColor: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[CUSTOM_TEXT_COLOR] = hexColor
        }
    }

    // Navigation Bar Style
    val navBarStyle: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[NAV_BAR_STYLE] ?: "bottom"
    }

    suspend fun setNavBarStyle(style: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[NAV_BAR_STYLE] = style
        }
    }

    // TMDB API Key
    val tmdbApiKey: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        val encrypted = prefs[TMDB_API_KEY]
        if (!encrypted.isNullOrEmpty()) com.hasanege.materialtv.utils.CryptoManager.decrypt(encrypted) else null
    }

    suspend fun setTmdbApiKey(key: String) {
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(key)
        context.settingsDataStore.edit { prefs ->
            prefs[TMDB_API_KEY] = encrypted
        }
    }

    // OpenSubtitles API Key
    val openSubtitlesApiKey: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        val encrypted = prefs[OPENSUBTITLES_API_KEY]
        if (!encrypted.isNullOrEmpty()) com.hasanege.materialtv.utils.CryptoManager.decrypt(encrypted) else null
    }

    suspend fun setOpenSubtitlesApiKey(key: String) {
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(key)
        context.settingsDataStore.edit { prefs ->
            prefs[OPENSUBTITLES_API_KEY] = encrypted
        }
    }

    // Preferred Subtitle Language (e.g. "tr", "en")
    val preferredSubtitleLanguage: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[PREFERRED_SUBTITLE_LANGUAGE]
    }

    suspend fun setPreferredSubtitleLanguage(lang: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[PREFERRED_SUBTITLE_LANGUAGE] = lang
        }
    }
}
