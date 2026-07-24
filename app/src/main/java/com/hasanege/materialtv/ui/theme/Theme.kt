package com.hasanege.materialtv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// Material 3 Expressive Light Color Scheme
private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

// Material 3 Expressive Dark Color Scheme
private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    surfaceDim = md_theme_dark_surfaceDim,
    surfaceBright = md_theme_dark_surfaceBright
)

private val AmoledDarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = androidx.compose.ui.graphics.Color.Black,
    onBackground = md_theme_dark_onBackground,
    surface = androidx.compose.ui.graphics.Color.Black,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF121212),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF000000)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialTVTheme(
  dynamicColor: Boolean = true,
  content: @Composable() () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val settingsRepository = androidx.compose.runtime.remember { com.hasanege.materialtv.data.SettingsRepository.getInstance(context) }
  val themeMode by settingsRepository.themeMode.collectAsState(initial = "system")
  val fontFamily by settingsRepository.fontFamily.collectAsState(initial = "default")
  val customAccentColor by settingsRepository.customAccentColor.collectAsState(initial = "#6750A4")
  val customBackgroundColor by settingsRepository.customBackgroundColor.collectAsState(initial = "#000000")
  val customTextColor by settingsRepository.customTextColor.collectAsState(initial = "#FFFFFF")

  val isSystemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode.lowercase()) {
      "light" -> false
      "dark", "amoled", "custom" -> true
      else -> isSystemDark
  }

  val colorScheme = when {
    themeMode.lowercase() == "custom" -> {
        val baseScheme = DarkColors
        val accentColorValue = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(customAccentColor)) } catch (e: Exception) { baseScheme.primary }
        val backgroundColorValue = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(customBackgroundColor)) } catch (e: Exception) { baseScheme.background }
        val textColorValue = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(customTextColor)) } catch (e: Exception) { baseScheme.onBackground }

        baseScheme.copy(
            primary = accentColorValue,
            primaryContainer = accentColorValue.copy(alpha = 0.3f),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            secondary = accentColorValue,
            secondaryContainer = accentColorValue.copy(alpha = 0.3f),
            tertiary = accentColorValue,
            background = backgroundColorValue,
            surface = backgroundColorValue,
            surfaceContainer = backgroundColorValue,
            surfaceContainerHigh = backgroundColorValue,
            surfaceContainerHighest = backgroundColorValue,
            surfaceContainerLow = backgroundColorValue,
            surfaceContainerLowest = backgroundColorValue,
            onBackground = textColorValue,
            onSurface = textColorValue,
            onSurfaceVariant = textColorValue
        )
    }
    themeMode.lowercase() == "amoled" -> {
      if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context).copy(
          background = androidx.compose.ui.graphics.Color.Black,
          surface = androidx.compose.ui.graphics.Color.Black,
          surfaceContainer = androidx.compose.ui.graphics.Color(0xFF000000),
          surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF121212),
          surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
          surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF000000),
          surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF000000)
        )
      } else {
        AmoledDarkColors
      }
    }
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColors
    else -> LightColors
  }

  val dynamicTypography = getTypographyForFontFamily(fontFamily)

  val view = androidx.compose.ui.platform.LocalView.current
  if (!view.isInEditMode) {
      androidx.compose.runtime.SideEffect {
          val window = (view.context as android.app.Activity).window
          window.statusBarColor = colorScheme.background.toArgb()
          androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
  }

  androidx.compose.material3.MaterialExpressiveTheme(
    colorScheme = colorScheme,
    typography = dynamicTypography,
    shapes = Shapes,
    motionScheme = androidx.compose.material3.MotionScheme.expressive(),
    content = content
  )
}
