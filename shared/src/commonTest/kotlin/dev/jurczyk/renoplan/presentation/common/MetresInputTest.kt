package dev.jurczyk.renoplan.presentation.common

import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetresInputTest {
    @Test
    fun parsesCommaAndDot() {
        assertEquals(2_650.mm, parseMetres("2,65"))
        assertEquals(2_650.mm, parseMetres(" 2.65 "))
        assertEquals(3_000.mm, parseMetres("3"))
        assertEquals(400.mm, parseMetres(",4"))
    }

    @Test
    fun roundsToWholeMillimetres() {
        assertEquals(2_655.mm, parseMetres("2,6549"))
    }

    @Test
    fun rejectsBlankMalformedAndNonPositive() {
        assertNull(parseMetres(""))
        assertNull(parseMetres("abc"))
        assertNull(parseMetres("2,6,5"))
        assertNull(parseMetres("0"))
        assertNull(parseMetres("-1"))
        assertNull(parseMetres("NaN"))
        assertNull(parseMetres("Infinity"))
    }

    @Test
    fun formatsWithoutTrailingZeros() {
        assertEquals("2,6", 2_600.mm.toMetresInput())
        assertEquals("2,65", 2_650.mm.toMetresInput())
        assertEquals("2,655", 2_655.mm.toMetresInput())
        assertEquals("3", 3_000.mm.toMetresInput())
        assertEquals("0,04", 40.mm.toMetresInput())
    }

    @Test
    fun squareMetres() {
        assertEquals(1.25, parseSquareMetres("1,25"))
        assertEquals(1.5, parseSquareMetres(" 1.5 "))
        assertNull(parseSquareMetres("0"))
        assertNull(parseSquareMetres("x"))
        assertEquals("1,5", 1.5.toSquareMetresInput())
        assertEquals("2", 2.0.toSquareMetresInput())
        assertEquals("0,05", 0.049.toSquareMetresInput())
    }

    @Test
    fun money() {
        assertEquals(Money(1_143), parseMoney("11,43"))
        assertEquals(Money(650), parseMoney("6.5"))
        assertEquals(Money.Zero, parseMoney(""))
        assertEquals(Money.Zero, parseMoney("0"))
        assertNull(parseMoney("-1"))
        assertNull(parseMoney("abc"))
        assertEquals("6,5", Money(650).toMoneyInput())
        assertEquals("11,43", Money(1_143).toMoneyInput())
        assertEquals("1500", Money(150_000).toMoneyInput())
        assertEquals("", Money.Zero.toMoneyInput())
    }
}
