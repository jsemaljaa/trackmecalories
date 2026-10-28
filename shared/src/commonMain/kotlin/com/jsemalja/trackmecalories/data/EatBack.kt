package com.jsemalja.trackmecalories.data


/**
 * How much of the watch's active calories to add to today's goal. Off by default:
 * wearables tend to overestimate what a workout burned.
 */
enum class EatBack(val percent: Int) {
    OFF(0),
    HALF(50),
    FULL(100);

    /** Extra kcal for today's goal; nothing when no activity was read. */
    fun bonus(activeKcal: Int?): Int = (activeKcal ?: 0).coerceAtLeast(0) * percent / 100
}
