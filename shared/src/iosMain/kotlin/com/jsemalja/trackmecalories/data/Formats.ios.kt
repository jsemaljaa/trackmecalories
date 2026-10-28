package com.jsemalja.trackmecalories.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

actual fun Long.grouped(): String = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterDecimalStyle
    maximumFractionDigits = 0u
}.stringFromNumber(NSNumber(longLong = this)) ?: toString()

actual fun Double.fixed(digits: Int): String = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterDecimalStyle
    usesGroupingSeparator = false
    minimumFractionDigits = digits.toULong()
    maximumFractionDigits = digits.toULong()
}.stringFromNumber(NSNumber(double = this)) ?: toString()

actual fun Double.fixedPlain(digits: Int): String = NSNumberFormatter().apply {
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
    usesGroupingSeparator = false
    minimumFractionDigits = digits.toULong()
    maximumFractionDigits = digits.toULong()
    minimumIntegerDigits = 1u
}.stringFromNumber(NSNumber(double = this)) ?: toString()

private fun LocalDate.toNSDate(): NSDate {
    val parts = NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.month.ordinal.toLong() + 1
        day = this@toNSDate.day.toLong()
        hour = 12
    }
    return NSCalendar.currentCalendar.dateFromComponents(parts)!!
}

actual fun formatDate(date: LocalDate, pattern: String): String = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
    dateFormat = pattern
}.stringFromDate(date.toNSDate())

actual fun formatLongDate(date: LocalDate): String = NSDateFormatter().apply {
    dateStyle = NSDateFormatterLongStyle
    timeStyle = NSDateFormatterNoStyle
}.stringFromDate(date.toNSDate())

actual fun DayOfWeek.shortName(): String {
    // shortWeekdaySymbols starts on Sunday.
    val symbols = NSDateFormatter().shortWeekdaySymbols
    return symbols[isoDayNumber % 7] as String
}

@Composable
actual fun rememberTimeFormat(): (LocalTime) -> String = remember {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }
    val formatTime: (LocalTime) -> String = { time ->
        val parts = NSDateComponents().apply {
            hour = time.hour.toLong()
            minute = time.minute.toLong()
        }
        formatter.stringFromDate(NSCalendar.currentCalendar.dateFromComponents(parts)!!)
    }
    formatTime
}

/** The locale's hour format has no AM/PM marker when the clock is 24-hour. */
@Composable
actual fun rememberIs24Hour(): Boolean = remember {
    NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)?.contains("a") != true
}
