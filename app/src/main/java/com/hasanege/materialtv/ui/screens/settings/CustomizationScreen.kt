package com.hasanege.materialtv.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import com.hasanege.materialtv.R
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(onBackClick: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    
    val themeMode by viewModel.themeMode.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val customBackgroundColor by viewModel.customBackgroundColor.collectAsState()
    val customTextColor by viewModel.customTextColor.collectAsState()
    val navBarStyle by viewModel.navBarStyle.collectAsState()
    val bottomNavOnlyIcons by viewModel.bottomNavOnlyIcons.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val fontLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.handleCustomFontSelection(context, it) }
    }
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showBackgroundColorDialog by remember { mutableStateOf(false) }
    var showTextColorDialog by remember { mutableStateOf(false) }
    var showNavBarStyleDialog by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.customization_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 0)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.customization_appearance),
                    icon = Icons.Default.Palette
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.BrightnessMedium,
                        title = stringResource(R.string.customization_theme),
                        value = when (themeMode) {
                            "light" -> stringResource(R.string.customization_theme_light)
                            "dark" -> stringResource(R.string.customization_theme_dark)
                            "amoled" -> stringResource(R.string.customization_theme_amoled)
                            "custom" -> stringResource(R.string.customization_theme_custom)
                            else -> stringResource(R.string.customization_theme_system)
                        },
                        onClick = { showThemeDialog = true }
                    )
                    
                    if (themeMode == "custom") {
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.ColorLens,
                            title = stringResource(R.string.customization_accent_color),
                            value = customAccentColor,
                            onClick = { showAccentColorDialog = true }
                        )
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.FormatColorFill,
                            title = stringResource(R.string.customization_background_color),
                            value = customBackgroundColor,
                            onClick = { showBackgroundColorDialog = true }
                        )
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.FormatColorText,
                            title = stringResource(R.string.customization_text_color),
                            value = customTextColor,
                            onClick = { showTextColorDialog = true }
                        )
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.FontDownload,
                        title = stringResource(R.string.customization_font_style),
                        value = when {
                            fontFamily == "serif" -> stringResource(R.string.customization_font_serif)
                            fontFamily == "sans-serif" -> stringResource(R.string.customization_font_sans_serif)
                            fontFamily == "monospace" -> stringResource(R.string.customization_font_monospace)
                            fontFamily == "cursive" -> stringResource(R.string.customization_font_cursive)
                            fontFamily == "default" -> stringResource(R.string.customization_font_default)
                            fontFamily.startsWith("/") -> stringResource(R.string.customization_font_custom)
                            else -> stringResource(R.string.customization_font_default)
                        },
                        onClick = { showFontDialog = true }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(delayMillis = 100)) +
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                SettingsSection(
                    title = stringResource(R.string.customization_navigation),
                    icon = Icons.Default.Menu
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.ViewAgenda,
                        title = stringResource(R.string.customization_navbar_style),
                        value = when (navBarStyle) {
                            "bottom" -> stringResource(R.string.customization_navbar_bottom)
                            "floating" -> stringResource(R.string.customization_navbar_floating)
                            "rail" -> stringResource(R.string.customization_navbar_rail)
                            else -> stringResource(R.string.customization_navbar_bottom)
                        },
                        onClick = { showNavBarStyleDialog = true }
                    )
                    
                    ExpressiveSettingSwitchItem(
                        icon = Icons.Default.FilterFrames,
                        title = stringResource(R.string.customization_bottom_nav_only_icons),
                        checked = bottomNavOnlyIcons,
                        onCheckedChange = { viewModel.setBottomNavOnlyIcons(it) }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = stringResource(R.string.customization_theme_mode),
            options = listOf("system", "light", "dark", "amoled", "custom"),
            currentValue = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { 
                viewModel.setThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    if (showFontDialog) {
        val loadCustomLabel = stringResource(R.string.customization_load_custom)
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = stringResource(R.string.customization_font_family),
            options = listOf("default", "serif", "monospace", "cursive", loadCustomLabel),
            currentValue = if (fontFamily.startsWith("custom_")) loadCustomLabel else fontFamily,
            onDismiss = { showFontDialog = false },
            onSelect = { 
                if (it == loadCustomLabel) {
                    fontLauncher.launch("*/*")
                } else {
                    viewModel.setFontFamily(it)
                }
                showFontDialog = false
            }
        )
    }

    if (showAccentColorDialog) {
        ColorInputDialog(
            title = stringResource(R.string.customization_select_accent_color),
            initialColor = customAccentColor,
            onDismiss = { showAccentColorDialog = false },
            onColorSelected = { 
                viewModel.setCustomAccentColor(it)
                showAccentColorDialog = false
            }
        )
    }

    if (showBackgroundColorDialog) {
        ColorInputDialog(
            title = stringResource(R.string.customization_select_background_color),
            initialColor = customBackgroundColor,
            onDismiss = { showBackgroundColorDialog = false },
            onColorSelected = { 
                viewModel.setCustomBackgroundColor(it)
                showBackgroundColorDialog = false
            }
        )
    }

    if (showTextColorDialog) {
        ColorInputDialog(
            title = stringResource(R.string.customization_select_text_color),
            initialColor = customTextColor,
            onDismiss = { showTextColorDialog = false },
            onColorSelected = { 
                viewModel.setCustomTextColor(it)
                showTextColorDialog = false
            }
        )
    }

    if (showNavBarStyleDialog) {
        val defaultBottomBarLabel = stringResource(R.string.customization_navbar_bottom)
        val floatingPillLabel = stringResource(R.string.customization_navbar_floating)
        val leftRailLabel = stringResource(R.string.customization_navbar_rail)
        
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = stringResource(R.string.customization_navbar_style),
            options = listOf(defaultBottomBarLabel, floatingPillLabel, leftRailLabel),
            currentValue = when (navBarStyle) {
                "bottom" -> defaultBottomBarLabel
                "floating" -> floatingPillLabel
                "rail" -> leftRailLabel
                else -> defaultBottomBarLabel
            },
            onDismiss = { showNavBarStyleDialog = false },
            onSelect = { selected ->
                val style = when (selected) {
                    defaultBottomBarLabel -> "bottom"
                    floatingPillLabel -> "floating"
                    leftRailLabel -> "rail"
                    else -> "bottom"
                }
                viewModel.setNavBarStyle(style)
                showNavBarStyleDialog = false
            }
        )
    }
}

@Composable
fun ColorInputDialog(
    title: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var colorHex by remember { mutableStateOf(initialColor) }
    val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
        "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
        "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
        "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#000000", "#FFFFFF"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    items(presetColors.size) { index ->
                        val hex = presetColors[index]
                        val color = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(color)
                                .clickable { colorHex = hex }
                                .border(
                                    width = if (colorHex.uppercase() == hex.uppercase()) 3.dp else 1.dp,
                                    color = if (colorHex.uppercase() == hex.uppercase()) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Gray,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
                
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text(stringResource(R.string.customization_color_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(colorHex) }) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
