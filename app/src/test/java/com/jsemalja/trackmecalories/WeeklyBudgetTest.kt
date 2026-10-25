package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.weeklyBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyBudgetTest {
    private val monday = LocalDate.of(2026, 10, 19)

    @Test
    fun `monday gets the plain goal`() {
        val b = weeklyBudget(2000, monday, monday, emptyMap())
        assertEquals(2000, b.todayTarget)
        assertEquals(14000, b.weekLeft)
        assertEquals(7, b.daysLeft)
    }

    @Test
    fun `savings are spread over the remaining days`() {
        // Ate 1700 Mon-Wed (900 saved); Thursday has 4 days left -> +225/day.
        val intake = (0L..2L).associate { monday.plusDays(it) to 1700 }
        val b = weeklyBudget(2000, monday, monday.plusDays(3), intake)
        assertEquals(2225, b.todayTarget)
        assertFalse(b.capped)
    }

    @Test
    fun `a big saturday nudges sunday within the limit`() {
        val intake = (0L..4L).associate { monday.plusDays(it) to 2000 } + (monday.plusDays(5) to 3500)
        val b = weeklyBudget(2000, monday, monday.plusDays(6), intake)
        // 500 left for Sunday would be harsh; capped at -25% = 1500.
        assertEquals(1500, b.todayTarget)
        assertEquals(500, b.weekLeft)
        assertTrue(b.capped)
    }

    @Test
    fun `unlogged and incomplete days are not savings`() {
        val intake = mapOf(monday to 900) // Monday incomplete, Tuesday nothing logged
        val b = weeklyBudget(2000, monday, monday.plusDays(2), intake, incompleteDays = setOf(monday))
        assertEquals(2000, b.todayTarget)
    }
}
