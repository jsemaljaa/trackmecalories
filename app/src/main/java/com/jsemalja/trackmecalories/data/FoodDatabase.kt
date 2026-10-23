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
    val time: Long = System.currentTimeMillis()
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
    val unit: String = "g" // "g", "ml", "pcs"
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
    val saltGoal: Double = 6.0
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY time ASC")
    fun getEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Insert
    suspend fun insertEntry(entry: FoodEntry)

    @Update
    suspend fun updateEntry(entry: FoodEntry)

    @Delete
    suspend fun deleteEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFood): Long

    @Query("SELECT * FROM custom_foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getCustomFoodByBarcode(barcode: String): CustomFood?

    @Query("SELECT * FROM custom_foods ORDER BY id DESC")
    fun getAllCustomFoods(): Flow<List<CustomFood>>

    @Query("SELECT * FROM user_goals WHERE id = 0")
    fun getUserGoal(): Flow<UserGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUserGoal(goal: UserGoal)

    @Query("DELETE FROM food_entries")
    suspend fun clearAllEntries()
}

@Database(entities = [FoodEntry::class, CustomFood::class, UserGoal::class], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}
