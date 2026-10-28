package com.jsemalja.trackmecalories.data

/**
 * Small persistent settings (SharedPreferences on Android, NSUserDefaults on iOS). Used for
 * preferences that aren't logged data, so they stay out of the Room database.
 */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getString(key: String): String?
    fun getStringSet(key: String): Set<String>?

    /** Applies the [block]'s changes together. */
    fun edit(block: Editor.() -> Unit)

    interface Editor {
        fun putBoolean(key: String, value: Boolean)
        fun putInt(key: String, value: Int)
        fun putString(key: String, value: String)
        fun putStringSet(key: String, value: Set<String>)
        fun remove(key: String)
    }
}
