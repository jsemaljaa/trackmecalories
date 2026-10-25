package com.jsemalja.trackmecalories.data

import androidx.annotation.StringRes
import com.jsemalja.trackmecalories.R

/**
 * How much of the watch's active calories to add to today's goal. Off by default:
 * wearables tend to overestimate what a workout burned.
 */
enum class EatBack(val percent: Int, @StringRes val label: Int) {
    OFF(0, R.string.eat_back_off),
    HALF(50, R.string.eat_back_half),
    FULL(100, R.string.eat_back_full);

    /** Extra kcal for today's goal; nothing when no activity was read. */
    fun bonus(activeKcal: Int?): Int = (activeKcal ?: 0).coerceAtLeast(0) * percent / 100
}
