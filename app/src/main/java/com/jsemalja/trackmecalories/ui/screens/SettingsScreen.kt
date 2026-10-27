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
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import com.jsemalja.trackmecalories.ui.theme.AccentColor
import com.jsemalja.trackmecalories.ui.theme.AccentPalette
import com.jsemalja.trackmecalories.ui.theme.MAX_ACCENT_CHROMA
import com.jsemalja.trackmecalories.ui.theme.colorAt
import com.jsemalja.trackmecalories.ui.theme.hexOf
import com.jsemalja.trackmecalories.ui.theme.hueChroma
import com.jsemalja.trackmecalories.ui.theme.parseHexColor
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.jsemalja.trackmecalories.ui.theme.AppFont
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.glow
import com.jsemalja.trackmecalories.ui.theme.hardShadow
import com.jsemalja.trackmecalories.ui.theme.themeShapes
import com.jsemalja.trackmecalories.ui.theme.themeTypography
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.RectangleShape
import com.jsemalja.trackmecalories.ui.theme.CornerStyle
import com.jsemalja.trackmecalories.ui.theme.LocalDarkTheme
import com.jsemalja.trackmecalories.ui.theme.ThemeMode
import com.jsemalja.trackmecalories.ui.theme.neutrals
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
    onThemeChange: (AppTheme) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onCustomAccentChange: (Int) -> Unit,
    onFontChange: (AppFont?) -> Unit,
    onCornersChange: (CornerStyle?) -> Unit,
    onModeChange: (ThemeMode) -> Unit
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
            AppearanceSettings(appearance, onThemeChange, onModeChange, onAccentChange, onCustomAccentChange, onFontChange, onCornersChange)
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
    onThemeChange: (AppTheme) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onCustomAccentChange: (Int) -> Unit,
    onFontChange: (AppFont?) -> Unit,
    onCornersChange: (CornerStyle?) -> Unit
) {
    val dark = LocalDarkTheme.current
    var editingCustom by rememberSaveable { mutableStateOf(false) }

    Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    // Scrolls sideways on narrow phones; the padding leaves room for Bold's shadow and Neon's glow.
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppTheme.entries.forEach { theme ->
            ThemeTile(
                theme = theme,
                tint = appearance.palette.primaryLight,
                dark = dark,
                selected = appearance.theme == theme,
                onClick = { onThemeChange(theme) }
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = appearance.mode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(mode.label)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    // The dots are unlabelled (TalkBack reads their names), so the chosen one is named here.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.accent_colour),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (appearance.accent == AccentColor.CUSTOM) stringResource(R.string.accent_custom_hex, hexOf(appearance.customAccent))
            else stringResource(appearance.accent.label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AccentColor.available.forEach { accent ->
            val selected = appearance.accent == accent
            when (accent) {
                // Wallpaper colours are unknown up front: a soft colour wheel.
                AccentColor.WALLPAPER -> AccentSwatch(
                    name = stringResource(accent.label),
                    fill = Brush.sweepGradient(listOf(Color(0xFF8ED6AE), Color(0xFFA8C8FF), Color(0xFFCFBDFF), Color(0xFFFFB0CA), Color(0xFFFFB68B), Color(0xFF8ED6AE))),
                    iconTint = Color.Black.copy(alpha = 0.7f),
                    selected = selected,
                    icon = Icons.Default.Wallpaper,
                    onClick = { onAccentChange(accent) }
                )
                // A vivid wheel that opens the editor; once picked it shows the colour, and
                // tapping it again edits it.
                AccentColor.CUSTOM -> {
                    val color = appearance.palette.primary(dark)
                    AccentSwatch(
                        name = stringResource(accent.label),
                        fill = if (selected) SolidColor(color) else Brush.sweepGradient(vividWheel),
                        iconTint = if (selected) checkTint(color) else Color.White,
                        selected = selected,
                        icon = if (selected) Icons.Default.Edit else Icons.Default.Palette,
                        onClick = { editingCustom = true }
                    )
                }
                else -> {
                    val color = accent.palette.primary(dark)
                    AccentSwatch(
                        name = stringResource(accent.label),
                        fill = SolidColor(color),
                        iconTint = checkTint(color),
                        selected = selected,
                        onClick = { onAccentChange(accent) }
                    )
                }
            }
        }
    }

    if (editingCustom) {
        val wallpaperPrimary = MaterialTheme.colorScheme.primary
        CustomAccentDialog(
            // Start from what's on screen, so a small nudge from a preset is easy.
            initial = when (appearance.accent) {
                AccentColor.CUSTOM -> appearance.customAccent
                AccentColor.WALLPAPER -> wallpaperPrimary.toArgb()
                else -> appearance.palette.primaryLight.toArgb()
            },
            dark = dark,
            onDismiss = { editingCustom = false },
            onApply = {
                onCustomAccentChange(it)
                editingCustom = false
            }
        )
    }

    // Corners and font follow the theme until overridden here; picking a theme resets them.
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.corners), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    val cornerOptions = listOf<CornerStyle?>(null) + CornerStyle.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        cornerOptions.forEachIndexed { i, corners ->
            SegmentedButton(
                selected = appearance.corners == corners,
                onClick = { onCornersChange(corners) },
                shape = SegmentedButtonDefaults.itemShape(i, cornerOptions.size),
                colors = appSegmentedColors()
            ) { FitText(stringResource(corners?.label ?: R.string.from_theme)) }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.font), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = appearance.font == null,
            onClick = { onFontChange(null) },
            label = { Text(stringResource(R.string.from_theme), fontFamily = themeTypography(appearance.theme).titleMedium.fontFamily) }
        )
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

/**
 * A miniature of [theme] on its own page colour: a card drawn the theme's way (Glass
 * frosted, Paper a ruled sheet, Bold ink-bordered with a hard shadow, Neon glowing) with
 * "Aa" in the theme's heading typeface and a progress bar in its style, so the themes can
 * be compared at a glance rather than by name.
 */
@Composable
private fun ThemeTile(
    theme: AppTheme,
    tint: Color,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val n = remember(theme, tint, dark) { neutrals(theme, tint, dark) }
    val shapes = remember(theme) { themeShapes(theme) }
    val headingFont = remember(theme) { themeTypography(theme).titleLarge.fontFamily }
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val frame = MaterialTheme.shapes.medium
    val card = shapes.medium

    val cardModifier = when (theme) {
        AppTheme.CLASSIC -> Modifier.background(n.containerLow, card)
        AppTheme.GLASS -> Modifier
            .background(Color.White.copy(alpha = if (dark) 0.1f else 0.55f), card)
            .border(1.dp, Color.White.copy(alpha = if (dark) 0.3f else 0.9f), card)
        AppTheme.PAPER -> Modifier.background(n.containerLow, card).border(1.dp, n.outlineVariant, card)
        AppTheme.BOLD -> Modifier
            .hardShadow(card, if (dark) primary else n.outline, 3.dp)
            .background(n.containerLow, card)
            .border(2.dp, n.outline, card)
        AppTheme.NEON -> Modifier
            .glow(card, primary.copy(alpha = if (dark) 0.5f else 0.3f), 8.dp)
            .background(n.containerLow, card)
            .border(1.dp, primary.copy(alpha = 0.7f), card)
    }
    val barHeight = when (theme) {
        AppTheme.PAPER -> 2.dp
        AppTheme.BOLD -> 7.dp
        AppTheme.NEON -> 3.dp
        else -> 5.dp
    }
    val barShape = if (theme == AppTheme.PAPER) RectangleShape else shapes.extraSmall

    Column(
        Modifier.width(92.dp).selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(frame)
                .background(n.background)
                .then(
                    if (theme == AppTheme.GLASS) Modifier.background(
                        Brush.linearGradient(listOf(primary.copy(alpha = 0.5f), colors.tertiary.copy(alpha = 0.3f), colors.secondary.copy(alpha = 0.35f)))
                    ) else Modifier
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) primary else colors.outlineVariant,
                    shape = frame
                )
                .padding(start = 10.dp, top = 10.dp, end = 13.dp, bottom = 13.dp)
        ) {
            Column(
                Modifier.fillMaxSize().then(cardModifier).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Aa",
                    fontFamily = headingFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = n.onBackground
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(barShape)
                        .background(if (theme == AppTheme.BOLD) Color.Transparent else primary.copy(alpha = 0.2f))
                        .then(if (theme == AppTheme.BOLD) Modifier.border(1.5.dp, n.outline, barShape) else Modifier)
                ) {
                    Box(Modifier.fillMaxWidth(0.6f).fillMaxHeight().clip(barShape).background(primary))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        FitText(
            stringResource(theme.label),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else null
        )
    }
}

/** Every hue at full accent strength, closed into a loop for a sweep gradient. */
private val vividWheel: List<Color> = (0..12).map { Color(colorAt(it * 30.0, MAX_ACCENT_CHROMA, 65.0)) }

private fun checkTint(color: Color) = if (color.luminance() > 0.4f) Color.Black.copy(alpha = 0.75f) else Color.White

/**
 * One accent dot, in the colour the current mode would use. Picking it shrinks the dot inside
 * a ring and shows a check (or [icon] for the special ones), like the system wallpaper picker.
 */
@Composable
private fun AccentSwatch(
    name: String,
    fill: Brush,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val inset by animateDpAsState(if (selected) 6.dp else 2.dp, label = "swatch inset")
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = name }
            .border(if (selected) 2.5.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .padding(inset)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        val shown = if (selected && icon == null) Icons.Default.Check else icon
        if (shown != null) Icon(shown, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Builds a custom accent from hue and vividness sliders, or a hex code. The sliders work in
 * OKLCh (see Tones.kt), so equal steps look equally far apart, and the preview is drawn with
 * the same generated tones the app will use in the current mode: what you see is what you get.
 */
@Composable
private fun CustomAccentDialog(initial: Int, dark: Boolean, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val start = remember { hueChroma(initial) }
    var hue by rememberSaveable { mutableFloatStateOf(start.first.toFloat()) }
    var chroma by rememberSaveable { mutableFloatStateOf(start.second.toFloat()) }
    var seed by rememberSaveable { mutableIntStateOf(initial) }
    var hex by rememberSaveable { mutableStateOf(hexOf(initial)) }
    val palette = remember(seed) { AccentPalette.fromSeed(seed) }
    val hexValid = parseHexColor(hex) != null

    // The seed from the sliders is the light-mode primary itself (tone 40).
    fun fromSliders(newHue: Float, newChroma: Float) {
        hue = newHue
        chroma = newChroma
        seed = colorAt(newHue.toDouble(), newChroma.toDouble(), 40.0)
        hex = hexOf(seed)
    }

    val vividness = remember(hue) { (0..6).map { Color(colorAt(hue.toDouble(), MAX_ACCENT_CHROMA * it / 6, 65.0)) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_accent_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AccentPreview(palette, dark)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.accent_hue), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GradientSlider(hue, 0f..360f, vividWheel) { fromSliders(it, chroma) }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.accent_vividness), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GradientSlider(chroma, 0f..MAX_ACCENT_CHROMA.toFloat(), vividness) { fromSliders(hue, it) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = hex,
                    onValueChange = { text ->
                        hex = text.removePrefix("#").filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6).uppercase()
                        parseHexColor(hex)?.let { typed ->
                            seed = typed
                            val (h, c) = hueChroma(typed)
                            hue = h.toFloat()
                            chroma = c.toFloat()
                        }
                    },
                    label = { Text(stringResource(R.string.accent_hex)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    isError = !hexValid,
                    supportingText = { if (!hexValid) Text(stringResource(R.string.accent_hex_invalid)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seed) }, enabled = hexValid) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** The palette's roles together: a ring and bar like the Today screen's, a filled and a tonal pill. */
@Composable
private fun AccentPreview(palette: AccentPalette, dark: Boolean) {
    val primary = palette.primary(dark)
    val onPrimary = if (dark) palette.onPrimaryDark else palette.onPrimaryLight
    val container = if (dark) palette.containerDark else palette.containerLight
    val onContainer = if (dark) palette.onContainerDark else palette.onContainerLight
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            progress = { 0.7f },
            color = primary,
            trackColor = container,
            strokeWidth = 6.dp,
            modifier = Modifier.size(52.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Aa",
                    style = MaterialTheme.typography.labelLarge,
                    color = onPrimary,
                    modifier = Modifier.clip(CircleShape).background(primary).padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Text(
                    "Aa",
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer,
                    modifier = Modifier.clip(CircleShape).background(container).padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            LinearProgressIndicator(
                progress = { 0.6f },
                color = primary,
                trackColor = container,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A slider over a gradient of what its values look like (the track itself is hidden). */
@Composable
private fun GradientSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    gradient: List<Color>,
    onChange: (Float) -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(gradient))
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onSurface,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
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
