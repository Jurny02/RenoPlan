package dev.jurczyk.renoplan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class MoneyTest {
    @Test
    fun convertsAndRoundsToGrosze() {
        assertEquals(Money(1_143), Money.fromZloty(11.43))
        assertEquals(11.43, Money(1_143).zloty)
        // 0.1 + 0.2 zł is exactly 30 gr.
        assertEquals(Money(30), Money.fromZloty(0.1) + Money.fromZloty(0.2))
    }

    @Test
    fun pricePerUnitTimesQuantityRoundsToTheNearestGrosz() {
        assertEquals(Money(16_783), Money.fromZloty(6.0) * 27.97166)
        assertEquals(Money(1), Money(1) * 0.5)
        assertEquals(Money.Zero, Money(1) * 0.49)
    }

    @Test
    fun sumAndComparison() {
        assertEquals(Money(350), listOf(Money(100), Money(250)).sum())
        assertEquals(Money.Zero, emptyList<Money>().sum())
        assertEquals(true, Money(1) < Money(2))
    }

    @Test
    fun operationCannotMixAreaAndLinearSurfaces() {
        assertFailsWith<IllegalArgumentException> {
            Operation(
                Uuid.random(),
                OperationType.Priming,
                setOf(Surface.Floor, Surface.Skirting),
                Money.Zero,
                Labour.SelfDone,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Operation(Uuid.random(), OperationType.Priming, emptySet(), Money.Zero, Labour.SelfDone)
        }
    }

    @Test
    fun unitFollowsTheSurfaces() {
        val skirting =
            Operation(Uuid.random(), OperationType.Skirting, setOf(Surface.Skirting), Money.Zero, Labour.SelfDone)
        val walls =
            Operation(
                Uuid.random(),
                OperationType.SkimCoat,
                setOf(Surface.Wall(1), Surface.Ceiling),
                Money.Zero,
                Labour.SelfDone,
            )

        assertEquals(MeasureUnit.METRE, skirting.unit)
        assertEquals(MeasureUnit.SQUARE_METRE, walls.unit)
    }
}
