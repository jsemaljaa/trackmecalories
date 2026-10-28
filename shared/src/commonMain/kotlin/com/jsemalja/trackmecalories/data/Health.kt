package com.jsemalja.trackmecalories.data

import kotlinx.datetime.LocalDate

/** Whether the platform's health store can be used, and whether we're allowed to read from it. */
enum class HealthStatus {
    /** Not supported on this phone. */
    UNAVAILABLE,
    /** Android 13 and older: the Health Connect app is missing or too old. */
    NEEDS_INSTALL,
    NOT_CONNECTED,
    /** At least one of the read permissions granted. */
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
    /** The tracker's own name for it; falls back to [kind] when blank. */
    val title: String?,
    val kind: WorkoutKind,
    val minutes: Int,
    val activeKcal: Int?
)

/** The workout types given a name of their own; everything else is [OTHER]. */
enum class WorkoutKind { WALKING, RUNNING, BIKING, SWIMMING, STRENGTH, HIKING, YOGA, ELLIPTICAL, ROWING, HIIT, OTHER }

/**
 * Steps, active calories and workouts from the platform's health store (Health Connect on
 * Android, where Zepp, Fitbit, Samsung Health etc. sync to). Read live; nothing is stored.
 */
interface HealthSource {
    /** Availability without asking what's granted (cheap, synchronous). */
    fun status(): HealthStatus

    /** [status], refined by what's been granted. */
    suspend fun currentStatus(): HealthStatus

    /** Something we ask for hasn't been granted (e.g. a type added in an app update). */
    suspend fun missingPermissions(): Boolean

    /** The day's activity, or null if nothing can be read. */
    suspend fun activityFor(date: LocalDate): DayActivity?
}
