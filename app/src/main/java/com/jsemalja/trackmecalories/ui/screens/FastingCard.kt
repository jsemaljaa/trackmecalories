package com.jsemalja.trackmecalories.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.Fast
import com.jsemalja.trackmecalories.data.HOUR_MS
import com.jsemalja.trackmecalories.data.fastProgress
import com.jsemalja.trackmecalories.data.fastStartFromClock
import com.jsemalja.trackmecalories.data.formatFastDuration
import com.jsemalja.trackmecalories.data.formatStopwatch
import com.jsemalja.trackmecalories.data.goalTime
import com.jsemalja.trackmecalories.data.nextFastDue
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.tintedCards
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

/**
 * The fasting timer on Today: while fasting, the elapsed time, progress toward the goal and an
 * End button (tap the card to correct when the fast started); between fasts, how long since the
 * last one and when the next is due, with a Start button.
 */
@Composable
fun FastingCard(
    active: Fast?,
    last: Fast?,
    planHours: Int,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onMoveStart: (Long) -> Unit
) {
    val context = LocalContext.current
    val hourUnit = stringResource(R.string.unit_h)
    val minuteUnit = stringResource(R.string.unit_min)
    fun duration(ms: Long) = formatFastDuration(ms, hourUnit, minuteUnit)
    fun clock(ms: Long): String = DateFormat.getTimeFormat(context).format(Date(ms))
    // Ticks every second, on the second, while the card is on screen.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            value = System.currentTimeMillis()
        }
    }
    var editingStart by remember { mutableStateOf(false) }
    var confirmingEnd by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tint = MaterialTheme.colorScheme.primaryContainer,
        onClick = if (active != null) ({ editingStart = true }) else null
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = if (tintedCards) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${stringResource(R.string.fasting)} · ${planHours}:${24 - planHours}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (active != null) {
                        val progress = fastProgress(active, now)
                        Text(formatStopwatch(progress.elapsedMs), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (progress.reached) stringResource(R.string.fasting_goal_reached, duration(progress.targetMs))
                            else stringResource(R.string.fasting_left, duration(progress.remainingMs), clock(active.goalTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (progress.reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            if (last?.endTime != null) stringResource(R.string.fasting_since_last, duration(now - last.endTime))
                            else stringResource(R.string.fasting_not_fasting),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        last?.endTime?.let { end ->
                            val due = nextFastDue(end, planHours)
                            val lastText = stringResource(R.string.fasting_last, duration(end - last.startTime))
                            Text(
                                if (due > now) "$lastText · ${stringResource(R.string.fasting_next_at, clock(due))}" else lastText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                val buttonPadding = PaddingValues(horizontal = 12.dp)
                if (active != null) {
                    OutlinedButton(
                        onClick = { if (fastProgress(active, now).reached) onEnd() else confirmingEnd = true },
                        contentPadding = buttonPadding
                    ) { Text(stringResource(R.string.fasting_end), maxLines = 1) }
                } else {
                    Button(onClick = onStart, contentPadding = buttonPadding) { Text(stringResource(R.string.fasting_start), maxLines = 1) }
                }
            }
            if (active != null) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { fastProgress(active, now).fraction },
                    modifier = Modifier.fillMaxWidth(),
                    drawStopIndicator = {}
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.fasting_started_at, clock(active.startTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (active != null && editingStart) {
        FastStartDialog(
            start = active.startTime,
            onDismiss = { editingStart = false },
            onSave = {
                onMoveStart(it)
                editingStart = false
            }
        )
    }

    if (active != null && confirmingEnd) {
        val progress = fastProgress(active, now)
        AlertDialog(
            onDismissRequest = { confirmingEnd = false },
            title = { Text(stringResource(R.string.fasting_end_early_title)) },
            text = { Text(stringResource(R.string.fasting_end_early_body, duration(progress.elapsedMs), duration(active.targetHours * HOUR_MS))) },
            confirmButton = {
                Button(onClick = {
                    onEnd()
                    confirmingEnd = false
                }) { Text(stringResource(R.string.fasting_end)) }
            },
            dismissButton = { TextButton(onClick = { confirmingEnd = false }) { Text(stringResource(R.string.fasting_keep)) } }
        )
    }
}

/** Picks when the fast really started, as a clock time within the last 24 hours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FastStartDialog(start: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val context = LocalContext.current
    val initial = remember(start) { Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime() }
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fasting_edit_start)) },
        text = { TimePicker(state) },
        confirmButton = {
            Button(onClick = {
                onSave(fastStartFromClock(LocalTime.of(state.hour, state.minute), ZonedDateTime.now()).toInstant().toEpochMilli())
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
