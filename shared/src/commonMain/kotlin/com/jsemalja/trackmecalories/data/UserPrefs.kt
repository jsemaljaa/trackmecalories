package com.jsemalja.trackmecalories.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Behaviour preferences (the "user_prefs" store; not part of the logged data). */
class UserPrefs(private val prefs: KeyValueStore) {

    private val _weeklyBudget = MutableStateFlow(prefs.getBoolean(KEY_WEEKLY_BUDGET, false))
    /** Let daily targets flex across the week (see [weeklyBudget]). */
    val weeklyBudget: StateFlow<Boolean> = _weeklyBudget.asStateFlow()

    fun setWeeklyBudget(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_WEEKLY_BUDGET, enabled) }
        _weeklyBudget.value = enabled
    }

    private val _eatBack = MutableStateFlow(
        EatBack.entries.firstOrNull { it.name == prefs.getString(KEY_EAT_BACK) } ?: EatBack.OFF
    )
    /** Share of today's active calories added to today's goal. */
    val eatBack: StateFlow<EatBack> = _eatBack.asStateFlow()

    fun setEatBack(value: EatBack) {
        prefs.edit { putString(KEY_EAT_BACK, value.name) }
        _eatBack.value = value
    }

    private val _waterTracking = MutableStateFlow(prefs.getBoolean(KEY_WATER_TRACKING, true))
    /** Show the water card on Today. Off only hides it; logged water is kept. */
    val waterTracking: StateFlow<Boolean> = _waterTracking.asStateFlow()

    fun setWaterTracking(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_WATER_TRACKING, enabled) }
        _waterTracking.value = enabled
    }

    private val _fasting = MutableStateFlow(
        FastingSettings(
            enabled = prefs.getBoolean(KEY_FASTING, false),
            planHours = prefs.getInt(KEY_FASTING_HOURS, DEFAULT_FASTING_HOURS),
            notifyOngoing = prefs.getBoolean(KEY_FASTING_NOTIFY_ONGOING, true),
            notifyGoal = prefs.getBoolean(KEY_FASTING_NOTIFY_GOAL, true),
            notifyNextFast = prefs.getBoolean(KEY_FASTING_NOTIFY_NEXT, false)
        )
    )
    /** The fasting timer on Today and its notifications. Off only hides it; fasts are kept. */
    val fasting: StateFlow<FastingSettings> = _fasting.asStateFlow()

    fun setFasting(value: FastingSettings) {
        prefs.edit {
            putBoolean(KEY_FASTING, value.enabled)
            putInt(KEY_FASTING_HOURS, value.planHours)
            putBoolean(KEY_FASTING_NOTIFY_ONGOING, value.notifyOngoing)
            putBoolean(KEY_FASTING_NOTIFY_GOAL, value.notifyGoal)
            putBoolean(KEY_FASTING_NOTIFY_NEXT, value.notifyNextFast)
        }
        _fasting.value = value
    }

    private val _gamification = MutableStateFlow(
        GamificationSettings(
            enabled = prefs.getBoolean(KEY_GAMIFICATION, false),
            streaks = prefs.getStringSet(KEY_GAMIFICATION_STREAKS)
                ?.mapNotNullTo(mutableSetOf()) { name -> StreakKind.entries.firstOrNull { it.name == name } }
                ?: StreakKind.DEFAULT,
            showOnToday = prefs.getBoolean(KEY_GAMIFICATION_TODAY, true),
            celebrations = prefs.getBoolean(KEY_GAMIFICATION_CELEBRATIONS, true)
        )
    )
    /** Streaks and badges. Off hides them all; nothing about them is stored but [seenBadges]. */
    val gamification: StateFlow<GamificationSettings> = _gamification.asStateFlow()

    fun setGamification(value: GamificationSettings) {
        prefs.edit {
            putBoolean(KEY_GAMIFICATION, value.enabled)
            putStringSet(KEY_GAMIFICATION_STREAKS, value.streaks.mapTo(mutableSetOf()) { it.name })
            putBoolean(KEY_GAMIFICATION_TODAY, value.showOnToday)
            putBoolean(KEY_GAMIFICATION_CELEBRATIONS, value.celebrations)
        }
        _gamification.value = value
    }

    private val _seenBadges = MutableStateFlow(prefs.getStringSet(KEY_SEEN_BADGES) ?: emptySet())
    /** [Badge] names already celebrated (or reached before streaks were turned on), so each shows once. */
    val seenBadges: StateFlow<Set<String>> = _seenBadges.asStateFlow()

    fun markBadgesSeen(badges: Collection<Badge>) {
        val seen = _seenBadges.value + badges.map { it.name }
        if (seen.size == _seenBadges.value.size) return
        prefs.edit { putStringSet(KEY_SEEN_BADGES, seen) }
        _seenBadges.value = seen
    }

    private companion object {
        const val KEY_WEEKLY_BUDGET = "weekly_budget"
        const val KEY_EAT_BACK = "eat_back"
        const val KEY_WATER_TRACKING = "water_tracking"
        const val KEY_FASTING = "fasting"
        const val KEY_FASTING_HOURS = "fasting_hours"
        const val KEY_FASTING_NOTIFY_ONGOING = "fasting_notify_ongoing"
        const val KEY_FASTING_NOTIFY_GOAL = "fasting_notify_goal"
        const val KEY_FASTING_NOTIFY_NEXT = "fasting_notify_next"
        const val KEY_GAMIFICATION = "gamification"
        const val KEY_GAMIFICATION_STREAKS = "gamification_streaks"
        const val KEY_GAMIFICATION_TODAY = "gamification_today"
        const val KEY_GAMIFICATION_CELEBRATIONS = "gamification_celebrations"
        const val KEY_SEEN_BADGES = "seen_badges"
    }
}

/**
 * @property planHours fasting hours per day (16 for 16:8), see [FASTING_PLANS].
 * @property notifyOngoing a running timer in the notification shade while fasting.
 * @property notifyGoal an alert when a fast reaches its goal.
 * @property notifyNextFast an alert when the eating window closes and the next fast is due.
 */
data class FastingSettings(
    val enabled: Boolean = false,
    val planHours: Int = DEFAULT_FASTING_HOURS,
    val notifyOngoing: Boolean = true,
    val notifyGoal: Boolean = true,
    val notifyNextFast: Boolean = false
)

/**
 * Streaks and badges (see Streaks.kt, Achievements.kt). Off by default.
 *
 * @property streaks which streaks are tracked.
 * @property showOnToday the streak chip on Today.
 * @property celebrations a short celebration when a badge is reached.
 */
data class GamificationSettings(
    val enabled: Boolean = false,
    val streaks: Set<StreakKind> = StreakKind.DEFAULT,
    val showOnToday: Boolean = true,
    val celebrations: Boolean = true
)
