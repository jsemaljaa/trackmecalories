package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

private val FULLNESS_LABELS = listOf(
    Res.string.fullness_1, Res.string.fullness_2, Res.string.fullness_3, Res.string.fullness_4, Res.string.fullness_5
)
private val ENERGY_LABELS = listOf(
    Res.string.energy_1, Res.string.energy_2, Res.string.energy_3, Res.string.energy_4, Res.string.energy_5
)

/** In a meal's header: "Rate", or the meal's fullness/energy once rated. */
@Composable
fun MealRatingChip(satiety: Int?, energy: Int?, onClick: () -> Unit) {
    val rated = listOfNotNull(
        satiety?.let { stringResource(Res.string.rating_fullness_short, it) },
        energy?.let { stringResource(Res.string.rating_energy_short, it) }
    )
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                if (rated.isEmpty()) stringResource(Res.string.rate_meal) else rated.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Outlined.SentimentSatisfied, contentDescription = null, modifier = Modifier.size(14.dp)) },
        // Compact, but grows with the system font size instead of clipping the label.
        modifier = Modifier.height(28.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        colors = if (rated.isEmpty()) AssistChipDefaults.assistChipColors() else AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    )
}

/**
 * How full and how energetic a meal left you, each 1–5. Saved on every entry of the meal,
 * so Trends > Insights can tell which foods actually keep you full.
 */
@Composable
fun RateMealDialog(
    meal: String,
    satiety: Int?,
    energy: Int?,
    onDismiss: () -> Unit,
    onSave: (satiety: Int?, energy: Int?) -> Unit
) {
    var fullness by remember { mutableStateOf(satiety) }
    var energyLevel by remember { mutableStateOf(energy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.rate_meal_title, mealLabel(meal))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(Res.string.rate_meal_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                RatingRow(Res.string.fullness, fullness, FULLNESS_LABELS) { fullness = it }
                Spacer(Modifier.height(16.dp))
                RatingRow(Res.string.energy, energyLevel, ENERGY_LABELS) { energyLevel = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(fullness, energyLevel) },
                enabled = fullness != null || energyLevel != null
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            if (satiety != null || energy != null) {
                TextButton(onClick = { onSave(null, null) }) { Text(stringResource(Res.string.rating_clear)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingRow(title: StringResource, value: Int?, labels: List<StringResource>, onChange: (Int?) -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        (1..5).forEach { n ->
            SegmentedButton(
                selected = value == n,
                // Tapping the selected value again clears it.
                onClick = { onChange(if (value == n) null else n) },
                shape = SegmentedButtonDefaults.itemShape(n - 1, 5),
                colors = appSegmentedColors(),
                icon = {}
            ) { Text("$n") }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        value?.let { stringResource(labels[it - 1]) } ?: stringResource(Res.string.rating_scale_hint, stringResource(labels[0]), stringResource(labels[4])),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
