package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.satietyInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatietyInsightsTest {
    private fun entry(
        name: String,
        day: Int,
        kcal: Int,
        meal: String = "Lunch",
        satiety: Int? = null,
        energy: Int? = null,
        foodId: Int? = null,
        time: Long = day * 1000L
    ) = FoodEntry(
        name = name, kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0, fiber = 0.0, salt = 0.0,
        date = "2026-10-%02d".format(day), mealType = meal, time = time,
        satiety = satiety, energyLevel = energy, foodId = foodId
    )

    /** One rated meal per day with [names] in it. */
    private fun meals(names: List<String>, fullness: List<Int>, kcal: Int = 500, energy: Int? = null) =
        fullness.flatMapIndexed { i, f -> names.map { entry(it, i + 1, kcal / names.size, satiety = f, energy = energy) } }

    @Test
    fun `ranks by average fullness, most filling first`() {
        val entries = meals(listOf("Oats"), listOf(5, 4, 4)) + meals(listOf("Croissant"), listOf(2, 1, 2)).map { it.copy(mealType = "Breakfast") }
        val r = satietyInsights(entries)
        assertEquals(listOf("Oats", "Croissant"), r.foods.map { it.name })
        assertEquals(13.0 / 3, r.foods[0].avgFullness, 1e-9)
        assertEquals(3, r.foods[0].meals)
        assertEquals(6, r.ratedMeals)
    }

    @Test
    fun `needs the minimum number of rated meals`() {
        val r = satietyInsights(meals(listOf("Oats"), listOf(5, 4)))
        assertTrue(r.foods.isEmpty())
        assertEquals(2, r.ratedMeals)
    }

    @Test
    fun `unrated meals and quick add are ignored`() {
        val entries = meals(listOf("Oats"), listOf(4, 4, 4)) +
            entry("Oats", 10, 300) + // never rated
            (11..13).map { entry("Quick Add", it, 400, meal = "Quick Add", satiety = 5) }
        val r = satietyInsights(entries)
        assertEquals(listOf("Oats"), r.foods.map { it.name })
        assertEquals(3, r.foods[0].meals)
        assertEquals(3, r.ratedMeals)
    }

    @Test
    fun `an entry added after rating still counts with the meal`() {
        val entries = (1..3).flatMap { d -> listOf(entry("Rice", d, 300, satiety = 4), entry("Chicken", d, 250)) }
        val r = satietyInsights(entries)
        assertEquals(setOf("Rice", "Chicken"), r.foods.map { it.name }.toSet())
        assertEquals(550.0, r.foods.first().avgMealKcal, 1e-9)
    }

    @Test
    fun `matches by food id, then saved name, and uses the newest name`() {
        val entries = listOf(
            entry("Skyr natur", 1, 150, satiety = 4, foodId = 7),
            entry("Skyr", 2, 150, satiety = 4), // older entry, no foodId: matched via the saved name
            entry("Skyr Natural", 3, 150, satiety = 5, foodId = 7, time = 99_000)
        )
        val r = satietyInsights(entries, foodIdByName = mapOf("Skyr" to 7))
        assertEquals(1, r.foods.size)
        assertEquals("Skyr Natural", r.foods[0].name)
        assertEquals(3, r.foods[0].meals)
    }

    @Test
    fun `the same food twice in a meal is one meal`() {
        val entries = (1..3).flatMap { d -> listOf(entry("Bread", d, 100, satiety = 3), entry("Bread", d, 100, satiety = 3, time = d * 1000L + 1)) }
        val r = satietyInsights(entries)
        assertEquals(3, r.foods.single().meals)
        assertEquals(200.0, r.foods.single().avgMealKcal, 1e-9)
    }

    @Test
    fun `fullness per 100 kcal and energy`() {
        val r = satietyInsights(meals(listOf("Soup"), listOf(4, 4, 4), kcal = 200, energy = 3))
        val soup = r.foods.single()
        assertEquals(2.0, soup.fullnessPer100Kcal!!, 1e-9)
        assertEquals(3.0, soup.avgEnergy!!, 1e-9)
        assertNull(satietyInsights(meals(listOf("Soup"), listOf(4, 4, 4))).foods.single().avgEnergy)
    }
}
