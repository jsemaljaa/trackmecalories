package com.jsemalja.trackmecalories.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.StreakKind
import com.jsemalja.trackmecalories.ui.screens.description
import com.jsemalja.trackmecalories.ui.screens.icon
import com.jsemalja.trackmecalories.ui.screens.label
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.resources.*

/**
 * Streaks and badges: the switch for all of it, which streaks to track (water and fasting
 * only while those features are on), and where they show.
 */
@Composable
internal fun GamificationSettingsSection(viewModel: MainViewModel) {
    val settings by viewModel.gamificationSettings.collectAsState()
    val waterOn by viewModel.waterTrackingEnabled.collectAsState()
    val fasting by viewModel.fastingSettings.collectAsState()

    SettingsSectionCard {
        SettingSwitch(stringResource(Res.string.gamification_title), stringResource(Res.string.gamification_desc), settings.enabled) {
            viewModel.setGamification(settings.copy(enabled = it))
        }
    }
    if (!settings.enabled) return

    SettingsSectionCard(title = stringResource(Res.string.gamification_streaks_heading)) {
        StreakKind.entries.forEachIndexed { i, kind ->
            val available = when (kind) {
                StreakKind.WATER -> waterOn
                StreakKind.FASTING -> fasting.enabled
                else -> true
            }
            if (i > 0) Spacer(Modifier.height(12.dp))
            StreakChoice(kind, checked = kind in settings.streaks && available, enabled = available) { on ->
                viewModel.setGamification(settings.copy(streaks = if (on) settings.streaks + kind else settings.streaks - kind))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.gamification_grace_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SettingsSectionCard {
        SettingSwitch(stringResource(Res.string.gamification_show_today), stringResource(Res.string.gamification_show_today_desc), settings.showOnToday) {
            viewModel.setGamification(settings.copy(showOnToday = it))
        }
        Spacer(Modifier.height(16.dp))
        SettingSwitch(stringResource(Res.string.gamification_celebrations), stringResource(Res.string.gamification_celebrations_desc), settings.celebrations) {
            viewModel.setGamification(settings.copy(celebrations = it))
        }
    }
}

/** A streak with its icon and what counts; greyed out with a hint when its feature is off. */
@Composable
private fun StreakChoice(kind: StreakKind, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(kind.icon, contentDescription = null, tint = if (enabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(kind.label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                stringResource(if (enabled) kind.description else Res.string.gamification_needs_feature),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
