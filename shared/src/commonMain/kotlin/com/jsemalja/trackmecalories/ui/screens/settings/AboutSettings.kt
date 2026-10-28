package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jsemalja.trackmecalories.ui.screens.NutritionScoreSettings
import com.jsemalja.trackmecalories.resources.*
import com.jsemalja.trackmecalories.ui.rememberAppVersion

/** The version, how the nutrition score works, and where the food data comes from. */
@Composable
internal fun AboutSettings() {
    val version = rememberAppVersion()

    SettingsSectionCard {
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(Res.string.settings_version, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SettingsSectionCard(title = stringResource(Res.string.nutrition_score)) {
        NutritionScoreSettings()
    }

    // Fineli's CC BY licence requires crediting it.
    SettingsSectionCard(title = stringResource(Res.string.settings_food_data)) {
        Text(
            stringResource(Res.string.food_data_credits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
