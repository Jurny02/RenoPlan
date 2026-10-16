package dev.jurczyk.renoplan.domain.model

import kotlin.uuid.Uuid

/** Finishing work on whole surfaces of a room, see docs/04. */
sealed interface OperationType {
    data class Painting(
        val colour: String,
    ) : OperationType

    data object SkimCoat : OperationType

    data object Priming : OperationType

    data object Tiling : OperationType

    data object WallPanels : OperationType

    data object Skirting : OperationType

    data object CrownMoulding : OperationType

    data class Custom(
        val name: String,
    ) : OperationType
}

enum class OperationStatus {
    TODO,
    IN_PROGRESS,
    DONE,
}

/** How labour is priced. */
sealed interface Labour {
    /** Per m² or per metre, like the material. */
    data class PerUnit(
        val price: Money,
    ) : Labour

    /** For the whole operation. */
    data class LumpSum(
        val amount: Money,
    ) : Labour

    /** "I do it myself": no labour cost. */
    data object SelfDone : Labour
}

/** The unit an operation is measured in; it follows from its surfaces. */
enum class MeasureUnit {
    SQUARE_METRE,
    METRE,
}

/** Skirting and crown moulding are measured in running metres, other surfaces in m². */
val Surface.unit: MeasureUnit
    get() =
        when (this) {
            Surface.Skirting, Surface.CrownMoulding -> MeasureUnit.METRE
            is Surface.Wall, Surface.Floor, Surface.Ceiling -> MeasureUnit.SQUARE_METRE
        }

/**
 * An operation on [surfaces] of one room. Area and linear surfaces cannot be mixed, because the
 * prices are per unit.
 */
data class Operation(
    val id: Uuid,
    val type: OperationType,
    val surfaces: Set<Surface>,
    /** Per [unit]. */
    val materialPrice: Money,
    val labour: Labour,
    val status: OperationStatus = OperationStatus.TODO,
) {
    init {
        require(surfaces.isNotEmpty()) { "An operation needs at least one surface" }
        require(surfaces.map { it.unit }.distinct().size == 1) { "Area and linear surfaces cannot be mixed" }
    }

    val unit: MeasureUnit get() = surfaces.first().unit
}

/**
 * Default type and prices for new operations. Adding an operation from a template copies the
 * values, so editing the template later does not change existing operations.
 */
data class OperationTemplate(
    val id: Uuid,
    val type: OperationType,
    val materialPrice: Money,
    val labour: Labour,
)
