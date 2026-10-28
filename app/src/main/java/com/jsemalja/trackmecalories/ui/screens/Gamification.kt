package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.Badge
import com.jsemalja.trackmecalories.data.BadgeProgress
import com.jsemalja.trackmecalories.data.GamificationState
import com.jsemalja.trackmecalories.data.Streak
import com.jsemalja.trackmecalories.data.StreakKind
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.viewmodel.weekStartOf
import java.time.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

val StreakKind.icon: ImageVector
    get() = when (this) {
        StreakKind.LOGGING -> Icons.Default.LocalFireDepartment
        StreakKind.COMPLETE_DAYS -> Icons.Default.TaskAlt
        StreakKind.PROTEIN -> Icons.Default.FitnessCenter
        StreakKind.WATER -> Icons.Default.WaterDrop
        StreakKind.CALORIES_IN_RANGE -> Icons.Default.TrackChanges
        StreakKind.FASTING -> Icons.Default.Timer
        StreakKind.WEIGH_IN -> Icons.Default.MonitorWeight
    }

val StreakKind.label: StringResource
    get() = when (this) {
        StreakKind.LOGGING -> Res.string.streak_logging
        StreakKind.COMPLETE_DAYS -> Res.string.streak_complete
        StreakKind.PROTEIN -> Res.string.streak_protein
        StreakKind.WATER -> Res.string.streak_water
        StreakKind.CALORIES_IN_RANGE -> Res.string.streak_calories
        StreakKind.FASTING -> Res.string.streak_fasting
        StreakKind.WEIGH_IN -> Res.string.streak_weigh_in
    }

val StreakKind.description: StringResource
    get() = when (this) {
        StreakKind.LOGGING -> Res.string.streak_logging_desc
        StreakKind.COMPLETE_DAYS -> Res.string.streak_complete_desc
        StreakKind.PROTEIN -> Res.string.streak_protein_desc
        StreakKind.WATER -> Res.string.streak_water_desc
        StreakKind.CALORIES_IN_RANGE -> Res.string.streak_calories_desc
        StreakKind.FASTING -> Res.string.streak_fasting_desc
        StreakKind.WEIGH_IN -> Res.string.streak_weigh_in_desc
    }

private val Badge.icon: ImageVector
    get() = when (this) {
        Badge.FIRST_DAY -> Icons.Default.Flag
        Badge.STREAK_7, Badge.STREAK_30, Badge.STREAK_100, Badge.STREAK_365 -> Icons.Default.LocalFireDepartment
        Badge.COMPLETE_WEEK -> Icons.Default.TaskAlt
        Badge.WEIGHED_50, Badge.WEIGHED_250 -> Icons.Default.Scale
        Badge.FIRST_SCAN -> Icons.Default.QrCodeScanner
        Badge.FIRST_RECIPE -> Icons.Default.RestaurantMenu
        Badge.FASTS_10 -> Icons.Default.Timer
        Badge.PROTEIN_30 -> Icons.Default.FitnessCenter
        Badge.WATER_30 -> Icons.Default.WaterDrop
        Badge.WEIGH_INS_10 -> Icons.Default.MonitorWeight
    }

private val Badge.title: StringResource
    get() = when (this) {
        Badge.FIRST_DAY -> Res.string.badge_first_day
        Badge.STREAK_7 -> Res.string.badge_streak_7
        Badge.STREAK_30 -> Res.string.badge_streak_30
        Badge.STREAK_100 -> Res.string.badge_streak_100
        Badge.STREAK_365 -> Res.string.badge_streak_365
        Badge.COMPLETE_WEEK -> Res.string.badge_complete_week
        Badge.WEIGHED_50 -> Res.string.badge_weighed_50
        Badge.WEIGHED_250 -> Res.string.badge_weighed_250
        Badge.FIRST_SCAN -> Res.string.badge_first_scan
        Badge.FIRST_RECIPE -> Res.string.badge_first_recipe
        Badge.FASTS_10 -> Res.string.badge_fasts_10
        Badge.PROTEIN_30 -> Res.string.badge_protein_30
        Badge.WATER_30 -> Res.string.badge_water_30
        Badge.WEIGH_INS_10 -> Res.string.badge_weigh_ins_10
    }

private val Badge.description: StringResource
    get() = when (this) {
        Badge.FIRST_DAY -> Res.string.badge_first_day_desc
        Badge.STREAK_7 -> Res.string.badge_streak_7_desc
        Badge.STREAK_30 -> Res.string.badge_streak_30_desc
        Badge.STREAK_100 -> Res.string.badge_streak_100_desc
        Badge.STREAK_365 -> Res.string.badge_streak_365_desc
        Badge.COMPLETE_WEEK -> Res.string.badge_complete_week_desc
        Badge.WEIGHED_50 -> Res.string.badge_weighed_50_desc
        Badge.WEIGHED_250 -> Res.string.badge_weighed_250_desc
        Badge.FIRST_SCAN -> Res.string.badge_first_scan_desc
        Badge.FIRST_RECIPE -> Res.string.badge_first_recipe_desc
        Badge.FASTS_10 -> Res.string.badge_fasts_10_desc
        Badge.PROTEIN_30 -> Res.string.badge_protein_30_desc
        Badge.WATER_30 -> Res.string.badge_water_30_desc
        Badge.WEIGH_INS_10 -> Res.string.badge_weigh_ins_10_desc
    }

/** "12 days", or "3 weeks" for a weekly streak. */
@Composable
private fun streakLength(kind: StreakKind, count: Int): String =
    pluralStringResource(if (kind.weekly) Res.plurals.streak_weeks else Res.plurals.streak_days, count, count)

