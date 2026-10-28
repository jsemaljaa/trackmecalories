package com.jsemalja.trackmecalories.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.FASTING_PLANS
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.resources.*

/**
 * The fasting timer: on/off, the plan, which notifications to show, and what's needed for them
 * (the notification permission; exact alarms so alerts aren't late).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FastingSettingsSection(viewModel: MainViewModel) {
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

    SettingSwitch(stringResource(Res.string.fasting_timer), stringResource(Res.string.fasting_timer_desc), settings.enabled) { on ->
        viewModel.setFastingSettings(settings.copy(enabled = on))
        if (on && anyAlert && !access.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (!settings.enabled) return

    Spacer(Modifier.height(16.dp))
    Text(stringResource(Res.string.fasting_plan), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Text(stringResource(Res.string.fasting_plan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(Modifier.height(16.dp))
    SettingSwitch(stringResource(Res.string.fasting_notify_ongoing), stringResource(Res.string.fasting_notify_ongoing_desc), settings.notifyOngoing) {
        viewModel.setFastingSettings(settings.copy(notifyOngoing = it))
    }
    Spacer(Modifier.height(12.dp))
    SettingSwitch(stringResource(Res.string.fasting_notify_goal), stringResource(Res.string.fasting_notify_goal_desc), settings.notifyGoal) {
        viewModel.setFastingSettings(settings.copy(notifyGoal = it))
    }
    Spacer(Modifier.height(12.dp))
    SettingSwitch(stringResource(Res.string.fasting_notify_next), stringResource(Res.string.fasting_notify_next_desc), settings.notifyNextFast) {
        viewModel.setFastingSettings(settings.copy(notifyNextFast = it))
    }

    if (anyAlert && !access.notifications) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.fasting_notifications_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = ::askForNotifications) { Text(stringResource(Res.string.fasting_allow_notifications)) }
    } else if ((settings.notifyGoal || settings.notifyNextFast) && !access.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.fasting_exact_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Coming back from the system screen re-checks it (MainActivity.onResume).
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
        }) { Text(stringResource(Res.string.fasting_allow_exact)) }
    }
}
