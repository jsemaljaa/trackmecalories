package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.MAX_ACCENT_CHROMA
import com.jsemalja.trackmecalories.ui.theme.MAX_CUSTOM_CHROMA
import com.jsemalja.trackmecalories.ui.theme.colorAt
import com.jsemalja.trackmecalories.ui.theme.hexOf
import com.jsemalja.trackmecalories.ui.theme.hueChroma
import com.jsemalja.trackmecalories.ui.theme.nearestPreset
import com.jsemalja.trackmecalories.ui.theme.parseHexColor
import com.jsemalja.trackmecalories.ui.theme.tone
import com.jsemalja.trackmecalories.ui.theme.toneOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TonesTest {
    private fun channels(argb: Int) = listOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)

    private fun assertClose(expected: Int, actual: Int, slack: Int) =
        channels(expected).zip(channels(actual)).forEach { (e, a) ->
            assertTrue("expected ${hexOf(expected)}, got ${hexOf(actual)}", abs(e - a) <= slack)
        }

    @Test
    fun `generated tones have the requested lightness`() {
        for (seed in listOf(0xFFD32F2F, 0xFF0000FF, 0xFFFFFF00, 0xFF00ACC1, 0xFF5F6368).map { it.toInt() }) {
            for (t in listOf(10.0, 20.0, 30.0, 40.0, 80.0, 90.0)) {
                assertEquals("${hexOf(seed)} at $t", t, toneOf(tone(seed, t)), 1.0)
            }
        }
    }

    @Test
    fun `a hand-tuned palette is reproduced from its own primary`() {
        // Blue's light primary regenerates Blue's other tones within a few levels.
        val seed = 0xFF345F99.toInt()
        assertClose(0xFF345F99.toInt(), tone(seed, 40.0), 2)
        assertClose(0xFFA8C8FF.toInt(), tone(seed, 80.0), 8)
        assertClose(0xFFD5E3FF.toInt(), tone(seed, 90.0), 6)
    }

    @Test
    fun `hue holds across tones`() {
        // In CIELAB a pale pure blue turns lavender; OKLCh keeps it blue.
        val (hue, _) = hueChroma(0xFF0000FF.toInt())
        for (t in listOf(30.0, 60.0, 90.0)) {
            val (h, _) = hueChroma(tone(0xFF0000FF.toInt(), t))
            assertEquals("tone $t", hue, h, 6.0)
        }
        val (r, g, b) = channels(tone(0xFF0000FF.toInt(), 80.0))
        assertTrue(b > g && g > r)
    }

    @Test
    fun `grey stays grey and chroma is capped`() {
        val (r, g, b) = channels(tone(0xFF808080.toInt(), 50.0))
        assertTrue(abs(r - g) <= 1 && abs(g - b) <= 1)
        assertEquals(MAX_ACCENT_CHROMA, hueChroma(0xFFFF00FF.toInt()).second, 1e-9)
    }

    @Test
    fun `colours out of gamut lose chroma, not lightness`() {
        // A dark, fully vivid yellow doesn't exist in sRGB: it becomes olive at the same tone.
        val olive = colorAt(110.0, MAX_ACCENT_CHROMA, 20.0)
        assertEquals(20.0, toneOf(olive), 1.0)
        assertTrue(hueChroma(olive).second < MAX_ACCENT_CHROMA)
    }

    @Test
    fun `hex parsing and formatting`() {
        assertEquals(0xFFA1B2C3.toInt(), parseHexColor("#a1b2c3"))
        assertEquals(0xFFA1B2C3.toInt(), parseHexColor(" A1B2C3 "))
        assertNull(parseHexColor("A1B2C"))
        assertNull(parseHexColor("G1B2C3"))
        assertEquals("0000FF", hexOf(0xFF0000FF.toInt()))
    }

    @Test
    fun `a custom colour may be more vivid than the presets`() {
        assertEquals(MAX_CUSTOM_CHROMA, hueChroma(0xFFFF00FF.toInt(), MAX_CUSTOM_CHROMA).second, 1e-9)
        assertTrue(hueChroma(tone(0xFFFF0000.toInt(), 40.0, MAX_CUSTOM_CHROMA), 1.0).second > MAX_ACCENT_CHROMA + 0.03)
    }

    @Test
    fun `custom colours are named after the nearest preset`() {
        assertEquals(AccentColor.BLUE, nearestPreset(0xFF345F99.toInt()))
        assertEquals(AccentColor.RED, nearestPreset(0xFFFF0000.toInt()))
        assertEquals(AccentColor.GRAPHITE, nearestPreset(0xFF808080.toInt()))
        assertEquals(AccentColor.TEAL, nearestPreset(0xFF006A65.toInt()))
        // Muted colours keep their hue's name; only a near-grey is Graphite.
        assertEquals(AccentColor.FUCHSIA, nearestPreset(0xFF6C5870.toInt()))
        assertEquals(AccentColor.BROWN, nearestPreset(0xFF795548.toInt()))
    }
}
