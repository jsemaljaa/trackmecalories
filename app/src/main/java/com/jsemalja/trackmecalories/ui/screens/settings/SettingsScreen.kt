package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.HealthStatus
import com.jsemalja.trackmecalories.data.StreakKind
import com.jsemalja.trackmecalories.ui.screens.GoalsEditor
import com.jsemalja.trackmecalories.ui.screens.ScreenHeader
import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.AppFont
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.CornerStyle
import com.jsemalja.trackmecalories.ui.theme.ThemeMode
import com.jsemalja.trackmecalories.ui.theme.hexOf
import com.jsemalja.trackmecalories.ui.theme.predictiveBack
import com.jsemalja.trackmecalories.ui.theme.rememberPredictiveBack
import com.jsemalja.trackmecalories.ui.theme.sharedAxisX
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

/** The pages under Settings; back from one goes to its [parent], or to the list. */
private enum class SettingsPage(@StringRes val title: Int, val parent: SettingsPage? = null) {
    GOALS(R.string.goals_title),
    FEATURES(R.string.settings_features),
    FASTING(R.string.fasting_timer, parent = FEATURES),
    STREAKS(R.string.gamification_title),
    HEALTH(R.string.health_data),
    APPEARANCE(R.string.appearance),
    DATA(R.string.data),
    ABOUT(R.string.settings_about);

    val depth: Int get() = 1 + (parent?.depth ?: 0)
}

