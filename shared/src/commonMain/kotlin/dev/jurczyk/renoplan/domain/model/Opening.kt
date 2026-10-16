package dev.jurczyk.renoplan.domain.model

import kotlin.uuid.Uuid

/** A window, door, passage or niche in a wall. Its position on the wall is irrelevant. */
data class Opening(
    val id: Uuid,
    val type: OpeningType,
    val size: OpeningSize,
    val startsAtFloor: Boolean = type.startsAtFloorByDefault,
) {
    init {
        require(!startsAtFloor || size is OpeningSize.Dimensions) {
            "An opening that starts at the floor needs a width to shorten the skirting"
        }
    }
}

enum class OpeningType(
    val startsAtFloorByDefault: Boolean,
) {
    WINDOW(startsAtFloorByDefault = false),
    DOOR(startsAtFloorByDefault = true),
    PASSAGE(startsAtFloorByDefault = true),
    NICHE(startsAtFloorByDefault = false),
}

sealed interface OpeningSize {
    data class Dimensions(
        val width: Millimetres,
        val height: Millimetres,
    ) : OpeningSize

    data class Area(
        val squareMetres: Double,
    ) : OpeningSize
}
