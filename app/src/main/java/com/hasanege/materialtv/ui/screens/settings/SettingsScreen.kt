package com.hasanege.materialtv.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hasanege.materialtv.BuildConfig
import com.hasanege.materialtv.R
import com.hasanege.materialtv.data.ProfilePreferences
import com.hasanege.materialtv.data.SettingsRepository
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
import kotlinx.coroutines.launch

data class SubtitleLanguageOption(val code: String, val name: String, val flag: String)

val ALL_SUBTITLE_LANGUAGES = listOf(
    SubtitleLanguageOption("tr", "Türkçe", "🇹🇷"),
    SubtitleLanguageOption("en", "English", "🇬🇧"),
    SubtitleLanguageOption("de", "Deutsch", "🇩🇪"),
    SubtitleLanguageOption("fr", "Français", "🇫🇷"),
    SubtitleLanguageOption("es", "Español", "🇪🇸"),
    SubtitleLanguageOption("it", "Italiano", "🇮🇹"),
    SubtitleLanguageOption("ru", "Русский", "🇷🇺"),
    SubtitleLanguageOption("pt", "Português", "🇵🇹"),
    SubtitleLanguageOption("ar", "العربية", "🇸🇦"),
    SubtitleLanguageOption("nl", "Nederlands", "🇳🇱"),
    SubtitleLanguageOption("pl", "Polski", "🇵🇱"),
    SubtitleLanguageOption("sv", "Svenska", "🇸🇪"),
    SubtitleLanguageOption("no", "Norsk", "🇳🇴"),
    SubtitleLanguageOption("fi", "Suomi", "🇫🇮"),
    SubtitleLanguageOption("da", "Dansk", "🇩🇰"),
    SubtitleLanguageOption("el", "Ελληνικά", "🇬🇷"),
    SubtitleLanguageOption("zh", "中文", "🇨🇳"),
    SubtitleLanguageOption("ja", "日本語", "🇯🇵"),
    SubtitleLanguageOption("ko", "한국어", "🇰🇷")
)

