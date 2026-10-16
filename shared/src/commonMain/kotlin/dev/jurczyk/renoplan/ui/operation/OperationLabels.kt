package dev.jurczyk.renoplan.ui.operation

import androidx.compose.runtime.Composable
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.ui.format.formatDecimal
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.labour_lump_sum
import renoplan.shared.generated.resources.labour_per_unit
import renoplan.shared.generated.resources.labour_self_done
import renoplan.shared.generated.resources.list_separator
import renoplan.shared.generated.resources.operation_status_done
import renoplan.shared.generated.resources.operation_status_in_progress
import renoplan.shared.generated.resources.operation_status_todo
import renoplan.shared.generated.resources.operation_type_crown_moulding
import renoplan.shared.generated.resources.operation_type_custom
import renoplan.shared.generated.resources.operation_type_painting
import renoplan.shared.generated.resources.operation_type_priming
import renoplan.shared.generated.resources.operation_type_skim_coat
import renoplan.shared.generated.resources.operation_type_skirting
import renoplan.shared.generated.resources.operation_type_tiling
import renoplan.shared.generated.resources.operation_type_wall_panels
import renoplan.shared.generated.resources.operation_type_with_text
import renoplan.shared.generated.resources.price_per_metre
import renoplan.shared.generated.resources.price_per_square_metre
import renoplan.shared.generated.resources.price_per_unit
import renoplan.shared.generated.resources.room_details_wall
import renoplan.shared.generated.resources.surface_ceiling
import renoplan.shared.generated.resources.surface_crown_moulding
import renoplan.shared.generated.resources.surface_floor
import renoplan.shared.generated.resources.surface_skirting
import renoplan.shared.generated.resources.surface_walls
import renoplan.shared.generated.resources.template_labour_lump_sum
import renoplan.shared.generated.resources.template_labour_self_done
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_square_metres
import renoplan.shared.generated.resources.unit_zloty

val OperationKind.label: StringResource
    get() =
        when (this) {
            OperationKind.PAINTING -> Res.string.operation_type_painting
            OperationKind.SKIM_COAT -> Res.string.operation_type_skim_coat
            OperationKind.PRIMING -> Res.string.operation_type_priming
            OperationKind.TILING -> Res.string.operation_type_tiling
            OperationKind.WALL_PANELS -> Res.string.operation_type_wall_panels
            OperationKind.SKIRTING -> Res.string.operation_type_skirting
            OperationKind.CROWN_MOULDING -> Res.string.operation_type_crown_moulding
            OperationKind.CUSTOM -> Res.string.operation_type_custom
        }

/** E.g. "Malowanie – biały", "Gładź" or the custom name. */
@Composable
fun operationTypeLabel(type: OperationType): String =
    when (type) {
        is OperationType.Painting -> {
            stringResource(
                Res.string.operation_type_with_text,
                stringResource(Res.string.operation_type_painting),
                type.colour,
            )
        }

        OperationType.SkimCoat -> {
            stringResource(Res.string.operation_type_skim_coat)
        }

        OperationType.Priming -> {
            stringResource(Res.string.operation_type_priming)
        }

        OperationType.Tiling -> {
            stringResource(Res.string.operation_type_tiling)
        }

        OperationType.WallPanels -> {
            stringResource(Res.string.operation_type_wall_panels)
        }

        OperationType.Skirting -> {
            stringResource(Res.string.operation_type_skirting)
        }

        OperationType.CrownMoulding -> {
            stringResource(Res.string.operation_type_crown_moulding)
        }

        is OperationType.Custom -> {
            type.name
        }
    }

/** A single surface, e.g. "Ściana 2" or "Sufit". */
@Composable
fun surfaceLabel(surface: Surface): String =
    when (surface) {
        is Surface.Wall -> stringResource(Res.string.room_details_wall, surface.number)
        Surface.Floor -> stringResource(Res.string.surface_floor)
        Surface.Ceiling -> stringResource(Res.string.surface_ceiling)
        Surface.Skirting -> stringResource(Res.string.surface_skirting)
        Surface.CrownMoulding -> stringResource(Res.string.surface_crown_moulding)
    }

/** E.g. "Ściany 1, 2, 4 · Sufit"; a single wall stays "Ściana 1". */
@Composable
fun surfacesLabel(surfaces: Set<Surface>): String {
    val walls = surfaces.filterIsInstance<Surface.Wall>().map { it.number }.sorted()
    val wallsLabel =
        when (walls.size) {
            0 -> null
            1 -> stringResource(Res.string.room_details_wall, walls.single())
            else -> stringResource(Res.string.surface_walls, walls.joinToString())
        }
    val others = surfaces.filterNot { it is Surface.Wall }.map { surfaceLabel(it) }
    return (listOfNotNull(wallsLabel) + others).joinToString(stringResource(Res.string.list_separator))
}

val OperationStatus.label: StringResource
    get() =
        when (this) {
            OperationStatus.TODO -> Res.string.operation_status_todo
            OperationStatus.IN_PROGRESS -> Res.string.operation_status_in_progress
            OperationStatus.DONE -> Res.string.operation_status_done
        }

val LabourMode.label: StringResource
    get() =
        when (this) {
            LabourMode.PER_UNIT -> Res.string.labour_per_unit
            LabourMode.LUMP_SUM -> Res.string.labour_lump_sum
            LabourMode.SELF_DONE -> Res.string.labour_self_done
        }

/** E.g. "735,00 zł". */
@Composable
fun moneyLabel(money: Money): String = stringResource(Res.string.unit_zloty, formatDecimal(money.zloty))

/** E.g. "6,00 zł/m²"; without a unit, "6,00 zł/jedn.". */
@Composable
fun unitPriceLabel(
    price: Money,
    unit: MeasureUnit?,
): String {
    val amount = formatDecimal(price.zloty)
    return when (unit) {
        MeasureUnit.SQUARE_METRE -> stringResource(Res.string.price_per_square_metre, amount)
        MeasureUnit.METRE -> stringResource(Res.string.price_per_metre, amount)
        null -> stringResource(Res.string.price_per_unit, amount)
    }
}

/** E.g. "15,00 zł/jedn.", "1500,00 zł ryczałt" or "robię sam". */
@Composable
fun labourLabel(
    labour: Labour,
    unit: MeasureUnit?,
): String =
    when (labour) {
        is Labour.PerUnit -> unitPriceLabel(labour.price, unit)
        is Labour.LumpSum -> stringResource(Res.string.template_labour_lump_sum, moneyLabel(labour.amount))
        Labour.SelfDone -> stringResource(Res.string.template_labour_self_done)
    }

/** E.g. "12,00 m²" or "14,00 m". */
@Composable
fun quantityLabel(
    quantity: Double,
    unit: MeasureUnit,
): String =
    when (unit) {
        MeasureUnit.SQUARE_METRE -> stringResource(Res.string.unit_square_metres, formatDecimal(quantity))
        MeasureUnit.METRE -> stringResource(Res.string.unit_metres, formatDecimal(quantity))
    }
