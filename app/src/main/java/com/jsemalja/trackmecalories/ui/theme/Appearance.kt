package com.jsemalja.trackmecalories.ui.theme

import com.jsemalja.trackmecalories.R
import androidx.annotation.StringRes
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Visual style of surfaces: solid flat cards, or frosted glass over a colourful backdrop. */
enum class DesignStyle(@StringRes val label: Int) {
    FLAT(R.string.style_flat),
    GLASS(R.string.style_glass)
}

/** Light or dark: follow the phone, or pin one regardless of the system setting. */
enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.mode_system),
    LIGHT(R.string.mode_light),
    DARK(R.string.mode_dark)
}

/**
 * The neutral family behind the accent: backgrounds, cards, outlines and text greys.
 * [CLASSIC] is grey faintly tinted with the accent; the others have a character of their
 * own — [BLACK] true black for OLED screens (pure white in light mode), [PAPER] warm
 * cream and sepia, [SLATE] cool navy and blue-grey. [outlinedCards] gives flat cards a
 * hairline edge, for themes where the card and the page are close in tone.
 */
enum class ThemePalette(@StringRes val label: Int, val outlinedCards: Boolean) {
    CLASSIC(R.string.palette_classic, outlinedCards = false),
    BLACK(R.string.palette_black, outlinedCards = true),
    PAPER(R.string.palette_paper, outlinedCards = true),
    SLATE(R.string.palette_slate, outlinedCards = false)
}

/**
 * Accent colour choices. Each defines the primary tones for dark and light mode; the
 * neutrals (backgrounds, cards, outlines) are tinted with the accent so the whole UI
 * shifts with it. [WALLPAPER] uses Android 12+'s wallpaper-based colours instead.
 */
