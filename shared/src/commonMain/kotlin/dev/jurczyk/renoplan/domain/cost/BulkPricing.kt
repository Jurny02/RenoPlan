package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room

// "Edit price for all" and "I do it myself" for a whole group (docs/06-screens.md, screen 10).

/** Which operations a bulk change applies to: one type (painting per colour) in one unit. */
data class OperationGroupKey(
    val type: OperationType,
    val unit: MeasureUnit,
) {
    fun matches(operation: Operation): Boolean = operation.type == type && operation.unit == unit
}

/** The room with [material] and [labour] set on every operation of the group; other operations are kept. */
fun Room.withPrices(
    group: OperationGroupKey,
    material: Money,
    labour: Labour,
): Room =
    copy(
        operations =
            operations.map { operation ->
                if (group.matches(operation)) operation.copy(materialPrice = material, labour = labour) else operation
            },
    )

/** The level with [material] and [labour] set on every operation of the group, in every room. */
fun Level.withPrices(
    group: OperationGroupKey,
    material: Money,
    labour: Labour,
): Level = copy(rooms = rooms.map { it.withPrices(group, material, labour) })

/** The level with self-done labour on every operation of the group, keeping material prices. */
fun Level.withSelfDoneLabour(group: OperationGroupKey): Level =
    copy(
        rooms =
            rooms.map { room ->
                room.copy(
                    operations =
                        room.operations.map { operation ->
                            if (group.matches(operation)) operation.copy(labour = Labour.SelfDone) else operation
                        },
                )
            },
    )

fun Level.operationCount(group: OperationGroupKey): Int = rooms.sumOf { room -> room.operations.count(group::matches) }

/** Cost of the group's operations on this level. */
fun Level.groupCost(group: OperationGroupKey): CostSummary =
    rooms
        .flatMap { room -> room.operations.filter(group::matches).map { room to it } }
        .map { (room, operation) ->
            val cost = room.cost(operation)
            CostSummary(cost.material, cost.labour, done = Money.Zero)
        }.fold(CostSummary.Zero, CostSummary::plus)
