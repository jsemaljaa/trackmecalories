package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodCategory
import com.jsemalja.trackmecalories.data.FoodCategory.*
import com.jsemalja.trackmecalories.data.guessCategory
import com.jsemalja.trackmecalories.data.healthOrder
import com.jsemalja.trackmecalories.data.ktCategory
import com.jsemalja.trackmecalories.data.nutriScoreGrade
import com.jsemalja.trackmecalories.data.nutriScorePoints
import com.jsemalja.trackmecalories.data.offCategory
import com.jsemalja.trackmecalories.data.rating
import com.jsemalja.trackmecalories.data.withRatingDataFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutriScoreTest {
    /** Grade of per-100 g values, as the official 2023 tables would give it. */
    private fun grade(
        category: FoodCategory, kcal: Double, sugar: Double, satFat: Double, salt: Double, protein: Double,
        fiber: Double, fat: Double = 0.0, fvl: Double = 0.0
    ): Char = nutriScoreGrade(category, kcal, nutriScorePoints(category, kcal, sugar, satFat, fat, salt, protein, fiber, fvl))

    private fun food(
        name: String = "x", kcal: Double, sugar: Double? = null, satFat: Double? = null, salt: Double = 0.0,
        protein: Double = 0.0, fiber: Double = 0.0, carbs: Double = 0.0, fat: Double = 0.0, unit: String = "g",
        category: FoodCategory? = GENERAL, fvl: Double? = null, nutriScore: String? = null
    ) = CustomFood(
        name = name, kcalPer100 = kcal, proteinPer100 = protein, carbsPer100 = carbs, fatPer100 = fat,
        fiberPer100 = fiber, saltPer100 = salt, unit = unit, sugarPer100 = sugar, saturatedFatPer100 = satFat,
        category = category?.key, fvlPercent = fvl, nutriScore = nutriScore
    )

    @Test
    fun `general foods`() {
        // Plain skyr, milk chocolate, rolled oats.
        assertEquals('A', grade(GENERAL, 60.0, 4.0, 0.1, 0.1, 11.0, 0.0))
        assertEquals('E', grade(GENERAL, 540.0, 55.0, 19.0, 0.2, 7.0, 2.0))
        assertEquals('A', grade(GENERAL, 380.0, 1.0, 1.3, 0.01, 13.0, 10.0))
    }

    @Test
    fun `fruit counts, so an apple is A, not C`() {
        assertEquals('A', grade(GENERAL, 52.0, 10.4, 0.03, 0.0, 0.26, 2.4, fvl = 100.0))
        assertEquals('C', grade(GENERAL, 52.0, 10.4, 0.03, 0.0, 0.26, 2.4, fvl = 0.0))
    }

    @Test
    fun `fats are judged by the share of saturated fat`() {
        assertEquals('B', grade(FAT, 884.0, 0.0, 13.8, 0.0, 0.0, 0.0, fat = 100.0, fvl = 100.0)) // olive oil
        assertEquals('C', grade(FAT, 884.0, 0.0, 10.3, 0.0, 0.0, 0.0, fat = 100.0)) // sunflower oil
        assertEquals('E', grade(FAT, 717.0, 0.06, 51.4, 0.03, 0.85, 0.0, fat = 81.1)) // unsalted butter
        assertEquals('A', grade(FAT, 579.0, 4.35, 3.8, 0.0, 21.2, 12.5, fat = 49.9, fvl = 100.0)) // almonds
        // As a general food, olive oil would be D.
        assertEquals('D', grade(GENERAL, 884.0, 0.0, 13.8, 0.0, 0.0, 0.0, fat = 100.0, fvl = 100.0))
    }

    @Test
    fun `red meat gets less credit for protein`() {
        // Ground beef, 20 % fat.
        val asMeat = nutriScorePoints(RED_MEAT, 254.0, 0.0, 7.6, 20.0, 0.18, 17.2, 0.0, 0.0)
        val asGeneral = nutriScorePoints(GENERAL, 254.0, 0.0, 7.6, 20.0, 0.18, 17.2, 0.0, 0.0)
        assertEquals(asGeneral + 5, asMeat)
    }

    @Test
    fun `cheese keeps its protein even when high in fat and salt`() {
        // Gouda: 356 kcal, 2.2 g sugar, 17.6 g sat fat, 2 g salt, 25 g protein.
        val cheese = nutriScorePoints(CHEESE, 356.0, 2.2, 17.6, 27.4, 2.0, 25.0, 0.0, 0.0)
        val general = nutriScorePoints(GENERAL, 356.0, 2.2, 17.6, 27.4, 2.0, 25.0, 0.0, 0.0)
        assertEquals(general - 7, cheese)
    }

    @Test
    fun `drinks`() {
        assertEquals('E', grade(BEVERAGE, 42.0, 10.6, 0.0, 0.0, 0.0, 0.0)) // cola
        assertEquals('B', grade(BEVERAGE, 46.0, 4.8, 0.9, 0.1, 3.4, 0.0)) // semi-skimmed milk
        assertEquals('C', grade(BEVERAGE, 45.0, 8.4, 0.02, 0.0, 0.7, 0.2, fvl = 100.0)) // orange juice
        assertEquals('A', grade(WATER, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        // A zero-calorie soft drink is not water.
        assertEquals('B', grade(BEVERAGE, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0))
    }

    @Test
    fun `score bands follow the letters and rank inside them`() {
        val apple = food(kcal = 52.0, sugar = 10.4, satFat = 0.03, protein = 0.26, fiber = 2.4, fvl = 100.0).rating()
        val oats = food(kcal = 380.0, sugar = 1.0, satFat = 1.3, salt = 0.01, protein = 13.0, fiber = 10.0).rating()
        val chocolate = food(kcal = 540.0, sugar = 55.0, satFat = 19.0, salt = 0.2, protein = 7.0, fiber = 2.0).rating()
        assertEquals('A', apple.grade)
        assertTrue(apple.score!! in 80.0..100.0 && oats.score!! in 80.0..100.0)
        assertTrue("oats (-5 points) beat an apple (-2)", oats.score!! > apple.score!!)
        assertEquals('E', chocolate.grade)
        assertTrue(chocolate.score!! < 20)
        assertEquals(100.0, food(name = "Voda", kcal = 0.0, unit = "ml", category = null).rating().score!!, 0.0)
    }

    @Test
    fun `unknown sugar and saturated fat give a range`() {
        val r = food(kcal = 250.0, carbs = 30.0, fat = 10.0, protein = 10.0, fiber = 2.0, salt = 1.0).rating()
        assertEquals('C', r.bestGrade)
        assertEquals('E', r.worstGrade)
        assertTrue(r.uncertain)
        assertTrue(r.estimated)
        // Midway between best (C, 58.75) and worst (E, 17.0).
        assertEquals('D', r.grade)
        assertEquals(37.9, r.score!!, 0.1)

        // A to E says nothing: no letter, but the midpoint still sorts it.
        val wide = food(kcal = 400.0, carbs = 60.0, fat = 20.0, protein = 10.0).rating()
        assertNull(wide.grade)
        assertNotNull(wide.score)

        val known = food(kcal = 250.0, sugar = 5.0, satFat = 2.0, carbs = 30.0, fat = 10.0, protein = 10.0).rating()
        assertFalse(known.uncertain)
    }

    @Test
    fun `official grade wins and our points place it inside its band`() {
        val f = food(kcal = 540.0, sugar = 55.0, satFat = 19.0, salt = 0.2, protein = 7.0, fiber = 2.0, nutriScore = "d")
        val r = f.rating()
        assertEquals('D', r.grade)
        assertFalse(r.estimated)
        assertTrue(r.score!! in 20.0..40.0)
        val estimate = f.copy(nutriScore = null).rating()
        assertEquals('E', estimate.grade)
        assertTrue(estimate.estimated)
    }

    @Test
    fun `a piece is rated per 100 g of its weight`() {
        val perPiece = food(kcal = 500.0, sugar = 2.0, satFat = 1.0, salt = 0.2, protein = 20.0, fiber = 4.0)
            .copy(unit = "pcs", referenceAmount = 1.0, pieceGrams = 200.0)
        val per100 = food(kcal = 250.0, sugar = 1.0, satFat = 0.5, salt = 0.1, protein = 10.0, fiber = 2.0)
        assertEquals(per100.rating().score!!, perPiece.rating().score!!, 0.001)
        assertEquals(2.5, perPiece.rating().energyDensity!!, 0.001)
        // Unknown weight: can't be rated.
        assertNull(perPiece.copy(pieceGrams = null).rating().score)
    }

    @Test
    fun `alcohol isn't rated`() {
        val beer = food(name = "Pivo", kcal = 43.0, sugar = 0.0, satFat = 0.0, unit = "ml", category = null).rating()
        assertEquals(ALCOHOL, beer.category)
        assertNull(beer.grade)
        assertNull(beer.score)
    }

    @Test
    fun `categories guessed from names`() {
        assertEquals(FAT, guessCategory("Olivový olej extra panenský", "g"))
        assertEquals(FAT, guessCategory("Масло сливочное 82%", "g"))
        assertEquals(FAT, guessCategory("Vlašské ořechy", "g"))
        assertEquals(CHEESE, guessCategory("Sýr Eidam 30%", "g"))
        assertEquals(CHEESE, guessCategory("Cottage cheese · Madeta", "g"))
        assertEquals(GENERAL, guessCategory("Toast se sýrem", "g"))
        assertEquals(GENERAL, guessCategory("Картофель, сырой", "g"))
        assertEquals(GENERAL, guessCategory("Salát s olivovým olejem", "g"))
        assertEquals(GENERAL, guessCategory("Баранки", "g"))
        assertEquals(RED_MEAT, guessCategory("Hovězí guláš", "g"))
        assertEquals(RED_MEAT, guessCategory("Свинина тушёная", "g"))
        assertEquals(BEVERAGE, guessCategory("Coca-Cola", "ml", 42.0))
        assertEquals(BEVERAGE, guessCategory("Nealkoholické pivo", "ml", 20.0))
        assertEquals(ALCOHOL, guessCategory("Pivo Pilsner Urquell", "ml", 42.0))
        assertEquals(WATER, guessCategory("Вода минеральная", "ml", 0.0))
        assertEquals(BEVERAGE, guessCategory("Kokosová voda", "ml", 19.0))
    }

    @Test
    fun `Kalorické tabulky types`() {
        assertEquals(FAT to 100.0, ktCategory("vareni-a-peceni/oleje-olivove"))
        assertEquals(FAT to null, ktCategory("vareni-a-peceni/oleje-ostatni"))
        assertEquals(CHEESE to null, ktCategory("mlecne-vyrobky/syry-tvrde-platkove-a-bloky"))
        assertEquals(FAT to null, ktCategory("mlecne-vyrobky/masla-klasicka"))
        assertEquals(BEVERAGE to null, ktCategory("mlecne-vyrobky/mleko-cerstve-a-trvanlive"))
        assertEquals(RED_MEAT to null, ktCategory("maso/maso-hovezi-a-vnitrnosti"))
        assertEquals(GENERAL to null, ktCategory("maso/maso-drubezi"))
        assertEquals(RED_MEAT to null, ktCategory("uzeniny-a-lahudky/sunky"))
        assertEquals(FAT to 100.0, ktCategory("orechy-a-semena/orechy-ciste"))
        assertEquals(GENERAL to 100.0, ktCategory("ovoce/ovoce-cerstve"))
        assertEquals(WATER to null, ktCategory("nealkoholicke-napoje/voda-neperliva"))
        assertEquals(ALCOHOL to null, ktCategory("alkoholicke-napoje/alkoholicke-pivo"))
        assertEquals(GENERAL to null, ktCategory("jidla-a-prilohy/fast-food"))
        assertEquals(null to null, ktCategory(null))
    }

    @Test
    fun `Open Food Facts categories`() {
        assertEquals(CHEESE, offCategory(listOf("en:dairies", "en:cheeses")))
        assertEquals(FAT, offCategory(listOf("en:fats", "en:vegetable-oils", "en:olive-oils")))
        assertEquals(WATER, offCategory(listOf("en:beverages", "en:waters")))
        assertEquals(BEVERAGE, offCategory(listOf("en:beverages", "en:waters", "en:flavoured-waters")))
        assertEquals(ALCOHOL, offCategory(listOf("en:beverages", "en:alcoholic-beverages", "en:beers")))
        assertEquals(GENERAL, offCategory(listOf("en:snacks")))
        assertNull(offCategory(emptyList()))
    }

    @Test
    fun `saved foods pick up missing rating data from the bundled list`() {
        val saved = food(kcal = 884.0, fat = 100.0, category = null).copy(id = 7, isFavorite = true)
        val bundled = food(kcal = 884.0, fat = 100.0, satFat = 13.8, sugar = 0.0, category = FAT, fvl = 100.0)
        val updated = saved.withRatingDataFrom(bundled)!!
        assertEquals("fat", updated.category)
        assertEquals(100.0, updated.fvlPercent!!, 0.0)
        assertEquals(13.8, updated.saturatedFatPer100!!, 0.0)
        assertTrue(updated.isFavorite)
        assertNull(updated.withRatingDataFrom(bundled))
        // Values per a different unit aren't copied.
        assertNull(saved.copy(unit = "pcs", referenceAmount = 1.0).withRatingDataFrom(bundled)!!.saturatedFatPer100)
    }

    @Test
    fun `health order puts the best score first and unrated last`() {
        val apple = food(kcal = 52.0, sugar = 10.4, satFat = 0.03, protein = 0.26, fiber = 2.4, fvl = 100.0)
        val chocolate = food(kcal = 540.0, sugar = 55.0, satFat = 19.0, salt = 0.2, protein = 7.0, fiber = 2.0)
        val beer = food(name = "Pivo", kcal = 43.0, unit = "ml", category = null)
        val sorted = listOf(beer, chocolate, apple).sortedWith(compareBy(healthOrder) { it.rating() })
        assertEquals(listOf(apple, chocolate, beer), sorted)
    }

    @Test
    fun `densities per 100 kcal`() {
        val r = food(kcal = 200.0, protein = 20.0, fiber = 4.0).rating()
        assertEquals(10.0, r.proteinPer100kcal, 0.001)
        assertEquals(2.0, r.fiberPer100kcal, 0.001)
        assertEquals(2.0, r.energyDensity!!, 0.001)
    }
}
