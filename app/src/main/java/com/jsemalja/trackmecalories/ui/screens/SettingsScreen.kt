package com.jsemalja.trackmecalories.ui.screens

import android.os.LocaleList
import android.os.Build
import android.app.LocaleManager
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.appSegmentedColors
import android.content.Intent
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
import com.jsemalja.trackmecalories.ui.theme.Appearance
import com.jsemalja.trackmecalories.ui.theme.DesignStyle
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
    onAccentChange: (AccentColor) -> Unit
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
            AppearanceSettings(appearance, onStyleChange, onAccentChange)
            LanguageSettings()
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.goals_title), titleColor = MaterialTheme.colorScheme.primary) {
            val weights by viewModel.weightEntries.collectAsState()
            val measured by viewModel.adaptiveMaintenance.collectAsState()
            GoalsEditor(
                goals = currentGoals,
                latestWeightKg = weights.maxByOrNull { it.date }?.weightKg,
                measuredMaintenance = measured?.kcal,
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
        }

        Spacer(Modifier.height(16.dp))

        SettingsSectionCard(title = stringResource(R.string.health_data), titleColor = MaterialTheme.colorScheme.primary) {
            HealthSettings(viewModel)
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
 * and how to get Zepp syncing into it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthSettings(viewModel: MainViewModel) {
    val status by viewModel.healthStatus.collectAsState()
    val context = LocalContext.current
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
        HealthStatus.CONNECTED -> OutlinedButton(
            onClick = {
                runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context)) }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.health_manage)) }
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
                ) { Text(stringResource(option.label)) }
            }
        }
    }
    if (status != HealthStatus.UNAVAILABLE) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.health_zepp_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onAccentChange: (AccentColor) -> Unit
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
            ) { Text(stringResource(style.label)) }
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
            ) { Text(stringResource(label)) }
        }
    }
}
