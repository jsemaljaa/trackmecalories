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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** True when the Glass theme is active. */
val isGlass: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalAppTheme.current == AppTheme.GLASS

/**
 * True when a card's tint becomes its fill (Classic fills it tonally, Glass frosts it, Bold
 * blocks it in solid), so content on it should use the matching on-container colour rather
 * than plain onSurface.
 */
val tintedCards: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalAppTheme.current.let { it == AppTheme.CLASSIC || it == AppTheme.GLASS || it == AppTheme.BOLD }

/**
 * The backdrop glass surfaces sit on: the theme's dark (or light) [base] under an aurora of
 * the accent and its two neighbouring hues, vivid but one colour family, so it never clashes
 * whatever the accent. The glows are radial gradients, i.e. already "blurred", so the
 * translucent cards on top read as frosted without a live blur.
 */
@Composable
fun GlassBackdrop(dark: Boolean, base: Color) {
    val primary = MaterialTheme.colorScheme.primary
    val aurora = remember(primary, dark) { auroraColors(primary, dark) }
    val glow = if (dark) 0.75f else 0.85f

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
            blob(aurora[0], Offset(w * 0.1f, h * 0.06f), w * 1.05f, glow)
            blob(aurora[1], Offset(w * 1.05f, h * 0.38f), w * 0.9f, glow * 0.8f)
            blob(aurora[2], Offset(w * -0.05f, h * 0.72f), w * 0.95f, glow * 0.75f)
            blob(aurora[0], Offset(w * 0.85f, h * 1.02f), w * 0.8f, glow * 0.7f)
        }
    }
}

/**
 * Glass's aurora: [primary]'s hue and its neighbours either side, at a tone for the mode,
 * a little more vivid than the accent itself (so a grey accent gets a grey aurora, not a
 * rainbow from its meaningless hue).
 */
fun auroraColors(primary: Color, dark: Boolean): List<Color> {
    val (hue, chroma) = hueChroma(primary.toArgb(), 1.0)
    val vivid = (chroma * 1.4).coerceAtMost(0.17)
    val tone = if (dark) 50.0 else 80.0
    return listOf(0.0, 55.0, -55.0).map { Color(colorAt((hue + it + 360) % 360, vivid, tone)) }
}

/**
 * Segmented buttons in the accent (the default uses the amber secondary colour). Bold and
 * Terminal fill the selected segment solid, like their nav bars.
 */
@Composable
fun appSegmentedColors() = if (LocalAppTheme.current.let { it == AppTheme.BOLD || it == AppTheme.TERMINAL }) {
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

/**
 * Frosted fill for a glass surface, optionally tinted (e.g. the Home calorie card): denser
 * at the top and thinning downward, the sheen of light falling on a pane.
 */
@Composable
@ReadOnlyComposable
fun glassFill(tint: Color? = null, strong: Boolean = false): Brush {
    val dark = LocalDarkTheme.current
    val (top, bottom) = when {
        tint != null -> if (dark) tint.copy(alpha = 0.6f) to tint.copy(alpha = 0.32f) else tint.copy(alpha = 0.78f) to tint.copy(alpha = 0.5f)
        dark -> Color.White.copy(alpha = if (strong) 0.17f else 0.13f) to Color.White.copy(alpha = if (strong) 0.08f else 0.04f)
        else -> Color.White.copy(alpha = if (strong) 0.85f else 0.74f) to Color.White.copy(alpha = if (strong) 0.62f else 0.46f)
    }
    return Brush.verticalGradient(listOf(top, bottom))
}

/** The light-catching edge of glass: bright at the top-left, fading toward the bottom. */
@Composable
@ReadOnlyComposable
fun glassBorder(): BorderStroke {
    val dark = LocalDarkTheme.current
    val top = if (dark) 0.4f else 1f
    val bottom = if (dark) 0.06f else 0.4f
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

/** Bold's shadow colour: ink in light mode, the accent in dark (a black shadow wouldn't show). */
val boldShadowColor: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

/**
 * How a floating element (nav bar, the add button) sits on the page in the current theme:
 * Classic lifts it on a soft shadow, Glass gives it a light edge, Bold a thick ink border
 * and a hard shadow, Terminal a hairline. Returns the extra modifier and the border to draw.
 */
@Composable
fun raisedEdge(shape: Shape): Pair<Modifier, BorderStroke?> = when (LocalAppTheme.current) {
    AppTheme.CLASSIC -> Modifier.shadow(6.dp, shape) to null
    AppTheme.GLASS -> Modifier to glassBorder()
    AppTheme.BOLD -> Modifier.hardShadow(shape, boldShadowColor) to BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    AppTheme.TERMINAL -> Modifier to BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    AppTheme.PAPER -> Modifier to null
}

/**
 * The app's one card, drawn in the current [AppTheme]:
 * - Classic: a white sheet (a raised tone in dark mode) on a soft shadow, filled with
 *   [tint] if given.
 * - Glass: a translucent frosted panel with a sheen and a light edge, coloured by [tint] if given.
 * - Paper: a sheet with a hairline edge.
 * - Bold: a solid block ([tint] if given, else white) with a thick ink border and a hard shadow.
 * - Terminal: a square panel ruled by a hairline; [tint] is ignored, colour stays in the content.
 * [strong] is for cards that need to stand out a little more (search results). [grouped]
 * is for rows stacked into one block (a meal's foods): no shadow, which would spill onto
 * the neighbouring rows.
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
        // The frost is a gradient, drawn under a clear Surface.
        AppTheme.GLASS -> Color.Transparent
        AppTheme.CLASSIC, AppTheme.BOLD -> tint ?: plain
        else -> plain
    }
    val contentColor = if (tintedCards && tint != null) contentColorFor(tint) else colors.onSurface
    val border = when (theme) {
        AppTheme.CLASSIC -> null
        AppTheme.GLASS -> glassBorder()
        AppTheme.PAPER -> BorderStroke(1.dp, colors.outlineVariant)
        AppTheme.BOLD -> BorderStroke(2.dp, colors.outline)
        AppTheme.TERMINAL -> BorderStroke(1.dp, if (strong) colors.outline else colors.outlineVariant)
    }
    val edge = when {
        theme == AppTheme.GLASS -> Modifier.background(glassFill(tint, strong), shape)
        grouped -> Modifier
        theme == AppTheme.BOLD -> Modifier.hardShadow(shape, boldShadowColor)
        // A dark-mode shadow is invisible; there the raised tone does the lifting.
        theme == AppTheme.CLASSIC && !LocalDarkTheme.current -> Modifier.shadow(if (strong) 3.dp else 1.5.dp, shape)
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

