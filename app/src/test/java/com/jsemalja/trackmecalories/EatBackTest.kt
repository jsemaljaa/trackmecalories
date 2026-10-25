package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.EatBack
import org.junit.Assert.assertEquals
import org.junit.Test

class EatBackTest {
    @Test
    fun `off adds nothing`() {
        assertEquals(0, EatBack.OFF.bonus(500))
    }

    @Test
    fun `half and full`() {
        assertEquals(250, EatBack.HALF.bonus(500))
        assertEquals(500, EatBack.FULL.bonus(500))
        // Rounds down: never credit more than was burned.
        assertEquals(150, EatBack.HALF.bonus(301))
    }

    @Test
    fun `no activity read means no bonus`() {
        assertEquals(0, EatBack.FULL.bonus(null))
        assertEquals(0, EatBack.FULL.bonus(-20))
    }
}
