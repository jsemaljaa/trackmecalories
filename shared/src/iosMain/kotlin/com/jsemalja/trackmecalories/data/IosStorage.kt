@file:OptIn(ExperimentalForeignApi::class)

package com.jsemalja.trackmecalories.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile

/**
 * The app's database in Documents. A fresh install starts at the current schema version, so
 * the Android migrations (5→12) aren't needed here; later schema changes will need common
 * Migrations registered on both platforms.
 */
fun createDatabase(): AppDatabase {
    val documents = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
    return Room.databaseBuilder<AppDatabase>(name = "${documents!!.path}/track-me-calories-db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // Only destructive on downgrade, as on Android.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
}

// The Android assets folder is copied into the app bundle as "assets" (see iosApp/project.yml).
private val assetsDir: String get() = "${NSBundle.mainBundle.resourcePath}/assets"

private fun readText(path: String): String =
    NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: error("Missing bundled file $path")

/** The bundled base-food list, from the app bundle. */
fun bundledBaseFoods(): BaseFoods = BaseFoods { readText("$assetsDir/base_foods.json") }

/** The bundled restaurant menus, from the app bundle. */
fun bundledRestaurantMenu(): RestaurantMenu = RestaurantMenu {
    val dir = "$assetsDir/restaurants"
    NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, null).orEmpty()
        .filterIsInstance<String>()
        .filter { it.endsWith(".json") }
        .map { readText("$dir/$it") }
}
