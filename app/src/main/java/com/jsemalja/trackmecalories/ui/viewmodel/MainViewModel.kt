@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.jsemalja.trackmecalories.ui.viewmodel

import com.jsemalja.trackmecalories.data.weeklyBudget
import com.jsemalja.trackmecalories.data.DayActivity
import com.jsemalja.trackmecalories.data.EatBack
import com.jsemalja.trackmecalories.data.HealthData
import com.jsemalja.trackmecalories.data.HealthStatus
import com.jsemalja.trackmecalories.data.WeeklyBudget
import com.jsemalja.trackmecalories.data.UserPrefs
import com.jsemalja.trackmecalories.data.suggestionHistoryStart
import com.jsemalja.trackmecalories.data.DayLog
import com.jsemalja.trackmecalories.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.data.FoodDao
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.FoodRepository
import com.jsemalja.trackmecalories.data.KalorickeTabulkyRepository
import com.jsemalja.trackmecalories.data.KtHit
import com.jsemalja.trackmecalories.data.Recipe
import com.jsemalja.trackmecalories.data.RecipeIngredient
import com.jsemalja.trackmecalories.data.RestaurantMenu
import com.jsemalja.trackmecalories.data.AdaptiveMaintenance
import com.jsemalja.trackmecalories.data.Confidence
import com.jsemalja.trackmecalories.data.WeightEntry
import com.jsemalja.trackmecalories.data.WeightPoint
import com.jsemalja.trackmecalories.data.adaptiveMaintenance
import com.jsemalja.trackmecalories.data.weightTrend
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.data.SearchHit
import com.jsemalja.trackmecalories.data.SearchMatcher
import com.jsemalja.trackmecalories.data.SearchSource
import com.jsemalja.trackmecalories.data.normalizedForSearch
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.WaterEntry
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.data.toEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Main ViewModel for the application, handling food logging, goals, and history.
 */
