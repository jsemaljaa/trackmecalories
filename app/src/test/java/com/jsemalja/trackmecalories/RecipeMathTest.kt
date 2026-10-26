package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.Recipe
import com.jsemalja.trackmecalories.data.dishWeightG
import com.jsemalja.trackmecalories.data.estimatedCookedWeightG
import com.jsemalja.trackmecalories.data.hasCookFactors
import com.jsemalja.trackmecalories.data.inPieces
import com.jsemalja.trackmecalories.data.missingWeights
import com.jsemalja.trackmecalories.data.per100g
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.data.toIngredient
import com.jsemalja.trackmecalories.data.totals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeMathTest {
    private fun food(name: String, kcal: Double, protein: Double, unit: String = "g") = CustomFood(
        name = name, kcalPer100 = kcal, proteinPer100 = protein, carbsPer100 = 0.0, fatPer100 = 0.0,
        fiberPer100 = 0.0, saltPer100 = 0.0, unit = unit, referenceAmount = if (unit == "pcs") 1.0 else 100.0
    )

    // 200 g dry rice (350 kcal/100 g) + 300 g raw chicken (110 kcal/100 g, 23 g protein)
    private val rice = food("Rice", 350.0, 7.0).toIngredient(200.0).copy(cookFactor = 2.5)
    private val chicken = food("Chicken", 110.0, 23.0).toIngredient(300.0).copy(cookFactor = 0.75)

    @Test
    fun `totals add up the ingredients`() {
        val t = listOf(rice, chicken).totals()
        assertEquals(700.0 + 330.0, t.kcal, 0.01)
        assertEquals(14.0 + 69.0, t.protein, 0.01)
    }

    @Test
    fun `cooking factors estimate the dish weight`() {
        // 200 g rice -> 500 g cooked, 300 g chicken -> 225 g cooked.
        assertEquals(725.0, listOf(rice, chicken).estimatedCookedWeightG(), 0.01)
    }

    @Test
    fun `weighed pot beats the estimate`() {
        val recipe = Recipe(name = "Bowl", cookedWeightG = 800.0, logBy = "g")
        assertEquals(800.0, recipe.dishWeightG(listOf(rice, chicken)), 0.01)
    }

    @Test
    fun `per serving food`() {
        val f = Recipe(name = "Bowl", servings = 4.0, logBy = "pcs").toCustomFood(listOf(rice, chicken), null)
        assertEquals("pcs", f.unit)
        assertEquals(1.0, f.referenceAmount, 0.0)
        assertEquals(1030.0 / 4, f.kcalPer100, 0.01)
    }

    @Test
    fun `by weight food is per 100 g of the cooked dish`() {
        val f = Recipe(name = "Bowl", cookedWeightG = 1030.0, logBy = "g").toCustomFood(listOf(rice, chicken), null)
        assertEquals("g", f.unit)
        assertEquals(100.0, f.kcalPer100, 0.01) // 1030 kcal in 1030 g
    }

    @Test
    fun `pieces count toward weight only with a piece weight`() {
        val egg = food("Egg", 75.0, 6.5, unit = "pcs").toIngredient(2.0)
        assertEquals(0.0, listOf(egg).estimatedCookedWeightG(), 0.0)
        assertEquals(110.0, listOf(egg.copy(gramsPerUnit = 55.0)).estimatedCookedWeightG(), 0.01)
        assertEquals(150.0, listOf(egg).totals().kcal, 0.01)
    }

    @Test
    fun `per 100 g of the raw ingredients when the dish isn't weighed`() {
        // 200 g rice + 300 g chicken, no cooking factors: 1030 kcal in 500 g.
        val raw = listOf(rice.copy(cookFactor = 1.0), chicken.copy(cookFactor = 1.0))
        assertEquals(false, raw.hasCookFactors())
        val per100 = raw.totals().per100g(raw.estimatedCookedWeightG())!!
        assertEquals(206.0, per100.kcal, 0.01)
        assertEquals(16.6, per100.protein, 0.01)
    }

    @Test
    fun `no per 100 g values without a weight`() {
        val egg = food("Egg", 75.0, 6.5, unit = "pcs").toIngredient(2.0)
        assertNull(listOf(egg).totals().per100g(listOf(egg).estimatedCookedWeightG()))
        assertEquals(listOf(egg), listOf(egg, rice).missingWeights())
        assertEquals(true, listOf(rice).hasCookFactors())
    }

    @Test
    fun `editing keeps the saved food's identity`() {
        val existing = food("Old name", 1.0, 1.0).copy(id = 42, isFavorite = true, lastUsedAt = 123L)
        val f = Recipe(name = "Bowl", servings = 2.0).toCustomFood(listOf(rice), existing)
        assertEquals(42, f.id)
        assertEquals(true, f.isFavorite)
        assertEquals(123L, f.lastUsedAt)
        assertEquals("Bowl", f.name)
    }

    @Test
    fun `a piece-counted ingredient reads as pieces only when whole or half`() {
        val tub = food("Doenjang", 900.0, 60.0, unit = "pcs").copy(pieceGrams = 500.0)
        assertEquals(500.0, tub.toIngredient(1.0).gramsPerUnit, 0.0)
        assertEquals(false, tub.toIngredient(6.0 / 500).inPieces())
        assertEquals(true, tub.toIngredient(6.0).inPieces())
        assertEquals(true, tub.toIngredient(0.5).inPieces())
        // Without a weight there's nothing else to show.
        assertEquals(true, food("Egg", 75.0, 6.5, unit = "pcs").toIngredient(0.3).inPieces())
        assertEquals(false, rice.inPieces())
    }
}