fun formatSelectedLanguagesDisplay(langCodesString: String): String {
    val codes = langCodesString.split(",")
        .map { it.trim().lowercase(java.util.Locale.getDefault()) }
        .filter { it.isNotEmpty() }
    if (codes.isEmpty()) return "Tüm Diller"
    val names = ALL_SUBTITLE_LANGUAGES
        .filter { lang -> codes.contains(lang.code) }
        .map { "${it.flag} ${it.name}" }
    return if (names.isNotEmpty()) names.joinToString(", ") else codes.joinToString(", ").uppercase(java.util.Locale.getDefault())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val uriHandler = LocalUriHandler.current
    
    // Profile customization
    val profilePreferences = remember { ProfilePreferences(context) }
    val profileName by profilePreferences.profileName.collectAsState(initial = "User")
    val profileImageUrl by profilePreferences.profileImageUrl.collectAsState(initial = "")
    var showEditProfileNameDialog by remember { mutableStateOf(false) }
    
    var selectedImageUriForCrop by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUriForCrop = it
        }
    }

    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val useVlcForDownloads by viewModel.useVlcForDownloads.collectAsState()
    val defaultPlayer by viewModel.defaultPlayer.collectAsState()
    val statsForNerds by viewModel.statsForNerds.collectAsState()
    val autoRetryFailedDownloads by viewModel.autoRetryFailedDownloads.collectAsState()
    val language by viewModel.language.collectAsState()
    val autoRestartOnSpeedDrop by viewModel.autoRestartOnSpeedDrop.collectAsState()
    val downloadNotificationsEnabled by viewModel.downloadNotificationsEnabled.collectAsState()
    val autoPlayNextEpisode by viewModel.autoPlayNextEpisode.collectAsState()
    val enableDownloadCovers by viewModel.enableDownloadCovers.collectAsState()
    
    var showDefaultPlayerDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    
    val themeMode by viewModel.themeMode.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val lastUpdatedDate by viewModel.lastUpdatedDate.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val tmdbValidationState by viewModel.tmdbValidationState.collectAsState()
    var showTmdbDialog by remember { mutableStateOf(false) }

    val openSubtitlesApiKey by viewModel.openSubtitlesApiKey.collectAsState()
    val openSubtitlesValidationState by viewModel.openSubtitlesValidationState.collectAsState()
    val openSubtitlesLanguages by viewModel.openSubtitlesLanguages.collectAsState()
    var showOpenSubtitlesDialog by remember { mutableStateOf(false) }
    var showOpenSubtitlesLanguagesDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    // Profile name edit dialog
    if (showEditProfileNameDialog) {
        ProfileNameEditDialog(
            currentName = profileName,
            onDismiss = { showEditProfileNameDialog = false },
            onSave = { newName ->
                scope.launch {
                    profilePreferences.setProfileName(newName)
                    showEditProfileNameDialog = false
                }
            }
        )
    }

    if (showLanguageDialog) {
        val systemLanguageLabel = stringResource(R.string.settings_language_option_system)
        val englishLanguageLabel = stringResource(R.string.settings_language_option_english)
        val turkishLanguageLabel = stringResource(R.string.settings_language_option_turkish)
        val spanishLanguageLabel = stringResource(R.string.settings_language_option_spanish)
        val germanLanguageLabel = stringResource(R.string.settings_language_option_german)
        val frenchLanguageLabel = stringResource(R.string.settings_language_option_french)
        val portugueseLanguageLabel = stringResource(R.string.settings_language_option_portuguese)
        val russianLanguageLabel = stringResource(R.string.settings_language_option_russian)
        val chineseLanguageLabel = stringResource(R.string.settings_language_option_chinese)
        val urduLanguageLabel = stringResource(R.string.settings_language_option_urdu)
        val japaneseLanguageLabel = stringResource(R.string.settings_language_option_japanese)
        val arabicLanguageLabel = stringResource(R.string.settings_language_option_arabic)

        val options = listOf(
            systemLanguageLabel, 
            englishLanguageLabel, 
            turkishLanguageLabel,
            spanishLanguageLabel,
            germanLanguageLabel,
            frenchLanguageLabel,
            portugueseLanguageLabel,
            russianLanguageLabel,
            chineseLanguageLabel,
            urduLanguageLabel,
            japaneseLanguageLabel,
            arabicLanguageLabel
        )
        val currentLabel = when (language) {
            "en" -> englishLanguageLabel
            "tr" -> turkishLanguageLabel
            "es" -> spanishLanguageLabel
            "de" -> germanLanguageLabel
            "fr" -> frenchLanguageLabel
            "pt" -> portugueseLanguageLabel
            "ru" -> russianLanguageLabel
            "zh" -> chineseLanguageLabel
            "ur" -> urduLanguageLabel
            "ja" -> japaneseLanguageLabel
            "ar" -> arabicLanguageLabel
            else -> systemLanguageLabel
        }
        ExpressiveSelectionDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            options = options,
            currentValue = currentLabel,
            onDismiss = { showLanguageDialog = false },
            onSelect = { selected ->
                val code = when (selected) {
                    englishLanguageLabel -> "en"
                    turkishLanguageLabel -> "tr"
                    spanishLanguageLabel -> "es"
                    germanLanguageLabel -> "de"
                    frenchLanguageLabel -> "fr"
                    portugueseLanguageLabel -> "pt"
                    russianLanguageLabel -> "ru"
                    chineseLanguageLabel -> "zh"
                    urduLanguageLabel -> "ur"
                    japaneseLanguageLabel -> "ja"
                    arabicLanguageLabel -> "ar"
                    else -> "system"
                }
                viewModel.setLanguage(code)
                showLanguageDialog = false
            }
        )
    }

    
    if (showDefaultPlayerDialog) {
        val exoplayerLabel = stringResource(R.string.settings_exoplayer)
        val vlcLabel = stringResource(R.string.settings_vlc)
        val hybridLabel = stringResource(R.string.settings_player_hybrid)
        ExpressiveSelectionDialog(
            title = stringResource(R.string.settings_default_player),
            options = listOf(exoplayerLabel, vlcLabel, hybridLabel),
            currentValue = when (defaultPlayer) {
                com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> exoplayerLabel
                com.hasanege.materialtv.data.PlayerPreference.VLC -> vlcLabel
                com.hasanege.materialtv.data.PlayerPreference.HYBRID -> hybridLabel
            },
            onDismiss = { showDefaultPlayerDialog = false },
            onSelect = { selected ->
                val preference = when (selected) {
                    exoplayerLabel -> com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER
                    vlcLabel -> com.hasanege.materialtv.data.PlayerPreference.VLC
                    else -> com.hasanege.materialtv.data.PlayerPreference.HYBRID
                }
                viewModel.setDefaultPlayerPreference(preference)
                showDefaultPlayerDialog = false
            }
        )
    }

    if (showThemeDialog) {
        val systemLabel = "System Default"
        val lightLabel = "Light"
        val darkLabel = "Dark"
        val amoledLabel = "Amoled Black"
        val customLabel = "Custom"
        ExpressiveSelectionDialog(
            title = "Theme Mode",
            options = listOf(systemLabel, lightLabel, darkLabel, amoledLabel, customLabel),
            currentValue = when (themeMode) {
                "light" -> lightLabel
                "dark" -> darkLabel
                "amoled" -> amoledLabel
                "custom" -> customLabel
                else -> systemLabel
            },
            onDismiss = { showThemeDialog = false },
            onSelect = { selected ->
                val mode = when (selected) {
                    lightLabel -> "light"
                    darkLabel -> "dark"
                    amoledLabel -> "amoled"
                    customLabel -> "custom"
                    else -> "system"
                }
                viewModel.setThemeMode(mode)
                showThemeDialog = false
            }
        )
    }

    if (showColorPickerDialog) {
        var hexInput by remember { mutableStateOf(customAccentColor) }
        val presetColors = listOf(
            "#6750A4", // Default Purple
            "#E53935", // Red
            "#43A047", // Green
            "#1E88E5", // Blue
            "#FB8C00", // Orange
            "#8E24AA", // Deep Purple
            "#00897B", // Teal
            "#F4511E"  // Deep Orange
        )
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Accent Color") },
            text = {
                Column {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            presetColors.take(4).forEach { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .clickable { hexInput = colorHex }
                                        .border(if (hexInput == colorHex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            presetColors.drop(4).take(4).forEach { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .clickable { hexInput = colorHex }
                                        .border(if (hexInput == colorHex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("Hex Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        android.graphics.Color.parseColor(hexInput) // Validate hex format
                        viewModel.setCustomAccentColor(hexInput)
                        showColorPickerDialog = false
                    } catch (e: Exception) {
                        // Invalid hex, ignore or show toast
                    }
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val fontPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.handleCustomFontSelection(context, uri)
        }
    }

    if (showFontDialog) {
        val defaultLabel = "Default"
        val serifLabel = "Serif"
        val sansSerifLabel = "Sans Serif"
        val monospaceLabel = "Monospace"
        val cursiveLabel = "Cursive"
        val customLabel = "Custom Font..."
        ExpressiveSelectionDialog(
            title = "Font Family",
            options = listOf(defaultLabel, serifLabel, sansSerifLabel, monospaceLabel, cursiveLabel, customLabel),
            currentValue = when {
                fontFamily == "serif" -> serifLabel
                fontFamily == "sans-serif" -> sansSerifLabel
                fontFamily == "monospace" -> monospaceLabel
                fontFamily == "cursive" -> cursiveLabel
                fontFamily.startsWith("/") -> customLabel
                else -> defaultLabel
            },
            onDismiss = { showFontDialog = false },
            onSelect = { selected ->
                if (selected == customLabel) {
                    fontPickerLauncher.launch("*/*")
                } else {
                    val font = when (selected) {
                        serifLabel -> "serif"
                        sansSerifLabel -> "sans-serif"
                        monospaceLabel -> "monospace"
                        cursiveLabel -> "cursive"
                        else -> "default"
                    }
                    viewModel.setFontFamily(font)
                }
                showFontDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_clear_history_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.clearWatchHistory()
                    showClearHistoryDialog = false
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_history_cleared), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.settings_clear_history_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }

    // Clear history logic simplified
    if (showClearSearchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchHistoryDialog = false },
            title = { Text(stringResource(R.string.settings_clear_search_history)) },
            text = { Text(stringResource(R.string.settings_clear_history_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Search history logic not yet implemented, but UI is ready
                    showClearSearchHistoryDialog = false
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_history_cleared), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.settings_clear_history_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchHistoryDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }

    if (showTmdbDialog) {
        var apiKeyInput by remember { mutableStateOf(tmdbApiKey ?: "") }
        AlertDialog(
            onDismissRequest = { 
                if (tmdbValidationState != SettingsViewModel.TmdbValidationState.Validating) {
                    showTmdbDialog = false 
                }
            },
            title = { Text(stringResource(R.string.settings_tmdb_api_key)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_tmdb_api_key_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when (tmdbValidationState) {
                        is SettingsViewModel.TmdbValidationState.Validating -> {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        is SettingsViewModel.TmdbValidationState.Success -> {
                            Text("Key geçerli ✓", color = Color.Green, style = MaterialTheme.typography.bodySmall)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1000)
                                showTmdbDialog = false
                            }
                        }
                        is SettingsViewModel.TmdbValidationState.Error -> {
                            Text((tmdbValidationState as SettingsViewModel.TmdbValidationState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.validateAndSaveTmdbApiKey(apiKeyInput) },
                    enabled = tmdbValidationState != SettingsViewModel.TmdbValidationState.Validating
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTmdbDialog = false },
                    enabled = tmdbValidationState != SettingsViewModel.TmdbValidationState.Validating
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }

    if (showOpenSubtitlesDialog) {
        var apiKeyInput by remember { mutableStateOf(openSubtitlesApiKey ?: "") }
        AlertDialog(
            onDismissRequest = { 
                if (openSubtitlesValidationState != SettingsViewModel.OpenSubtitlesValidationState.Validating) {
                    showOpenSubtitlesDialog = false 
                }
            },
            title = { Text(stringResource(R.string.settings_opensubtitles_api_key)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_opensubtitles_api_key_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when (openSubtitlesValidationState) {
                        is SettingsViewModel.OpenSubtitlesValidationState.Validating -> {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        is SettingsViewModel.OpenSubtitlesValidationState.Success -> {
                            Text("Key kaydedildi ✓", color = Color.Green, style = MaterialTheme.typography.bodySmall)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1000)
                                showOpenSubtitlesDialog = false
                            }
                        }
                        is SettingsViewModel.OpenSubtitlesValidationState.Error -> {
                            Text((openSubtitlesValidationState as SettingsViewModel.OpenSubtitlesValidationState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.validateAndSaveOpenSubtitlesApiKey(apiKeyInput) },
                    enabled = openSubtitlesValidationState != SettingsViewModel.OpenSubtitlesValidationState.Validating
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOpenSubtitlesDialog = false },
                    enabled = openSubtitlesValidationState != SettingsViewModel.OpenSubtitlesValidationState.Validating
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }

    if (showOpenSubtitlesLanguagesDialog) {
        val currentCodes = remember(openSubtitlesLanguages) {
            val initialSet = openSubtitlesLanguages.split(",")
                .map { it.trim().lowercase(java.util.Locale.getDefault()) }
                .filter { it.isNotEmpty() }
                .toSet()
            androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initialSet) }
        }

        AlertDialog(
            onDismissRequest = { showOpenSubtitlesLanguagesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.opensubtitles_languages_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_opensubtitles_languages_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(ALL_SUBTITLE_LANGUAGES) { item ->
                            val isSelected = currentCodes.contains(item.code)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            currentCodes.remove(item.code)
                                        } else {
                                            currentCodes.add(item.code)
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            currentCodes.add(item.code)
                                        } else {
                                            currentCodes.remove(item.code)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = item.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val selectedString = currentCodes.joinToString(",")
                    viewModel.saveOpenSubtitlesLanguages(selectedString)
                    showOpenSubtitlesLanguagesDialog = false
                }) {
                    Text(stringResource(R.string.profile_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSubtitlesLanguagesDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            shape = ExpressiveShapes.ExtraLarge
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Settings Section (Hero Card at Top)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 0)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.Large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_profile),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Profile Preview Hero Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar (Round Circle without Border)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profileImageUrl)
                                            .crossfade(300)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .build(),
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profileName.firstOrNull()?.uppercase() ?: "U",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profileName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_profile_edit_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Action items
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Edit,
                            title = stringResource(R.string.settings_profile_name),
                            value = profileName,
                            onClick = { showEditProfileNameDialog = true }
                        )
                        
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.settings_profile_photo),
                            value = if (profileImageUrl.isNotBlank()) stringResource(R.string.settings_profile_photo_change) else stringResource(R.string.settings_profile_photo_add),
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }
            }

            // 2. Appearance & Customization Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 30)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.customization_appearance_title),
                    icon = Icons.Default.Palette
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Brush,
                        title = stringResource(R.string.customization_title),
                        value = stringResource(R.string.customization_appearance_desc),
                        onClick = {
                            context.startActivity(android.content.Intent(context, com.hasanege.materialtv.CustomizationActivity::class.java))
                        }
                    )
                }
            }

            // 3. Account Information Section
            val userInfo = com.hasanege.materialtv.network.SessionManager.userInfo
            val loginType = com.hasanege.materialtv.network.SessionManager.loginType
            val serverUrl = com.hasanege.materialtv.network.SessionManager.serverUrl

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 60)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_account_info),
                    icon = Icons.Default.Cloud
                ) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    
                    fun copyToClipboard(text: String) {
                        clipboardManager.setText(AnnotatedString(text))
                        android.widget.Toast.makeText(context, context.getString(R.string.settings_copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Dns,
                        title = stringResource(R.string.settings_account_server),
                        value = serverUrl ?: stringResource(R.string.unknown),
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = { serverUrl?.let { copyToClipboard(it) } }
                    )

                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.login_username_label),
                        value = com.hasanege.materialtv.network.SessionManager.username ?: stringResource(R.string.unknown),
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = { com.hasanege.materialtv.network.SessionManager.username?.let { copyToClipboard(it) } }
                    )

                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.login_password_label),
                        value = com.hasanege.materialtv.network.SessionManager.password ?: stringResource(R.string.unknown),
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = { com.hasanege.materialtv.network.SessionManager.password?.let { copyToClipboard(it) } }
                    )
                    
                    if (loginType == com.hasanege.materialtv.network.SessionManager.LoginType.XTREAM) {
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_account_status),
                            value = if (userInfo?.status == "Active") stringResource(R.string.settings_account_status_active) else userInfo?.status ?: stringResource(R.string.unknown),
                            onClick = {}
                        )

                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Cable,
                            title = stringResource(R.string.settings_account_connections),
                            value = userInfo?.maxConnections ?: "1",
                            onClick = {}
                        )
                    } else {
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Link,
                            title = stringResource(R.string.login_tab_m3u),
                            value = stringResource(R.string.login_tab_m3u),
                            onClick = {}
                        )
                    }

                    if (lastUpdatedDate > 0) {
                        val formattedDate = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastUpdatedDate))
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.Update,
                            title = stringResource(R.string.settings_last_updated),
                            value = formattedDate,
                            onClick = {}
                        )
                    }
                }
            }
            
            // Download Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 50)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_downloads),
                    icon = Icons.Default.Download
                ) {
                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notifications),
                        checked = downloadNotificationsEnabled,
                        onCheckedChange = { viewModel.setDownloadNotificationsEnabled(it) }
                    )


                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.VideoLibrary,
                        title = stringResource(R.string.settings_use_vlc_downloads),
                        checked = useVlcForDownloads,
                        onCheckedChange = { viewModel.setUseVlcForDownloads(it) }
                    )

                    
                    // Concurrent downloads limit slider
                    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
                    var sliderValue by remember(maxConcurrentDownloads) { 
                        mutableStateOf(maxConcurrentDownloads.toFloat()) 
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_concurrent_limit),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "${sliderValue.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = sliderValue,
                            onValueChange = { 
                                sliderValue = it
                                // Small tick haptic
                                if (it.toInt().toFloat() == it) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onValueChangeFinished = {
                                viewModel.setMaxConcurrentDownloads(sliderValue.toInt())
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = if (sliderValue.toInt() == 1) 
                                stringResource(R.string.settings_concurrent_desc_single)
                            else 
                                stringResource(R.string.settings_concurrent_desc_multiple, sliderValue.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Auto-restart on speed drop toggle
                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.settings_auto_restart_speed_drop),
                        checked = autoRestartOnSpeedDrop,
                        onCheckedChange = { viewModel.setAutoRestartOnSpeedDrop(it) }
                    )
                    
                    // Speed threshold slider
                    AnimatedVisibility(visible = autoRestartOnSpeedDrop) {
                        val minSpeedKbps by viewModel.minDownloadSpeedKbps.collectAsState()
                        val speedOptions = listOf(50, 100, 200, 500, 1000, 2000, 5000)
                        val currentIndex = speedOptions.indexOfFirst { it >= minSpeedKbps }.takeIf { it >= 0 } ?: 0
                        var sliderValueSpeed by remember(minSpeedKbps) { 
                            mutableStateOf(currentIndex.toFloat()) 
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_min_speed_threshold),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                val speedValue = speedOptions[sliderValueSpeed.toInt()]
                                val displaySpeed = if (speedValue >= 1000) {
                                    "${speedValue / 1000} ${stringResource(R.string.unit_mbps)}"
                                } else {
                                    "$speedValue ${stringResource(R.string.unit_kbps)}"
                                }
                                Text(
                                    text = displaySpeed,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = sliderValueSpeed,
                                onValueChange = { 
                                    sliderValueSpeed = it 
                                    if (it.toInt().toFloat() == it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onValueChangeFinished = {
                                    viewModel.setMinDownloadSpeedKbps(speedOptions[sliderValueSpeed.toInt()])
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                valueRange = 0f..(speedOptions.size - 1).toFloat(),
                                steps = speedOptions.size - 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = stringResource(R.string.settings_min_speed_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Speed restart delay slider
                    AnimatedVisibility(visible = autoRestartOnSpeedDrop) {
                        val restartDelaySeconds by viewModel.speedRestartDelaySeconds.collectAsState()
                        var delaySliderValue by remember(restartDelaySeconds) { 
                            mutableStateOf(restartDelaySeconds.toFloat()) 
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_restart_delay),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = "${delaySliderValue.toInt()} ${stringResource(R.string.unit_sec)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = delaySliderValue,
                                onValueChange = { 
                                    delaySliderValue = it 
                                    if (it.toInt().toFloat() == it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onValueChangeFinished = {
                                    viewModel.setSpeedRestartDelaySeconds(delaySliderValue.toInt())
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                valueRange = 0f..60f,
                                steps = 59,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = stringResource(R.string.settings_restart_delay_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }


                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.settings_auto_retry),
                        checked = autoRetryFailedDownloads,
                        onCheckedChange = { viewModel.setAutoRetryFailedDownloads(it) }
                    )

                }
            }


            // Player Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 100)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_player),
                    icon = Icons.Default.PlayCircle
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.PlayArrow,
                        title = stringResource(R.string.settings_default_player),
                        value = when(defaultPlayer) {
                            com.hasanege.materialtv.data.PlayerPreference.EXOPLAYER -> stringResource(R.string.settings_exoplayer)
                            com.hasanege.materialtv.data.PlayerPreference.VLC -> stringResource(R.string.settings_vlc)
                            com.hasanege.materialtv.data.PlayerPreference.HYBRID -> stringResource(R.string.settings_player_hybrid)
                        },
                        onClick = { showDefaultPlayerDialog = true }
                    )


                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Replay,
                        title = stringResource(R.string.settings_autoplay),
                        checked = autoPlayNextEpisode,
                        onCheckedChange = { viewModel.setAutoPlayNextEpisode(it) }                    )
                    
                    // Next Episode Threshold Slider
                    val thresholdMinutes by viewModel.nextEpisodeThresholdMinutes.collectAsState()
                    var thresholdSliderValue by remember(thresholdMinutes) { 
                        mutableStateOf(thresholdMinutes.toFloat()) 
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                         Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_threshold_label),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "${thresholdSliderValue.toInt()} ${stringResource(R.string.unit_min)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = thresholdSliderValue,
                            onValueChange = { 
                                thresholdSliderValue = it 
                                if (it.toInt().toFloat() == it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onValueChangeFinished = {
                                viewModel.setNextEpisodeThresholdMinutes(thresholdSliderValue.toInt())
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = stringResource(R.string.settings_threshold_desc, thresholdSliderValue.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.Analytics,
                        title = stringResource(R.string.settings_stats_for_nerds),
                        checked = statsForNerds,
                        onCheckedChange = { viewModel.setStatsForNerds(it) }
                    )
                }
            }
            
            // General Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_general),
                    icon = Icons.Default.Settings
                ) {
                    val languageLabel = when (language) {
                        "en" -> stringResource(R.string.settings_language_option_english)
                        "tr" -> stringResource(R.string.settings_language_option_turkish)
                        "es" -> stringResource(R.string.settings_language_option_spanish)
                        "de" -> stringResource(R.string.settings_language_option_german)
                        "fr" -> stringResource(R.string.settings_language_option_french)
                        "pt" -> stringResource(R.string.settings_language_option_portuguese)
                        "ru" -> stringResource(R.string.settings_language_option_russian)
                        else -> stringResource(R.string.settings_language_option_system)
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language),
                        value = languageLabel,
                        onClick = { showLanguageDialog = true }
                    )
                    
                    var showStartPageDialog by remember { mutableStateOf(false) }
                    val startPage by viewModel.startPage.collectAsState()
                    
                    // Get localized labels for start page options
                    val homeLabel = stringResource(R.string.nav_home)
                    val favoritesLabel = stringResource(R.string.nav_favorites)
                    val downloadsLabel = stringResource(R.string.nav_downloads)
                    val profileLabel = stringResource(R.string.nav_profile)
                    val startPageTitleLabel = stringResource(R.string.start_page_title)
                    
                    val startPageOptions = listOf(
                        "home" to homeLabel,
                        "favorites" to favoritesLabel,
                        "downloads" to downloadsLabel,
                        "profile" to profileLabel
                    )
                    
                    if (showStartPageDialog) {
                        ExpressiveSelectionDialog(
                            title = startPageTitleLabel,
                            options = startPageOptions.map { it.second },
                            currentValue = startPageOptions.find { it.first == startPage }?.second ?: homeLabel,
                            onDismiss = { showStartPageDialog = false },
                            onSelect = { selected ->
                                val page = startPageOptions.find { it.second == selected }?.first ?: "home"
                                viewModel.setStartPage(page)
                                showStartPageDialog = false
                            }
                        )
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Home,
                        title = startPageTitleLabel,
                        value = startPageOptions.find { it.first == startPage }?.second ?: homeLabel,
                        onClick = { showStartPageDialog = true }
                    )
                }
            }

            // Privacy Settings Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 300)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_privacy),
                    icon = Icons.Default.Security
                ) {
                    ExpressiveSettingActionItem(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.settings_clear_search_history),
                        onClick = { showClearSearchHistoryDialog = true }
                    )

                    ExpressiveSettingActionItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_clear_history),
                        isDestructive = true,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // Current Features Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 150)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                val features = listOf(
                    Triple(Icons.Default.Palette, stringResource(R.string.settings_feature_ui_title), stringResource(R.string.settings_feature_ui_desc)),
                    Triple(Icons.Default.LiveTv, stringResource(R.string.settings_feature_streaming_title), stringResource(R.string.settings_feature_streaming_desc)),
                    Triple(Icons.Default.Search, stringResource(R.string.settings_feature_search_title), stringResource(R.string.settings_feature_search_desc)),
                    Triple(Icons.Default.History, stringResource(R.string.settings_feature_history_title), stringResource(R.string.settings_feature_history_desc)),
                    Triple(Icons.Default.CloudDownload, stringResource(R.string.settings_feature_offline_title), stringResource(R.string.settings_feature_offline_desc)),
                    Triple(Icons.Default.Memory, stringResource(R.string.settings_feature_player_title), stringResource(R.string.settings_feature_player_desc)),
                    Triple(Icons.Default.Bolt, stringResource(R.string.settings_feature_performance_title), stringResource(R.string.settings_feature_performance_desc))
                )
                
                SettingsSection(
                    title = stringResource(R.string.settings_features_title),
                    icon = Icons.Default.AutoAwesome
                ) {
                    val carouselState = rememberCarouselState { features.count() }
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 280.dp,
                        itemSpacing = 16.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.height(140.dp)
                    ) { i ->
                        val (icon, title, desc) = features[i]
                        ExpressiveFeatureCard(
                            icon, 
                            title, 
                            desc,
                            modifier = Modifier.maskClip(ExpressiveShapes.Medium)
                        )
                    }
                }
            }

            // Developer Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_tmdb_integration),
                    icon = Icons.Default.Movie
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Key,
                        title = stringResource(R.string.settings_tmdb_api_key),
                        value = if (tmdbApiKey.isNullOrBlank()) stringResource(R.string.settings_tmdb_not_set) else "••••••••",
                        onClick = { showTmdbDialog = true }
                    )
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Subtitles,
                        title = stringResource(R.string.settings_opensubtitles_api_key),
                        value = if (openSubtitlesApiKey.isNullOrBlank()) stringResource(R.string.settings_tmdb_not_set) else "••••••••",
                        onClick = { showOpenSubtitlesDialog = true }
                    )
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Translate,
                        title = stringResource(R.string.settings_opensubtitles_languages_title),
                        value = formatSelectedLanguagesDisplay(openSubtitlesLanguages),
                        onClick = { showOpenSubtitlesLanguagesDialog = true }
                    )
                }
            }

            // Developer Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_developer_title),
                    icon = Icons.Default.Code
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.Public,
                        title = stringResource(R.string.settings_github_label),
                        value = stringResource(R.string.settings_github_url),
                        onClick = { uriHandler.openUri("https://github.com/hasan-ege") }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 800)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.settings_updates_title),
                    icon = Icons.Default.SystemUpdate
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.CloudDownload,
                        title = stringResource(R.string.settings_app_update_title),
                        value = when (updateState) {
                            is SettingsViewModel.UpdateState.Checking -> stringResource(R.string.settings_update_checking)
                            is SettingsViewModel.UpdateState.UpdateAvailable -> "${stringResource(R.string.settings_update_available)} ${(updateState as SettingsViewModel.UpdateState.UpdateAvailable).info.latestVersion}"
                            is SettingsViewModel.UpdateState.NoUpdate -> "${stringResource(R.string.settings_update_up_to_date)} (v${BuildConfig.VERSION_NAME})"
                            is SettingsViewModel.UpdateState.Error -> "${stringResource(R.string.settings_update_error)} ${(updateState as SettingsViewModel.UpdateState.Error).message}"
                            else -> stringResource(R.string.settings_update_check_hint)
                        },
                        onClick = {
                            if (updateState is SettingsViewModel.UpdateState.UpdateAvailable) {
                                viewModel.downloadUpdate(context, (updateState as SettingsViewModel.UpdateState.UpdateAvailable).info)
                            } else {
                                viewModel.checkForUpdates(context)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    selectedImageUriForCrop?.let { cropUri ->
        com.hasanege.materialtv.ui.components.ImageCropDialog(
            imageUri = cropUri,
            onDismiss = { selectedImageUriForCrop = null },
            onCropSuccess = { croppedBitmap ->
                scope.launch {
                    profilePreferences.setProfileImageFromBitmap(croppedBitmap)
                }
            }
        )
    }
}

// Expressive Feature Card for Carousel
@Composable
fun ExpressiveFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.width(280.dp).height(120.dp),
        shape = ExpressiveShapes.Medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Settings Section Card
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.Large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

// Expressive Switch Setting Item
@Composable
fun ExpressiveSettingSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                }
            )
            .padding(if (isNarrow) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isNarrow) 36.dp else 40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isNarrow) 18.dp else 20.dp)
                )
            }
            Text(
                text = title,
                style = if (isNarrow) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked, 
            onCheckedChange = null,
            modifier = if (isNarrow) Modifier.scale(0.85f) else Modifier
        )
    }
}

