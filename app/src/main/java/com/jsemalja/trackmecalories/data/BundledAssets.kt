package com.jsemalja.trackmecalories.data

import android.content.res.AssetManager
import android.util.Log
import java.util.Locale

// The parts of the food sources that read the APK's assets; the rest is in :shared.

/** The bundled base-food list, parsed on first use (callers run on Dispatchers.Default). */
class BaseFoods(private val assets: AssetManager) {
    private val foods: List<BaseFood> by lazy {
        try {
            parseBaseFoods(assets.open("base_foods.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            Log.e("BaseFoods", "Couldn't load base foods", e)
            emptyList()
        }
    }

    // Names follow the app's language; Czech isn't a UI language, so those users get English.
    private fun language() = Locale.getDefault().language.takeIf { it == "ru" } ?: "en"

    fun search(query: String): List<SearchHit> = searchBaseFoods(foods, query, language())

    fun czechQuery(query: String): String? = czechQueryFor(foods, query)

    /** The bundled food behind a saved "base:<key>" food, if it's still in the list. */
    fun byBarcode(barcode: String): BaseFood? =
        barcode.removePrefix("base:").takeIf { it != barcode }?.let { key -> foods.firstOrNull { it.key == key } }
}

/** The bundled restaurant menus, read from the JSON files in assets/restaurants when first searched. */
fun RestaurantMenu(assets: AssetManager): RestaurantMenu = RestaurantMenu {
    assets.list("restaurants").orEmpty().filter { it.endsWith(".json") }
        .map { file -> assets.open("restaurants/$file").bufferedReader().use { it.readText() } }
}
