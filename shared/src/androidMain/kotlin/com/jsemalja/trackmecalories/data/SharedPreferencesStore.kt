package com.jsemalja.trackmecalories.data

import android.content.Context
import android.content.SharedPreferences

/** [KeyValueStore] over a SharedPreferences file, e.g. "user_prefs". */
class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    constructor(context: Context, name: String) : this(context.getSharedPreferences(name, Context.MODE_PRIVATE))

    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun getString(key: String): String? = prefs.getString(key, null)
    // getStringSet's set mustn't be changed, so it's copied.
    override fun getStringSet(key: String): Set<String>? = prefs.getStringSet(key, null)?.toSet()

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        object : KeyValueStore.Editor {
            override fun putBoolean(key: String, value: Boolean) { editor.putBoolean(key, value) }
            override fun putInt(key: String, value: Int) { editor.putInt(key, value) }
            override fun putString(key: String, value: String) { editor.putString(key, value) }
            override fun putStringSet(key: String, value: Set<String>) { editor.putStringSet(key, value.toMutableSet()) }
            override fun remove(key: String) { editor.remove(key) }
        }.block()
        editor.apply()
    }
}

/** The app's behaviour preferences, in the "user_prefs" SharedPreferences file. */
fun UserPrefs(context: Context): UserPrefs = UserPrefs(SharedPreferencesStore(context, "user_prefs"))
