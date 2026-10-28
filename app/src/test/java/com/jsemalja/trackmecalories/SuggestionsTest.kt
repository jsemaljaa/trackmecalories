package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.timeSuggestions
import com.jsemalja.trackmecalories.data.whatFits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.jsemalja.trackmecalories.data.toEpochMillis
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class SuggestionsTest {
    private val zone = TimeZone.UTC
    // A Monday morning.
    private val now = LocalDateTime(2026, 10, 26, 8, 0)

    private fun food(id: Int, name: String, kcal: Double, protein: Double, fiber: Double = 0.0) = CustomFood(
        id = id, name = name, kcalPer100 = kcal, proteinPer100 = protein, carbsPer100 = 0.0,
        fatPer100 = 0.0, fiberPer100 = fiber, saltPer100 = 0.0
    )

    private fun eaten(food: CustomFood, at: LocalDateTime, amount: Double = 100.0) = FoodEntry(
        name = food.name, kcal = 0, protein = 0.0, carbs = 0.0, fat = 0.0, fiber = 0.0, salt = 0.0,
        amount = amount, date = at.toLocalDate().toString(), time = at.toEpochMillis(zone),
        foodId = food.id
    )

    private val skyr = food(1, "Skyr", 60.0, 11.0)
    private val oats = food(2, "Oats", 380.0, 13.0, fiber = 10.0)
    private val pizza = food(3, "Pizza", 270.0, 11.0)

    @Test
    fun `suggests what you usually have at this time`() {
        val entries = (1L..5L).flatMap { d ->
            listOf(eaten(skyr, now.minusDays(d).withHour(7).withMinute(50)), eaten(pizza, now.minusDays(d).withHour(19)))
        }
        val s = timeSuggestions(entries, listOf(skyr, oats, pizza), now, zone)
        assertEquals(listOf("Skyr"), s.map { it.food.name })
    }

    @Test
    fun `skips what you just had`() {
        val entries = (1L..5L).map { eaten(skyr, now.minusDays(it).withHour(8)) } + eaten(skyr, now.minusHours(1))
        assertTrue(timeSuggestions(entries, listOf(skyr), now, zone).isEmpty())
    }

    @Test
    fun `a one-off is not a habit`() {
        val entries = listOf(eaten(oats, now.minusDays(2).withHour(8)))
        assertTrue(timeSuggestions(entries, listOf(oats), now, zone).isEmpty())
    }

    @Test
    fun `what fits respects the remaining calories and prefers protein`() {
        val entries = listOf(eaten(skyr, now.minusDays(1), 200.0), eaten(pizza, now.minusDays(2), 300.0), eaten(oats, now.minusDays(3), 60.0))
        val fits = whatFits(entries, listOf(skyr, oats, pizza), remainingKcal = 300.0, remainingProtein = 30.0)
        // Pizza at 300 g = 810 kcal doesn't fit; skyr (120 kcal, 22 g protein) ranks first.
        assertEquals("Skyr", fits.first().food.name)
        assertTrue(fits.none { it.food.name == "Pizza" })
    }
}
