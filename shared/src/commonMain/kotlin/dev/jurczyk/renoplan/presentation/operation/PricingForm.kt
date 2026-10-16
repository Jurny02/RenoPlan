package dev.jurczyk.renoplan.presentation.operation

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.presentation.common.parseMoney
import dev.jurczyk.renoplan.presentation.common.toMoneyInput

/** Operation types as the editor lists them; Painting and Custom also need a text. */
enum class OperationKind {
    PAINTING,
    SKIM_COAT,
    PRIMING,
    TILING,
    WALL_PANELS,
    SKIRTING,
    CROWN_MOULDING,
    CUSTOM,
}

enum class LabourMode {
    PER_UNIT,
    LUMP_SUM,
    SELF_DONE,
}

/**
 * Type and prices, shared by the operation editor and the template editor. Prices are typed in
 * złoty; a blank price counts as 0 zł.
 */
data class PricingForm(
    val kind: OperationKind = OperationKind.PAINTING,
    /** Painting colour or custom name. */
    val text: String = "",
    val materialPrice: String = "",
    val labourMode: LabourMode = LabourMode.PER_UNIT,
    val labourPrice: String = "",
) {
    val needsText: Boolean get() = kind == OperationKind.PAINTING || kind == OperationKind.CUSTOM

    val type: OperationType?
        get() {
            val text = text.trim()
            return when (kind) {
                OperationKind.PAINTING -> text.takeIf { it.isNotEmpty() }?.let { OperationType.Painting(it) }
                OperationKind.SKIM_COAT -> OperationType.SkimCoat
                OperationKind.PRIMING -> OperationType.Priming
                OperationKind.TILING -> OperationType.Tiling
                OperationKind.WALL_PANELS -> OperationType.WallPanels
                OperationKind.SKIRTING -> OperationType.Skirting
                OperationKind.CROWN_MOULDING -> OperationType.CrownMoulding
                OperationKind.CUSTOM -> text.takeIf { it.isNotEmpty() }?.let { OperationType.Custom(it) }
            }
        }

    val material: Money? get() = parseMoney(materialPrice)

    val labour: Labour?
        get() =
            when (labourMode) {
                LabourMode.PER_UNIT -> parseMoney(labourPrice)?.let { Labour.PerUnit(it) }
                LabourMode.LUMP_SUM -> parseMoney(labourPrice)?.let { Labour.LumpSum(it) }
                LabourMode.SELF_DONE -> Labour.SelfDone
            }

    val materialPriceError: Boolean get() = material == null
    val labourPriceError: Boolean get() = labourMode != LabourMode.SELF_DONE && labour == null

    val isValid: Boolean get() = type != null && material != null && labour != null

    companion object {
        fun from(
            type: OperationType,
            materialPrice: Money,
            labour: Labour,
        ): PricingForm {
            val (kind, text) =
                when (type) {
                    is OperationType.Painting -> OperationKind.PAINTING to type.colour
                    OperationType.SkimCoat -> OperationKind.SKIM_COAT to ""
                    OperationType.Priming -> OperationKind.PRIMING to ""
                    OperationType.Tiling -> OperationKind.TILING to ""
                    OperationType.WallPanels -> OperationKind.WALL_PANELS to ""
                    OperationType.Skirting -> OperationKind.SKIRTING to ""
                    OperationType.CrownMoulding -> OperationKind.CROWN_MOULDING to ""
                    is OperationType.Custom -> OperationKind.CUSTOM to type.name
                }
            val (mode, price) =
                when (labour) {
                    is Labour.PerUnit -> LabourMode.PER_UNIT to labour.price.toMoneyInput()
                    is Labour.LumpSum -> LabourMode.LUMP_SUM to labour.amount.toMoneyInput()
                    Labour.SelfDone -> LabourMode.SELF_DONE to ""
                }
            return PricingForm(kind, text, materialPrice.toMoneyInput(), mode, price)
        }
    }
}
