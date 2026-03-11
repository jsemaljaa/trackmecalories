package com.jsemalja.trackmecalories

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

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
    val date: String // Store as ISO-8601 string (e.g., "2026-03-09")
)

@Entity(tableName = "custom_foods")
data class CustomFood(
    @PrimaryKey val barcode: String,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val unit: String = "g" // "g", "ml", "pcs"
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Insert
    suspend fun insertEntry(entry: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFood)

    @Query("SELECT * FROM custom_foods WHERE barcode = :barcode")
    suspend fun getCustomFoodByBarcode(barcode: String): CustomFood?
}

@Database(entities = [FoodEntry::class, CustomFood::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}