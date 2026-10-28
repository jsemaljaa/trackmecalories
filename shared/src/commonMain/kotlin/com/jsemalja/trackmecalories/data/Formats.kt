package com.jsemalja.trackmecalories.data

import androidx.compose.runtime.Composable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// Locale-aware formatting in the app's language (what String.format / DateTimeFormatter with
// the default locale gave before the screens moved to :shared): "1 234" and "1,5" in Russian.

/**
 * Fills the %1$s / %1$d placeholders of a string resource resolved ahead of time (e.g. outside
 * composition), the way stringResource(res, args) does.
 */
fun String.withArgs(vararg args: Any?): String =
    Regex("""%(\d+)\$[ds]""").replace(this) { args[it.groupValues[1].toInt() - 1].toString() }

/** A whole number with grouping, like "%,d": "1,234". */
expect fun Long.grouped(): String

fun Int.grouped(): String = toLong().grouped()

/** [digits] decimals in the app's locale, like "%.1f": "1.5" / "1,5". */
expect fun Double.fixed(digits: Int): String

fun Float.fixed(digits: Int): String = toDouble().fixed(digits)

/** [digits] decimals with a dot whatever the locale, like "%.2f" with Locale.US (for exports). */
expect fun Double.fixedPlain(digits: Int): String

/** [date] in a CLDR [pattern] such as "d MMM" or "EEE d MMM yyyy", in the app's language. */
expect fun formatDate(date: LocalDate, pattern: String): String

/** The locale's long date, "28 October 2026" / "28 октября 2026 г.". */
expect fun formatLongDate(date: LocalDate): String

/** The weekday's short name, "Mon" / "пн". */
expect fun DayOfWeek.shortName(): String

/** A clock time in the phone's 12/24-hour setting (needs the platform context, hence composable). */
@Composable
expect fun rememberTimeFormat(): (LocalTime) -> String

/** Whether the phone shows 24-hour clock times (for time pickers). */
@Composable
expect fun rememberIs24Hour(): Boolean
