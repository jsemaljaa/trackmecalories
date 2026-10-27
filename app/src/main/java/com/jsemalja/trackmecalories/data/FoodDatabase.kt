package com.jsemalja.trackmecalories.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.OnConflictStrategy
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Index
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing a single food log entry.
 */
@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val salt: Double,
    val amount: Double = 100.0,
    val unit: String = "g",
    val date: String, // ISO-8601 string (e.g., "2026-03-09")
    val mealType: String = "Snack", // Breakfast, Lunch, Dinner, Snack
    val time: Long = System.currentTimeMillis(),
    // The amount the per-100-style macros below are relative to (100 for g/ml, 1 for "pcs").
    val referenceAmount: Double = 100.0,
    // How sure the amount is: WEIGHED, ESTIMATED or GUESSED (see [Confidence]).
    val confidence: String = Confidence.ESTIMATED.name,
    // How full / energetic the user felt after this meal, 1-5; null = not rated.
    val satiety: Int? = null,
    val energyLevel: Int? = null,
    // The saved food this came from, when known (older entries are matched by name).
    val foodId: Int? = null,
    // Logged as a number of portions ("2 medium eggs"): the portion key (see [Portion]) and
    // its weight at the time. [amount] stays in grams, so totals never depend on these.
    val portion: String? = null,
    val portionGrams: Double? = null
)

/**
 * Entity representing a food template (e.g., from a barcode or manual save).
 * Includes a unique index on barcode to prevent duplicates while populating recent foods.
 */
@Entity(
    tableName = "custom_foods",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String? = null,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val unit: String = "g", // "g", "ml", "pcs"
    // The amount the macros above are relative to: 100 for g/ml, 1 for "pcs".
    val referenceAmount: Double = 100.0,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L,
    // Pre-filled amount when logging: the package size for a scanned product, then
    // whatever was logged last time.
    val defaultAmount: Double? = null,
    // Extra nutrients for the nutrition rating; null when the source doesn't have them.
    val sugarPer100: Double? = null,
    val saturatedFatPer100: Double? = null,
    // Official Nutri-Score grade (a-e) from Open Food Facts, if it has one.
    val nutriScore: String? = null,
    // Natural servings, encoded "small=42;medium=51" (see [parsePortions]); null if none.
    val portions: String? = null,
    // The portion to preselect when logging: the last one used, else the food's usual one.
    val defaultPortion: String? = null,
    // Weight of one piece in g (ml for liquids), so a food counted in pieces can be logged
    // by weight and one weighed by the piece (see [unitPortion]); null if unknown.
    val pieceGrams: Double? = null
)

/**
 * Entity for user-defined nutritional goals.
 */
@Entity(tableName = "user_goals")
data class UserGoal(
    @PrimaryKey val id: Int = 0,
    val calorieGoal: Int = 2000,
    val proteinGoal: Double = 150.0,
    val carbGoal: Double = 200.0,
    val fatGoal: Double = 70.0,
    val fiberGoal: Double = 30.0,
    val saltGoal: Double = 6.0,
    val waterGoalMl: Double = 2000.0,
    // [Diet] name; CUSTOM means the macro goals above are entered by hand.
    val dietType: String = Diet.CUSTOM.name,
    val targetWeightKg: Double? = null,
    // Profile for the calorie calculator (all optional).
    val heightCm: Double? = null,
    val birthYear: Int? = null,
    val sex: String? = null,
    val activityLevel: String? = null
)

/** One weigh-in per day (logging again the same day replaces it). */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val weightKg: Double,
    val time: Long = System.currentTimeMillis()
)

/**
 * A recipe built from foods. It's logged through [customFoodId], a regular saved food whose
 * values are recalculated from the ingredients whenever the recipe is saved.
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val servings: Double = 1.0,
    // Weight after cooking, if weighed; otherwise the raw ingredients' total is used.
    val cookedWeightG: Double? = null,
    // "pcs" = logged per serving, "g" = per 100 g of the finished dish.
    val logBy: String = "pcs",
    val customFoodId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * An ingredient line. Nutrition is a snapshot of the food at the time it was added (per
 * [referenceAmount] of [unit]), so a recipe survives its source food being edited/deleted.
 */
@Entity(
    tableName = "recipe_ingredients",
    indices = [Index(value = ["recipeId"])]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeId: Int,
    val position: Int,
    val name: String,
    val amount: Double,
    val unit: String,
    val referenceAmount: Double,
    // Approximate grams per [unit] (1 for g/ml); lets "pcs" ingredients count toward the
    // dish's weight. 0 when unknown.
    val gramsPerUnit: Double,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    // Cooked weight per raw weight (rice ≈ 2.5, chicken ≈ 0.75); 1 = unchanged. Used to
    // estimate the dish's weight when the pot isn't weighed.
    val cookFactor: Double = 1.0
)

/**
 * A single water intake log (kept as discrete entries, like food, so logging is undoable).
 */
@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Double,
    val date: String,
    val time: Long = System.currentTimeMillis()
)

