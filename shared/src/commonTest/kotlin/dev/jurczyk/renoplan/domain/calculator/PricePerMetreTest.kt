package dev.jurczyk.renoplan.domain.calculator

import dev.jurczyk.renoplan.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PricePerMetreTest {
    @Test
    fun skirtingStripOf2Point5Metres() {
        assertEquals(Money.fromZloty(20.0), pricePerMetre(Money.fromZloty(50.0), metres = 2.5))
    }

    @Test
    fun roundsToTheNearestGrosz() {
        // 39.99 zł / 2.4 m = 16.6625 zł/m.
        assertEquals(Money(1_666), pricePerMetre(Money.fromZloty(39.99), metres = 2.4))
    }

    @Test
    fun rejectsZeroMetres() {
        assertFailsWith<IllegalArgumentException> { pricePerMetre(Money.fromZloty(10.0), metres = 0.0) }
    }
}
