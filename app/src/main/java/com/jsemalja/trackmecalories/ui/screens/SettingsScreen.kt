package com.jsemalja.trackmecalories.ui.screens

import android.os.LocaleList
import android.os.Build
import android.app.LocaleManager
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import android.content.Intent
import android.Manifest
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.jsemalja.trackmecalories.data.FASTING_PLANS
import com.jsemalja.trackmecalories.data.FastingSettings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.jsemalja.trackmecalories.data.HealthData
import com.jsemalja.trackmecalories.data.EatBack
import com.jsemalja.trackmecalories.data.HealthStatus
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.AppFont
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.DesignStyle
import com.jsemalja.trackmecalories.ui.theme.CornerStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * Settings screen for user configuration.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    appearance: Appearance,
    onStyleChange: (DesignStyle) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onFontChange: (AppFont) -> Unit,
    onCornersChange: (CornerStyle) -> Unit
) {
    val currentGoals by viewModel.userGoal.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportChooserTitle = stringResource(R.string.export_chooser)
    val exportFailed = stringResource(R.string.export_failed)

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader(Icons.Default.Settings, stringResource(R.string.nav_settings))

        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = stringResource(R.string.appearance), titleColor = MaterialTheme.colorScheme.primary) {
            AppearanceSettings(appearance, onStyleChange, onAccentChange, onFontChange, onCornersChange)
            LanguageSettings()
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.goals_title), titleColor = MaterialTheme.colorScheme.primary) {
            val weights by viewModel.weightEntries.collectAsState()
            val measured by viewModel.adaptiveMaintenance.collectAsState()
            val waterOn by viewModel.waterTrackingEnabled.collectAsState()
            GoalsEditor(
                goals = currentGoals,
                latestWeightKg = weights.maxByOrNull { it.date }?.weightKg,
                measuredMaintenance = measured?.kcal,
                showWater = waterOn,
                onSave = viewModel::updateGoals
            )
            val weeklyOn by viewModel.weeklyBudgetEnabled.collectAsState()
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.weekly_budget), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.weekly_budget_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = weeklyOn, onCheckedChange = viewModel::setWeeklyBudget)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.water_tracking), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.water_tracking_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = waterOn, onCheckedChange = viewModel::setWaterTracking)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.fasting), titleColor = MaterialTheme.colorScheme.primary) {
            FastingSettingsSection(viewModel)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.health_data), titleColor = MaterialTheme.colorScheme.primary) {
            HealthSettings(viewModel)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.nutrition_score), titleColor = MaterialTheme.colorScheme.primary) {
            NutritionScoreSettings()
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.data), titleColor = MaterialTheme.colorScheme.primary) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        exportError = try {
                            val csv = viewModel.exportCsv()
                            val file = File(context.cacheDir, "trackmecalories_export_${LocalDate.now()}.csv")
                            file.writeText(csv)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, exportChooserTitle))
                            null
                        } catch (e: Exception) {
                            exportFailed.format(e.message ?: "")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.export_csv))
            }

            exportError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            // Fineli's CC BY licence requires crediting it.
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.food_data_credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.danger_zone), titleColor = MaterialTheme.colorScheme.error) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wipe_all))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.wipe_all_title)) },
            text = { Text(stringResource(R.string.wipe_all_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.wipe_all_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * Health Connect: connection status, the button to grant access (or fix why it can't be),
 * and how to get Zepp or Samsung Health syncing into it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthSettings(viewModel: MainViewModel) {
    val status by viewModel.healthStatus.collectAsState()
    val missing by viewModel.healthMissing.collectAsState()
    val context = LocalContext.current
    val samsungHealth = remember { context.packageManager.getLaunchIntentForPackage(HealthData.SAMSUNG_HEALTH) }
    var denied by remember { mutableStateOf(false) }
    val requestPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        denied = granted.isEmpty()
        viewModel.refreshHealth()
    }

    Text(
        stringResource(
            when (status) {
                HealthStatus.CONNECTED -> R.string.health_status_connected
                HealthStatus.NOT_CONNECTED -> R.string.health_status_not_connected
                HealthStatus.NEEDS_INSTALL -> R.string.health_status_needs_install
                HealthStatus.UNAVAILABLE -> R.string.health_status_unavailable
            }
        ),
        style = MaterialTheme.typography.bodyMedium
    )
    if (denied && status == HealthStatus.NOT_CONNECTED) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.health_denied), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    when (status) {
        HealthStatus.NOT_CONNECTED -> Button(onClick = { requestPermissions.launch(HealthData.permissions) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.health_connect))
        }
        // Granting more (or revoking) happens in Health Connect's own screen.
        HealthStatus.CONNECTED -> {
            // E.g. total calories, added for Samsung Health after the user had connected.
            if (missing) {
                Button(onClick = { requestPermissions.launch(HealthData.permissions) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.health_grant_missing))
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.health_manage)) }
        }
        HealthStatus.NEEDS_INSTALL -> OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"))
                            .setPackage("com.android.vending")
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.health_install)) }
        HealthStatus.UNAVAILABLE -> Unit
    }
    if (status == HealthStatus.CONNECTED) {
        val eatBack by viewModel.eatBack.collectAsState()
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.eat_back), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.eat_back_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            EatBack.entries.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = eatBack == option,
                    onClick = { viewModel.setEatBack(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, EatBack.entries.size),
                    colors = appSegmentedColors()
                ) { FitText(stringResource(option.label)) }
            }
        }
    }
    if (status != HealthStatus.UNAVAILABLE) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.health_zepp_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.health_samsung_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (samsungHealth != null) {
            TextButton(onClick = { runCatching { context.startActivity(samsungHealth) } }) {
                Text(stringResource(R.string.health_open_samsung))
            }
        }
    }
}