enum class AccentColor(
    @StringRes val label: Int,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val containerDark: Color,
    val onContainerDark: Color,
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val containerLight: Color,
    val onContainerLight: Color
) {
    GREEN(
        R.string.accent_green,
        Color(0xFF8ED6AE), Color(0xFF00391C), Color(0xFF00522F), Color(0xFFA6F2C5),
        Color(0xFF1B6E4F), Color(0xFFFFFFFF), Color(0xFFA6F2C5), Color(0xFF00210F)
    ),
    TEAL(
        R.string.accent_teal,
        Color(0xFF4FDAD1), Color(0xFF003734), Color(0xFF00504C), Color(0xFF71F7EE),
        Color(0xFF006A65), Color(0xFFFFFFFF), Color(0xFF71F7EE), Color(0xFF00201E)
    ),
    BLUE(
        R.string.accent_blue,
        Color(0xFFA8C8FF), Color(0xFF00315F), Color(0xFF194880), Color(0xFFD5E3FF),
        Color(0xFF345F99), Color(0xFFFFFFFF), Color(0xFFD5E3FF), Color(0xFF001C3B)
    ),
    VIOLET(
        R.string.accent_violet,
        Color(0xFFCFBDFF), Color(0xFF36196F), Color(0xFF4D3287), Color(0xFFE9DDFF),
        Color(0xFF6650A1), Color(0xFFFFFFFF), Color(0xFFE9DDFF), Color(0xFF21005D)
    ),
    ROSE(
        R.string.accent_rose,
        Color(0xFFFFB0CA), Color(0xFF5E1134), Color(0xFF7B294B), Color(0xFFFFD9E3),
        Color(0xFF984063), Color(0xFFFFFFFF), Color(0xFFFFD9E3), Color(0xFF3E001E)
    ),
    ORANGE(
        R.string.accent_orange,
        Color(0xFFFFB68B), Color(0xFF522300), Color(0xFF743500), Color(0xFFFFDBC8),
        Color(0xFF964A12), Color(0xFFFFFFFF), Color(0xFFFFDBC8), Color(0xFF321300)
    ),
    WALLPAPER(
        R.string.accent_wallpaper,
        // Fallback (pre-Android 12) is the brand green.
        Color(0xFF8ED6AE), Color(0xFF00391C), Color(0xFF00522F), Color(0xFFA6F2C5),
        Color(0xFF1B6E4F), Color(0xFFFFFFFF), Color(0xFFA6F2C5), Color(0xFF00210F)
    );

    companion object {
        /** Accents offered on this device (wallpaper colours need Android 12). */
        val available: List<AccentColor>
            get() = entries.filter { it != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    }
}

/**
 * Typeface choices: the phone's own font, or one of the bundled Google Fonts (static
 * 400–700 weights in res/font, so they work offline and without Play services).
 * Oxygen ships only regular and bold; Medium/SemiBold fall back to the nearest of those.
 */
enum class AppFont(@StringRes val label: Int, val family: FontFamily) {
    SYSTEM(R.string.font_system, FontFamily.Default),
    ROBOTO(R.string.font_roboto, weights(R.font.roboto_regular, R.font.roboto_medium, R.font.roboto_semibold, R.font.roboto_bold)),
    PUBLIC_SANS(R.string.font_public_sans, weights(R.font.public_sans_regular, R.font.public_sans_medium, R.font.public_sans_semibold, R.font.public_sans_bold)),
    SPACE_GROTESK(R.string.font_space_grotesk, weights(R.font.space_grotesk_regular, R.font.space_grotesk_medium, R.font.space_grotesk_semibold, R.font.space_grotesk_bold)),
    MULISH(R.string.font_mulish, weights(R.font.mulish_regular, R.font.mulish_medium, R.font.mulish_semibold, R.font.mulish_bold)),
    OXYGEN(R.string.font_oxygen, FontFamily(Font(R.font.oxygen_regular, FontWeight.Normal), Font(R.font.oxygen_bold, FontWeight.Bold)))
}

private fun weights(regular: Int, medium: Int, semiBold: Int, bold: Int) = FontFamily(
    Font(regular, FontWeight.Normal),
    Font(medium, FontWeight.Medium),
    Font(semiBold, FontWeight.SemiBold),
    Font(bold, FontWeight.Bold)
)

data class Appearance(
    val style: DesignStyle = DesignStyle.FLAT,
    val accent: AccentColor = AccentColor.GREEN,
    val font: AppFont = AppFont.SYSTEM,
    val corners: CornerStyle = CornerStyle.ROUNDED,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.CLASSIC
)

/**
 * Persists [Appearance] in SharedPreferences: it's a UI preference, so it stays out of the
 * Room database (no schema change, never touched by "Wipe All Data").
 */
class AppearancePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(read())
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    fun setStyle(style: DesignStyle) = update(_appearance.value.copy(style = style))

    fun setAccent(accent: AccentColor) = update(_appearance.value.copy(accent = accent))

    fun setFont(font: AppFont) = update(_appearance.value.copy(font = font))

    fun setCorners(corners: CornerStyle) = update(_appearance.value.copy(corners = corners))

    fun setMode(mode: ThemeMode) = update(_appearance.value.copy(mode = mode))

    fun setPalette(palette: ThemePalette) = update(_appearance.value.copy(palette = palette))

    private fun update(value: Appearance) {
        prefs.edit()
            .putString(KEY_STYLE, value.style.name)
            .putString(KEY_ACCENT, value.accent.name)
            .putString(KEY_FONT, value.font.name)
            .putString(KEY_CORNERS, value.corners.name)
            .putString(KEY_MODE, value.mode.name)
            .putString(KEY_PALETTE, value.palette.name)
            .apply()
        _appearance.value = value
    }

    private fun read() = Appearance(
        style = prefs.getString(KEY_STYLE, null)
            ?.let { name -> DesignStyle.entries.firstOrNull { it.name == name } } ?: DesignStyle.FLAT,
        accent = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> AccentColor.entries.firstOrNull { it.name == name } } ?: AccentColor.GREEN,
        font = prefs.getString(KEY_FONT, null)
            ?.let { name -> AppFont.entries.firstOrNull { it.name == name } } ?: AppFont.SYSTEM,
        corners = prefs.getString(KEY_CORNERS, null)
            ?.let { name -> CornerStyle.entries.firstOrNull { it.name == name } } ?: CornerStyle.ROUNDED,
        mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
        palette = prefs.getString(KEY_PALETTE, null)
            ?.let { name -> ThemePalette.entries.firstOrNull { it.name == name } } ?: ThemePalette.CLASSIC
    )

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_ACCENT = "accent"
        const val KEY_FONT = "font"
        const val KEY_CORNERS = "corners"
        const val KEY_MODE = "mode"
        const val KEY_PALETTE = "palette"
    }
}
