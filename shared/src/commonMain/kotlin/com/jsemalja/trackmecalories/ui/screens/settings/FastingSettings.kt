package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.FASTING_PLANS
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.resources.*
import com.jsemalja.trackmecalories.ui.rememberFastingPermissions

/**
 * The fasting timer: on/off, the plan, which notifications to show, and what's needed for them
 * (the notification permission; exact alarms so alerts aren't late).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FastingSettingsSection(viewModel: MainViewModel) {
    val settings by viewModel.fastingSettings.collectAsState()
    val access by viewModel.fastingAccess.collectAsState()
    val permissions = rememberFastingPermissions(onResult = viewModel::refreshFasting)
    val anyAlert = settings.notifyOngoing || settings.notifyGoal || settings.notifyNextFast

    SettingSwitch(stringResource(Res.string.fasting_timer), stringResource(Res.string.fasting_timer_desc), settings.enabled) { on ->
        viewModel.setFastingSettings(settings.copy(enabled = on))
        if (on && anyAlert && !access.notifications) permissions.promptForNotifications?.invoke()
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
        TextButton(onClick = permissions.askForNotifications) { Text(stringResource(Res.string.fasting_allow_notifications)) }
    } else if ((settings.notifyGoal || settings.notifyNextFast) && !access.exactAlarms && permissions.openExactAlarmSettings != null) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.fasting_exact_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = permissions.openExactAlarmSettings) { Text(stringResource(Res.string.fasting_allow_exact)) }
    }
}