/** Done today: the accent; still to do: muted; at risk: the error colour. */
@Composable
private fun streakTint(streak: Streak): Color = when {
    streak.atRisk -> MaterialTheme.colorScheme.error
    streak.pending || streak.current == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

/**
 * The tracked streaks on Today, as an icon and a count each; tapping opens [StreaksSheet].
 * The icons aren't labelled on screen, so TalkBack reads each streak's name and length.
 */
@Composable
fun StreakStrip(streaks: Map<StreakKind, Streak>, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                streaks.forEach { (kind, streak) ->
                    val description = stringResource(Res.string.streak_item, stringResource(kind.label), streakLength(kind, streak.current))
                    Row(
                        Modifier.clearAndSetSemantics { contentDescription = description },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(kind.icon, contentDescription = null, tint = streakTint(streak), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${streak.current}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.streaks_sheet_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Every tracked streak with its best and what's still to do, then the badges. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreaksSheet(state: GamificationState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            if (state.streaks.isNotEmpty()) {
                Text(stringResource(Res.string.streaks_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                state.streaks.forEach { (kind, streak) ->
                    StreakRow(kind, streak)
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    stringResource(Res.string.gamification_grace_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }

            val unlocked = state.badges.count { it.unlocked }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.badges_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(Res.string.badges_count, unlocked, state.badges.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            // Reached ones first; within each, the catalogue's order.
            state.badges.sortedByDescending { it.unlocked }.forEach { progress ->
                BadgeRow(progress)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StreakRow(kind: StreakKind, streak: Streak) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconDisc(kind.icon, streakTint(streak), filled = !streak.pending && streak.current > 0)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(kind.label), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val status = when {
                streak.atRisk -> stringResource(if (kind.weekly) Res.string.streak_at_risk_week else Res.string.streak_at_risk_day)
                streak.pending -> stringResource(if (kind.weekly) Res.string.streak_pending_week else Res.string.streak_pending_day)
                else -> null
            }
            Text(
                listOfNotNull(status, stringResource(Res.string.streak_best, streakLength(kind, streak.best))).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (streak.atRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (streak.current == 0) stringResource(Res.string.streak_none) else streakLength(kind, streak.current),
            style = if (streak.current == 0) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (streak.current == 0) null else FontWeight.Bold,
            color = if (streak.current == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BadgeRow(progress: BadgeProgress) {
    val badge = progress.badge
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconDisc(
            badge.icon,
            if (progress.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            filled = progress.unlocked
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(badge.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (progress.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(stringResource(badge.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!progress.unlocked) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.badge_progress, progress.current, badge.target),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** An icon in a circle: tinted and filled when done, outlined in grey when not. */
@Composable
private fun IconDisc(icon: ImageVector, tint: Color, filled: Boolean) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) tint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Shown on Today when badges have been reached since they were last shown. */
@Composable
fun BadgeCelebrationDialog(badges: List<Badge>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(if (badges.size == 1) Res.string.badge_new_one else Res.string.badge_new_many)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                badges.forEach { badge ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconDisc(badge.icon, MaterialTheme.colorScheme.primary, filled = true)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(badge.title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(badge.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.badge_nice)) } }
    )
}

private const val CONSISTENCY_WEEKS = 12

/**
 * The last [CONSISTENCY_WEEKS] weeks on Trends, a column a week (Monday at the top): complete
 * days in the accent, incomplete ones paler, empty days grey.
 */
@Composable
fun ConsistencyCard(state: GamificationState, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val complete = colors.primary
    val incomplete = colors.primary.copy(alpha = 0.35f)
    val none = colors.surfaceContainerHighest
    val firstWeek = weekStartOf(today).minusWeeks(CONSISTENCY_WEEKS - 1L)
    val shown = (0 until CONSISTENCY_WEEKS * 7).map { firstWeek.plusDays(it.toLong()) }.filter { !it.isAfter(today) }
    val logged = shown.count { it.toKotlinLocalDate() in state.loggedDays }
    val bestLogging = state.streaks[StreakKind.LOGGING]?.best

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.consistency_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.consistency_desc), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            // The grid is a picture of the numbers below, so TalkBack skips it.
            BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                val gap = 4.dp
                val cell = ((maxWidth - gap * (CONSISTENCY_WEEKS - 1)) / CONSISTENCY_WEEKS).coerceAtMost(22.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(CONSISTENCY_WEEKS) { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            repeat(7) { dow ->
                                val day = firstWeek.plusDays(week * 7L + dow)
                                val fill = when {
                                    day.isAfter(today) -> Color.Transparent
                                    day.toKotlinLocalDate() in state.completeDays -> complete
                                    day.toKotlinLocalDate() in state.loggedDays -> incomplete
                                    else -> none
                                }
                                Box(
                                    Modifier
                                        .size(cell)
                                        .clip(RoundedCornerShape(cell / 4))
                                        .background(fill)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(complete, stringResource(Res.string.consistency_complete))
                LegendDot(incomplete, stringResource(Res.string.consistency_incomplete))
                LegendDot(none, stringResource(Res.string.consistency_none))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                ConsistencyStat(stringResource(Res.string.consistency_days_logged), stringResource(Res.string.badge_progress, logged, shown.size), Modifier.weight(1f))
                if (bestLogging != null) {
                    ConsistencyStat(stringResource(Res.string.consistency_best_streak), streakLength(StreakKind.LOGGING, bestLogging), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConsistencyStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { }) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