/**
 * Offline cache of raw Open Food Facts lookups, keyed by barcode, independent of the
 * user-curated [CustomFood] list so a scan that's never confirmed still avoids a repeat
 * network call.
 */
@Entity(
    tableName = "food_cache",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class FoodCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val unit: String = "g",
    val referenceAmount: Double = 100.0,
    val cachedAt: Long = System.currentTimeMillis(),
    val quantity: String? = null,
    val sugarPer100: Double? = null,
    val saturatedFatPer100: Double? = null,
    val nutriScore: String? = null
)

/**
 * Whether a day's log is finished. Days marked incomplete are left out of averages, so a
 * forgotten dinner doesn't read as eating less. Days without a row count as complete.
 */
@Entity(tableName = "day_logs")
data class DayLog(
    @PrimaryKey val date: String,
    val complete: Boolean
)

/**
 * One fast of the fasting timer. [endTime] is null while it's running; at most one fast runs
 * at a time. [targetHours] is the plan when it ran (16 for 16:8), so changing the plan later
 * doesn't rewrite past fasts. Times are epoch milliseconds.
 */
@Entity(tableName = "fasts")
data class Fast(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val targetHours: Int
)

/**
 * Aggregated per-day totals used by the weekly trends screen. Not an entity — just a
 * query result shape.
 */
