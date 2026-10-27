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
enum class AppTheme(@StringRes val label: Int) {
    CLASSIC(R.string.theme_classic),
    GLASS(R.string.theme_glass),
    PAPER(R.string.theme_paper),
    BOLD(R.string.theme_bold),
    NEON(R.string.theme_neon)
}

/** Light or dark: follow the phone, or pin one regardless of the system setting. */
enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.mode_system),
    LIGHT(R.string.mode_light),
    DARK(R.string.mode_dark)
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

/** [font] and [corners] are overrides; null means the [theme]'s own. */
data class Appearance(
    val theme: AppTheme = AppTheme.CLASSIC,
    val accent: AccentColor = AccentColor.GREEN,
    val font: AppFont? = null,
    val corners: CornerStyle? = null,
    val mode: ThemeMode = ThemeMode.SYSTEM
)

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
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM
    )

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_STYLE = "style"
        const val KEY_ACCENT = "accent"
        const val KEY_FONT = "font"
        const val KEY_CORNERS = "corners"
        const val KEY_MODE = "mode"
        const val THEME_DEFAULT = "THEME"
    }
}
