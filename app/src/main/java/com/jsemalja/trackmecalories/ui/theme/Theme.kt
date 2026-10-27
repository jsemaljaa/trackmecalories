package com.jsemalja.trackmecalories.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

/** The active [DesignStyle], for components that draw differently in glass mode. */
val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.FLAT }

/** The active [ThemePalette], for components that adapt to it (e.g. outlined cards). */
val LocalThemePalette = staticCompositionLocalOf { ThemePalette.CLASSIC }

/** Whether the app draws dark; differs from the system when [ThemeMode] pins light or dark. */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun TrackmecaloriesTheme(
    appearance: Appearance = Appearance(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val glass = appearance.style == DesignStyle.GLASS
    val solidScheme = remember(appearance, darkTheme) {
        if (appearance.accent == AccentColor.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Classic keeps the wallpaper's own neutrals; the other themes bring theirs.
            if (appearance.palette == ThemePalette.CLASSIC) scheme
            else scheme.withNeutrals(neutrals(appearance.palette, scheme.primary, darkTheme))
        } else {
            accentScheme(appearance.accent, darkTheme)
                .withNeutrals(neutrals(appearance.palette, appearance.accent.primaryLight, darkTheme))
        }
    }
    val colorScheme = if (glass) solidScheme.toGlass(darkTheme) else solidScheme
    val backdrop = when (appearance.palette) {
        ThemePalette.CLASSIC ->
            if (darkTheme) lerp(Color(0xFF07090A), solidScheme.primary, 0.06f)
            else lerp(Color(0xFFF2F5F3), solidScheme.primary, 0.05f)
        else -> solidScheme.background
    }
    val typography = remember(appearance.font) { appTypography(appearance.font.family) }
    val shapes = remember(appearance.corners) { appShapes(appearance.corners) }

    SystemBarsFollow(darkTheme)

    CompositionLocalProvider(
        LocalDesignStyle provides appearance.style,
        LocalThemePalette provides appearance.palette,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        ) {
            // Same tree in both styles (only the backdrop layer differs), so switching style
            // doesn't reset the UI state below — e.g. which tab is open.
            Box(Modifier.fillMaxSize()) {
                if (glass) GlassBackdrop(darkTheme, backdrop)
                content()
            }
        }
    }
}

/**
 * Status and navigation bar icons follow the app's light/dark, not the system's: with the
 * mode pinned to dark on a light phone, the default would draw dark icons on a dark page.
 */
@Composable
private fun SystemBarsFollow(darkTheme: Boolean) {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { darkTheme },
            // Same scrims as the androidx defaults (only shown with 3-button navigation).
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme }
        )
        onDispose { }
    }
}

private val LightScrim = android.graphics.Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * A full scheme from an accent: its primary tones and the fixed amber/blue brand roles.
 * The neutrals are filled in by [withNeutrals] from the chosen [ThemePalette].
 */
private fun accentScheme(accent: AccentColor, dark: Boolean): ColorScheme = if (dark) {
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
        surfaceTint = accent.primaryDark
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
        surfaceTint = accent.primaryLight
    )
}

/** Backgrounds, cards, outlines and text greys: everything in a scheme that isn't a colour role. */
internal class Neutrals(
    val background: Color,
    val onBackground: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val containerLowest: Color,
    val containerLow: Color,
    val container: Color,
    val containerHigh: Color,
    val containerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color
)

private fun ColorScheme.withNeutrals(n: Neutrals) = copy(
    background = n.background,
    onBackground = n.onBackground,
    surface = n.background,
    onSurface = n.onBackground,
    surfaceVariant = n.surfaceVariant,
    onSurfaceVariant = n.onSurfaceVariant,
    surfaceDim = n.surfaceDim,
    surfaceBright = n.surfaceBright,
    surfaceContainerLowest = n.containerLowest,
    surfaceContainerLow = n.containerLow,
    surfaceContainer = n.container,
    surfaceContainerHigh = n.containerHigh,
    surfaceContainerHighest = n.containerHighest,
    outline = n.outline,
    outlineVariant = n.outlineVariant,
    inverseSurface = n.inverseSurface,
    inverseOnSurface = n.inverseOnSurface
)

/**
 * The neutrals of [palette], faintly tinted with the accent ([tint]) so the whole UI reads
 * as one colour family instead of accent-on-grey. Classic is grey nudged toward the accent;
 * the other palettes start from their own hue and take only a trace of it.
 */
