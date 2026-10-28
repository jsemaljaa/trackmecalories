package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

/** Settings > Nutrition score: one line and a button to the full explanation. */
@Composable
fun NutritionScoreSettings() {
    var open by remember { mutableStateOf(false) }
    Text(
        stringResource(Res.string.nutrition_score_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Info, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.nutrition_score_how))
    }
    if (open) NutritionScoreDialog(onDismiss = { open = false })
}

/** How the grade and the 0–100 score are worked out (see data/NutritionRating.kt). */
@Composable
private fun NutritionScoreDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nutrition_score_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(Res.string.nutrition_score_intro), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                GradeScale()
                Section(Res.string.nutrition_score_against_title, Res.string.nutrition_score_against)
                Section(Res.string.nutrition_score_for_title, Res.string.nutrition_score_for)
                Section(Res.string.nutrition_score_groups_title, Res.string.nutrition_score_groups)
                Section(Res.string.nutrition_score_scale_title, Res.string.nutrition_score_scale)
                Section(Res.string.nutrition_score_badges_title, Res.string.nutrition_score_badges)
                Section(Res.string.nutrition_score_missing_title, Res.string.nutrition_score_missing)
                Section(Res.string.nutrition_score_limits_title, Res.string.nutrition_score_limits)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } }
    )
}

/** The five letters with their score ranges, A 80–100 down to E 0–19. */
@Composable
private fun GradeScale() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf('A' to "80–100", 'B' to "60–79", 'C' to "40–59", 'D' to "20–39", 'E' to "0–19").forEach { (grade, range) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NutriScoreBadge(grade, estimated = false, size = 28.dp)
                Spacer(Modifier.height(4.dp))
                Text(range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: StringResource, body: StringResource) {
    Spacer(Modifier.height(12.dp))
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(2.dp))
    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
