package com.jsemalja.trackmecalories.ui.theme

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// Material-style tonal palettes from one seed colour, for accents without hand-tuned tones
// (the newer presets and the custom colour). A "tone" is CIE L* (0 black, 100 white), the
// scale Material's palettes use, so tone 40 is the light-mode primary and 80 the dark one.
// Hue and chroma come from OKLCh rather than CIELAB: in Lab a light blue drifts to lavender.
// All colours are opaque ARGB ints so this stays plain JVM code.

/** OKLCh chroma cap, about Material's "tonal spot" primary: even a vivid seed sits calmly in the UI. */
const val MAX_ACCENT_CHROMA = 0.13

/** The colour at L* [tone] with [seed]'s hue and (capped) chroma. */
fun tone(seed: Int, tone: Double): Int {
    val (hue, chroma) = hueChroma(seed)
    return colorAt(hue, chroma, tone)
}

/** OKLCh hue (degrees, 0-360) and chroma of [argb], the chroma capped at [MAX_ACCENT_CHROMA]. */
fun hueChroma(argb: Int): Pair<Double, Double> {
    val r = linear(argb shr 16 and 0xFF)
    val g = linear(argb shr 8 and 0xFF)
    val b = linear(argb and 0xFF)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val labA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val labB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val hue = (Math.toDegrees(atan2(labB, labA)) + 360) % 360
    return hue to min(hypot(labA, labB), MAX_ACCENT_CHROMA)
}

/**
 * The colour with OKLCh [hue] and [chroma] at L* [tone]. Where sRGB can't hold that much
 * chroma at that lightness (a dark yellow, a pale blue), the chroma is lowered until it fits;
 * the hue and lightness are kept.
 */
fun colorAt(hue: Double, chroma: Double, tone: Double): Int {
    val h = Math.toRadians(hue)
    fun inGamut(c: Double): DoubleArray? {
        val a = c * cos(h)
        val b = c * sin(h)
        return oklabToLinear(lightnessFor(tone, a, b), a, b).takeIf { rgb -> rgb.all { it in -1e-5..1 + 1e-5 } }
    }
    var fit = inGamut(chroma)
    if (fit == null) {
        // Grey always fits; binary search for the most chroma that still does.
        var lo = 0.0
        var hi = chroma
        fit = inGamut(0.0)!!
        repeat(24) {
            val mid = (lo + hi) / 2
            val rgb = inGamut(mid)
            if (rgb != null) { lo = mid; fit = rgb } else hi = mid
        }
    }
    val (r, g, b) = fit!!
    return (0xFF shl 24) or (encode(r) shl 16) or (encode(g) shl 8) or encode(b)
}

/** CIE L* of [argb]: its tone. */
fun toneOf(argb: Int): Double =
    lstar(0.2126 * linear(argb shr 16 and 0xFF) + 0.7152 * linear(argb shr 8 and 0xFF) + 0.0722 * linear(argb and 0xFF))

/** "#a1b2c3" or "A1B2C3" to an opaque colour; null unless it's exactly six hex digits. */
fun parseHexColor(text: String): Int? {
    val hex = text.trim().removePrefix("#")
    if (hex.length != 6 || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return hex.toInt(16) or (0xFF shl 24)
}

/** Six uppercase hex digits, no "#". */
fun hexOf(argb: Int): String = String.format(Locale.ROOT, "%06X", argb and 0xFFFFFF)

/** The OKLab lightness that gives L* [tone] at this a/b (luminance rises with it, so bisect). */
private fun lightnessFor(tone: Double, a: Double, b: Double): Double {
    var lo = 0.0
    var hi = 1.0
    repeat(30) {
        val mid = (lo + hi) / 2
        val (r, g, bl) = oklabToLinear(mid, a, b)
        if (lstar(0.2126 * r + 0.7152 * g + 0.0722 * bl) < tone) lo = mid else hi = mid
    }
    return (lo + hi) / 2
}

private fun oklabToLinear(lightness: Double, a: Double, b: Double): DoubleArray {
    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return doubleArrayOf(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    )
}

private fun lstar(y: Double): Double {
    val v = y.coerceAtLeast(0.0)
    return if (v > 216.0 / 24389) 116 * cbrt(v) - 16 else v * 24389 / 27
}

private fun linear(channel: Int): Double {
    val c = channel / 255.0
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

private fun encode(linear: Double): Int {
    val c = linear.coerceIn(0.0, 1.0)
    return ((if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055) * 255).roundToInt()
}