internal fun neutrals(palette: ThemePalette, tint: Color, dark: Boolean): Neutrals {
    // Classic: grey level (0-255) nudged toward the accent.
    fun n(level: Int, amount: Float = 0.06f) = lerp(Color(level, level, level), tint, amount)
    // The rest: a fixed colour with a trace of the accent.
    fun h(argb: Long) = lerp(Color(argb), tint, 0.03f)

    return when (palette) {
        ThemePalette.CLASSIC -> if (dark) Neutrals(
            background = n(0x10), onBackground = n(0xE0, 0.04f),
            surfaceVariant = n(0x3D), onSurfaceVariant = n(0xC2, 0.05f),
            surfaceDim = n(0x10), surfaceBright = n(0x36),
            containerLowest = n(0x0B), containerLow = n(0x18), container = n(0x1C),
            containerHigh = n(0x25), containerHighest = n(0x30),
            outline = n(0x8B), outlineVariant = n(0x3D),
            inverseSurface = n(0xE0, 0.04f), inverseOnSurface = n(0x2D)
        ) else Neutrals(
            background = n(0xF7, 0.04f), onBackground = n(0x18),
            surfaceVariant = n(0xDD, 0.05f), onSurfaceVariant = n(0x42),
            surfaceDim = n(0xD7, 0.04f), surfaceBright = n(0xF7, 0.04f),
            containerLowest = Color.White, containerLow = n(0xF1, 0.04f), container = n(0xEB, 0.04f),
            containerHigh = n(0xE5, 0.04f), containerHighest = n(0xDF, 0.04f),
            outline = n(0x72), outlineVariant = n(0xC1, 0.04f),
            inverseSurface = n(0x2D), inverseOnSurface = n(0xEE, 0.04f)
        )
        // True black page (OLED pixels off) with cards a step above it and a hairline edge;
        // in light mode, pure white with crisp greys.
        ThemePalette.BLACK -> if (dark) Neutrals(
            background = Color.Black, onBackground = h(0xFFE8E8E8),
            surfaceVariant = h(0xFF303030), onSurfaceVariant = h(0xFFBDBDBD),
            surfaceDim = Color.Black, surfaceBright = h(0xFF2A2A2A),
            containerLowest = Color.Black, containerLow = h(0xFF0F0F0F), container = h(0xFF151515),
            containerHigh = h(0xFF1C1C1C), containerHighest = h(0xFF262626),
            outline = h(0xFF7E7E7E), outlineVariant = h(0xFF2C2C2C),
            inverseSurface = h(0xFFE8E8E8), inverseOnSurface = h(0xFF222222)
        ) else Neutrals(
            background = Color.White, onBackground = h(0xFF0B0B0B),
            surfaceVariant = h(0xFFE6E6E6), onSurfaceVariant = h(0xFF3C3C3C),
            surfaceDim = h(0xFFDCDCDC), surfaceBright = Color.White,
            containerLowest = Color.White, containerLow = h(0xFFF6F6F6), container = h(0xFFF0F0F0),
            containerHigh = h(0xFFEAEAEA), containerHighest = h(0xFFE3E3E3),
            outline = h(0xFF6E6E6E), outlineVariant = h(0xFFD6D6D6),
            inverseSurface = h(0xFF1B1B1B), inverseOnSurface = h(0xFFF3F3F3)
        )
        // Warm cream and sepia, softer contrast; in light mode cards are lighter sheets on
        // the page rather than darker wells.
        ThemePalette.PAPER -> if (dark) Neutrals(
            background = h(0xFF1B1814), onBackground = h(0xFFECE4D7),
            surfaceVariant = h(0xFF403A31), onSurfaceVariant = h(0xFFCDC1AF),
            surfaceDim = h(0xFF1B1814), surfaceBright = h(0xFF3C362E),
            containerLowest = h(0xFF14120F), containerLow = h(0xFF23201B), container = h(0xFF28241F),
            containerHigh = h(0xFF312C26), containerHighest = h(0xFF3B352E),
            outline = h(0xFF978B7A), outlineVariant = h(0xFF3A342C),
            inverseSurface = h(0xFFECE4D7), inverseOnSurface = h(0xFF302B25)
        ) else Neutrals(
            background = h(0xFFF4EEE3), onBackground = h(0xFF2B251D),
            surfaceVariant = h(0xFFE7DDCC), onSurfaceVariant = h(0xFF5B5145),
            surfaceDim = h(0xFFDDD3C2), surfaceBright = h(0xFFF8F3EA),
            containerLowest = h(0xFFFFFCF7), containerLow = h(0xFFFBF7F0), container = h(0xFFEFE8DB),
            containerHigh = h(0xFFE9E1D3), containerHighest = h(0xFFE2D9CA),
            outline = h(0xFF8B7F6F), outlineVariant = h(0xFFDDD2C0),
            inverseSurface = h(0xFF36302A), inverseOnSurface = h(0xFFF6F0E6)
        )
        // Cool navy and blue-grey.
        ThemePalette.SLATE -> if (dark) Neutrals(
            background = h(0xFF0E131B), onBackground = h(0xFFE0E6EF),
            surfaceVariant = h(0xFF364152), onSurfaceVariant = h(0xFFBAC4D3),
            surfaceDim = h(0xFF0E131B), surfaceBright = h(0xFF323C4B),
            containerLowest = h(0xFF090D13), containerLow = h(0xFF151B25), container = h(0xFF19202B),
            containerHigh = h(0xFF212937), containerHighest = h(0xFF2B3443),
            outline = h(0xFF8591A3), outlineVariant = h(0xFF333D4D),
            inverseSurface = h(0xFFE0E6EF), inverseOnSurface = h(0xFF283040)
        ) else Neutrals(
            background = h(0xFFF0F3F8), onBackground = h(0xFF141A24),
            surfaceVariant = h(0xFFDAE1EB), onSurfaceVariant = h(0xFF424B59),
            surfaceDim = h(0xFFD3D9E3), surfaceBright = h(0xFFF5F7FB),
            containerLowest = Color.White, containerLow = h(0xFFE8EDF4), container = h(0xFFE2E8F0),
            containerHigh = h(0xFFDCE2EB), containerHighest = h(0xFFD5DCE6),
            outline = h(0xFF6F7A8A), outlineVariant = h(0xFFC3CAD6),
            inverseSurface = h(0xFF283040), inverseOnSurface = h(0xFFECF0F6)
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
