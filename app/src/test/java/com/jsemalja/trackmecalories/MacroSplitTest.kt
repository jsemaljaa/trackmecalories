package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.Diet
import com.jsemalja.trackmecalories.data.MacroSplit
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.withDietMacros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroSplitTest {
    @Test
    fun `split round-trips through dietType and reads as custom`() {
        val s = MacroSplit(20, 30, 50)
        assertEquals(s, MacroSplit.parse(s.encode()))
        assertEquals(Diet.CUSTOM, Diet.from(s.encode()))
    }

    @Test
    fun `bad or non-split dietType parses to null`() {
        assertNull(MacroSplit.parse("BALANCED"))
        assertNull(MacroSplit.parse(null))
        assertNull(MacroSplit.parse("PCT:20/30"))
        assertNull(MacroSplit.parse("PCT:20/30/40"))
        assertNull(MacroSplit.parse("PCT:a/30/70"))
    }

    @Test
    fun `grams for 1800 kcal at 20-30-50`() {
        val m = MacroSplit(20, 30, 50).macrosFor(1800)
        assertEquals(90.0, m.protein, 0.0)
        assertEquals(135.0, m.carbs, 0.0)
        assertEquals(100.0, m.fat, 0.0)
    }

    @Test
    fun `split from grams always adds up to 100`() {
        assertEquals(MacroSplit(20, 30, 50), MacroSplit.fromGrams(90.0, 135.0, 100.0))
        assertEquals(100, MacroSplit.fromGrams(33.0, 33.0, 15.0)!!.total)
        assertNull(MacroSplit.fromGrams(0.0, 0.0, 0.0))
    }

    @Test
    fun `goal with a saved split follows calorie changes`() {
        val g = UserGoal(calorieGoal = 1800, dietType = MacroSplit(20, 30, 50).encode()).withDietMacros()
        assertEquals(90.0, g.proteinGoal, 0.0)
    }
}
