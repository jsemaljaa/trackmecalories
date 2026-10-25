package com.jsemalja.trackmecalories.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.LocalDate
import kotlin.math.roundToInt

/** Whether Health Connect can be used, and whether we're allowed to read from it. */
enum class HealthStatus {
    /** Not supported on this phone. */
    UNAVAILABLE,
    /** Android 13 and older: the Health Connect app is missing or too old. */
    NEEDS_INSTALL,
    NOT_CONNECTED,
    /** At least one of [HealthData.permissions] granted. */
    CONNECTED
}

/** What a wearable synced for one day. Null fields weren't granted or had no data. */
data class DayActivity(
    val steps: Long?,
    val activeKcal: Int?,
    val workouts: List<Workout>
) {
    val isEmpty get() = steps == null && activeKcal == null && workouts.isEmpty()
}

data class Workout(
    /** The tracker's own name for it; falls back to [exerciseType] when blank. */
    val title: String?,
    val exerciseType: Int,
    val minutes: Int,
    val activeKcal: Int?
)

/**
 * Reads steps, active calories and workouts from Health Connect (where Zepp, Fitbit, Samsung
 * Health etc. sync to). Read live for the day being viewed; nothing is stored in our database.
 */
class HealthData(private val context: Context) {

    fun status(): HealthStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStatus.NOT_CONNECTED
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_INSTALL
        else -> HealthStatus.UNAVAILABLE
    }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /** [status], refined by what's been granted. */
    suspend fun currentStatus(): HealthStatus {
        val base = status()
        if (base != HealthStatus.NOT_CONNECTED) return base
        return if (granted().isNotEmpty()) HealthStatus.CONNECTED else HealthStatus.NOT_CONNECTED
    }

    suspend fun granted(): Set<String> = try {
        client.permissionController.getGrantedPermissions() intersect permissions
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptySet()
    }

    /** Something we ask for hasn't been granted (e.g. a type added in an app update). */
    suspend fun missingPermissions(): Boolean = granted() != permissions

    /**
     * The day's activity, or null if nothing can be read (not connected, the app is in the
     * background, Health Connect's rate limit...). Each data type is read only if granted,
     * since the user may allow some and not others.
     */
    suspend fun activityFor(date: LocalDate): DayActivity? = try {
        val granted = granted()
        if (granted.isEmpty()) {
            null
        } else {
            // Local time, so "the day" follows the phone's clock even after travelling.
            val day = TimeRangeFilter.between(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            val steps = if (STEPS in granted) {
                client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), day))[StepsRecord.COUNT_TOTAL]
            } else null
            val workouts = if (EXERCISE in granted) {
                client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, day)).records
                    .sortedBy { it.startTime }
                    .map { s ->
                        Workout(
                            title = s.title?.takeIf { it.isNotBlank() },
                            exerciseType = s.exerciseType,
                            minutes = Duration.between(s.startTime, s.endTime).toMinutes().toInt(),
                            activeKcal = (if (ACTIVE in granted) activeKcal(TimeRangeFilter.between(s.startTime, s.endTime)) else null)
                                ?: if (TOTAL in granted) samsungWorkoutKcal(s) else null
                        )
                    }
            } else emptyList()
            // Samsung Health syncs no active calories, only each workout's; count those instead.
            val active = (if (ACTIVE in granted) activeKcal(day) else null)
                ?: workouts.mapNotNull { it.activeKcal }.takeIf { it.isNotEmpty() }?.sum()
            DayActivity(steps, active, workouts)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun activeKcal(range: TimeRangeFilter): Int? =
        client.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), range))[
            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
        ]?.inKilocalories?.roundToInt()

    /**
     * Samsung Health writes a workout's calories as a total-calories record of its own, not as
     * active calories. Only trusted from Samsung: other apps' totals include resting burn.
     */
    private suspend fun samsungWorkoutKcal(s: ExerciseSessionRecord): Int? {
        if (s.metadata.dataOrigin.packageName != SAMSUNG_HEALTH) return null
        return client.readRecords(
            ReadRecordsRequest(
                TotalCaloriesBurnedRecord::class,
                TimeRangeFilter.between(s.startTime, s.endTime),
                dataOriginFilter = setOf(DataOrigin(SAMSUNG_HEALTH))
            )
        ).records.takeIf { it.isNotEmpty() }?.sumOf { it.energy.inKilocalories }?.roundToInt()
    }

    companion object {
        const val SAMSUNG_HEALTH = "com.sec.android.app.shealth"

        private val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        private val ACTIVE = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        private val EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val TOTAL = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)

        /** Everything we ask for; must match the manifest. */
        val permissions = setOf(STEPS, ACTIVE, EXERCISE, TOTAL)
    }
}
