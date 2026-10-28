package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.WeightEntry
import com.jsemalja.trackmecalories.data.adaptiveMaintenance
import com.jsemalja.trackmecalories.data.weightStats
import com.jsemalja.trackmecalories.data.weightTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.LocalDate

class WeightMathTest {
    private val today = LocalDate(2026, 10, 25)

    private fun entries(vararg kgByDaysAgo: Pair<Long, Double>) =
        kgByDaysAgo.map { (ago, kg) -> WeightEntry(date = today.minusDays(ago).toString(), weightKg = kg) }

    @Test
    fun `trend damps a one-day spike`() {
        val points = weightTrend(entries(3L to 80.0, 2L to 80.0, 1L to 82.0, 0L to 80.0))
        // A 2 kg water spike moves the trend only ~0.2 kg.
        assertEquals(80.2, points[2].trend, 0.01)
        assertTrue(points.last().trend < 80.2)
    }

    @Test
    fun `a gap of several days moves the trend further`() {
        val oneDay = weightTrend(entries(1L to 80.0, 0L to 78.0)).last().trend
        val week = weightTrend(entries(7L to 80.0, 0L to 78.0)).last().trend
        assertTrue(week < oneDay)
    }

    @Test
    fun `steady loss gives weekly rate and goal date`() {
        // -0.1 kg/day for 28 days = -0.7 kg/week.
        val list = (28L downTo 0L).map { it to 80.0 - (28 - it) * 0.1 }.toTypedArray()
        val stats = weightStats(weightTrend(entries(*list)), goalKg = 70.0, today = today)!!
        assertEquals(-0.7, stats.weeklyRate!!, 0.15)
        assertNotNull(stats.projectedGoalDate)
        assertTrue(stats.projectedGoalDate!!.isAfter(today))
    }

    @Test
    fun `no goal date when moving away from the goal`() {
        val list = (14L downTo 0L).map { it to 80.0 + (14 - it) * 0.05 }.toTypedArray()
        val stats = weightStats(weightTrend(entries(*list)), goalKg = 75.0, today = today)!!
        assertNull(stats.projectedGoalDate)
    }

    @Test
    fun `measured maintenance from intake and trend`() {
        // Stable weight while eating 2200/day -> maintenance ~2200.
        val list = (27L downTo 0L).map { it to 75.0 }.toTypedArray()
        val intake = (27L downTo 1L).associate { today.minusDays(it) to 2200 }
        val m = adaptiveMaintenance(weightTrend(entries(*list)), intake, today)!!
        assertEquals(2200, m.kcal)
    }

    @Test
    fun `measured maintenance accounts for losing weight`() {
        // Losing 0.5 kg/week (~550 kcal/day deficit) on 1700 kcal -> maintenance ~2250.
        val list = (27L downTo 0L).map { it to 80.0 - (27 - it) * (0.5 / 7) }.toTypedArray()
        val intake = (27L downTo 1L).associate { today.minusDays(it) to 1700 }
        val m = adaptiveMaintenance(weightTrend(entries(*list)), intake, today)!!
        assertEquals(2250.0, m.kcal.toDouble(), 120.0)
    }

    @Test
    fun `measured maintenance needs enough data`() {
        val list = (5L downTo 0L).map { it to 75.0 }.toTypedArray()
        val intake = (5L downTo 1L).associate { today.minusDays(it) to 2000 }
        assertNull(adaptiveMaintenance(weightTrend(entries(*list)), intake, today))
    }
}