// Expressive Value Setting Item
@Composable
fun ExpressiveSettingValueItem(
    icon: ImageVector,
    title: String,
    value: String,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(if (isNarrow) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isNarrow) 36.dp else 40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isNarrow) 18.dp else 20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (isNarrow) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = if (isNarrow) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).size(if (isNarrow) 14.dp else 16.dp)
        )
    }
}

// Expressive Action Setting Item
@Composable
fun ExpressiveSettingActionItem(
    icon: ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Medium)
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
            .padding(if (isNarrow) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isNarrow) 36.dp else 40.dp)
                    .clip(ExpressiveShapes.Medium)
                    .background(
                        if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isNarrow) 18.dp else 20.dp)
                )
            }
            Text(
                text = title,
                style = if (isNarrow) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (isNarrow) 14.dp else 16.dp)
        )
    }
}


// Expressive Selection Dialog
@Composable
fun ExpressiveSelectionDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveShapes.Medium)
                            .clickable { onSelect(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = option == currentValue,
                            onClick = null
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
        shape = ExpressiveShapes.ExtraLarge
    )
}

// Profile Name Edit Dialog
@Composable
fun ProfileNameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveShapes.Large,
        title = {
            Text(
                text = stringResource(R.string.settings_profile_name_edit_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_profile_name_label)) },
                singleLine = true,
                shape = ExpressiveShapes.Small,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                shape = ExpressiveShapes.Small
            ) {
                Text(stringResource(R.string.settings_profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_profile_cancel))
            }
        }
    )
}
