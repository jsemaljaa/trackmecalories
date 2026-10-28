package com.jsemalja.trackmecalories

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// java.time-style helpers for kotlinx-datetime, so the tests read the same as before the
// domain code moved to :shared (which can't use java.time).

fun LocalDate.plusDays(days: Long): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other

fun LocalDateTime.toLocalDate(): LocalDate = date
fun LocalDateTime.minusDays(days: Long): LocalDateTime = LocalDateTime(date.minusDays(days), time)
fun LocalDateTime.withHour(hour: Int): LocalDateTime = LocalDateTime(date, LocalTime(hour, minute, second, nanosecond))
fun LocalDateTime.withMinute(minute: Int): LocalDateTime = LocalDateTime(date, LocalTime(hour, minute, second, nanosecond))

/** Wall-clock arithmetic like java.time's (no time zone involved). */
fun LocalDateTime.minusHours(hours: Long): LocalDateTime {
    val seconds = time.toSecondOfDay() - hours * 3600
    val days = Math.floorDiv(seconds, 86_400L)
    return LocalDateTime(date.plusDays(days), LocalTime.fromSecondOfDay(Math.floorMod(seconds, 86_400L).toInt()))
}
