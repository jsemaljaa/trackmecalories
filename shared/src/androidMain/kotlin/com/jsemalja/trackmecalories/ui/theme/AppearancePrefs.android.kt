package com.jsemalja.trackmecalories.ui.theme

import android.content.Context
import com.jsemalja.trackmecalories.data.SharedPreferencesStore

/** The look of the app, in the "appearance" SharedPreferences file. */
fun AppearancePrefs(context: Context): AppearancePrefs = AppearancePrefs(SharedPreferencesStore(context, "appearance"))
