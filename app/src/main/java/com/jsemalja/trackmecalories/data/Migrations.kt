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

/**
 * v7 -> v8: weight tracking, recipes, diet/profile fields on the goals, day completeness,
 * per-entry confidence and meal ratings, remembered portions, and extra nutrients for the
 * nutrition rating. Additive only — every existing row is kept. Existing goals get the CUSTOM
 * diet so the macro numbers already set stay exactly as they are; existing entries are
 * marked ESTIMATED. The barcode cache is cleared so products are re-fetched with the new
 * fields (it's only a cache; saved foods are untouched).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_goals ADD COLUMN dietType TEXT NOT NULL DEFAULT 'CUSTOM'")
        db.execSQL("ALTER TABLE user_goals ADD COLUMN targetWeightKg REAL")
        db.execSQL("ALTER TABLE user_goals ADD COLUMN heightCm REAL")
        db.execSQL("ALTER TABLE user_goals ADD COLUMN birthYear INTEGER")
        db.execSQL("ALTER TABLE user_goals ADD COLUMN sex TEXT")
        db.execSQL("ALTER TABLE user_goals ADD COLUMN activityLevel TEXT")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `weightKg` REAL NOT NULL, `time` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_entries_date` ON `weight_entries` (`date`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `servings` REAL NOT NULL, `cookedWeightG` REAL, `logBy` TEXT NOT NULL, " +
                "`customFoodId` INTEGER, `createdAt` INTEGER NOT NULL)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recipeId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, `unit` TEXT NOT NULL, `referenceAmount` REAL NOT NULL, " +
                "`gramsPerUnit` REAL NOT NULL, `kcalPer100` REAL NOT NULL, `proteinPer100` REAL NOT NULL, " +
                "`carbsPer100` REAL NOT NULL, `fatPer100` REAL NOT NULL, `fiberPer100` REAL NOT NULL, " +
                "`saltPer100` REAL NOT NULL, `cookFactor` REAL NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `day_logs` (`date` TEXT NOT NULL, `complete` INTEGER NOT NULL, PRIMARY KEY(`date`))")

        db.execSQL("ALTER TABLE food_entries ADD COLUMN confidence TEXT NOT NULL DEFAULT 'ESTIMATED'")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN satiety INTEGER")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN energyLevel INTEGER")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN foodId INTEGER")

        db.execSQL("ALTER TABLE custom_foods ADD COLUMN defaultAmount REAL")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN sugarPer100 REAL")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN saturatedFatPer100 REAL")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN nutriScore TEXT")

        db.execSQL("ALTER TABLE food_cache ADD COLUMN quantity TEXT")
        db.execSQL("ALTER TABLE food_cache ADD COLUMN sugarPer100 REAL")
        db.execSQL("ALTER TABLE food_cache ADD COLUMN saturatedFatPer100 REAL")
        db.execSQL("ALTER TABLE food_cache ADD COLUMN nutriScore TEXT")
        db.execSQL("DELETE FROM food_cache")
    }
}

/**
 * v8 -> v9: natural portions ("1 medium egg"). Saved foods get their list of portions and the
 * last-used one; entries remember which portion they were logged as. Additive only; existing
 * rows keep null, which means "logged in grams/ml/pieces" exactly as before.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN portions TEXT")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN defaultPortion TEXT")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN portion TEXT")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN portionGrams REAL")
    }
}

/**
 * v9 -> v10: the weight of one piece, so any food can be logged in pieces, grams or spoons.
 * Additive only; existing foods keep null (unknown) until a menu, recipe or the log sheet fills it.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN pieceGrams REAL")
    }
}
