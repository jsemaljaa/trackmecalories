package com.jsemalja.trackmecalories.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodDao
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.UserGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Main ViewModel for the application, handling food logging, goals, and history.
 *
 * @property dao The Data Access Object for database operations.
 */
class MainViewModel(private val dao: FoodDao) : ViewModel() {
    
    // The currently selected date for viewing/logging
    val selectedDate = MutableStateFlow(LocalDate.now())

    // List of food entries for the selected date
    val foodEntries: StateFlow<List<FoodEntry>> = selectedDate
        .flatMapLatest { date -> dao.getEntriesForDate(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User nutritional goals
    val userGoal: StateFlow<UserGoal> = dao.getUserGoal()
        .map { it ?: UserGoal() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserGoal())

    // Frequently used foods (all custom foods for now)
    val recentFoods: StateFlow<List<CustomFood>> = dao.getAllCustomFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Adds a new food entry to the database.
     */
    fun addEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.insertEntry(entry)
        }
    }

    /**
     * Updates an existing food entry.
     */
    fun updateEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.updateEntry(entry)
        }
    }

    /**
     * Deletes a food entry.
     */
    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    /**
     * Quickly adds a calorie-only entry for the current day.
     */
    fun addQuickCalories(kcal: Int) {
        viewModelScope.launch {
            dao.insertEntry(
                FoodEntry(
                    name = "Quick Add",
                    kcal = kcal,
                    protein = 0.0,
                    carbs = 0.0,
                    fat = 0.0,
                    fiber = 0.0,
                    salt = 0.0,
                    date = selectedDate.value.toString(),
                    mealType = "Quick Add"
                )
            )
        }
    }

    /**
     * Saves a food template for future quick access.
     */
    fun saveCustomFood(food: CustomFood) {
        viewModelScope.launch {
            dao.insertCustomFood(food)
        }
    }

    /**
     * Retrieves a custom food by its barcode.
     */
    suspend fun getCustomFood(barcode: String): CustomFood? {
        return dao.getCustomFoodByBarcode(barcode)
    }

    /**
     * Updates the user's nutritional goals.
     */
    fun updateGoals(goal: UserGoal) {
        viewModelScope.launch {
            dao.setUserGoal(goal)
        }
    }

    /**
     * Changes the current view date.
     */
    fun changeDate(date: LocalDate) {
        selectedDate.value = date
    }

    /**
     * Copies an entry to a target date.
     */
    fun copyEntryToDate(entry: FoodEntry, targetDate: LocalDate) {
        viewModelScope.launch {
            dao.insertEntry(entry.copy(id = 0, date = targetDate.toString(), time = System.currentTimeMillis()))
        }
    }

    /**
     * Clears all food entries from the database.
     */
    fun clearData() {
        viewModelScope.launch {
            dao.clearAllEntries()
        }
    }

    /**
     * Calculation logic: Converts "per 100g" values to actual consumed amount.
     * Total = (ValuePer100 / 100) * ConsumedAmount
     */
    fun calculateValue(per100: Double, amount: Double): Double {
        return (per100 / 100.0) * amount
    }

    fun calculateKcal(per100: Double, amount: Double): Int {
        return ((per100 / 100.0) * amount).toInt()
    }
}

class MainViewModelFactory(private val dao: FoodDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
