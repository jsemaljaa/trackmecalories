package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.Portion
import com.jsemalja.trackmecalories.data.encodePortions
import com.jsemalja.trackmecalories.data.initialPortion
import com.jsemalja.trackmecalories.data.parsePortions
import com.jsemalja.trackmecalories.data.toEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortionsTest {

    private val medium = Portion("medium", 51.0)
    private val egg = CustomFood(
        name = "Egg", kcalPer100 = 143.0, proteinPer100 = 12.6, carbsPer100 = 0.7, fatPer100 = 9.5,
        fiberPer100 = 0.0, saltPer100 = 0.36,
        portions = encodePortions(listOf(Portion("small", 42.0), medium, Portion("large", 60.0))),
        defaultPortion = "medium"
    )

    @Test
    fun `portions survive encoding, unknown keys are dropped`() {
        assertEquals(listOf(Portion("small", 42.0), medium, Portion("large", 60.0)), parsePortions(egg.portions))
        assertEquals(listOf(medium), parsePortions("medium=51;bucket=900;large=oops;=3"))
        assertEquals(emptyList<Portion>(), parsePortions(null))
        assertNull(encodePortions(emptyList()))
    }

    @Test
    fun `a new food starts at one usual portion`() {
        assertEquals(medium to 1.0, initialPortion(egg))
    }

    @Test
    fun `last time's portion and count are remembered`() {
        assertEquals(medium to 2.0, initialPortion(egg.copy(defaultAmount = 102.0)))
        assertEquals(Portion("large", 60.0) to 1.5, initialPortion(egg.copy(defaultAmount = 90.0, defaultPortion = "large")))
    }

    @Test
    fun `weighed last time, or an odd amount, starts in grams`() {
        assertNull(initialPortion(egg.copy(defaultAmount = 80.0, defaultPortion = null)))
        assertNull(initialPortion(egg.copy(defaultAmount = 137.0)))
        assertNull(initialPortion(egg.copy(portions = null)))
    }

    @Test
    fun `an entry logged in portions stores grams plus the portion`() {
        val entry = egg.toEntry(102.0, "Breakfast", "2026-10-26", portion = medium)
        assertEquals(102.0, entry.amount, 0.0)
        assertEquals("g", entry.unit)
        assertEquals(145, entry.kcal)
        assertEquals("medium", entry.portion)
        assertEquals(51.0, entry.portionGrams!!, 0.0)
        assertNull(egg.toEntry(100.0, "Breakfast", "2026-10-26").portion)
    }
}
