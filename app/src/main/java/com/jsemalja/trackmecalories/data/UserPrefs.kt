package com.jsemalja.trackmecalories.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Behaviour preferences (SharedPreferences; not part of the logged data). */
class UserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _weeklyBudget = MutableStateFlow(prefs.getBoolean(KEY_WEEKLY_BUDGET, false))
    /** Let daily targets flex across the week (see [weeklyBudget]). */
    val weeklyBudget: StateFlow<Boolean> = _weeklyBudget.asStateFlow()

    fun setWeeklyBudget(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_BUDGET, enabled).apply()
        _weeklyBudget.value = enabled
    }

    private val _eatBack = MutableStateFlow(
        EatBack.entries.firstOrNull { it.name == prefs.getString(KEY_EAT_BACK, null) } ?: EatBack.OFF
    )
    /** Share of today's active calories added to today's goal. */
    val eatBack: StateFlow<EatBack> = _eatBack.asStateFlow()

    fun setEatBack(value: EatBack) {
        prefs.edit().putString(KEY_EAT_BACK, value.name).apply()
        _eatBack.value = value
    }

    private val _waterTracking = MutableStateFlow(prefs.getBoolean(KEY_WATER_TRACKING, true))
    /** Show the water card on Today. Off only hides it; logged water is kept. */
    val waterTracking: StateFlow<Boolean> = _waterTracking.asStateFlow()

    fun setWaterTracking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WATER_TRACKING, enabled).apply()
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
        prefs.edit()
            .putBoolean(KEY_FASTING, value.enabled)
            .putInt(KEY_FASTING_HOURS, value.planHours)
            .putBoolean(KEY_FASTING_NOTIFY_ONGOING, value.notifyOngoing)
            .putBoolean(KEY_FASTING_NOTIFY_GOAL, value.notifyGoal)
            .putBoolean(KEY_FASTING_NOTIFY_NEXT, value.notifyNextFast)
            .apply()
        _fasting.value = value
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
