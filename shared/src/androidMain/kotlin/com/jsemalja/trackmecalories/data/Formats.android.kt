package com.jsemalja.trackmecalories.data

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

actual fun Long.grouped(): String = "%,d".format(this)

actual fun Double.fixed(digits: Int): String = "%.${digits}f".format(this)

actual fun Double.fixedPlain(digits: Int): String = "%.${digits}f".format(Locale.US, this)

actual fun formatDate(date: LocalDate, pattern: String): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

actual fun formatLongDate(date: LocalDate): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

actual fun DayOfWeek.shortName(): String = toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())

@Composable
actual fun rememberTimeFormat(): (LocalTime) -> String {
    val context = LocalContext.current
    return remember(context) {
        val format = DateFormat.getTimeFormat(context)
        val calendar = Calendar.getInstance()
        val formatTime: (LocalTime) -> String = { time ->
            calendar.set(Calendar.HOUR_OF_DAY, time.hour)
            calendar.set(Calendar.MINUTE, time.minute)
            format.format(calendar.time)
        }
        formatTime
    }
}

@Composable
actual fun rememberIs24Hour(): Boolean {
    val context = LocalContext.current
    return remember(context) { DateFormat.is24HourFormat(context) }
}
