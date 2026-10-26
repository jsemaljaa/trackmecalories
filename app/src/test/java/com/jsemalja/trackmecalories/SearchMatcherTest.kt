package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.SearchMatcher
import com.jsemalja.trackmecalories.data.normalizedForSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    private fun score(query: String, name: String, brand: String? = null) =
        SearchMatcher.score(SearchMatcher.tokens(query), name, brand)

    @Test
    fun `accents and case are ignored`() {
        assertEquals("kureci prsa", "Kuřecí PRSA".normalizedForSearch())
        assertNotNull(score("kureci prsa", "kuřecí prsa grilovaná"))
        assertNotNull(score("kuřecí", "Kureci prsa"))
    }

    @Test
    fun `half-typed last word matches by prefix`() {
        assertNotNull(score("don pacin", "Hawaii Don Pačínko"))
        assertNotNull(score("don pa", "Don Pacinko Capricciosa"))
        assertNotNull(score("capric", "Capricciosa", "Don Pačínko"))
        assertNotNull(score("bageterie cae", "Caesar bageta Bageterie Boulevard"))
    }

    @Test
    fun `fuzzy junk from online sources is rejected`() {
        // What Kalorické tabulky returned for these half-typed queries.
        assertNull(score("don pacin", "Céréal pain d’épices con miele, segale e crusca di segale"))
        assertNull(score("don pacin", "Mon pain multi-céréales Francine"))
        assertNull(score("capric", "kuře na paprice"))
        assertNull(score("don pa", "Double cheese hot dog røkt på bøkeflis Rema"))
    }

    @Test
    fun `czech word endings are tolerated`() {
        assertNotNull(score("kureci prsa", "kuřecí prso vařené"))
        assertNotNull(score("jogurt bily", "Bílého jogurtu"))
    }

    @Test
    fun `brand can satisfy a query word`() {
        assertNotNull(score("tvaroh tatra", "Tvaroh polotučný", "Tatra"))
    }

    @Test
    fun `exact and concise names rank higher`() {
        val exact = score("skyr", "Skyr")!!
        val long = score("skyr", "Skyr drink with strawberries and vanilla flavour")!!
        val startsWith = score("big mac", "Big Mac")!!
        val contains = score("big mac", "Sauce for Big Mac")!!
        assertTrue(exact > long)
        assertTrue(startsWith > contains)
    }

    @Test
    fun `single letters are ignored unless alone`() {
        assertEquals(listOf("mcdonald"), SearchMatcher.tokens("McDonald's"))
        assertEquals(listOf("a"), SearchMatcher.tokens("a"))
    }

    @Test
    fun `online variants drop the half-typed last word`() {
        assertEquals(listOf("bageterie cae", "bageterie"), SearchMatcher.onlineVariants("bageterie cae"))
        assertEquals(listOf("skyr"), SearchMatcher.onlineVariants("skyr"))
        // A too-short remainder isn't worth a request.
        assertEquals(listOf("mc do"), SearchMatcher.onlineVariants("mc do"))
    }

    @Test
    fun `cyrillic is kept, with yo and short i folded`() {
        assertEquals("яицо куриное", "Яйцо куриное".normalizedForSearch())
        assertEquals("свекла", "Свёкла".normalizedForSearch())
        assertNotNull(score("свекла", "Свёкла, варёная"))
        assertNotNull(score("яйцо", "Яйцо куриное, сырое"))
        assertNull(score("молоко", "Кефир"))
    }

    @Test
    fun `russian word endings are tolerated`() {
        assertNotNull(score("куриная грудка", "Куриная грудка, сырая"))
        assertNotNull(score("курица грудка", "Куриная грудка, сырая"))
        assertNotNull(score("яйца", "Яйцо куриное, сырое"))
    }
}
