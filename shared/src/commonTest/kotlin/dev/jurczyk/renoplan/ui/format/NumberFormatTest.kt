package dev.jurczyk.renoplan.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    @Test
    fun roundsToTwoDecimalsWithAComma() {
        assertEquals("7,84", formatDecimal(7.84))
        assertEquals("11,40", formatDecimal(11.4))
        assertEquals("0,05", formatDecimal(0.049))
        assertEquals("20,80", formatDecimal(20.8))
        assertEquals("1,00", formatDecimal(0.999))
    }

    @Test
    fun negativeValuesKeepTheirSignUnlessTheyRoundToZero() {
        assertEquals("-1,50", formatDecimal(-1.5))
        assertEquals("0,00", formatDecimal(-0.001))
    }

    @Test
    fun otherPrecisions() {
        assertEquals("3", formatDecimal(2.6, decimals = 0))
        assertEquals("2,655", formatDecimal(2.655, decimals = 3))
    }
}