data class DailyTotal(
    val date: String,
    val totalKcal: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    // Sum of squared per-entry errors (kcal²), from each entry's confidence. Its square
    // root is the day's ± band; see [Confidence].
    val kcalVariance: Double = 0.0
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY time ASC")
    fun getEntriesForDate(date: String): Flow<List<FoodEntry>>

    // One-shot reads for the home-screen widget (no Flow outside the app's UI).
    @Query("SELECT * FROM food_entries WHERE date = :date")
    suspend fun getEntriesForDateOnce(date: String): List<FoodEntry>

    @Query("SELECT * FROM user_goals WHERE id = 0")
    suspend fun getUserGoalOnce(): UserGoal?

    @Query("SELECT * FROM food_entries ORDER BY date ASC, time ASC")
    suspend fun getAllEntriesForExport(): List<FoodEntry>

    @Query(
        """
        SELECT date,
               SUM(kcal) AS totalKcal,
               SUM(protein) AS totalProtein,
               SUM(carbs) AS totalCarbs,
               SUM(fat) AS totalFat,
               -- Relative errors must match Confidence: weighed 5%, estimated 15%, guessed 30%.
               SUM(
                   (kcal * CASE confidence WHEN 'WEIGHED' THEN 0.05 WHEN 'GUESSED' THEN 0.30 ELSE 0.15 END) *
                   (kcal * CASE confidence WHEN 'WEIGHED' THEN 0.05 WHEN 'GUESSED' THEN 0.30 ELSE 0.15 END)
               ) AS kcalVariance
        FROM food_entries
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        """
    )
    fun getDailyTotalsBetween(startDate: String, endDate: String): Flow<List<DailyTotal>>

    @Insert
    suspend fun insertEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: FoodEntry)

    @Update
    suspend fun updateEntry(entry: FoodEntry)

    @Delete
    suspend fun deleteEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFood): Long

    @Update
    suspend fun updateCustomFood(food: CustomFood)

    @Delete
    suspend fun deleteCustomFood(food: CustomFood)

    @Query("SELECT * FROM custom_foods ORDER BY name COLLATE NOCASE ASC")
    fun getAllCustomFoods(): Flow<List<CustomFood>>

    // Log entries don't reference the food they came from, so they're matched by the
    // template's name and unit (the same pair that produced them via toEntry()).
    @Query("SELECT COUNT(*) FROM food_entries WHERE name = :name AND unit = :unit AND referenceAmount = :referenceAmount")
    suspend fun countEntriesForFood(name: String, unit: String, referenceAmount: Double): Int

    @Query(
        """
        UPDATE food_entries SET
            name = :newName,
            kcal = CAST(ROUND(:kcal * amount / referenceAmount) AS INTEGER),
            protein = :protein * amount / referenceAmount,
            carbs = :carbs * amount / referenceAmount,
            fat = :fat * amount / referenceAmount,
            fiber = :fiber * amount / referenceAmount,
            salt = :salt * amount / referenceAmount
        WHERE name = :oldName AND unit = :unit AND referenceAmount = :referenceAmount
        """
    )
    suspend fun rescaleEntriesForFood(
        oldName: String,
        unit: String,
        referenceAmount: Double,
        newName: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        salt: Double
    )

    @Query("UPDATE custom_foods SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun touchLastUsed(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE custom_foods SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM custom_foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getCustomFoodByBarcode(barcode: String): CustomFood?

    // Most recently used, non-favorited foods. Favorites get their own list so they
    // don't scroll off as Recents fills up with noise.
    @Query("SELECT * FROM custom_foods WHERE isFavorite = 0 ORDER BY lastUsedAt DESC, id DESC LIMIT 30")
    fun getRecentFoods(): Flow<List<CustomFood>>

    @Query("SELECT * FROM custom_foods WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFoods(): Flow<List<CustomFood>>

    @Query(
        "SELECT * FROM custom_foods WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY isFavorite DESC, lastUsedAt DESC LIMIT 8"
    )
    fun searchCustomFoods(query: String): Flow<List<CustomFood>>

    @Query("SELECT * FROM user_goals WHERE id = 0")
    fun getUserGoal(): Flow<UserGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUserGoal(goal: UserGoal)

    @Query("DELETE FROM food_entries")
    suspend fun clearAllEntries()

    @Query("DELETE FROM water_entries")
    suspend fun clearAllWaterEntries()

    @Insert
    suspend fun insertWaterEntry(entry: WaterEntry)

    @Delete
    suspend fun deleteWaterEntry(entry: WaterEntry)

    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY time ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntry>>

    // --- Fasting ---

    @Query("SELECT * FROM fasts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun getActiveFast(): Flow<Fast?>

    @Query("SELECT * FROM fasts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveFastOnce(): Fast?

    @Query("SELECT * FROM fasts WHERE endTime IS NOT NULL ORDER BY endTime DESC LIMIT 1")
    fun getLastFast(): Flow<Fast?>

    @Query("SELECT * FROM fasts WHERE endTime IS NOT NULL ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastFastOnce(): Fast?

    @Insert
    suspend fun insertFast(fast: Fast)

    @Update
    suspend fun updateFast(fast: Fast)

    // --- Day completeness ---

    @Query("SELECT * FROM day_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getDayLogsBetween(startDate: String, endDate: String): Flow<List<DayLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDayLog(dayLog: DayLog)

    @Query("DELETE FROM day_logs WHERE date = :date")
    suspend fun clearDayLog(date: String)

    // --- History for suggestions, "what fits" and satiety insights ---

    @Query("SELECT * FROM food_entries WHERE date >= :sinceDate ORDER BY time DESC")
    suspend fun getEntriesSince(sinceDate: String): List<FoodEntry>

    @Query("SELECT * FROM food_entries WHERE date >= :sinceDate ORDER BY time DESC")
    fun observeEntriesSince(sinceDate: String): Flow<List<FoodEntry>>

    @Query("UPDATE custom_foods SET defaultAmount = :amount, defaultPortion = :portion WHERE id = :id")
    suspend fun setDefaultAmount(id: Int, amount: Double, portion: String?)

    @Query("UPDATE custom_foods SET pieceGrams = :grams WHERE id = :id")
    suspend fun setPieceGrams(id: Int, grams: Double)

    @Query("UPDATE food_entries SET satiety = :satiety, energyLevel = :energy WHERE date = :date AND mealType = :mealType")
    suspend fun rateMeal(date: String, mealType: String, satiety: Int?, energy: Int?)

    // --- Weight ---

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    fun getWeightEntries(): Flow<List<WeightEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightEntry(entry: WeightEntry)

    @Delete
    suspend fun deleteWeightEntry(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): WeightEntry?

    // --- Recipes ---

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    fun getRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Int): Recipe?

    @Query("SELECT * FROM recipes WHERE customFoodId = :foodId LIMIT 1")
    suspend fun getRecipeForFood(foodId: Int): Recipe?

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY position ASC")
    suspend fun getIngredients(recipeId: Int): List<RecipeIngredient>

    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeRow(id: Int)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Int)

    @Query("SELECT * FROM custom_foods WHERE id = :id")
    suspend fun getCustomFood(id: Int): CustomFood?

    /**
     * Saves a recipe with its ingredients and its loggable food in one transaction, so a
     * crash can't leave a recipe without ingredients or pointing at a missing food.
     * Returns the recipe id.
     */
    @Transaction
    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>, food: CustomFood): Int {
        val foodId = if (food.id != 0) {
            updateCustomFood(food)
            food.id
        } else {
            insertCustomFood(food).toInt()
        }
        val recipeId = if (recipe.id != 0) {
            updateRecipe(recipe.copy(customFoodId = foodId))
            recipe.id
        } else {
            insertRecipe(recipe.copy(customFoodId = foodId)).toInt()
        }
        deleteIngredients(recipeId)
        insertIngredients(ingredients.mapIndexed { i, ing -> ing.copy(id = 0, recipeId = recipeId, position = i) })
        return recipeId
    }

    /** Deletes a recipe and its ingredients. Its saved food (and anything logged) stays. */
    @Transaction
    suspend fun deleteRecipe(id: Int) {
        deleteIngredients(id)
        deleteRecipeRow(id)
    }

    @Query("SELECT * FROM food_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProduct(barcode: String): FoodCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProduct(cache: FoodCache)
}

@Database(
    entities = [
        FoodEntry::class, CustomFood::class, UserGoal::class, WaterEntry::class, FoodCache::class,
        WeightEntry::class, Recipe::class, RecipeIngredient::class, DayLog::class, Fast::class
    ],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}
