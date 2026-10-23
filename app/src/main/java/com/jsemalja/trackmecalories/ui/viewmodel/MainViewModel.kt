@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.jsemalja.trackmecalories.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.data.FoodDao
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.FoodRepository
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.WaterEntry
import com.jsemalja.trackmecalories.data.toEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Main ViewModel for the application, handling food logging, goals, and history.
 */
class MainViewModel(private val dao: FoodDao) : ViewModel() {

    private val repository = FoodRepository(dao)

    // The currently selected date for viewing/logging
    val selectedDate = MutableStateFlow(LocalDate.now())

    // List of food entries for the selected date
    val foodEntries: StateFlow<List<FoodEntry>> = selectedDate
        .flatMapLatest { date -> dao.getEntriesForDate(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Water logged for the selected date
    val waterEntries: StateFlow<List<WaterEntry>> = selectedDate
        .flatMapLatest { date -> dao.getWaterEntriesForDate(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User nutritional goals
    val userGoal: StateFlow<UserGoal> = dao.getUserGoal()
        .map { it ?: UserGoal() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserGoal())

    // Whether goals have ever been saved. Default true so the onboarding dialog doesn't
    // flash on screen before the first DB emission confirms there's really nothing saved.
    val hasSetGoals: StateFlow<Boolean> = dao.getUserGoal()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Frequently used foods (excludes favorites, which get their own list)
    val recentFoods: StateFlow<List<CustomFood>> = dao.getRecentFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFoods: StateFlow<List<CustomFood>> = dao.getFavoriteFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Per-day totals for the trailing 7 days (including today), used by the trends screen.
    val weeklyTotals: StateFlow<List<DailyTotal>> = run {
        val end = LocalDate.now()
        val start = end.minusDays(6)
        dao.getDailyTotalsBetween(start.toString(), end.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search state. The query lives here (not in the screen) so results and the text field
    // stay in sync when switching tabs.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<FoodInfo>>(emptyList())
    val searchResults: StateFlow<List<FoodInfo>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    private val _retryTick = MutableStateFlow(0)

    // The user's own saved foods matching the query — instant and offline.
    val savedFoodMatches: StateFlow<List<CustomFood>> = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q -> if (q.length < 2) flowOf(emptyList()) else dao.searchCustomFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        combine(_searchQuery.debounce(400), _retryTick) { q, tick -> q.trim() to tick }
            .distinctUntilChanged()
            .onEach { (q, _) ->
                _searchError.value = null
                if (q.length < 2) {
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                } else {
                    _isSearching.value = true
                }
            }
            .mapLatest { (q, _) ->
                if (q.length < 2) return@mapLatest null
                try {
                    Result.success(repository.searchProducts(q))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .onEach { result ->
                result ?: return@onEach
                result
                    .onSuccess { _searchResults.value = it }
                    .onFailure {
                        _searchResults.value = emptyList()
                        _searchError.value = "Couldn't reach Open Food Facts. Check your connection and try again."
                    }
                _isSearching.value = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Updates the search query. The network search is debounced.
     */
    fun searchFoods(query: String) {
        _searchQuery.value = query
    }

    fun retrySearch() {
        _retryTick.value++
    }

    /**
     * Looks up a scanned barcode, via the repository's offline cache when possible.
     */
    suspend fun lookupBarcode(barcode: String): FoodInfo? = repository.getProductInfo(barcode)

    /**
     * Adds a new food entry to the database.
     */
    fun addEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.insertEntry(entry)
        }
    }

    /**
     * Logs [amount] of [food] as a new entry for [date]. If [food] hasn't been saved yet
     * (id == 0, e.g. a fresh search/scan result), it's saved so it shows up in Recents;
     * otherwise its recency is just bumped. This is the single path every add-flow
     * (scan, search, recents, favorites, manual) should go through.
     */
    fun logCustomFood(food: CustomFood, amount: Double, mealType: String, date: String = selectedDate.value.toString()) {
        viewModelScope.launch {
            dao.insertEntry(food.toEntry(amount, mealType, date))
            if (food.id != 0) {
                dao.touchLastUsed(food.id)
            } else {
                dao.insertCustomFood(food.copy(lastUsedAt = System.currentTimeMillis()))
            }
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
     * Applies edits to an already-saved food template (e.g. fixing a typo'd macro value).
     */
    fun updateCustomFood(food: CustomFood) {
        viewModelScope.launch {
            dao.updateCustomFood(food)
        }
    }

    /**
     * Toggles whether a saved food is pinned to the Favorites list.
     */
    fun toggleFavorite(food: CustomFood) {
        viewModelScope.launch {
            dao.setFavorite(food.id, !food.isFavorite)
        }
    }

    /**
     * Retrieves a custom food by its barcode.
     */
    suspend fun getCustomFood(barcode: String): CustomFood? {
        return dao.getCustomFoodByBarcode(barcode)
    }

    /**
     * Logs water intake for the current day.
     */
    fun addWater(amountMl: Double) {
        viewModelScope.launch {
            dao.insertWaterEntry(WaterEntry(amountMl = amountMl, date = selectedDate.value.toString()))
        }
    }

    fun deleteWaterEntry(entry: WaterEntry) {
        viewModelScope.launch {
            dao.deleteWaterEntry(entry)
        }
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
     * Builds a CSV export of every logged food entry, oldest first.
     */
    suspend fun exportCsv(): String {
        val entries = dao.getAllEntriesForExport()
        val header = "Date,Time,MealType,Name,Amount,Unit,Kcal,Protein,Carbs,Fat,Fiber,Salt"
        val rows = entries.joinToString("\n") { e ->
            val time = java.time.Instant.ofEpochMilli(e.time)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
            listOf(
                e.date, time.toString(), e.mealType, e.name.csvEscape(),
                e.amount, e.unit, e.kcal, e.protein, e.carbs, e.fat, e.fiber, e.salt
            ).joinToString(",")
        }
        return if (rows.isEmpty()) header else "$header\n$rows"
    }

    private fun String.csvEscape(): String = "\"${replace("\"", "\"\"")}\""

    /**
     * Clears all food and water entries from the database. Goals and saved foods are kept.
     */
    fun clearData() {
        viewModelScope.launch {
            dao.clearAllEntries()
            dao.clearAllWaterEntries()
        }
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
