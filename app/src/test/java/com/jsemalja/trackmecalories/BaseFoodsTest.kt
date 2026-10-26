package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.BaseFood
import com.jsemalja.trackmecalories.data.PORTION_KEYS
import com.jsemalja.trackmecalories.data.SearchSource
import com.jsemalja.trackmecalories.data.czechQueryFor
import com.jsemalja.trackmecalories.data.parseBaseFoods
import com.jsemalja.trackmecalories.data.parsePortions
import com.jsemalja.trackmecalories.data.searchBaseFoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs against the real bundled list, so a bad regeneration of the asset fails here. */
class BaseFoodsTest {

    private val foods: List<BaseFood> = parseBaseFoods(File("src/main/assets/base_foods.json").readText())

    private fun keys(query: String, language: String = "en") =
        searchBaseFoods(foods, query, language).sortedByDescending { it.score }.map { it.key.removePrefix("base:") }

    @Test
    fun `every food is complete`() {
        assertTrue(foods.size > 150)
        assertEquals(foods.size, foods.map { it.key }.toSet().size)
        for (food in foods) {
            assertEquals(food.key, setOf("en", "ru", "cs"), food.names.keys)
            assertTrue(food.key, food.kcal >= 0 && food.protein >= 0 && food.carbs >= 0 && food.fat >= 0)
            assertTrue(food.key, food.portions.all { it.key in PORTION_KEYS && it.grams > 0 })
            if (food.portions.isNotEmpty()) assertTrue(food.key, food.portions.any { it.key == food.defaultPortion })
        }
    }

    @Test
    fun `raw comes first, then cooked`() {
        val chicken = keys("chicken breast")
        assertEquals("chicken_breast", chicken.first())
        assertTrue(chicken.take(5).all { it.startsWith("chicken_breast") })
        assertEquals(listOf("carrot", "carrot_boiled"), keys("carrot").take(2))
        assertEquals("egg", keys("egg").first())
    }

    @Test
    fun `russian and czech queries find the same food`() {
        assertEquals("carrot", keys("морковь", "ru").first())
        assertEquals("carrot", keys("mrkev").first())
        assertEquals("egg", keys("яйцо", "ru").first())
        assertEquals("chicken_breast", keys("куриная грудка", "ru").first())
        assertEquals("chicken_breast", keys("kuřecí prsa").first())
        assertEquals("buckwheat", keys("гречка", "ru").first())
    }

    @Test
    fun `results are named in the app language`() {
        val hit = searchBaseFoods(foods, "carrot", "ru").first { it.key == "base:carrot" }
        assertEquals("Морковь", hit.name)
        assertEquals("Морковь", hit.food!!.name)
        assertEquals(SearchSource.BASIC, hit.source)
        assertEquals("Carrot", searchBaseFoods(foods, "морковь", "en").first { it.key == "base:carrot" }.name)
    }

    @Test
    fun `base foods carry their portions into the saved food`() {
        val egg = searchBaseFoods(foods, "egg", "en").first { it.key == "base:egg" }.food!!
        assertEquals("base:egg", egg.barcode)
        assertEquals("medium", egg.defaultPortion)
        assertEquals(listOf("small", "medium", "large", "extra_large"), parsePortions(egg.portions).map { it.key })
    }

    @Test
    fun `russian queries get a czech name for the online databases`() {
        assertEquals("Kuřecí prsa", czechQueryFor(foods, "куриная грудка"))
        assertEquals("Mrkev", czechQueryFor(foods, "морковь"))
        assertNull(czechQueryFor(foods, "chicken breast"))
        assertNull(czechQueryFor(foods, "абракадабра"))
    }
}
