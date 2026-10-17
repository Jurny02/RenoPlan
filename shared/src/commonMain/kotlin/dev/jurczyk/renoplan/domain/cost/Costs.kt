package dev.jurczyk.renoplan.domain.cost

import dev.jurczyk.renoplan.domain.geometry.ceilingArea
import dev.jurczyk.renoplan.domain.geometry.crownMouldingLength
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.skirtingLength
import dev.jurczyk.renoplan.domain.geometry.wallNetArea
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface

// Formulas from docs/04-operations-and-costs.md.

/** Net area in m² (walls, floor, ceiling) or length in m (mouldings). A wall that no longer exists counts as 0. */
fun Room.quantity(surface: Surface): Double =
    when (surface) {
        is Surface.Wall -> walls.find { it.number == surface.number }?.let { wallNetArea(it) } ?: 0.0
        Surface.Floor -> floorArea
        Surface.Ceiling -> ceilingArea
        Surface.Skirting -> skirtingLength.metres
        Surface.CrownMoulding -> crownMouldingLength.metres
        is Surface.Custom -> surface.quantity
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

/** Cost of a set of operations: material and labour, how much is done, and the rest. */
data class CostSummary(
    val material: Money,
    val labour: Money,
    val done: Money,
) {
    val total: Money get() = material + labour
    val remaining: Money get() = total - done

    /** Share of the cost that is done, 0..1; 0 when nothing costs anything. */
    val progress: Double get() = if (total.grosze == 0L) 0.0 else done.grosze.toDouble() / total.grosze

    operator fun plus(other: CostSummary): CostSummary =
        CostSummary(material + other.material, labour + other.labour, done + other.done)

    companion object {
        val Zero = CostSummary(Money.Zero, Money.Zero, Money.Zero)
    }
}

val Room.costSummary: CostSummary
    get() =
        operations
            .map { operation ->
                val cost = cost(operation)
                val done = if (operation.status == OperationStatus.DONE) cost.total else Money.Zero
                CostSummary(cost.material, cost.labour, done)
            }.fold(CostSummary.Zero, CostSummary::plus)

val Level.costSummary: CostSummary get() = rooms.map { it.costSummary }.fold(CostSummary.Zero, CostSummary::plus)
