package com.jsemalja.trackmecalories.data

import android.content.res.AssetManager

// The parts of the food sources that read the APK's assets; the rest is in :shared.

/** The bundled base-food list, read from assets/base_foods.json when first searched. */
fun BaseFoods(assets: AssetManager): BaseFoods = BaseFoods {
    assets.open("base_foods.json").bufferedReader().use { it.readText() }
}

/** The bundled restaurant menus, read from the JSON files in assets/restaurants when first searched. */
fun RestaurantMenu(assets: AssetManager): RestaurantMenu = RestaurantMenu {
    assets.list("restaurants").orEmpty().filter { it.endsWith(".json") }
        .map { file -> assets.open("restaurants/$file").bufferedReader().use { it.readText() } }
}
