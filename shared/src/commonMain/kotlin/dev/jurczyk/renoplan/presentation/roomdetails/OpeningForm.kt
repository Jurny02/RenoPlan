package dev.jurczyk.renoplan.presentation.roomdetails

import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.presentation.common.parseMetres
import dev.jurczyk.renoplan.presentation.common.parseSquareMetres
import dev.jurczyk.renoplan.presentation.common.toMetresInput
import dev.jurczyk.renoplan.presentation.common.toSquareMetresInput
import kotlin.uuid.Uuid

enum class SizeMode {
    DIMENSIONS,
    AREA,
}

/** The opening sheet: adds an opening, or edits the one with [id]. Sizes are typed in metres / m². */
data class OpeningForm(
    val id: Uuid,
    val isNew: Boolean,
    val wallNumber: Int,
    val type: OpeningType = OpeningType.WINDOW,
    val sizeMode: SizeMode = SizeMode.DIMENSIONS,
    val width: String = "",
    val height: String = "",
    val area: String = "",
    val startsAtFloor: Boolean = type.startsAtFloorByDefault,
) {
    val widthError: Boolean get() = width.isNotBlank() && parseMetres(width) == null
    val heightError: Boolean get() = height.isNotBlank() && parseMetres(height) == null
    val areaError: Boolean get() = area.isNotBlank() && parseSquareMetres(area) == null

    /** An opening at the floor needs a width for the skirting, so it must be given by dimensions. */
    val canUseArea: Boolean get() = !startsAtFloor

    val size: OpeningSize?
        get() =
            when (sizeMode) {
                SizeMode.DIMENSIONS -> {
                    val w = parseMetres(width)
                    val h = parseMetres(height)
                    if (w != null && h != null) OpeningSize.Dimensions(w, h) else null
                }

                SizeMode.AREA -> {
                    parseSquareMetres(area)?.let { OpeningSize.Area(it) }
                }
            }

    /** The opening as it would be saved, or `null` while the size is incomplete. */
    val opening: Opening? get() = size?.let { Opening(id = id, type = type, size = it, startsAtFloor = startsAtFloor) }

    /** Changing the type resets "starts at floor" to the type's default. */
    fun withType(type: OpeningType): OpeningForm = copy(type = type).withStartsAtFloor(type.startsAtFloorByDefault)

    /** Turning "starts at floor" on switches to dimensions. */
    fun withStartsAtFloor(startsAtFloor: Boolean): OpeningForm =
        copy(startsAtFloor = startsAtFloor, sizeMode = if (startsAtFloor) SizeMode.DIMENSIONS else sizeMode)

    fun withSizeMode(mode: SizeMode): OpeningForm =
        if (mode == SizeMode.AREA &&
            !canUseArea
        ) {
            this
        } else {
            copy(sizeMode = mode)
        }

    companion object {
        fun new(wallNumber: Int): OpeningForm = OpeningForm(id = Uuid.random(), isNew = true, wallNumber = wallNumber)

        fun edit(
            opening: Opening,
            wallNumber: Int,
        ): OpeningForm {
            val form =
                OpeningForm(
                    id = opening.id,
                    isNew = false,
                    wallNumber = wallNumber,
                    type = opening.type,
                    startsAtFloor = opening.startsAtFloor,
                )
            return when (val size = opening.size) {
                is OpeningSize.Dimensions -> {
                    form.copy(width = size.width.toMetresInput(), height = size.height.toMetresInput())
                }

                is OpeningSize.Area -> {
                    form.copy(sizeMode = SizeMode.AREA, area = size.squareMetres.toSquareMetresInput())
                }
            }
        }
    }
}