class MainViewModel(
    private val dao: FoodDao,
    private val restaurantMenu: RestaurantMenu,
    private val userPrefs: UserPrefs,
    private val health: HealthData
) : ViewModel() {

    private val repository = FoodRepository(dao)
    private val kaloricke = KalorickeTabulkyRepository()

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

    // Every saved food, alphabetically — the "All" tab, so any food can be found and edited,
    // not just the 30 most recent.
    val allCustomFoods: StateFlow<List<CustomFood>> = dao.getAllCustomFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // First day of the calendar week shown on the trends screen (same week definition as
    // Home's week strip).
    val trendsWeekStart = MutableStateFlow(weekStartOf(LocalDate.now()))

    // Per-day totals for the trends week *and* the week before it, so the screen can
    // compare against last week.
    val trendsTotals: StateFlow<List<DailyTotal>> = trendsWeekStart
        .flatMapLatest { start ->
            dao.getDailyTotalsBetween(start.minusWeeks(1).toString(), start.plusDays(6).toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeTrendsWeek(weeks: Long) {
        trendsWeekStart.value = trendsWeekStart.value.plusWeeks(weeks)
    }

    /** Dates marked incomplete in the trends range (this week and the one before). */
    val trendsIncompleteDays: StateFlow<Set<String>> = trendsWeekStart
        .flatMapLatest { start ->
            dao.getDayLogsBetween(start.minusWeeks(1).toString(), start.plusDays(6).toString())
        }
        .map { logs -> logs.filter { !it.complete }.map { it.date }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Completeness of the selected day: true, false, or null if never marked. */
    val selectedDayComplete: StateFlow<Boolean?> = selectedDate
        .flatMapLatest { d -> dao.getDayLogsBetween(d.toString(), d.toString()) }
        .map { it.firstOrNull()?.complete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Marks a day complete or incomplete (null clears it). Incomplete days are left out of
     * averages and measured maintenance, so a forgotten meal doesn't skew them.
     */
    fun setDayComplete(date: LocalDate, complete: Boolean?) {
        viewModelScope.launch {
            if (complete == null) dao.clearDayLog(date.toString())
            else dao.setDayLog(DayLog(date.toString(), complete))
        }
    }

    /** The last 90 days of entries: the history suggestions and "What fits?" learn from. */
    val historyEntries: StateFlow<List<FoodEntry>> = dao.observeEntriesSince(suggestionHistoryStart().toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Weekly budgeting ---

    val weeklyBudgetEnabled: StateFlow<Boolean> = userPrefs.weeklyBudget

    fun setWeeklyBudget(enabled: Boolean) = userPrefs.setWeeklyBudget(enabled)

    /** This week's budget and today's flexible target; null when weekly budgeting is off. */
    val weeklyBudget: StateFlow<WeeklyBudget?> = run {
        val today = LocalDate.now()
        val start = weekStartOf(today)
        combine(
            userPrefs.weeklyBudget,
            userGoal,
            dao.getDailyTotalsBetween(start.toString(), today.toString()),
            dao.getDayLogsBetween(start.toString(), today.toString())
        ) { enabled, goal, totals, logs ->
            if (!enabled) return@combine null
            weeklyBudget(
                dailyGoal = goal.calorieGoal,
                weekStart = start,
                today = today,
                intakeByDate = totals.associate { LocalDate.parse(it.date) to it.totalKcal },
                incompleteDays = logs.filter { !it.complete }.map { LocalDate.parse(it.date) }.toSet()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Health Connect ---

    private val _healthStatus = MutableStateFlow(health.status())
    val healthStatus: StateFlow<HealthStatus> = _healthStatus
    private val healthTick = MutableStateFlow(0)

    /**
     * Re-checks permissions and re-reads the day's activity. Called on resume (the watch may
     * have synced, or permissions changed in Health Connect) and after a permission request.
     */
    fun refreshHealth() {
        viewModelScope.launch {
            _healthStatus.value = health.currentStatus()
            healthTick.value++
        }
    }

    /** Steps, active calories and workouts for the selected day; read live, never stored. */
    val dayActivity: StateFlow<DayActivity?> = combine(selectedDate, healthTick) { date, _ -> date }
        .mapLatest { date ->
            if (_healthStatus.value != HealthStatus.CONNECTED || date.isAfter(LocalDate.now())) null
            else health.activityFor(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val eatBack: StateFlow<EatBack> = userPrefs.eatBack

    fun setEatBack(value: EatBack) = userPrefs.setEatBack(value)

    // --- Weight ---

    val weightEntries: StateFlow<List<WeightEntry>> = dao.getWeightEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Logs a weigh-in; a second one on the same day replaces the first. */
    fun logWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            dao.upsertWeightEntry(WeightEntry(date = date.toString(), weightKg = weightKg))
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch { dao.deleteWeightEntry(entry) }
    }

    fun restoreWeight(entry: WeightEntry) {
        viewModelScope.launch { dao.upsertWeightEntry(entry) }
    }

    /** Weigh-ins with their smoothed trend. */
    val weightPoints: StateFlow<List<WeightPoint>> = weightEntries
        .map { weightTrend(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Maintenance measured from the last 4 weeks of intake and weight trend. Days marked
     * incomplete are left out, so forgotten meals don't make it look lower.
     */
    val adaptiveMaintenance: StateFlow<AdaptiveMaintenance?> = run {
        val end = LocalDate.now()
        val start = end.minusDays(28)
        combine(
            weightPoints,
            dao.getDailyTotalsBetween(start.toString(), end.toString()),
            dao.getDayLogsBetween(start.toString(), end.toString())
        ) { points, totals, dayLogs ->
            val incomplete = dayLogs.filter { !it.complete }.map { it.date }.toSet()
            val intake = totals.filter { it.date !in incomplete }
                .associate { LocalDate.parse(it.date) to it.totalKcal }
            adaptiveMaintenance(points, intake, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTargetWeight(kg: Double?) {
        viewModelScope.launch { dao.setUserGoal(userGoal.value.copy(targetWeightKg = kg)) }
    }

    // --- Recipes ---

    /** Recipes with the saved food they're logged through (for per-serving kcal etc.). */
    val recipes: StateFlow<List<RecipeListItem>> = combine(dao.getRecipes(), dao.getAllCustomFoods()) { recipes, foods ->
        val byId = foods.associateBy { it.id }
        recipes.map { RecipeListItem(it, it.customFoodId?.let(byId::get)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun loadRecipe(id: Int): Pair<Recipe, List<RecipeIngredient>>? {
        val recipe = dao.getRecipe(id) ?: return null
        return recipe to dao.getIngredients(id)
    }

    /** The recipe a saved food belongs to, if it's a recipe's food. */
    suspend fun recipeIdForFood(foodId: Int): Int? = dao.getRecipeForFood(foodId)?.id

    /**
     * Saves the recipe and (re)computes its loggable food from the ingredients, keeping the
     * food's id, favorite flag and recency on edits. Returns the recipe id.
     */
    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>): Int {
        val existing = recipe.customFoodId?.let { dao.getCustomFood(it) }
        return dao.saveRecipe(recipe, ingredients, recipe.toCustomFood(ingredients, existing))
    }

    fun deleteRecipe(id: Int) {
        viewModelScope.launch { dao.deleteRecipe(id) }
    }

    // Search state. The query lives here (not in the screen) so results and the text field
    // stay in sync when switching tabs.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Live results per online source, already relevance-filtered and scored.
    private val _offHits = MutableStateFlow<List<SearchHit>>(emptyList())
    private val _ktHits = MutableStateFlow<List<SearchHit>>(emptyList())

    private val _isSearchingOff = MutableStateFlow(false)
    private val _isSearchingKt = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = combine(_isSearchingOff, _isSearchingKt) { a, b -> a || b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _offError = MutableStateFlow(false)
    private val _ktError = MutableStateFlow(false)
    val searchError: StateFlow<Int?> = combine(_offError, _ktError) { off, kt ->
        when {
            off && kt -> R.string.search_error_all
            off -> R.string.search_error_off
            kt -> R.string.search_error_kt
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _retryTick = MutableStateFlow(0)

    // The user's own saved foods matching the query — instant, offline, accent-insensitive
    // and prefix-based ("pacin" finds "Pačínko"), best match first.
    val savedFoodMatches: StateFlow<List<CustomFood>> = combine(
        _searchQuery.map { it.trim() }.distinctUntilChanged(),
        dao.getAllCustomFoods()
    ) { q, foods ->
        val tokens = SearchMatcher.tokens(q)
        if (q.length < 2 || tokens.isEmpty()) return@combine emptyList()
        foods.mapNotNull { food -> SearchMatcher.score(tokens, food.name)?.let { food to it } }
            .sortedWith(
                compareByDescending<Pair<CustomFood, Double>> { it.second + if (it.first.isFavorite) 0.3 else 0.0 }
                    .thenByDescending { it.first.lastUsedAt }
            )
            .take(8)
            .map { it.first }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bundled restaurant menus — instant and offline.
    private val menuHits: Flow<List<SearchHit>> = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .map { q -> if (q.length < 2) emptyList() else restaurantMenu.search(q) }
        .flowOn(Dispatchers.Default)

    /**
     * Every source merged into one list, best match first. Bundled menu items win over the
     * same item from live Kalorické tabulky (they already have macros and serving sizes).
     */
    val searchResults: StateFlow<List<SearchHit>> = combine(menuHits, _ktHits, _offHits) { menu, kt, off ->
        (menu + kt + off)
            .distinctBy { it.key }
            .sortedByDescending { it.score }
            // Open Food Facts often has one product under several barcodes, and KT/OFF
            // overlap; keep only the best-ranked copy of "same words, same calories".
            .distinctBy { it.duplicateKey() }
            .take(MAX_RESULTS)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        launchOnlineSearch(_offHits, _isSearchingOff, _offError) { q, publish ->
            val tokens = SearchMatcher.tokens(q)
            publish(
                repository.searchProducts(q).withIndex().mapNotNull { (i, info) ->
                    val score = SearchMatcher.score(tokens, info.name, info.brand) ?: return@mapNotNull null
                    info.toSearchHit(score + (if (info.isCzech) CZECH_BOOST else 0.0) - 0.005 * i)
                }
            )
        }
        launchOnlineSearch(_ktHits, _isSearchingKt, _ktError) { q, publish ->
            val tokens = SearchMatcher.tokens(q)
            val hits = kaloricke.search(q).withIndex().mapNotNull { (i, hit) ->
                val score = SearchMatcher.score(tokens, hit.title, hit.brand) ?: return@mapNotNull null
                // KT orders by popularity; keep that as a tie-breaker. It is a Czech database.
                hit.toSearchHit(score + CZECH_BOOST - 0.01 * i)
            }.sortedByDescending { it.score }.take(30)
            publish(hits)

            // Fill in P/C/F for the top results so they can be compared without tapping.
            val withMacros = coroutineScope {
                hits.mapIndexed { i, h ->
                    async {
                        if (i >= KT_MACRO_PREFETCH) return@async h
                        try {
                            val m = kaloricke.macros(h.ktHit!!)
                            h.copy(protein = m.protein, carbs = m.carbs, fat = m.fat)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            h
                        }
                    }
                }.awaitAll()
            }
            publish(withMacros)
        }
    }

    /**
     * Runs one online source against the debounced query, publishing its results, loading
     * and error state independently so a slow or failing source doesn't hold up the others.
     * A new keystroke cancels the previous search; [publish] refuses to post stale results.
     */
    private fun launchOnlineSearch(
        results: MutableStateFlow<List<SearchHit>>,
        loading: MutableStateFlow<Boolean>,
        failed: MutableStateFlow<Boolean>,
        search: suspend (query: String, publish: suspend (List<SearchHit>) -> Unit) -> Unit
    ) {
        viewModelScope.launch {
            combine(_searchQuery.debounce(SEARCH_DEBOUNCE_MS), _retryTick) { q, tick -> q.trim() to tick }
                .distinctUntilChanged()
                .collectLatest { (q, _) ->
                    failed.value = false
                    if (q.length < 2) {
                        results.value = emptyList()
                        loading.value = false
                        return@collectLatest
                    }
                    loading.value = true
                    try {
                        search(q) { hits ->
                            currentCoroutineContext().ensureActive()
                            results.value = hits
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        results.value = emptyList()
                        failed.value = true
                    } finally {
                        loading.value = false
                    }
                }
        }
    }

    private fun SearchHit.duplicateKey(): String {
        val words = "$name ${brand.orEmpty()}".normalizedForSearch().split(' ').toSortedSet()
        return "${words.joinToString(" ")}|$unit|${(kcal / 10).roundToInt()}"
    }

    private fun FoodInfo.toSearchHit(score: Double) = SearchHit(
        key = "off:${barcode ?: "$name|$brand"}",
        name = name,
        brand = brand,
        detail = quantity,
        kcal = kcalPer100,
        protein = proteinPer100,
        carbs = carbsPer100,
        fat = fatPer100,
        unit = unit,
        source = SearchSource.OPEN_FOOD_FACTS,
        score = score,
        food = toCustomFood(),
        isCzech = isCzech
    )

    private fun KtHit.toSearchHit(score: Double) = SearchHit(
        key = "kt:$id",
        name = title,
        brand = brand?.takeUnless { title.contains(it, ignoreCase = true) },
        detail = null,
        kcal = kcalPer100,
        protein = null,
        carbs = null,
        fat = null,
        unit = unit,
        source = SearchSource.KALORICKE,
        score = score,
        ktHit = this,
        isCzech = true
    )

    /**
     * Loads full macros and the serving size for a kaloricketabulky hit. Returns null when
     * the lookup fails (offline, or the site changed).
     */
    suspend fun resolveKtFood(hit: KtHit): CustomFood? = try {
        kaloricke.resolve(hit).toCustomFood()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
    fun logCustomFood(
        food: CustomFood,
        amount: Double,
        mealType: String,
        date: String = selectedDate.value.toString(),
        confidence: Confidence = Confidence.ESTIMATED
    ) {
        viewModelScope.launch {
            // A search result may already be saved (same barcode / restaurant item id). Bump
            // that one instead of REPLACE-inserting, which would drop its favorite flag and edits.
            val saved = if (food.id != 0) food else food.barcode?.let { dao.getCustomFoodByBarcode(it) }
            val foodId = if (saved != null) {
                dao.touchLastUsed(saved.id)
                saved.id
            } else {
                dao.insertCustomFood(food.copy(lastUsedAt = System.currentTimeMillis())).toInt()
            }
            // Linked to its saved food, so suggestions and insights can find it again.
            dao.insertEntry(food.copy(id = foodId).toEntry(amount, mealType, date, confidence))
            // Next time, start from the portion actually eaten.
            dao.setDefaultAmount(foodId, amount)
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
     * Puts back an entry removed by [deleteEntry] (the "Undo" on Home), keeping its id,
     * date and time so it lands exactly where it was.
     */
    fun restoreEntry(entry: FoodEntry) {
        viewModelScope.launch {
            // Upsert: restoring must never fail, even if the delete hadn't landed yet.
            dao.upsertEntry(entry)
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
                    mealType = "Quick Add",
                    // A bare number is the least certain kind of entry.
                    confidence = Confidence.GUESSED.name
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
     * With [updateLoggedEntries], entries already logged from [original] are recalculated
     * with the new values too (only possible while the unit stays the same, since the
     * logged amounts are in that unit).
     */
    fun updateCustomFood(original: CustomFood, updated: CustomFood, updateLoggedEntries: Boolean = false) {
        viewModelScope.launch {
            dao.updateCustomFood(updated)
            if (updateLoggedEntries && canRescaleEntries(original, updated)) {
                dao.rescaleEntriesForFood(
                    oldName = original.name,
                    unit = original.unit,
                    referenceAmount = original.referenceAmount,
                    newName = updated.name,
                    kcal = updated.kcalPer100,
                    protein = updated.proteinPer100,
                    carbs = updated.carbsPer100,
                    fat = updated.fatPer100,
                    fiber = updated.fiberPer100,
                    salt = updated.saltPer100
                )
            }
        }
    }

    fun canRescaleEntries(original: CustomFood, updated: CustomFood): Boolean =
        original.unit == updated.unit && original.referenceAmount == updated.referenceAmount

    /** How many log entries were made from [food] (matched by name and unit). */
    suspend fun countLoggedEntries(food: CustomFood): Int =
        dao.countEntriesForFood(food.name, food.unit, food.referenceAmount)

    /**
     * Removes a saved food. Entries already logged from it stay in the log.
     */
    fun deleteCustomFood(food: CustomFood) {
        viewModelScope.launch {
            dao.deleteCustomFood(food)
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

/** A recipe and the saved food it's logged through (null if that food was deleted). */
data class RecipeListItem(val recipe: Recipe, val food: CustomFood?)

private const val SEARCH_DEBOUNCE_MS = 350L
private const val MAX_RESULTS = 150 // a chain shortcut lists its whole menu
private const val KT_MACRO_PREFETCH = 8
// Czech products (Kalorické tabulky, OFF items sold in CZ) outrank equally relevant
// foreign ones; a clearly better name match still wins.
private const val CZECH_BOOST = 0.7

/** First day of [date]'s week, by the device locale (Monday in Czechia). */
fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))

class MainViewModelFactory(
    private val dao: FoodDao,
    private val restaurantMenu: RestaurantMenu,
    private val userPrefs: UserPrefs,
    private val health: HealthData
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dao, restaurantMenu, userPrefs, health) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
