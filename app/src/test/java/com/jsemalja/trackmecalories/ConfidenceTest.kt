package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.Confidence
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.kcalUncertainty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceTest {
    private fun entry(kcal: Int, c: Confidence) = FoodEntry(
        name = "x", kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0, fiber = 0.0, salt = 0.0,
        date = "2026-10-25", confidence = c.name
    )

    @Test
    fun `single entry error is its relative error`() {
        assertEquals(50.0, listOf(entry(1000, Confidence.WEIGHED)).kcalUncertainty(), 0.001)
        assertEquals(300.0, listOf(entry(1000, Confidence.GUESSED)).kcalUncertainty(), 0.001)
    }

    @Test
    fun `independent errors partly cancel`() {
        val entries = listOf(entry(500, Confidence.ESTIMATED), entry(500, Confidence.ESTIMATED))
        // sqrt(75² + 75²) ≈ 106, less than the 150 a plain sum would give.
        assertEquals(106.07, entries.kcalUncertainty(), 0.01)
        assertTrue(entries.kcalUncertainty() < 150.0)
    }

    @Test
    fun `unknown confidence counts as estimated`() {
        val e = entry(1000, Confidence.ESTIMATED).copy(confidence = "SOMETHING_OLD")
        assertEquals(150.0, listOf(e).kcalUncertainty(), 0.001)
    }
}
