package com.jsemalja.trackmecalories.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors

private val FULLNESS_LABELS = listOf(
    R.string.fullness_1, R.string.fullness_2, R.string.fullness_3, R.string.fullness_4, R.string.fullness_5
)
private val ENERGY_LABELS = listOf(
    R.string.energy_1, R.string.energy_2, R.string.energy_3, R.string.energy_4, R.string.energy_5
)

/** In a meal's header: "Rate", or the meal's fullness/energy once rated. */
@Composable
fun MealRatingChip(satiety: Int?, energy: Int?, onClick: () -> Unit) {
    val rated = listOfNotNull(
        satiety?.let { stringResource(R.string.rating_fullness_short, it) },
        energy?.let { stringResource(R.string.rating_energy_short, it) }
    )
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                if (rated.isEmpty()) stringResource(R.string.rate_meal) else rated.joinToString(" · "),
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
        title = { Text(stringResource(R.string.rate_meal_title, mealLabel(meal))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.rate_meal_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                RatingRow(R.string.fullness, fullness, FULLNESS_LABELS) { fullness = it }
                Spacer(Modifier.height(16.dp))
                RatingRow(R.string.energy, energyLevel, ENERGY_LABELS) { energyLevel = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(fullness, energyLevel) },
                enabled = fullness != null || energyLevel != null
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            if (satiety != null || energy != null) {
                TextButton(onClick = { onSave(null, null) }) { Text(stringResource(R.string.rating_clear)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingRow(@StringRes title: Int, value: Int?, labels: List<Int>, onChange: (Int?) -> Unit) {
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
        value?.let { stringResource(labels[it - 1]) } ?: stringResource(R.string.rating_scale_hint, stringResource(labels[0]), stringResource(labels[4])),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
