package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.resources.*

/**
 * A soft highlight sweeping across placeholder blocks, so a loading list reads as "coming"
 * rather than empty. Built from the theme's own neutrals, so it suits flat and glass.
 */
@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(listOf(base, highlight, base), start = Offset(x, 0f), end = Offset(x + 400f, 0f))
}

@Composable
private fun Bone(brush: Brush, width: Dp? = null, fraction: Float = 1f, height: Dp) {
    Box(
        (if (width != null) Modifier.width(width) else Modifier.fillMaxWidth(fraction))
            .height(height)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(brush)
    )
}

/**
 * Placeholder in the shape of a search result (title, subtitle, macro line, calories), shown
 * while the online databases answer, so results slot in without the list jumping.
 */
@Composable
fun SkeletonResultCard() {
    val brush = shimmerBrush()
    val loading = stringResource(Res.string.loading)
    AppCard(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = loading },
        shape = MaterialTheme.shapes.medium,
        strong = true
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Bone(brush, fraction = 0.7f, height = 14.dp)
                Bone(brush, fraction = 0.4f, height = 10.dp)
                Bone(brush, fraction = 0.55f, height = 9.dp)
            }
            Spacer(Modifier.width(24.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Bone(brush, width = 40.dp, height = 16.dp)
                Bone(brush, width = 56.dp, height = 9.dp)
            }
        }
    }
}
