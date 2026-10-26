package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.Portion
import com.jsemalja.trackmecalories.data.encodePortions
import com.jsemalja.trackmecalories.data.initialPortion
import com.jsemalja.trackmecalories.data.parsePortions
import com.jsemalja.trackmecalories.data.stepAmount
import com.jsemalja.trackmecalories.data.toEntry
import com.jsemalja.trackmecalories.data.amountIn
import com.jsemalja.trackmecalories.data.unitPortion
import com.jsemalja.trackmecalories.data.usesPieceWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `minus and plus land on round amounts`() {
        assertEquals(160.0, stepAmount(152.0, counting = false, up = true), 0.0)
        assertEquals(150.0, stepAmount(152.0, counting = false, up = false), 0.0)
        assertEquals(140.0, stepAmount(150.0, counting = false, up = false), 0.0)
        assertEquals(2.0, stepAmount(1.5, counting = true, up = true), 0.0)
        assertEquals(1.0, stepAmount(1.5, counting = true, up = false), 0.0)
        assertEquals(1.0, stepAmount(0.0, counting = true, up = true), 0.0)
    }

    @Test
    fun `minus never goes below one step`() {
        assertEquals(10.0, stepAmount(10.0, counting = false, up = false), 0.0)
        assertEquals(1.0, stepAmount(1.0, counting = true, up = false), 0.0)
        assertEquals(0.5, stepAmount(0.5, counting = true, up = false), 0.0)
        assertEquals(5.0, stepAmount(5.0, counting = false, up = false), 0.0)
    }

    private val burger = CustomFood(
        name = "Big Mac", kcalPer100 = 508.0, proteinPer100 = 26.0, carbsPer100 = 41.0, fatPer100 = 26.0,
        fiberPer100 = 3.0, saltPer100 = 2.2, unit = "pcs", referenceAmount = 1.0, pieceGrams = 215.0
    )

    @Test
    fun `a food counted in pieces converts to grams through its piece weight`() {
        assertEquals(Portion("piece", 1.0), unitPortion(burger, "piece"))
        assertEquals(1 / 215.0, unitPortion(burger, "g")!!.grams, 1e-12)
        assertEquals(15 / 215.0, unitPortion(burger, "tbsp")!!.grams, 1e-12)
        // 150 g of it is 150/215 of a burger.
        val entry = burger.toEntry(150 * unitPortion(burger, "g")!!.grams, "Lunch", "2026-10-26", portion = unitPortion(burger, "g"))
        assertEquals(354, entry.kcal)
        assertEquals("pcs", entry.unit)
        assertEquals(150.0, entry.amount / entry.portionGrams!!, 1e-9)
    }

    @Test
    fun `without a piece weight, pieces and weight don't convert`() {
        val unknown = burger.copy(pieceGrams = null)
        assertNull(unitPortion(unknown, "g"))
        assertTrue(usesPieceWeight(unknown, "g"))
        assertFalse(usesPieceWeight(unknown, "piece"))
        assertNull(unitPortion(egg, "piece"))
        assertEquals(Portion("piece", 58.0), unitPortion(egg, "piece", pieceGrams = 58.0))
    }

    @Test
    fun `weighed foods take ml as g and spoons by volume, unless they have their own spoon`() {
        assertEquals(Portion("g", 1.0), unitPortion(egg, "g"))
        assertEquals(Portion("ml", 1.0), unitPortion(egg, "ml"))
        assertEquals(Portion("tsp", 5.0), unitPortion(egg, "tsp"))
        val oil = egg.copy(portions = "tbsp=13.5")
        assertEquals(Portion("tbsp", 13.5), unitPortion(oil, "tbsp"))
        assertFalse(usesPieceWeight(oil, "tbsp"))
    }

    @Test
    fun `last time's unit is remembered`() {
        val (grams, count) = initialPortion(burger.copy(defaultAmount = 150 / 215.0, defaultPortion = "g"))!!
        assertEquals("g", grams.key)
        assertEquals(150.0, count, 1e-9)
        assertEquals(Portion("tsp", 5.0) to 2.0, initialPortion(egg.copy(defaultAmount = 10.0, defaultPortion = "tsp")))
        // The food's own unit isn't a portion.
        assertNull(initialPortion(burger.copy(defaultAmount = 2.0, defaultPortion = "piece")))
    }

    @Test
    fun `switching units keeps the quantity`() {
        assertEquals(0.7, amountIn(150.0 / 215, null, counting = true), 0.0)
        assertEquals(150.0, amountIn(150.0 / 215, unitPortion(burger, "g"), counting = false), 0.0)
        assertEquals(3.0, amountIn(45.0, Portion("tbsp", 15.0), counting = true), 0.0)
        assertEquals(1.0, amountIn(0.2, null, counting = false), 0.0)
    }
}
