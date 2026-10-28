@file:OptIn(ExperimentalForeignApi::class)

package com.jsemalja.trackmecalories.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import platform.Foundation.NSDate
import platform.Foundation.NSPredicate
import platform.Foundation.NSSortDescriptor
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectQueryNoLimit
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionStrictStartDate
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKSampleSortIdentifierStartDate
import platform.HealthKit.HKStatisticsOptionCumulativeSum
import platform.HealthKit.HKStatisticsQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.HKWorkout
import platform.HealthKit.HKWorkoutActivityTypeCycling
import platform.HealthKit.HKWorkoutActivityTypeElliptical
import platform.HealthKit.HKWorkoutActivityTypeFunctionalStrengthTraining
import platform.HealthKit.HKWorkoutActivityTypeHighIntensityIntervalTraining
import platform.HealthKit.HKWorkoutActivityTypeHiking
import platform.HealthKit.HKWorkoutActivityTypeRowing
import platform.HealthKit.HKWorkoutActivityTypeRunning
import platform.HealthKit.HKWorkoutActivityTypeSwimming
import platform.HealthKit.HKWorkoutActivityTypeTraditionalStrengthTraining
import platform.HealthKit.HKWorkoutActivityTypeWalking
import platform.HealthKit.HKWorkoutActivityTypeYoga
import platform.HealthKit.countUnit
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import platform.HealthKit.workoutType
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

/** The one HealthKit source, shared by the view model and the Health settings page. */
val healthKit: HealthKitSource by lazy { HealthKitSource(UserDefaultsStore("health")) }

/**
 * Steps, active calories and workouts from Apple Health (where Apple Watch, Zepp etc. write).
 * HealthKit never says whether reading was allowed, so "connected" means access was asked for;
 * reads just come back empty if it was declined.
 */
class HealthKitSource(private val prefs: KeyValueStore) : HealthSource {
    private val store = HKHealthStore()

    private val stepType get() = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierStepCount!!)!!
    private val activeType get() = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierActiveEnergyBurned!!)!!

    override fun status(): HealthStatus =
        if (HKHealthStore.isHealthDataAvailable()) HealthStatus.NOT_CONNECTED else HealthStatus.UNAVAILABLE

    override suspend fun currentStatus(): HealthStatus {
        val base = status()
        if (base != HealthStatus.NOT_CONNECTED) return base
        return if (prefs.getBoolean(KEY_ASKED, false)) HealthStatus.CONNECTED else HealthStatus.NOT_CONNECTED
    }

    override suspend fun missingPermissions(): Boolean = false

    /** Shows Apple Health's permission sheet; true once it was answered (not whether reading was allowed). */
    suspend fun requestAccess(): Boolean = suspendCancellableCoroutine { cont ->
        store.requestAuthorizationToShareTypes(null, setOf(stepType, activeType, HKObjectType.workoutType())) { success, _ ->
            if (success) prefs.edit { putBoolean(KEY_ASKED, true) }
            cont.resume(success)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun activityFor(date: LocalDate): DayActivity? {
        if (currentStatus() != HealthStatus.CONNECTED) return null
        val zone = TimeZone.currentSystemDefault()
        val start = NSDate.dateWithTimeIntervalSince1970(date.atStartOfDayIn(zone).epochSeconds.toDouble())
        val end = NSDate.dateWithTimeIntervalSince1970(date.plusDays(1).atStartOfDayIn(zone).epochSeconds.toDouble())
        val day = HKQuery.predicateForSamplesWithStartDate(start, end, HKQueryOptionStrictStartDate)
        val steps = sum(stepType, HKUnit.countUnit(), day)?.toLong()
        val workouts = workouts(day)
        val active = sum(activeType, HKUnit.kilocalorieUnit(), day)?.roundToInt()
            ?: workouts.mapNotNull { it.activeKcal }.takeIf { it.isNotEmpty() }?.sum()
        return DayActivity(steps, active, workouts)
    }

    private suspend fun sum(type: HKQuantityType, unit: HKUnit, predicate: NSPredicate?): Double? = suspendCancellableCoroutine { cont ->
        val query = HKStatisticsQuery(type, predicate, HKStatisticsOptionCumulativeSum) { _, stats, _ ->
            cont.resume(stats?.sumQuantity()?.doubleValueForUnit(unit))
        }
        store.executeQuery(query)
        cont.invokeOnCancellation { store.stopQuery(query) }
    }

    private suspend fun workouts(predicate: NSPredicate?): List<Workout> = suspendCancellableCoroutine { cont ->
        val sort = NSSortDescriptor(key = HKSampleSortIdentifierStartDate, ascending = true)
        val query = HKSampleQuery(HKObjectType.workoutType(), predicate, HKObjectQueryNoLimit, listOf(sort)) { _, samples, _ ->
            cont.resume(samples.orEmpty().filterIsInstance<HKWorkout>().map { w ->
                Workout(
                    title = null,
                    kind = kindOf(w.workoutActivityType),
                    minutes = (w.duration / 60).toInt(),
                    @Suppress("DEPRECATION")
                    activeKcal = w.totalEnergyBurned?.doubleValueForUnit(HKUnit.kilocalorieUnit())?.roundToInt()
                )
            })
        }
        store.executeQuery(query)
        cont.invokeOnCancellation { store.stopQuery(query) }
    }

    private fun kindOf(type: ULong): WorkoutKind = when (type) {
        HKWorkoutActivityTypeWalking -> WorkoutKind.WALKING
        HKWorkoutActivityTypeRunning -> WorkoutKind.RUNNING
        HKWorkoutActivityTypeCycling -> WorkoutKind.BIKING
        HKWorkoutActivityTypeSwimming -> WorkoutKind.SWIMMING
        HKWorkoutActivityTypeTraditionalStrengthTraining, HKWorkoutActivityTypeFunctionalStrengthTraining -> WorkoutKind.STRENGTH
        HKWorkoutActivityTypeHiking -> WorkoutKind.HIKING
        HKWorkoutActivityTypeYoga -> WorkoutKind.YOGA
        HKWorkoutActivityTypeElliptical -> WorkoutKind.ELLIPTICAL
        HKWorkoutActivityTypeRowing -> WorkoutKind.ROWING
        HKWorkoutActivityTypeHighIntensityIntervalTraining -> WorkoutKind.HIIT
        else -> WorkoutKind.OTHER
    }

    private companion object {
        const val KEY_ASKED = "asked"
    }
}
