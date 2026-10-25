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

    private companion object {
        const val KEY_WEEKLY_BUDGET = "weekly_budget"
        const val KEY_EAT_BACK = "eat_back"
    }
}
