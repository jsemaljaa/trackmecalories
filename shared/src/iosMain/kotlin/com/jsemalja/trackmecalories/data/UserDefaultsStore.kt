package com.jsemalja.trackmecalories.data

import platform.Foundation.NSUserDefaults

/** [KeyValueStore] over NSUserDefaults; [prefix] keeps separate stores ("user_prefs", "appearance") apart. */
class UserDefaultsStore(private val prefix: String, private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : KeyValueStore {
    private fun k(key: String) = "$prefix.$key"

    override fun getBoolean(key: String, default: Boolean) =
        if (defaults.objectForKey(k(key)) == null) default else defaults.boolForKey(k(key))

    override fun getInt(key: String, default: Int) =
        if (defaults.objectForKey(k(key)) == null) default else defaults.integerForKey(k(key)).toInt()

    override fun getString(key: String): String? = defaults.stringForKey(k(key))

    override fun getStringSet(key: String): Set<String>? =
        defaults.stringArrayForKey(k(key))?.filterIsInstance<String>()?.toSet()

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, k(key))
            override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), k(key))
            override fun putString(key: String, value: String) = defaults.setObject(value, k(key))
            override fun putStringSet(key: String, value: Set<String>) = defaults.setObject(value.toList(), k(key))
            override fun remove(key: String) = defaults.removeObjectForKey(k(key))
        }.block()
    }
}
