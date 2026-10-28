package com.jsemalja.trackmecalories.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.StringResource

/**
 * A whole design language, not just a palette: neutrals, cards, nav bar, headers, corner
 * shapes, typefaces and progress marks change together. Accent and light/dark are picked
 * separately and work with every theme.
 * - [CLASSIC]: quiet filled cards, soft corners, a floating pill nav.
 * - [GLASS]: frosted translucent cards over glows of the accent, smooth corners.
 * - [PAPER]: editorial — serif headings, near-square outlined sheets on cream, ruled
 *   headers, thin ink progress lines and a docked nav bar with labels.
 * - [BOLD]: neo-brutalist — thick black borders, hard offset shadows, solid colour-block
 *   cards, chunky square progress, uppercase headings in Space Grotesk.
 * - [NEON]: true black with glowing accent edges, glowing rings and bars, big round corners.
 */
enum class AppTheme(val label: StringResource) {
    CLASSIC(Res.string.theme_classic),
    GLASS(Res.string.theme_glass),
    PAPER(Res.string.theme_paper),
    BOLD(Res.string.theme_bold),
    NEON(Res.string.theme_neon)
}

/** Light or dark: follow the phone, or pin one regardless of the system setting. */
enum class ThemeMode(val label: StringResource) {
    SYSTEM(Res.string.mode_system),
    LIGHT(Res.string.mode_light),
    DARK(Res.string.mode_dark)
}

/** The primary tones of an accent for dark and light mode (Material tones 80/20/30/90 and 40/100/90/10). */
data class AccentPalette(
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val containerDark: Color,
    val onContainerDark: Color,
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val containerLight: Color,
    val onContainerLight: Color
) {
    fun primary(dark: Boolean) = if (dark) primaryDark else primaryLight

    companion object {
        /** A palette generated from one colour; see [tone]. */
        fun fromSeed(seed: Int) = AccentPalette(
            Color(tone(seed, 80.0)), Color(tone(seed, 20.0)), Color(tone(seed, 30.0)), Color(tone(seed, 90.0)),
            Color(tone(seed, 40.0)), Color.White, Color(tone(seed, 90.0)), Color(tone(seed, 10.0))
        )

        fun fromSeed(seed: Long) = fromSeed(seed.toInt())
    }
}

private val BrandGreen = AccentPalette(
    Color(0xFF8ED6AE), Color(0xFF00391C), Color(0xFF00522F), Color(0xFFA6F2C5),
    Color(0xFF1B6E4F), Color(0xFFFFFFFF), Color(0xFFA6F2C5), Color(0xFF00210F)
)

/**
 * Accent colour choices, in hue order. Each has a palette of primary tones for dark and
 * light mode; the neutrals (backgrounds, cards, outlines) are tinted with the accent so the
 * whole UI shifts with it. The first six are hand-tuned, the later ones generated from a
 * seed the same way. [WALLPAPER] uses Android 12+'s wallpaper-based colours instead, and
 * [CUSTOM] a palette from [Appearance.customAccent] (see [Appearance.palette]).
 * Saved by name, so the order can change freely.
 */
