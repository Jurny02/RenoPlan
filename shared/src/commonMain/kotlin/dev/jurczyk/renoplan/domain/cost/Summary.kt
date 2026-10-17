package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.geometry.ceilingArea
import dev.jurczyk.renoplan.domain.geometry.crownMouldingLength
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.skirtingLength
import dev.jurczyk.renoplan.domain.geometry.wallsNetArea
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.sum

// Summaries from docs/04-operations-and-costs.md and docs/06-screens.md (screens 7 and 10).

/** How many surfaces of each kind a group of operations covers, e.g. "10 walls, 3 ceilings". */
data class SurfaceCounts(
    val walls: Int = 0,
    val floors: Int = 0,
    val ceilings: Int = 0,
    val skirtings: Int = 0,
    val crownMouldings: Int = 0,
) {
    operator fun plus(other: SurfaceCounts): SurfaceCounts =
        SurfaceCounts(
            walls + other.walls,
            floors + other.floors,
            ceilings + other.ceilings,
            skirtings + other.skirtings,
            crownMouldings + other.crownMouldings,
        )

    companion object {
        fun of(surfaces: Set<Surface>): SurfaceCounts =
            SurfaceCounts(
                walls = surfaces.count { it is Surface.Wall },
                floors = surfaces.count { it == Surface.Floor },
                ceilings = surfaces.count { it == Surface.Ceiling },
                skirtings = surfaces.count { it == Surface.Skirting },
                crownMouldings = surfaces.count { it == Surface.CrownMoulding },
            )
    }
}

/**
 * Operations of one type (painting per colour, custom work per name) and unit, added together, e.g.
 * "Skim coat – 10 walls – 100.00 m²".
 */
data class OperationGroup(
    val type: OperationType,
    val unit: MeasureUnit,
    val surfaces: SurfaceCounts,
    /** m² or m. */
    val quantity: Double,
    val cost: CostSummary,
    val statusCounts: Map<OperationStatus, Int>,
)

/** Groups the operations of [rooms] by type and unit, in the order the types first appear. */
fun groupOperations(rooms: List<Room>): List<OperationGroup> =
    rooms
        .flatMap { room -> room.operations.map { room to it } }
        .groupBy { (_, operation) -> operation.type to operation.unit }
        .map { (key, entries) ->
            val costs = entries.map { (room, operation) -> room.cost(operation) }
            OperationGroup(
                type = key.first,
                unit = key.second,
                surfaces =
                    entries
                        .map { (_, operation) ->
                            SurfaceCounts.of(operation.surfaces)
                        }.fold(SurfaceCounts(), SurfaceCounts::plus),
                quantity = costs.sumOf { it.quantity },
                cost =
                    CostSummary(
                        material = costs.map { it.material }.sum(),
                        labour = costs.map { it.labour }.sum(),
                        done =
                            entries
                                .zip(costs)
                                .filter { (entry, _) -> entry.second.status == OperationStatus.DONE }
                                .map { (_, cost) -> cost.total }
                                .sum(),
                    ),
                statusCounts = entries.groupingBy { (_, operation) -> operation.status }.eachCount(),
            )
        }

val Room.operationGroups: List<OperationGroup> get() = groupOperations(listOf(this))

val Level.operationGroups: List<OperationGroup> get() = groupOperations(rooms)

/** Geometry added up over rooms. Areas in m². */
data class GeometryTotals(
    val floorArea: Double,
    val ceilingArea: Double,
    val wallsNetArea: Double,
    val skirtingLength: Millimetres,
    val crownMouldingLength: Millimetres,
)

fun geometryTotals(rooms: List<Room>): GeometryTotals =
    GeometryTotals(
        floorArea = rooms.sumOf { it.floorArea },
        ceilingArea = rooms.sumOf { it.ceilingArea },
        wallsNetArea = rooms.sumOf { it.wallsNetArea },
        skirtingLength = Millimetres(rooms.sumOf { it.skirtingLength.value }),
        crownMouldingLength = Millimetres(rooms.sumOf { it.crownMouldingLength.value }),
    )

val Level.geometryTotals: GeometryTotals get() = geometryTotals(rooms)
