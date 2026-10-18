package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class BulkPricingTest {
    private val white = OperationGroupKey(OperationType.Painting("biały"), MeasureUnit.SQUARE_METRE)

    private fun op(
        type: OperationType,
        surface: Surface,
        material: Double,
        labour: Labour = Labour.PerUnit(Money.fromZloty(15.0)),
    ) = Operation(Uuid.random(), type, setOf(surface), Money.fromZloty(material), labour)

    /** Floors 12 m² and 4 m². */
    private val living =
        Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    op(OperationType.Painting("biały"), Surface.Ceiling, 6.0),
                    op(OperationType.Painting("szary"), Surface.Floor, 6.0),
                ),
        )
    private val bathroom =
        Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 2_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    op(OperationType.Painting("biały"), Surface.Ceiling, 8.0, Labour.LumpSum(Money.fromZloty(100.0))),
                ),
        )
    private val level = Level(Uuid.random(), "Parter", listOf(living, bathroom))

    @Test
    fun countAndCostOfAGroup() {
        assertEquals(2, level.operationCount(white))
        // Living: 12 · (6 + 15) = 252; bathroom: 4 · 8 + 100 = 132.
        assertEquals(Money.fromZloty(384.0), level.groupCost(white).total)
    }

    @Test
    fun editPriceForAllChangesOnlyTheGroup() {
        val changed = level.withPrices(white, Money.fromZloty(5.0), Labour.PerUnit(Money.fromZloty(10.0)))

        assertEquals(Money.fromZloty(240.0), changed.groupCost(white).total)
        val grey = changed.rooms[0].operations[1]
        assertEquals(Money.fromZloty(6.0), grey.materialPrice)
        assertEquals(Labour.PerUnit(Money.fromZloty(15.0)), grey.labour)
    }

    @Test
    fun selfDoneForAGroupKeepsMaterialAndShowsTheSaving() {
        val before = level.groupCost(white)
        val after = level.withSelfDoneLabour(white).groupCost(white)

        assertEquals(before.material, after.material)
        assertEquals(Money.Zero, after.labour)
        assertEquals(Money.fromZloty(280.0), before.total - after.total)
    }

    @Test
    fun unitIsPartOfTheGroup() {
        val linear = OperationGroupKey(OperationType.Painting("biały"), MeasureUnit.METRE)

        assertEquals(0, level.operationCount(linear))
        assertEquals(level, level.withSelfDoneLabour(linear))
    }
}