enum class AccentColor(val label: StringResource, val palette: AccentPalette) {
    RED(Res.string.accent_red, AccentPalette.fromSeed(0xFFD32F2F)),
    ORANGE(
        Res.string.accent_orange,
        AccentPalette(
            Color(0xFFFFB68B), Color(0xFF522300), Color(0xFF743500), Color(0xFFFFDBC8),
            Color(0xFF964A12), Color(0xFFFFFFFF), Color(0xFFFFDBC8), Color(0xFF321300)
        )
    ),
    LIME(Res.string.accent_lime, AccentPalette.fromSeed(0xFF7CB342)),
    GREEN(Res.string.accent_green, BrandGreen),
    TEAL(
        Res.string.accent_teal,
        AccentPalette(
            Color(0xFF4FDAD1), Color(0xFF003734), Color(0xFF00504C), Color(0xFF71F7EE),
            Color(0xFF006A65), Color(0xFFFFFFFF), Color(0xFF71F7EE), Color(0xFF00201E)
        )
    ),
    CYAN(Res.string.accent_cyan, AccentPalette.fromSeed(0xFF00ACC1)),
    BLUE(
        Res.string.accent_blue,
        AccentPalette(
            Color(0xFFA8C8FF), Color(0xFF00315F), Color(0xFF194880), Color(0xFFD5E3FF),
            Color(0xFF345F99), Color(0xFFFFFFFF), Color(0xFFD5E3FF), Color(0xFF001C3B)
        )
    ),
    INDIGO(Res.string.accent_indigo, AccentPalette.fromSeed(0xFF3949AB)),
    VIOLET(
        Res.string.accent_violet,
        AccentPalette(
            Color(0xFFCFBDFF), Color(0xFF36196F), Color(0xFF4D3287), Color(0xFFE9DDFF),
            Color(0xFF6650A1), Color(0xFFFFFFFF), Color(0xFFE9DDFF), Color(0xFF21005D)
        )
    ),
    FUCHSIA(Res.string.accent_fuchsia, AccentPalette.fromSeed(0xFFB0279F)),
    ROSE(
        Res.string.accent_rose,
        AccentPalette(
            Color(0xFFFFB0CA), Color(0xFF5E1134), Color(0xFF7B294B), Color(0xFFFFD9E3),
            Color(0xFF984063), Color(0xFFFFFFFF), Color(0xFFFFD9E3), Color(0xFF3E001E)
        )
    ),
    BROWN(Res.string.accent_brown, AccentPalette.fromSeed(0xFF795548)),
    GRAPHITE(Res.string.accent_graphite, AccentPalette.fromSeed(0xFF5F6368)),
    // Fallback (pre-Android 12) is the brand green.
    WALLPAPER(Res.string.accent_wallpaper, BrandGreen),
    // The real palette comes from Appearance.customAccent.
    CUSTOM(Res.string.accent_custom, BrandGreen);

    companion object {
        /** Accents offered on this device (wallpaper colours need Android 12). */
        val available: List<AccentColor>
            get() = entries.filter { it != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    }
}

/** Starting point for the custom colour before one is picked: a periwinkle none of the presets cover. */
const val DEFAULT_CUSTOM_ACCENT = 0xFF5B6CD9.toInt()

/**
 * Typeface choices: the phone's own font, or one of the bundled Google Fonts (static
 * 400–700 weights in composeResources/font, so they work offline and without Play services).
 * Oxygen ships only regular and bold; Medium/SemiBold fall back to the nearest of those.
 */
enum class AppFont(val label: StringResource, private val faces: List<Pair<FontResource, FontWeight>>) {
    SYSTEM(Res.string.font_system, emptyList()),
    ROBOTO(Res.string.font_roboto, weights(Res.font.roboto_regular, Res.font.roboto_medium, Res.font.roboto_semibold, Res.font.roboto_bold)),
    PUBLIC_SANS(Res.string.font_public_sans, weights(Res.font.public_sans_regular, Res.font.public_sans_medium, Res.font.public_sans_semibold, Res.font.public_sans_bold)),
    SPACE_GROTESK(Res.string.font_space_grotesk, weights(Res.font.space_grotesk_regular, Res.font.space_grotesk_medium, Res.font.space_grotesk_semibold, Res.font.space_grotesk_bold)),
    MULISH(Res.string.font_mulish, weights(Res.font.mulish_regular, Res.font.mulish_medium, Res.font.mulish_semibold, Res.font.mulish_bold)),
    OXYGEN(Res.string.font_oxygen, listOf(Res.font.oxygen_regular to FontWeight.Normal, Res.font.oxygen_bold to FontWeight.Bold));

