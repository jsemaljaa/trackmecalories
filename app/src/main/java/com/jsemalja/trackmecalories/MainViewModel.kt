package com.jsemalja.trackmecalories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val dao: FoodDao) : ViewModel() {
    val selectedDate = MutableStateFlow(LocalDate.now())

    val foodEntries: StateFlow<List<FoodEntry>> = selectedDate
        .flatMapLatest { date -> dao.getEntriesForDate(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.insertEntry(entry)
        }
    }

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
                    date = selectedDate.value.toString()
                )
            )
        }
    }

    fun saveCustomFood(food: CustomFood) {
        viewModelScope.launch {
            dao.insertCustomFood(food)
        }
    }

    suspend fun getCustomFood(barcode: String): CustomFood? {
        return dao.getCustomFoodByBarcode(barcode)
    }

    fun changeDate(date: LocalDate) {
        selectedDate.value = date
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