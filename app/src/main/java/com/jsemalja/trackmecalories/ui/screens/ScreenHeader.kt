package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.LocalAppTheme
import com.jsemalja.trackmecalories.ui.theme.hardShadow
import com.jsemalja.trackmecalories.ui.theme.boldShadowColor

/**
 * The title at the top of each non-Home tab, in the current theme's voice: Paper is a
 * serif masthead over a double rule, Bold an uppercase title beside an ink-bordered icon
 * block, Terminal a `>` prompt with a blinking cursor, Classic the icon in a tonal disc.
 */
@Composable
fun ScreenHeader(icon: ImageVector, title: String) {
    val colors = MaterialTheme.colorScheme
    when (LocalAppTheme.current) {
        AppTheme.PAPER -> Column(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(thickness = 2.dp, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(thickness = 1.dp, color = colors.onSurface)
        }
        AppTheme.BOLD -> Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = MaterialTheme.shapes.small
            Box(
                Modifier
                    .size(36.dp)
                    .hardShadow(shape, boldShadowColor, 3.dp)
                    .clip(shape)
                    .background(colors.primary)
                    .border(2.dp, colors.outline, shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        AppTheme.TERMINAL -> Row(verticalAlignment = Alignment.CenterVertically) {
            // Blinks by redrawing only the cursor; with animations off it just stays lit.
            val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(keyframes {
                    durationMillis = 1100
                    1f at 549
                    0f at 550
                }),
                label = "cursorBlink"
            )
            Text(">", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                title.lowercase(),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(width = 14.dp, height = 28.dp).graphicsLayer { alpha = blink }.background(colors.primary))
        }
        AppTheme.CLASSIC -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
