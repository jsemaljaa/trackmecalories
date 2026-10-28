package com.jsemalja.trackmecalories.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Milliseconds since the epoch (what `System.currentTimeMillis()` gives on the JVM). */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Today in the device's time zone. */
@OptIn(ExperimentalTime::class)
fun currentDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** The wall-clock date and time in the device's time zone. */
@OptIn(ExperimentalTime::class)
fun currentDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** The local date [millis] (epoch ms, like [FoodEntry.time]) falls on. */
@OptIn(ExperimentalTime::class)
fun dateOfMillis(millis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date

/** Epoch ms of this wall-clock time in [zone]. */
@OptIn(ExperimentalTime::class)
fun LocalDateTime.toEpochMillis(zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    toInstant(zone).toEpochMilliseconds()

/** A date column value ("2026-03-09", see [FoodEntry.date]). */
fun parseDate(iso: String): LocalDate = LocalDate.parse(iso)

/** Unicode canonical decomposition (NFD): "é" becomes "e" plus a combining accent. */
internal expect fun String.decomposed(): String
