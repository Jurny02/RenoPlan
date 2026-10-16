package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.geometry.ceilingArea
import dev.jurczyk.renoplan.domain.geometry.crownMouldingLength
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.skirtingLength
import dev.jurczyk.renoplan.domain.geometry.wallNetArea
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.sum

// Formulas from docs/04-operations-and-costs.md.

/** Net area in m² (walls, floor, ceiling) or length in m (mouldings). A wall that no longer exists counts as 0. */
fun Room.quantity(surface: Surface): Double =
    when (surface) {
        is Surface.Wall -> walls.find { it.number == surface.number }?.let { wallNetArea(it) } ?: 0.0
        Surface.Floor -> floorArea
        Surface.Ceiling -> ceilingArea
        Surface.Skirting -> skirtingLength.metres
        Surface.CrownMoulding -> crownMouldingLength.metres
    }

data class OperationCost(
    val quantity: Double,
    val unit: MeasureUnit,
    val material: Money,
    val labour: Money,
) {
    val total: Money get() = material + labour
}

fun Room.cost(operation: Operation): OperationCost {
    val quantity = operation.surfaces.sumOf { quantity(it) }
    val labour =
        when (val labour = operation.labour) {
            is Labour.PerUnit -> labour.price * quantity
            is Labour.LumpSum -> labour.amount
            Labour.SelfDone -> Money.Zero
        }
    return OperationCost(
        quantity = quantity,
        unit = operation.unit,
        material = operation.materialPrice * quantity,
        labour = labour,
    )
}

/** Cost of the room's operations: everything, done, and the rest. */
data class CostSummary(
    val total: Money,
    val done: Money,
) {
    val remaining: Money get() = total - done

    /** Share of the cost that is done, 0..1; 0 when nothing costs anything. */
    val progress: Double get() = if (total.grosze == 0L) 0.0 else done.grosze.toDouble() / total.grosze
}

val Room.costSummary: CostSummary
    get() {
        val costs = operations.map { it to cost(it).total }
        return CostSummary(
            total = costs.map { it.second }.sum(),
            done = costs.filter { it.first.status == OperationStatus.DONE }.map { it.second }.sum(),
        )
    }
