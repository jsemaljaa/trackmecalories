package com.jsemalja.trackmecalories

import android.content.Context
import android.content.Intent

/**
 * Things a launcher shortcut or a widget button can ask the app to open straight into.
 * Each maps to an intent action on [MainActivity].
 */
enum class QuickAction(val action: String) {
    ADD_FOOD("com.jsemalja.trackmecalories.action.ADD_FOOD"),
    QUICK_ADD("com.jsemalja.trackmecalories.action.QUICK_ADD"),
    SCAN("com.jsemalja.trackmecalories.action.SCAN"),
    LOG_WEIGHT("com.jsemalja.trackmecalories.action.LOG_WEIGHT");

    fun intent(context: Context): Intent = Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    companion object {
        fun from(intent: Intent?): QuickAction? = entries.firstOrNull { it.action == intent?.action }
    }
}
