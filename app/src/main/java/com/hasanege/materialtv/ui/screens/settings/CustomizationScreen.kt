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
                title = { Text("Customization") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    title = "Appearance",
                    icon = Icons.Default.Palette
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.BrightnessMedium,
                        title = "Theme",
                        value = when (themeMode) {
                            "light" -> "Light"
                            "dark" -> "Dark"
                            "amoled" -> "Amoled Black"
                            "custom" -> "Custom"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    
                    if (themeMode == "custom") {
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.ColorLens,
                            title = "Accent Color",
                            value = customAccentColor,
                            onClick = { showAccentColorDialog = true }
                        )
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.FormatColorFill,
                            title = "Background Color",
                            value = customBackgroundColor,
                            onClick = { showBackgroundColorDialog = true }
                        )
                        ExpressiveSettingValueItem(
                            icon = Icons.Default.FormatColorText,
                            title = "Text Color",
                            value = customTextColor,
                            onClick = { showTextColorDialog = true }
                        )
                    }
                    
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.FontDownload,
                        title = "Font Style",
                        value = when {
                            fontFamily == "serif" -> "Serif"
                            fontFamily == "sans-serif" -> "Sans Serif"
                            fontFamily == "monospace" -> "Monospace"
                            fontFamily == "cursive" -> "Cursive"
                            fontFamily == "default" -> "Default"
                            fontFamily.startsWith("/") -> "Custom Font"
                            else -> "Default"
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
                    title = "Navigation",
                    icon = Icons.Default.Menu
                ) {
                    ExpressiveSettingValueItem(
                        icon = Icons.Default.ViewAgenda,
                        title = "Navigation Bar Style",
                        value = when (navBarStyle) {
                            "bottom" -> "Default Bottom Bar"
                            "floating" -> "Floating Pill"
                            "rail" -> "Left Rail"
                            else -> "Default Bottom Bar"
                        },
                        onClick = { showNavBarStyleDialog = true }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = "Theme Mode",
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
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = "Font Family",
            options = listOf("default", "serif", "monospace", "cursive", "Load Custom..."),
            currentValue = if (fontFamily.startsWith("custom_")) "Load Custom..." else fontFamily,
            onDismiss = { showFontDialog = false },
            onSelect = { 
                if (it == "Load Custom...") {
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
            title = "Select Accent Color",
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
            title = "Select Background Color",
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
            title = "Select Text Color",
            initialColor = customTextColor,
            onDismiss = { showTextColorDialog = false },
            onColorSelected = { 
                viewModel.setCustomTextColor(it)
                showTextColorDialog = false
            }
        )
    }

    if (showNavBarStyleDialog) {
        com.hasanege.materialtv.ui.screens.settings.ExpressiveSelectionDialog(
            title = "Navigation Bar Style",
            options = listOf("Default Bottom Bar", "Floating Pill", "Left Rail"),
            currentValue = when (navBarStyle) {
                "bottom" -> "Default Bottom Bar"
                "floating" -> "Floating Pill"
                "rail" -> "Left Rail"
                else -> "Default Bottom Bar"
            },
            onDismiss = { showNavBarStyleDialog = false },
            onSelect = { selected ->
                val style = when (selected) {
                    "Default Bottom Bar" -> "bottom"
                    "Floating Pill" -> "floating"
                    "Left Rail" -> "rail"
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
                    label = { Text("Color HEX (e.g. #FF0000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(colorHex) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
