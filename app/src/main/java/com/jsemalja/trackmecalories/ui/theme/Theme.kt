package com.jsemalja.trackmecalories.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/** The active [DesignStyle], for components that draw differently in glass mode. */
val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.FLAT }

@Composable
fun TrackmecaloriesTheme(
    appearance: Appearance = Appearance(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val glass = appearance.style == DesignStyle.GLASS
    val colorScheme = remember(appearance, darkTheme) {
        val base = if (appearance.accent == AccentColor.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            accentScheme(appearance.accent, darkTheme)
        }
        if (glass) base.toGlass(darkTheme) else base
    }

    CompositionLocalProvider(LocalDesignStyle provides appearance.style) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
        ) {
            // Same tree in both styles (only the backdrop layer differs), so switching style
            // doesn't reset the UI state below — e.g. which tab is open.
            Box(Modifier.fillMaxSize()) {
                if (glass) GlassBackdrop(darkTheme)
                content()
            }
        }
    }
}

/**
 * A full scheme from an accent: its primary tones, the fixed amber/blue brand roles, and
 * neutrals (backgrounds, cards, outlines) faintly tinted with the accent so the whole UI
 * reads as one colour family instead of accent-on-grey.
 */
private fun accentScheme(accent: AccentColor, dark: Boolean): ColorScheme {
    val tint = accent.primaryLight
    // Grey level (0-255) nudged toward the accent.
    fun n(level: Int, amount: Float = 0.06f) = lerp(Color(level, level, level), tint, amount)

    return if (dark) {
        darkColorScheme(
            primary = accent.primaryDark,
            onPrimary = accent.onPrimaryDark,
            primaryContainer = accent.containerDark,
            onPrimaryContainer = accent.onContainerDark,
            inversePrimary = accent.primaryLight,
            secondary = AmberSecondaryDark,
            onSecondary = OnAmberSecondaryDark,
            secondaryContainer = AmberSecondaryContainerDark,
            onSecondaryContainer = OnAmberSecondaryContainerDark,
            tertiary = BlueTertiaryDark,
            onTertiary = OnBlueTertiaryDark,
            tertiaryContainer = BlueTertiaryContainerDark,
            onTertiaryContainer = OnBlueTertiaryContainerDark,
            background = n(0x10),
            onBackground = n(0xE0, 0.04f),
            surface = n(0x10),
            onSurface = n(0xE0, 0.04f),
            surfaceVariant = n(0x3D),
            onSurfaceVariant = n(0xC2, 0.05f),
            surfaceTint = accent.primaryDark,
            surfaceDim = n(0x10),
            surfaceBright = n(0x36),
            surfaceContainerLowest = n(0x0B),
            surfaceContainerLow = n(0x18),
            surfaceContainer = n(0x1C),
            surfaceContainerHigh = n(0x25),
            surfaceContainerHighest = n(0x30),
            outline = n(0x8B),
            outlineVariant = n(0x3D),
            inverseSurface = n(0xE0, 0.04f),
            inverseOnSurface = n(0x2D)
        )
    } else {
        lightColorScheme(
            primary = accent.primaryLight,
            onPrimary = accent.onPrimaryLight,
            primaryContainer = accent.containerLight,
            onPrimaryContainer = accent.onContainerLight,
            inversePrimary = accent.primaryDark,
            secondary = AmberSecondaryLight,
            onSecondary = OnAmberSecondaryLight,
            secondaryContainer = AmberSecondaryContainerLight,
            onSecondaryContainer = OnAmberSecondaryContainerLight,
            tertiary = BlueTertiaryLight,
            onTertiary = OnBlueTertiaryLight,
            tertiaryContainer = BlueTertiaryContainerLight,
            onTertiaryContainer = OnBlueTertiaryContainerLight,
            background = n(0xF7, 0.04f),
            onBackground = n(0x18),
            surface = n(0xF7, 0.04f),
            onSurface = n(0x18),
            surfaceVariant = n(0xDD, 0.05f),
            onSurfaceVariant = n(0x42),
            surfaceTint = accent.primaryLight,
            surfaceDim = n(0xD7, 0.04f),
            surfaceBright = n(0xF7, 0.04f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = n(0xF1, 0.04f),
            surfaceContainer = n(0xEB, 0.04f),
            surfaceContainerHigh = n(0xE5, 0.04f),
            surfaceContainerHighest = n(0xDF, 0.04f),
            outline = n(0x72),
            outlineVariant = n(0xC1, 0.04f),
            inverseSurface = n(0x2D),
            inverseOnSurface = n(0xEE, 0.04f)
        )
    }
}

/**
 * Glass variant: page backgrounds become transparent so the [GlassBackdrop] shows through,
 * and floating surfaces (dialogs, menus, date picker) become slightly translucent but stay
 * readable over whatever is behind them. Cards draw their own frosted fill (see AppCard).
 */
private fun ColorScheme.toGlass(dark: Boolean): ColorScheme {
    val popupAlpha = if (dark) 0.94f else 0.92f
    return copy(
        background = Color.Transparent,
        surface = Color.Transparent,
        surfaceContainerLow = surfaceContainerLow.copy(alpha = popupAlpha),
        surfaceContainer = surfaceContainer.copy(alpha = popupAlpha),
        surfaceContainerHigh = surfaceContainerHigh.copy(alpha = popupAlpha),
        surfaceContainerHighest = surfaceContainerHighest.copy(alpha = popupAlpha)
    )
}
