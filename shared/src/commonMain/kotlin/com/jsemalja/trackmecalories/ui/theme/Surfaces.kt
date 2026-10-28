package com.jsemalja.trackmecalories.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.blur

/** True when the Glass theme is active. */
val isGlass: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalAppTheme.current == AppTheme.GLASS

/**
 * True when a card's tint becomes its fill (Glass frosts it, Bold blocks it in solid), so
 * content on it should use the matching on-container colour rather than plain onSurface.
 */
val tintedCards: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalAppTheme.current.let { it == AppTheme.GLASS || it == AppTheme.BOLD }

/**
 * The colourful backdrop glass surfaces sit on: the theme's dark (or light) [base] with
 * large, soft glows of the accent, amber and blue. The glows are radial gradients, i.e.
 * already "blurred", so translucent cards on top read as frosted without a live blur.
 */
@Composable
fun GlassBackdrop(dark: Boolean, base: Color) {
    val colors = MaterialTheme.colorScheme
    val glow = if (dark) 0.55f else 0.45f

    Box(Modifier.fillMaxSize().background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun blob(color: Color, center: Offset, radius: Float, alpha: Float) = drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            blob(colors.primary, Offset(w * 0.05f, h * 0.12f), w * 0.95f, glow)
            blob(colors.secondary, Offset(w * 1.0f, h * 0.45f), w * 0.75f, glow * 0.55f)
            blob(colors.tertiary, Offset(w * 0.15f, h * 0.88f), w * 0.85f, glow * 0.6f)
            blob(colors.primary, Offset(w * 0.9f, h * 1.02f), w * 0.6f, glow * 0.5f)
        }
    }
}

/**
 * Segmented buttons in the accent (the default uses the amber secondary colour). Bold
 * fills the selected segment solid, like its nav bar.
 */
@Composable
fun appSegmentedColors() = if (LocalAppTheme.current == AppTheme.BOLD) {
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.outline,
        inactiveBorderColor = MaterialTheme.colorScheme.outline,
        inactiveContainerColor = Color.Transparent
    )
} else {
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        activeContentColor = MaterialTheme.colorScheme.primary,
        activeBorderColor = MaterialTheme.colorScheme.outline,
        inactiveContainerColor = Color.Transparent
    )
}

/** Frosted fill for a glass surface, optionally tinted (e.g. the Home calorie card). */
@Composable
@ReadOnlyComposable
fun glassFill(tint: Color? = null, strong: Boolean = false): Color {
    val dark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    return when {
        tint != null -> tint.copy(alpha = if (dark) 0.42f else 0.55f)
        dark -> Color.White.copy(alpha = if (strong) 0.11f else 0.07f)
        else -> Color.White.copy(alpha = if (strong) 0.62f else 0.5f)
    }
}

/** The light-catching edge of glass: brighter at the top-left, fading toward the bottom. */
@Composable
@ReadOnlyComposable
fun glassBorder(): BorderStroke {
    val dark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val top = if (dark) 0.28f else 0.95f
    val bottom = if (dark) 0.05f else 0.35f
    return BorderStroke(
        1.dp,
        Brush.linearGradient(listOf(Color.White.copy(alpha = top), Color.White.copy(alpha = bottom)))
    )
}

/** Bold's hard shadow: the [shape] repeated solid, [offset] down and right, no blur. */
fun Modifier.hardShadow(shape: Shape, color: Color, offset: Dp = 4.dp): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val o = offset.toPx()
    translate(o, o) { drawOutline(outline, color) }
}

/**
 * Neon's glow: the [shape] blurred by [radius] behind the element, so a light bleeds out
 * around its edge. Blur masks need hardware support (Android 9+); older phones draw no glow.
 */
fun Modifier.glow(shape: Shape, color: Color, radius: Dp = 18.dp): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val paint = Paint().apply {
        this.color = color
        blur(radius.toPx())
    }
    drawIntoCanvas { it.drawOutline(outline, paint) }
}

/** Bold's shadow colour: ink in light mode, the accent in dark (a black shadow wouldn't show). */
val boldShadowColor: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

/** Neon's glow colour, softer in light mode where it would otherwise smudge. */
val neonGlowColor: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = if (LocalDarkTheme.current) 0.85f else 0.45f)

/**
 * How a floating element (nav bar, the add button) sits on the page in the current theme:
 * Bold gives it a thick ink border and a hard shadow, Neon an accent edge and a glow.
 * Returns the extra modifier and the border to draw.
 */
@Composable
fun raisedEdge(shape: Shape): Pair<Modifier, BorderStroke?> = when (LocalAppTheme.current) {
    AppTheme.BOLD -> Modifier.hardShadow(shape, boldShadowColor) to BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    AppTheme.NEON -> Modifier.glow(shape, neonGlowColor) to BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    else -> Modifier to null
}

/**
 * The app's one card, drawn in the current [AppTheme]:
 * - Classic: a quiet neutral container with no shadow, border or colour block — the
 *   accent is left to the content (rings, bars, buttons).
 * - Glass: a translucent frosted panel with a light edge, coloured by [tint] if given.
 * - Paper: a sheet with a hairline edge.
 * - Bold: a solid block ([tint] if given, else white) with a thick ink border and a hard shadow.
 * - Neon: near-black with an accent edge that glows.
 * [strong] is for cards that need to stand out a little more (search results). [grouped]
 * is for rows stacked into one block (a meal's foods): no shadow or glow, which would
 * spill onto the neighbouring rows.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color? = null,
    strong: Boolean = false,
    grouped: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val theme = LocalAppTheme.current
    val plain = if (strong) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val container = when (theme) {
        AppTheme.GLASS -> glassFill(tint, strong)
        AppTheme.BOLD -> tint ?: plain
        else -> plain
    }
    val contentColor = if (tintedCards && tint != null) contentColorFor(tint) else colors.onSurface
    val border = when (theme) {
        AppTheme.CLASSIC -> null
        AppTheme.GLASS -> glassBorder()
        AppTheme.PAPER -> BorderStroke(1.dp, colors.outlineVariant)
        AppTheme.BOLD -> BorderStroke(2.dp, colors.outline)
        AppTheme.NEON -> BorderStroke(if (grouped) 1.dp else 1.5.dp, colors.primary.copy(alpha = if (grouped) 0.4f else 0.9f))
    }
    val edge = when {
        grouped -> Modifier
        theme == AppTheme.BOLD -> Modifier.hardShadow(shape, boldShadowColor)
        theme == AppTheme.NEON -> Modifier.glow(shape, neonGlowColor)
        else -> Modifier
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.then(edge),
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier.then(edge),
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border
        ) { Column(content = content) }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