    /** The family to draw with; composable because the bundled files load through Compose resources. */
    val family: FontFamily
        @Composable get() = if (faces.isEmpty()) FontFamily.Default
        else FontFamily(faces.map { (file, weight) -> Font(file, weight) })
}

private fun weights(regular: FontResource, medium: FontResource, semiBold: FontResource, bold: FontResource) = listOf(
    regular to FontWeight.Normal,
    medium to FontWeight.Medium,
    semiBold to FontWeight.SemiBold,
    bold to FontWeight.Bold
)

/**
 * [font] and [corners] are overrides; null means the [theme]'s own. [customAccent] is kept
 * while another accent is picked, so going back to Custom finds it again.
 */
data class Appearance(
    val theme: AppTheme = AppTheme.CLASSIC,
    val accent: AccentColor = AccentColor.GREEN,
    val font: AppFont? = null,
    val corners: CornerStyle? = null,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val customAccent: Int = DEFAULT_CUSTOM_ACCENT
) {
    /** The accent's tones: the preset's own, or generated from [customAccent]. */
    val palette: AccentPalette by lazy {
        if (accent == AccentColor.CUSTOM) AccentPalette.fromSeed(customAccent) else accent.palette
    }
}

/**
 * Persists [Appearance] in SharedPreferences: it's a UI preference, so it stays out of the
 * Room database (no schema change, never touched by "Wipe All Data").
 */
class AppearancePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(read())
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    /** A new theme brings its own font and corners, so earlier overrides are dropped. */
    fun setTheme(theme: AppTheme) = update(_appearance.value.copy(theme = theme, font = null, corners = null))

    fun setAccent(accent: AccentColor) = update(_appearance.value.copy(accent = accent))

    /** Picks [AccentColor.CUSTOM] with the palette generated from [argb]. */
    fun setCustomAccent(argb: Int) = update(_appearance.value.copy(accent = AccentColor.CUSTOM, customAccent = argb))

    fun setFont(font: AppFont?) = update(_appearance.value.copy(font = font))

    fun setCorners(corners: CornerStyle?) = update(_appearance.value.copy(corners = corners))

    fun setMode(mode: ThemeMode) = update(_appearance.value.copy(mode = mode))

    private fun update(value: Appearance) {
        prefs.edit()
            .putString(KEY_THEME, value.theme.name)
            .putString(KEY_ACCENT, value.accent.name)
            .putString(KEY_FONT, value.font?.name ?: THEME_DEFAULT)
            .putString(KEY_CORNERS, value.corners?.name ?: THEME_DEFAULT)
            .putString(KEY_MODE, value.mode.name)
            .putInt(KEY_CUSTOM_ACCENT, value.customAccent)
            .remove(KEY_STYLE)
            .apply()
        _appearance.value = value
    }

    private fun read() = Appearance(
        // Before themes there was only Flat or Glass ("style").
        theme = prefs.getString(KEY_THEME, null)
            ?.let { name -> AppTheme.entries.firstOrNull { it.name == name } }
            ?: if (prefs.getString(KEY_STYLE, null) == "GLASS") AppTheme.GLASS else AppTheme.CLASSIC,
        accent = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> AccentColor.entries.firstOrNull { it.name == name } } ?: AccentColor.GREEN,
        // A font or corners saved before themes stays as an override, so nothing changes on update.
        font = prefs.getString(KEY_FONT, null)
            ?.let { name -> AppFont.entries.firstOrNull { it.name == name } },
        corners = prefs.getString(KEY_CORNERS, null)
            ?.let { name -> CornerStyle.entries.firstOrNull { it.name == name } },
        mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
        customAccent = prefs.getInt(KEY_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT)
    )

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_STYLE = "style"
        const val KEY_ACCENT = "accent"
        const val KEY_FONT = "font"
        const val KEY_CORNERS = "corners"
        const val KEY_MODE = "mode"
        const val KEY_CUSTOM_ACCENT = "custom_accent"
        const val THEME_DEFAULT = "THEME"
    }
}
