package com.jsemalja.trackmecalories.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * How corners are drawn. [ROUNDED] is plain circular arcs; [CONTINUOUS] eases the curve
 * into the straight edge (a "squircle", like iOS icons) so corners read softer at the same
 * radius; [COMPACT] is the rounded style with tighter radii.
 */
enum class CornerStyle(@StringRes val label: Int) {
    ROUNDED(R.string.corners_rounded),
    CONTINUOUS(R.string.corners_continuous),
    COMPACT(R.string.corners_compact)
}

/**
 * A single corner scale so every card/dialog/chip in the app shares the same rhythm
 * instead of each screen picking its own radius.
 */
fun appShapes(style: CornerStyle): Shapes = when (style) {
    CornerStyle.ROUNDED -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    // Slightly larger radii: a smoothed corner starts curving earlier but looks tighter.
    CornerStyle.CONTINUOUS -> Shapes(
        extraSmall = ContinuousCornerShape(CornerSize(7.dp)),
        small = ContinuousCornerShape(CornerSize(12.dp)),
        medium = ContinuousCornerShape(CornerSize(16.dp)),
        large = ContinuousCornerShape(CornerSize(24.dp)),
        extraLarge = ContinuousCornerShape(CornerSize(32.dp))
    )
    CornerStyle.COMPACT -> Shapes(
        extraSmall = RoundedCornerShape(3.dp),
        small = RoundedCornerShape(5.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(16.dp)
    )
}

/**
 * Rounded rectangle with continuous ("smoothed") corners, after Figma's corner smoothing:
 * each corner is a short circular arc with a cubic Bézier on either side that blends it
 * into the edge, so the curvature ramps up instead of jumping from straight to round.
 * [smoothing] 0 is a plain rounded corner; 0.6 matches iOS. On a small shape the corners
 * run out of room, and the smoothing shrinks first so the shape stays a clean pill.
 */
class ContinuousCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val smoothing: Float = 0.6f
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(all: CornerSize, smoothing: Float = 0.6f) : this(all, all, all, all, smoothing)

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize) =
        ContinuousCornerShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) return Outline.Rectangle(size.toRect())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val w = size.width
        val h = size.height
        val budget = min(w, h) / 2f
        val path = Path()
        // Clockwise from the top-right corner; u runs along the incoming edge, v the outgoing one.
        val corners = listOf(
            Corner(Offset(w, 0f), Offset(1f, 0f), Offset(0f, 1f), if (ltr) topEnd else topStart),
            Corner(Offset(w, h), Offset(0f, 1f), Offset(-1f, 0f), if (ltr) bottomEnd else bottomStart),
            Corner(Offset(0f, h), Offset(-1f, 0f), Offset(0f, -1f), if (ltr) bottomStart else bottomEnd),
            Corner(Offset(0f, 0f), Offset(0f, -1f), Offset(1f, 0f), if (ltr) topStart else topEnd)
        )
        corners.forEachIndexed { i, corner -> path.addCorner(corner, budget, first = i == 0) }
        path.close()
        return Outline.Generic(path)
    }

    private class Corner(val point: Offset, val u: Offset, val v: Offset, val radius: Float)

    private fun Path.addCorner(corner: Corner, budget: Float, first: Boolean) {
        val r = corner.radius.coerceIn(0f, budget)
        if (r == 0f) {
            if (first) moveTo(corner.point.x, corner.point.y) else lineTo(corner.point.x, corner.point.y)
            return
        }
        // How far along each edge the corner reaches; give up smoothing before radius.
        val s = smoothing.coerceAtMost(budget / r - 1f).coerceAtLeast(0f)
        val p = (1f + s) * r
        val arcDegrees = 90f * (1f - s)
        val arcLength = sin(rad(arcDegrees / 2f)) * r * sqrt(2f)
        val alpha = (90f - arcDegrees) / 2f
        val p3ToP4 = r * tan(rad(alpha / 2f))
        val beta = 45f * s
        val c = p3ToP4 * cos(rad(beta))
        val d = c * tan(rad(beta))
        val b = (p - arcLength - c - d) / 3f
        val a = 2f * b

        // Local (x along u, y along v) -> absolute, measured from where the corner starts.
        val origin = corner.point - corner.u * p
        fun at(x: Float, y: Float) = origin + corner.u * x + corner.v * y

        val start = at(0f, 0f)
        if (first) moveTo(start.x, start.y) else lineTo(start.x, start.y)
        val e1 = at(a + b + c, d)
        at(a, 0f).let { c1 -> at(a + b, 0f).let { c2 -> cubicTo(c1.x, c1.y, c2.x, c2.y, e1.x, e1.y) } }
        if (arcDegrees > 0f) {
            val center = corner.point - corner.u * r + corner.v * r
            val startAngle = Math.toDegrees(atan2((e1.y - center.y).toDouble(), (e1.x - center.x).toDouble())).toFloat()
            arcTo(Rect(center, r), startAngle, arcDegrees, forceMoveTo = false)
        }
        val x2 = a + b + c + arcLength
        val y2 = d + arcLength
        val c1 = at(x2 + d, y2 + c)
        val c2 = at(x2 + d, y2 + b + c)
        val end = at(x2 + d, y2 + a + b + c)
        cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
    }

    private fun rad(degrees: Float) = Math.toRadians(degrees.toDouble()).toFloat()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContinuousCornerShape) return false
        return topStart == other.topStart && topEnd == other.topEnd && bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart && smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }

    override fun toString() = "ContinuousCornerShape(topStart = $topStart, topEnd = $topEnd, bottomEnd = " +
        "$bottomEnd, bottomStart = $bottomStart, smoothing = $smoothing)"
}
