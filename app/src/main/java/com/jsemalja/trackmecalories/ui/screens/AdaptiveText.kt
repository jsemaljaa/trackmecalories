package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * One line of text that shrinks (down to [minScale]) to fit its width, then ellipsizes.
 * For labels in fixed slots — segmented buttons, grid cells, big numbers in tiles — where
 * a longer translation or a large system font would otherwise wrap mid-word or be clipped.
 *
 * A plain [Layout] rather than BoxWithConstraints, so it also works inside
 * `IntrinsicSize.Min` rows (subcomposition can't answer intrinsic measurements).
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    minScale: Float = 0.6f
) = FitText(AnnotatedString(text), modifier, style, color, fontWeight, textAlign, minScale)

/** [FitText] with spans; size spans in `em` so they shrink along with the rest. */
@Composable
fun FitText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    minScale: Float = 0.6f
) {
    val measurer = rememberTextMeasurer()
    val base = style.copy(
        color = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } },
        fontWeight = fontWeight ?: style.fontWeight
    )
    // Written while measuring, read only while drawing: a new layout just redraws.
    var drawn by remember { mutableStateOf<Pair<TextLayoutResult, Offset>?>(null) }

    Layout(
        modifier = modifier
            .semantics { this.text = text }
            .drawBehind { drawn?.let { (layout, offset) -> drawText(layout, topLeft = offset) } }
    ) { _, constraints ->
        val fontSize = if (base.fontSize.isSpecified) base.fontSize else 14.sp
        fun styleAt(scale: Float) = base.copy(
            fontSize = fontSize * scale,
            lineHeight = if (base.lineHeight.isSpecified) base.lineHeight * scale else base.lineHeight
        )
        fun naturalWidth(scale: Float) =
            measurer.measure(text, styleAt(scale), maxLines = 1, softWrap = false).size.width

        var scale = 1f
        if (constraints.hasBoundedWidth) {
            while (scale > minScale && naturalWidth(scale) > constraints.maxWidth) {
                scale = (scale - 0.05f).coerceAtLeast(minScale)
            }
        }
        val layout = measurer.measure(
            text,
            styleAt(scale),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = constraints.maxWidth)
        )
        val width = constraints.constrainWidth(layout.size.width)
        val height = constraints.constrainHeight(layout.size.height)
        val x = when (textAlign) {
            TextAlign.Center -> (width - layout.size.width) / 2f
            TextAlign.End, TextAlign.Right -> (width - layout.size.width).toFloat()
            else -> 0f
        }
        val y = (height - layout.size.height) / 2f
        drawn = layout to Offset(x, y)
        val baseline = (y + layout.firstBaseline).roundToInt()
        layout(width, height, mapOf(FirstBaseline to baseline, LastBaseline to baseline)) {}
    }
}

/**
 * A big number with a small unit after it ("1 834 kcal"), sharing a baseline. The unit
 * always keeps its size; the number shrinks if the two don't fit side by side.
 */
@Composable
fun ValueWithUnit(
    value: String,
    unit: String,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    unitStyle: TextStyle = MaterialTheme.typography.labelMedium,
    valueColor: Color = Color.Unspecified,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight? = FontWeight.Bold,
    gap: Dp = 4.dp
) {
    Row(modifier) {
        FitText(
            value,
            style = valueStyle,
            color = valueColor,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f, fill = false).alignByBaseline()
        )
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(gap))
            Text(unit, style = unitStyle, color = unitColor, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
        }
    }
}
