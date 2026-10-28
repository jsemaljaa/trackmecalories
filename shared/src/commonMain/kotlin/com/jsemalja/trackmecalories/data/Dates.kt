package com.jsemalja.trackmecalories.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// java.time-style date arithmetic for kotlinx-datetime, so the screens kept their logic as it
// was when they moved to :shared.

fun LocalDate.plusDays(days: Long): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.plusWeeks(weeks: Long): LocalDate = plus(weeks * 7, DateTimeUnit.DAY)
fun LocalDate.minusWeeks(weeks: Long): LocalDate = minus(weeks * 7, DateTimeUnit.DAY)
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other
fun LocalDate.isBefore(other: LocalDate): Boolean = this < other

/** The wall-clock time [millis] (epoch ms, like [FoodEntry.time]) falls on in the device's zone. */
@OptIn(ExperimentalTime::class)
fun timeOfMillis(millis: Long): LocalTime =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).time

/** Midnight UTC of this date, the way Material's date picker counts days. */
@OptIn(ExperimentalTime::class)
fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/** The date a Material date picker's [millis] (midnight UTC) stands for. */
fun dateOfPickerMillis(millis: Long): LocalDate = dateOfMillis(millis, TimeZone.UTC)
