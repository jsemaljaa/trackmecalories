package com.jsemalja.trackmecalories.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.jsemalja.trackmecalories.data.DayActivity
import com.jsemalja.trackmecalories.data.Workout
import com.jsemalja.trackmecalories.ui.theme.AppCard
import com.jsemalja.trackmecalories.ui.theme.tintedCards
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource

/** The day's steps, active calories and workouts, as synced to Health Connect by a watch. */
@Composable
fun ActivityCard(activity: DayActivity) {
    val kcal = stringResource(Res.string.unit_kcal)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tint = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = if (tintedCards) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.health_activity), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (activity.isEmpty) {
                    Text(stringResource(Res.string.health_nothing_synced), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val summary = listOfNotNull(
                        activity.steps?.let { pluralStringResource(Res.plurals.steps_count, it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), "%,d".format(it)) },
                        activity.activeKcal?.let { stringResource(Res.string.health_active_kcal, "%,d".format(it)) }
                    )
                    if (summary.isNotEmpty()) Text(summary.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    activity.workouts.forEach { w ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            listOfNotNull(
                                w.title ?: workoutTypeLabel(w.exerciseType),
                                "${w.minutes} ${stringResource(Res.string.unit_min)}",
                                w.activeKcal?.takeIf { it > 0 }?.let { "$it $kcal" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** A readable name for a workout the tracker didn't title; the common types only. */
@Composable
private fun workoutTypeLabel(type: Int): String = stringResource(
    when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> Res.string.workout_walking
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> Res.string.workout_running
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> Res.string.workout_biking
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> Res.string.workout_swimming
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> Res.string.workout_strength
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> Res.string.workout_hiking
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> Res.string.workout_yoga
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> Res.string.workout_elliptical
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> Res.string.workout_rowing
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> Res.string.workout_hiit
        else -> Res.string.workout_other
    }
)
