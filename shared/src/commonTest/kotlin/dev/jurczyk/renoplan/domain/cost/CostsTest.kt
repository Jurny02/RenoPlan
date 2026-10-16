package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class CostsTest {
    private val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))

    /** 4 × 3 m, 2.5 m high; walls 1 and 3: 10 m², walls 2 and 4: 7.5 m²; a 1.6 m² door on wall 1. */
    private val room = Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm).withOpening(1, door)

    private fun operation(
        vararg surfaces: Surface,
        material: Double = 0.0,
        labour: Labour = Labour.SelfDone,
        status: OperationStatus = OperationStatus.TODO,
    ) = Operation(
        Uuid.random(),
        OperationType.Painting("biały"),
        surfaces.toSet(),
        Money.fromZloty(material),
        labour,
        status,
    )

    @Test
    fun quantityPerSurface() {
        assertEquals(8.4, room.quantity(Surface.Wall(1)), 1e-9)
        assertEquals(7.5, room.quantity(Surface.Wall(2)), 1e-9)
        assertEquals(12.0, room.quantity(Surface.Floor), 1e-9)
        assertEquals(12.0, room.quantity(Surface.Ceiling), 1e-9)
        assertEquals(13.2, room.quantity(Surface.Skirting), 1e-9)
        assertEquals(14.0, room.quantity(Surface.CrownMoulding), 1e-9)
    }

    @Test
    fun wallThatNoLongerExistsCountsAsZero() {
        assertEquals(0.0, room.quantity(Surface.Wall(7)))
    }

    @Test
    fun materialAndLabourPerUnit() {
        // All walls: 8.4 + 7.5 + 10 + 7.5 = 33.4 m².
        val painting =
            operation(
                Surface.Wall(1),
                Surface.Wall(2),
                Surface.Wall(3),
                Surface.Wall(4),
                material = 6.0,
                labour = Labour.PerUnit(Money.fromZloty(15.0)),
            )

        val cost = room.cost(painting)

        assertEquals(33.4, cost.quantity, 1e-9)
        assertEquals(MeasureUnit.SQUARE_METRE, cost.unit)
        assertEquals(Money.fromZloty(200.40), cost.material)
        assertEquals(Money.fromZloty(501.00), cost.labour)
        assertEquals(Money.fromZloty(701.40), cost.total)
    }

    @Test
    fun lumpSumLabourDoesNotDependOnQuantity() {
        val tiling = operation(Surface.Floor, material = 80.0, labour = Labour.LumpSum(Money.fromZloty(1_500.0)))

        val cost = room.cost(tiling)

        assertEquals(Money.fromZloty(960.0), cost.material)
        assertEquals(Money.fromZloty(1_500.0), cost.labour)
    }

    @Test
    fun selfDoneLabourIsFree() {
        val skirting = operation(Surface.Skirting, material = 25.0)

        val cost = room.cost(skirting)

        assertEquals(MeasureUnit.METRE, cost.unit)
        assertEquals(Money.fromZloty(330.0), cost.material)
        assertEquals(Money.Zero, cost.labour)
    }

    @Test
    fun changingTheRoomChangesTheCost() {
        val floor = operation(Surface.Floor, material = 10.0)
        val bigger = room.withWalls(ShapeInput.Rectangle(5_000.mm, 3_000.mm).toWalls())

        assertEquals(Money.fromZloty(120.0), room.cost(floor).total)
        assertEquals(Money.fromZloty(150.0), bigger.cost(floor).total)
    }

    @Test
    fun summaryOfDoneAndRemaining() {
        val done = operation(Surface.Floor, material = 10.0, status = OperationStatus.DONE)
        val inProgress = operation(Surface.Ceiling, material = 20.0, status = OperationStatus.IN_PROGRESS)
        val summary = room.copy(operations = listOf(done, inProgress)).costSummary

        assertEquals(Money.fromZloty(360.0), summary.total)
        assertEquals(Money.fromZloty(120.0), summary.done)
        assertEquals(Money.fromZloty(240.0), summary.remaining)
        assertEquals(1.0 / 3, summary.progress, 1e-9)
    }

    @Test
    fun emptyRoomCostsNothing() {
        val summary = room.costSummary

        assertEquals(Money.Zero, summary.total)
        assertEquals(0.0, summary.progress)
    }
}
