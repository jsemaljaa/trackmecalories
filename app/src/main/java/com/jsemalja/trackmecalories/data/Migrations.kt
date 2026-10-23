package com.jsemalja.trackmecalories.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6: adds unit-aware reference amounts, favorites/recency on custom foods,
 * water tracking, and an offline barcode-lookup cache.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_entries ADD COLUMN referenceAmount REAL NOT NULL DEFAULT 100.0")

        db.execSQL("ALTER TABLE custom_foods ADD COLUMN referenceAmount REAL NOT NULL DEFAULT 100.0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE user_goals ADD COLUMN waterGoalMl REAL NOT NULL DEFAULT 2000.0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS water_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amountMl REAL NOT NULL,
                date TEXT NOT NULL,
                time INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                barcode TEXT NOT NULL,
                name TEXT NOT NULL,
                kcalPer100 REAL NOT NULL,
                proteinPer100 REAL NOT NULL,
                carbsPer100 REAL NOT NULL,
                fatPer100 REAL NOT NULL,
                fiberPer100 REAL NOT NULL,
                saltPer100 REAL NOT NULL,
                unit TEXT NOT NULL DEFAULT 'g',
                referenceAmount REAL NOT NULL DEFAULT 100.0,
                cachedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_food_cache_barcode ON food_cache(barcode)")
    }
}

/**
 * v6 -> v7: no schema change. Products cached before this version were parsed with the
 * wrong protein key ("protein_100g" instead of OFF's "proteins_100g") and so all have
 * protein = 0; drop them so they're re-fetched correctly.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM food_cache")
    }
}
