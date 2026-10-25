package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.packageAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackageAmountTest {
    @Test
    fun `plain grams and millilitres`() {
        assertEquals(350.0, packageAmount("350 g", "g"))
        assertEquals(250.0, packageAmount("250ml", "ml"))
    }

    @Test
    fun `larger units are converted`() {
        assertEquals(500.0, packageAmount("0,5 l", "ml"))
        assertEquals(1000.0, packageAmount("1 kg", "g"))
        assertEquals(330.0, packageAmount("33 cl", "ml"))
    }

    @Test
    fun `multipacks give one item`() {
        assertEquals(125.0, packageAmount("4 x 125 g", "g"))
        assertEquals(125.0, packageAmount("4×125g", "g"))
    }

    @Test
    fun `nothing usable`() {
        assertNull(packageAmount(null, "g"))
        assertNull(packageAmount("1 pack", "g"))
        assertNull(packageAmount("350 g", "pcs"))
    }
}
