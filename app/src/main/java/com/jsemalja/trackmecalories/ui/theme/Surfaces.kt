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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** True when the glass style is active. */
val isGlass: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalDesignStyle.current == DesignStyle.GLASS

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

/** Segmented buttons in the accent (the default uses the amber secondary colour). */
@Composable
fun appSegmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    activeContentColor = MaterialTheme.colorScheme.primary,
    activeBorderColor = MaterialTheme.colorScheme.outline,
    inactiveContainerColor = Color.Transparent
)

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

/**
 * The app's one card. Flat (minimal): a quiet neutral container with no shadow or colour
 * block — the accent is left to the content (rings, bars, buttons) — and a hairline edge
 * only in themes where card and page are close in tone ([ThemePalette.outlinedCards]). Glass: a
 * translucent frosted panel with a light edge, coloured by [tint] if given.
 * [strong] is for cards that need to stand out a little more (search results).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color? = null,
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val glass = isGlass
    val container = when {
        glass -> glassFill(tint, strong)
        strong -> colors.surfaceContainerHigh
        else -> colors.surfaceContainerLow
    }
    val contentColor = if (glass && tint != null) contentColorFor(tint) else colors.onSurface
    val border = when {
        glass -> glassBorder()
        LocalThemePalette.current.outlinedCards -> BorderStroke(1.dp, colors.outlineVariant)
        else -> null
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border
        ) { Column(content = content) }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
