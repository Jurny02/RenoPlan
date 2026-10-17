package dev.jurczyk.renoplan.data.mapper

import dev.jurczyk.renoplan.data.local.OperationEntity
import dev.jurczyk.renoplan.data.local.TemplateEntity
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Surface
import kotlin.uuid.Uuid

// Text encodings stored in the database. Changing them needs a migration.

private const val SURFACE_SEPARATOR = ","
private const val WALL_PREFIX = "WALL:"
private const val CUSTOM_PREFIX = "CUSTOM:"
private const val CUSTOM_PART_SEPARATOR = ":"

private fun OperationType.kind(): String =
    when (this) {
        is OperationType.Painting -> "PAINTING"
        OperationType.SkimCoat -> "SKIM_COAT"
        OperationType.Priming -> "PRIMING"
        OperationType.Tiling -> "TILING"
        OperationType.WallPanels -> "WALL_PANELS"
        OperationType.Skirting -> "SKIRTING"
        OperationType.CrownMoulding -> "CROWN_MOULDING"
        is OperationType.Custom -> "CUSTOM"
    }

private fun OperationType.text(): String? =
    when (this) {
        is OperationType.Painting -> colour
        is OperationType.Custom -> name
        else -> null
    }

private fun operationType(
    kind: String,
    text: String?,
): OperationType =
    when (kind) {
        "PAINTING" -> OperationType.Painting(text.orEmpty())
        "SKIM_COAT" -> OperationType.SkimCoat
        "PRIMING" -> OperationType.Priming
        "TILING" -> OperationType.Tiling
        "WALL_PANELS" -> OperationType.WallPanels
        "SKIRTING" -> OperationType.Skirting
        "CROWN_MOULDING" -> OperationType.CrownMoulding
        "CUSTOM" -> OperationType.Custom(text.orEmpty())
        else -> error("Unknown operation type $kind")
    }

/** E.g. `WALL:1,WALL:2,CEILING`; a custom quantity is `CUSTOM:SQUARE_METRE:4.5`. */
private fun Set<Surface>.encode(): String =
    joinToString(SURFACE_SEPARATOR) { surface ->
        when (surface) {
            is Surface.Wall -> "$WALL_PREFIX${surface.number}"
            Surface.Floor -> "FLOOR"
            Surface.Ceiling -> "CEILING"
            Surface.Skirting -> "SKIRTING"
            Surface.CrownMoulding -> "CROWN_MOULDING"
            is Surface.Custom -> "$CUSTOM_PREFIX${surface.unit.name}$CUSTOM_PART_SEPARATOR${surface.quantity}"
        }
    }

private fun decodeSurfaces(text: String): Set<Surface> =
    text
        .split(SURFACE_SEPARATOR)
        .map { code ->
            when {
                code.startsWith(WALL_PREFIX) -> {
                    Surface.Wall(code.removePrefix(WALL_PREFIX).toInt())
                }

                code.startsWith(CUSTOM_PREFIX) -> {
                    val (unit, quantity) = code.removePrefix(CUSTOM_PREFIX).split(CUSTOM_PART_SEPARATOR)
                    Surface.Custom(quantity.toDouble(), MeasureUnit.valueOf(unit))
                }

                code == "FLOOR" -> {
                    Surface.Floor
                }

                code == "CEILING" -> {
                    Surface.Ceiling
                }

                code == "SKIRTING" -> {
                    Surface.Skirting
                }

                code == "CROWN_MOULDING" -> {
                    Surface.CrownMoulding
                }

                else -> {
                    error("Unknown surface $code")
                }
            }
        }.toCollection(LinkedHashSet())

private fun Labour.kind(): String =
    when (this) {
        is Labour.PerUnit -> "PER_UNIT"
        is Labour.LumpSum -> "LUMP_SUM"
        Labour.SelfDone -> "SELF_DONE"
    }

private fun Labour.grosze(): Long? =
    when (this) {
        is Labour.PerUnit -> price.grosze
        is Labour.LumpSum -> amount.grosze
        Labour.SelfDone -> null
    }

private fun labour(
    kind: String,
    grosze: Long?,
): Labour =
    when (kind) {
        "PER_UNIT" -> Labour.PerUnit(Money(grosze ?: 0))
        "LUMP_SUM" -> Labour.LumpSum(Money(grosze ?: 0))
        "SELF_DONE" -> Labour.SelfDone
        else -> error("Unknown labour $kind")
    }

fun Operation.toEntity(
    roomId: String,
    position: Int,
): OperationEntity =
    OperationEntity(
        id = id.toString(),
        roomId = roomId,
        position = position,
        typeKind = type.kind(),
        typeText = type.text(),
        surfaces = surfaces.encode(),
        materialPriceGrosze = materialPrice.grosze,
        labourKind = labour.kind(),
        labourGrosze = labour.grosze(),
        status = status.name,
    )

fun OperationEntity.toDomain(): Operation =
    Operation(
        id = Uuid.parse(id),
        type = operationType(typeKind, typeText),
        surfaces = decodeSurfaces(surfaces),
        materialPrice = Money(materialPriceGrosze),
        labour = labour(labourKind, labourGrosze),
        status = OperationStatus.valueOf(status),
    )

/** The DAO sets the position. */
fun OperationTemplate.toEntity(): TemplateEntity =
    TemplateEntity(
        id = id.toString(),
        position = 0,
        typeKind = type.kind(),
        typeText = type.text(),
        materialPriceGrosze = materialPrice.grosze,
        labourKind = labour.kind(),
        labourGrosze = labour.grosze(),
    )

fun TemplateEntity.toDomain(): OperationTemplate =
    OperationTemplate(
        id = Uuid.parse(id),
        type = operationType(typeKind, typeText),
        materialPrice = Money(materialPriceGrosze),
        labour = labour(labourKind, labourGrosze),
    )
