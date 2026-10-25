package com.jsemalja.trackmecalories.ui.theme

import com.jsemalja.trackmecalories.R
import androidx.annotation.StringRes
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Visual style of surfaces: solid flat cards, or frosted glass over a colourful backdrop. */
enum class DesignStyle(@StringRes val label: Int) {
    FLAT(R.string.style_flat),
    GLASS(R.string.style_glass)
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

data class Appearance(
    val style: DesignStyle = DesignStyle.FLAT,
    val accent: AccentColor = AccentColor.GREEN
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

    private fun update(value: Appearance) {
        prefs.edit()
            .putString(KEY_STYLE, value.style.name)
            .putString(KEY_ACCENT, value.accent.name)
            .apply()
        _appearance.value = value
    }

    private fun read() = Appearance(
        style = prefs.getString(KEY_STYLE, null)
            ?.let { name -> DesignStyle.entries.firstOrNull { it.name == name } } ?: DesignStyle.FLAT,
        accent = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> AccentColor.entries.firstOrNull { it.name == name } } ?: AccentColor.GREEN
    )

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_ACCENT = "accent"
    }
}
