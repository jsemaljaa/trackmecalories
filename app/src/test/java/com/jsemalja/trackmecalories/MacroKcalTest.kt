package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.kcalFromMacros
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroKcalTest {
    @Test
    fun `protein and carbs are 4 kcal per gram, fat 9`() {
        assertEquals(4, kcalFromMacros(1.0, 0.0, 0.0))
        assertEquals(4, kcalFromMacros(0.0, 1.0, 0.0))
        assertEquals(9, kcalFromMacros(0.0, 0.0, 1.0))
        assertEquals(30 * 4 + 50 * 4 + 20 * 9, kcalFromMacros(30.0, 50.0, 20.0))
    }

    @Test
    fun `fractional grams round to the nearest kcal`() {
        assertEquals(15, kcalFromMacros(2.5, 0.0, 0.5))
        assertEquals(0, kcalFromMacros(0.0, 0.0, 0.0))
    }
}
