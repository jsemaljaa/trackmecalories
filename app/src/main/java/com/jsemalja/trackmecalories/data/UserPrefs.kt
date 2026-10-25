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

    private companion object {
        const val KEY_WEEKLY_BUDGET = "weekly_budget"
    }
}