/**
 * Settings: a short list of categories, each showing what it's set to, that open their own
 * page. Back (with the predictive gesture) steps up one page at a time.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    appearance: Appearance,
    onThemeChange: (AppTheme) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onCustomAccentChange: (Int) -> Unit,
    onFontChange: (AppFont?) -> Unit,
    onCornersChange: (CornerStyle?) -> Unit,
    onModeChange: (ThemeMode) -> Unit
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    // Kept out here so coming back from a page lands where the list was.
    val listScroll = rememberScrollState()
    val back = rememberPredictiveBack(enabled = page != null) { page = page?.parent }
    val goBack = { page = page?.parent }

    AnimatedContent(
        targetState = page,
        transitionSpec = { sharedAxisX(forward = (targetState?.depth ?: 0) > (initialState?.depth ?: 0)) },
        modifier = modifier.predictiveBack(back),
        label = "settings page"
    ) { shown ->
        val pageModifier = Modifier.fillMaxSize()
        when (shown) {
            null -> SettingsList(viewModel, appearance, listScroll, pageModifier) { page = it }
            SettingsPage.GOALS -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                SettingsSectionCard {
                    val goals by viewModel.userGoal.collectAsState()
                    val weights by viewModel.weightEntries.collectAsState()
                    val measured by viewModel.adaptiveMaintenance.collectAsState()
                    val waterOn by viewModel.waterTrackingEnabled.collectAsState()
                    GoalsEditor(
                        goals = goals,
                        latestWeightKg = weights.maxByOrNull { it.date }?.weightKg,
                        measuredMaintenance = measured?.kcal,
                        showWater = waterOn,
                        onSave = viewModel::updateGoals
                    )
                }
            }
            SettingsPage.FEATURES -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                FeaturesSettings(viewModel, onOpen = { page = it })
            }
            SettingsPage.FASTING -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                SettingsSectionCard { FastingSettingsSection(viewModel) }
            }
            SettingsPage.STREAKS -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                GamificationSettingsSection(viewModel)
            }
            SettingsPage.HEALTH -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                SettingsSectionCard { HealthSettings(viewModel) }
            }
            SettingsPage.APPEARANCE -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                SettingsSectionCard {
                    AppearanceSettings(appearance, onThemeChange, onModeChange, onAccentChange, onCustomAccentChange, onFontChange, onCornersChange)
                }
            }
            SettingsPage.DATA -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                DataSettings(viewModel)
            }
            SettingsPage.ABOUT -> SettingsPageScaffold(stringResource(shown.title), goBack, pageModifier) {
                AboutSettings()
            }
        }
    }
}

/** The top level: three blocks of rows, each row summarising its page. */
@Composable
private fun SettingsList(
    viewModel: MainViewModel,
    appearance: Appearance,
    scroll: ScrollState,
    modifier: Modifier,
    onOpen: (SettingsPage) -> Unit
) {
    val goals by viewModel.userGoal.collectAsState()
    val weeklyOn by viewModel.weeklyBudgetEnabled.collectAsState()
    val waterOn by viewModel.waterTrackingEnabled.collectAsState()
    val fasting by viewModel.fastingSettings.collectAsState()
    val gamification by viewModel.gamificationSettings.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    var choosingLanguage by remember { mutableStateOf(false) }
    val language = rememberAppLanguage()

    Column(modifier.verticalScroll(scroll).padding(16.dp)) {
        ScreenHeader(Icons.Default.Settings, stringResource(R.string.nav_settings))
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SettingsRow(
                0, 4, Icons.Default.Flag, stringResource(SettingsPage.GOALS.title),
                "%,d ".format(goals.calorieGoal) + stringResource(R.string.unit_kcal) + " · " +
                    stringResource(
                        R.string.macro_line,
                        goals.proteinGoal.roundToInt().toString(),
                        goals.carbGoal.roundToInt().toString(),
                        goals.fatGoal.roundToInt().toString()
                    )
            ) { onOpen(SettingsPage.GOALS) }
            val features = listOfNotNull(
                stringResource(R.string.weekly_budget).takeIf { weeklyOn },
                stringResource(R.string.water_tracking).takeIf { waterOn },
                stringResource(R.string.settings_fasting_plan, fastingPlanLabel(fasting.planHours)).takeIf { fasting.enabled }
            )
            SettingsRow(
                1, 4, Icons.Default.Tune, stringResource(SettingsPage.FEATURES.title),
                if (features.isEmpty()) stringResource(R.string.settings_features_none) else features.joinToString(" · ")
            ) { onOpen(SettingsPage.FEATURES) }
            // Water and fasting streaks only count while those features are on.
            val tracked = gamification.streaks.count {
                (it != StreakKind.WATER || waterOn) && (it != StreakKind.FASTING || fasting.enabled)
            }
            SettingsRow(
                2, 4, Icons.Default.LocalFireDepartment, stringResource(SettingsPage.STREAKS.title),
                if (gamification.enabled) pluralStringResource(R.plurals.gamification_on_summary, tracked, tracked)
                else stringResource(R.string.settings_off)
            ) { onOpen(SettingsPage.STREAKS) }
            SettingsRow(
                3, 4, Icons.Default.MonitorHeart, stringResource(SettingsPage.HEALTH.title),
                stringResource(
                    when (healthStatus) {
                        HealthStatus.CONNECTED -> R.string.settings_health_connected
                        HealthStatus.NOT_CONNECTED -> R.string.settings_health_not_connected
                        HealthStatus.NEEDS_INSTALL -> R.string.settings_health_needs_install
                        HealthStatus.UNAVAILABLE -> R.string.settings_health_unavailable
                    }
                )
            ) { onOpen(SettingsPage.HEALTH) }
        }

        Spacer(Modifier.height(16.dp))
        // Language needs Android 13's per-app languages; before that the row is left out.
        val lookCount = if (language != null) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SettingsRow(
                0, lookCount, Icons.Default.Palette, stringResource(SettingsPage.APPEARANCE.title),
                listOf(
                    stringResource(appearance.theme.label),
                    stringResource(appearance.mode.label),
                    if (appearance.accent == AccentColor.CUSTOM) stringResource(R.string.accent_custom_hex, hexOf(appearance.customAccent))
                    else stringResource(appearance.accent.label)
                ).joinToString(" · ")
            ) { onOpen(SettingsPage.APPEARANCE) }
            if (language != null) {
                SettingsRow(1, 2, Icons.Default.Language, stringResource(R.string.language), stringResource(language.current.label)) {
                    choosingLanguage = true
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SettingsRow(0, 2, Icons.Default.Storage, stringResource(SettingsPage.DATA.title), stringResource(R.string.settings_data_summary)) {
                onOpen(SettingsPage.DATA)
            }
            SettingsRow(1, 2, Icons.Outlined.Info, stringResource(SettingsPage.ABOUT.title), stringResource(R.string.settings_about_summary)) {
                onOpen(SettingsPage.ABOUT)
            }
        }
    }

    if (choosingLanguage && language != null) {
        LanguageDialog(language, onDismiss = { choosingLanguage = false })
    }
}

/** Optional parts of Today that can be switched off. */
@Composable
private fun FeaturesSettings(viewModel: MainViewModel, onOpen: (SettingsPage) -> Unit) {
    val weeklyOn by viewModel.weeklyBudgetEnabled.collectAsState()
    val waterOn by viewModel.waterTrackingEnabled.collectAsState()
    val fasting by viewModel.fastingSettings.collectAsState()

    SettingsSectionCard {
        SettingSwitch(stringResource(R.string.weekly_budget), stringResource(R.string.weekly_budget_desc), weeklyOn, viewModel::setWeeklyBudget)
        Spacer(Modifier.height(16.dp))
        SettingSwitch(stringResource(R.string.water_tracking), stringResource(R.string.water_tracking_desc), waterOn, viewModel::setWaterTracking)
    }
    SettingsRow(
        0, 1, Icons.Default.Timer, stringResource(R.string.fasting_timer),
        if (fasting.enabled) fastingPlanLabel(fasting.planHours) else stringResource(R.string.settings_off)
    ) { onOpen(SettingsPage.FASTING) }
}

/** "16:8": fasting hours, then the eating window. */
internal fun fastingPlanLabel(hours: Int) = "$hours:${24 - hours}"
