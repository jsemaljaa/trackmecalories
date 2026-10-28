package com.jsemalja.trackmecalories

/**
 * Things a launcher shortcut or a widget button can ask the app to open straight into.
 * On Android each maps to an intent action on MainActivity (see QuickAction.intent there).
 */
enum class QuickAction(val action: String) {
    ADD_FOOD("com.jsemalja.trackmecalories.action.ADD_FOOD"),
    QUICK_ADD("com.jsemalja.trackmecalories.action.QUICK_ADD"),
    SCAN("com.jsemalja.trackmecalories.action.SCAN"),
    LOG_WEIGHT("com.jsemalja.trackmecalories.action.LOG_WEIGHT");

    companion object
}