/**
 * The fasting timer: on/off, the plan, which notifications to show, and what's needed for them
 * (the notification permission; exact alarms so alerts aren't late).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FastingSettingsSection(viewModel: MainViewModel) {
    val settings by viewModel.fastingSettings.collectAsState()
    val access by viewModel.fastingAccess.collectAsState()
    val context = LocalContext.current
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Denied for good earlier: the system won't ask again, so open the app's notification settings.
        if (!granted) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        }
        viewModel.refreshFasting()
    }
    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        }
    }
    val anyAlert = settings.notifyOngoing || settings.notifyGoal || settings.notifyNextFast

    SettingSwitch(stringResource(R.string.fasting_timer), stringResource(R.string.fasting_timer_desc), settings.enabled) { on ->
        viewModel.setFastingSettings(settings.copy(enabled = on))
        if (on && anyAlert && !access.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (!settings.enabled) return

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.fasting_plan), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FASTING_PLANS.forEach { hours ->
            FilterChip(
                selected = settings.planHours == hours,
                onClick = { viewModel.setFastingSettings(settings.copy(planHours = hours)) },
                label = { Text("$hours:${24 - hours}") }
            )
        }
    }
    Text(stringResource(R.string.fasting_plan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(Modifier.height(16.dp))
    SettingSwitch(stringResource(R.string.fasting_notify_ongoing), stringResource(R.string.fasting_notify_ongoing_desc), settings.notifyOngoing) {
        viewModel.setFastingSettings(settings.copy(notifyOngoing = it))
    }
    Spacer(Modifier.height(12.dp))
    SettingSwitch(stringResource(R.string.fasting_notify_goal), stringResource(R.string.fasting_notify_goal_desc), settings.notifyGoal) {
        viewModel.setFastingSettings(settings.copy(notifyGoal = it))
    }
    Spacer(Modifier.height(12.dp))
    SettingSwitch(stringResource(R.string.fasting_notify_next), stringResource(R.string.fasting_notify_next_desc), settings.notifyNextFast) {
        viewModel.setFastingSettings(settings.copy(notifyNextFast = it))
    }

    if (anyAlert && !access.notifications) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.fasting_notifications_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = ::askForNotifications) { Text(stringResource(R.string.fasting_allow_notifications)) }
    } else if ((settings.notifyGoal || settings.notifyNextFast) && !access.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.fasting_exact_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Coming back from the system screen re-checks it (MainActivity.onResume).
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
        }) { Text(stringResource(R.string.fasting_allow_exact)) }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettings(
    appearance: Appearance,
    onStyleChange: (DesignStyle) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onFontChange: (AppFont) -> Unit,
    onCornersChange: (CornerStyle) -> Unit
) {
    Text(stringResource(R.string.style), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        DesignStyle.entries.forEachIndexed { i, style ->
            SegmentedButton(
                selected = appearance.style == style,
                onClick = { onStyleChange(style) },
                shape = SegmentedButtonDefaults.itemShape(i, DesignStyle.entries.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(style.label)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.corners), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        CornerStyle.entries.forEachIndexed { i, corners ->
            SegmentedButton(
                selected = appearance.corners == corners,
                onClick = { onCornersChange(corners) },
                shape = SegmentedButtonDefaults.itemShape(i, CornerStyle.entries.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(corners.label)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.accent_colour), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    val dark = isSystemInDarkTheme()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentColor.available.forEach { accent ->
            AccentSwatch(
                accent = accent,
                color = if (dark) accent.primaryDark else accent.primaryLight,
                selected = appearance.accent == accent,
                onClick = { onAccentChange(accent) }
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.font), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppFont.entries.forEach { font ->
            FilterChip(
                selected = appearance.font == font,
                onClick = { onFontChange(font) },
                // Each chip is set in its own font, so it doubles as a preview.
                label = { Text(stringResource(font.label), fontFamily = font.family) }
            )
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (accent == AccentColor.WALLPAPER) {
                        // Wallpaper colours are unknown up front: show a colour wheel.
                        Modifier.background(Brush.sweepGradient(listOf(Color(0xFF8ED6AE), Color(0xFFA8C8FF), Color(0xFFCFBDFF), Color(0xFFFFB0CA), Color(0xFFFFB68B), Color(0xFF8ED6AE))))
                    } else {
                        Modifier.background(color)
                    }
                )
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (color.luminance() > 0.4f) Color.Black.copy(alpha = 0.75f) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(accent.label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * In-app language (Android 13+ per-app language; the same setting appears in the system's
 * app info). "System" follows the phone's language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    var current by remember { mutableStateOf(localeManager.applicationLocales.toLanguageTags()) }
    val options = listOf("" to R.string.language_system, "en" to R.string.language_english, "ru" to R.string.language_russian)

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (tag, label) ->
            SegmentedButton(
                selected = current == tag,
                onClick = {
                    current = tag
                    // The activity restarts in the new language; the open tab is kept.
                    localeManager.applicationLocales =
                        if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = appSegmentedColors(),
                icon = {}
            ) { FitText(stringResource(label)) }
        }
    }
}
