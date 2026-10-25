package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.nutriScore2023
import com.jsemalja.trackmecalories.data.rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutriScoreTest {
    @Test
    fun `plain skyr is A`() {
        // 60 kcal, 4 g sugar, 0.1 g sat fat, 0.1 g salt, 11 g protein, 0 fiber.
        assertEquals('A', nutriScore2023(60.0, 4.0, 0.1, 0.1, 11.0, 0.0, beverage = false))
    }

    @Test
    fun `milk chocolate is E`() {
        // 540 kcal, 55 g sugar, 19 g sat fat, 0.2 g salt, 7 g protein, 2 g fiber.
        assertEquals('E', nutriScore2023(540.0, 55.0, 19.0, 0.2, 7.0, 2.0, beverage = false))
    }

    @Test
    fun `oats are A`() {
        // 380 kcal, 1 g sugar, 1.3 g sat fat, 0.01 g salt, 13 g protein, 10 g fiber.
        assertEquals('A', nutriScore2023(380.0, 1.0, 1.3, 0.01, 13.0, 10.0, beverage = false))
    }

    @Test
    fun `cola is E and water is A`() {
        assertEquals('E', nutriScore2023(42.0, 10.6, 0.0, 0.0, 0.0, 0.0, beverage = true))
        assertEquals('A', nutriScore2023(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, beverage = true))
    }

    @Test
    fun `unknown sugar gives no grade`() {
        assertNull(nutriScore2023(100.0, Double.NaN, 1.0, 0.1, 5.0, 1.0, beverage = false))
    }

    @Test
    fun `official grade wins over the estimate`() {
        val f = CustomFood(name = "x", kcalPer100 = 540.0, proteinPer100 = 7.0, carbsPer100 = 0.0, fatPer100 = 0.0,
            fiberPer100 = 2.0, saltPer100 = 0.2, sugarPer100 = 55.0, saturatedFatPer100 = 19.0, nutriScore = "d")
        val r = f.rating()
        assertEquals('D', r.grade)
        assertFalse(r.estimated)
        val estimate = f.copy(nutriScore = null).rating()
        assertEquals('E', estimate.grade)
        assertTrue(estimate.estimated)
    }

    @Test
    fun `densities per 100 kcal`() {
        val f = CustomFood(name = "x", kcalPer100 = 200.0, proteinPer100 = 20.0, carbsPer100 = 0.0, fatPer100 = 0.0,
            fiberPer100 = 4.0, saltPer100 = 0.0)
        val r = f.rating()
        assertEquals(10.0, r.proteinPer100kcal, 0.001)
        assertEquals(2.0, r.fiberPer100kcal, 0.001)
        assertEquals(2.0, r.energyDensity!!, 0.001)
    }
}
