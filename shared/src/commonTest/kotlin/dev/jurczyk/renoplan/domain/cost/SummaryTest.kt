package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class SummaryTest {
    private fun operation(
        type: OperationType,
        vararg surfaces: Surface,
        material: Double = 0.0,
        labour: Labour = Labour.SelfDone,
        status: OperationStatus = OperationStatus.TODO,
    ) = Operation(Uuid.random(), type, surfaces.toSet(), Money.fromZloty(material), labour, status)

    private val allWalls = (1..4).map { Surface.Wall(it) }.toTypedArray()

    /** 4 × 3 m, 2.5 m high: walls 35 m², floor 12 m², perimeter 14 m. */
    private val living =
        Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    operation(OperationType.SkimCoat, *allWalls, material = 2.0, status = OperationStatus.DONE),
                    operation(OperationType.Painting("biały"), *allWalls, Surface.Ceiling, material = 6.0),
                    operation(
                        OperationType.Skirting,
                        Surface.Skirting,
                        material = 25.0,
                        labour = Labour.LumpSum(Money.fromZloty(100.0)),
                    ),
                ),
        )

    /** 2 × 2 m, 2.5 m high: walls 20 m², floor 4 m², perimeter 8 m. */
    private val bathroom =
        Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 2_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    operation(OperationType.SkimCoat, *allWalls, material = 2.0, status = OperationStatus.IN_PROGRESS),
                    operation(OperationType.Painting("szary"), Surface.Ceiling, material = 6.0),
                    operation(OperationType.Tiling, Surface.Floor, material = 80.0),
                ),
        )

    private val level = Level(Uuid.random(), "Parter", listOf(living, bathroom))

    @Test
    fun wholeLevelGroupsByTypeAndKeepsColoursApart() {
        val groups = level.operationGroups

        assertEquals(
            listOf(
                OperationType.SkimCoat,
                OperationType.Painting("biały"),
                OperationType.Skirting,
                OperationType.Painting("szary"),
                OperationType.Tiling,
            ),
            groups.map { it.type },
        )
        val skimCoat = groups.first()
        assertEquals(SurfaceCounts(walls = 8), skimCoat.surfaces)
        assertEquals(55.0, skimCoat.quantity, 1e-9)
        assertEquals(Money.fromZloty(110.0), skimCoat.cost.total)
        assertEquals(Money.fromZloty(70.0), skimCoat.cost.done)
        assertEquals(mapOf(OperationStatus.DONE to 1, OperationStatus.IN_PROGRESS to 1), skimCoat.statusCounts)
    }

    @Test
    fun groupKnowsItsUnitAndSurfaces() {
        val (_, white, skirting) = living.operationGroups

        assertEquals(SurfaceCounts(walls = 4, ceilings = 1), white.surfaces)
        assertEquals(47.0, white.quantity, 1e-9)
        assertEquals(MeasureUnit.METRE, skirting.unit)
        assertEquals(14.0, skirting.quantity, 1e-9)
        assertEquals(Money.fromZloty(350.0), skirting.cost.material)
        assertEquals(Money.fromZloty(100.0), skirting.cost.labour)
    }

    @Test
    fun levelCostAddsUpRooms() {
        val summary = level.costSummary

        // Living: 70 + 282 + 450; bathroom: 40 + 24 + 320.
        assertEquals(Money.fromZloty(1_186.0), summary.total)
        assertEquals(Money.fromZloty(1_086.0), summary.material)
        assertEquals(Money.fromZloty(100.0), summary.labour)
        assertEquals(Money.fromZloty(70.0), summary.done)
        assertEquals(summary.total, living.costSummary.total + bathroom.costSummary.total)
    }

    @Test
    fun geometryTotalsAddUpRooms() {
        val totals = level.geometryTotals

        assertEquals(16.0, totals.floorArea, 1e-9)
        assertEquals(16.0, totals.ceilingArea, 1e-9)
        assertEquals(55.0, totals.wallsNetArea, 1e-9)
        assertEquals(22_000.mm, totals.skirtingLength)
        assertEquals(22_000.mm, totals.crownMouldingLength)
    }

    @Test
    fun emptyLevel() {
        val empty = Level(Uuid.random(), "Piętro")

        assertEquals(emptyList(), empty.operationGroups)
        assertEquals(CostSummary.Zero, empty.costSummary)
        assertEquals(0.0, empty.geometryTotals.floorArea)
    }
}
