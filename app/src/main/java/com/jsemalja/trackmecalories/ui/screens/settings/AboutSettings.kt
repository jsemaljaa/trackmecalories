package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.ui.screens.NutritionScoreSettings

/** The version, how the nutrition score works, and where the food data comes from. */
@Composable
internal fun AboutSettings() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    SettingsSectionCard {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.settings_version, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SettingsSectionCard(title = stringResource(R.string.nutrition_score)) {
        NutritionScoreSettings()
    }

    // Fineli's CC BY licence requires crediting it.
    SettingsSectionCard(title = stringResource(R.string.settings_food_data)) {
        Text(
            stringResource(R.string.food_data_credits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
