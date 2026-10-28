package com.jsemalja.trackmecalories.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jsemalja.trackmecalories.data.BaseFoods
import com.jsemalja.trackmecalories.data.FastingControls
import com.jsemalja.trackmecalories.data.FoodDao
import com.jsemalja.trackmecalories.data.HealthSource
import com.jsemalja.trackmecalories.data.RestaurantMenu
import com.jsemalja.trackmecalories.data.UserPrefs

class MainViewModelFactory(
    private val dao: FoodDao,
    private val baseFoods: BaseFoods,
    private val restaurantMenu: RestaurantMenu,
    private val userPrefs: UserPrefs,
    private val health: HealthSource,
    private val fasting: FastingControls
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dao, baseFoods, restaurantMenu, userPrefs, health, fasting) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
