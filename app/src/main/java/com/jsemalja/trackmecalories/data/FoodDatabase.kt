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
    val referenceAmount: Double = 100.0
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
    val lastUsedAt: Long = 0L
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
    val waterGoalMl: Double = 2000.0
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
    val cachedAt: Long = System.currentTimeMillis()
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
    val totalFat: Double
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY time ASC")
    fun getEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, time ASC")
    suspend fun getAllEntriesForExport(): List<FoodEntry>

    @Query(
        """
        SELECT date,
               SUM(kcal) AS totalKcal,
               SUM(protein) AS totalProtein,
               SUM(carbs) AS totalCarbs,
               SUM(fat) AS totalFat
        FROM food_entries
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        """
    )
    fun getDailyTotalsBetween(startDate: String, endDate: String): Flow<List<DailyTotal>>

    @Insert
    suspend fun insertEntry(entry: FoodEntry)

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

    @Query("SELECT * FROM food_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProduct(barcode: String): FoodCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProduct(cache: FoodCache)
}

@Database(
    entities = [FoodEntry::class, CustomFood::class, UserGoal::class, WaterEntry::class, FoodCache::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}
