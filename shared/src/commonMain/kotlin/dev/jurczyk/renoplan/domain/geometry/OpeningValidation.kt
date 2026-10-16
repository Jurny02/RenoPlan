package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.Room
import kotlin.uuid.Uuid

sealed interface OpeningError {
    val wallNumber: Int

    data class WiderThanWall(
        override val wallNumber: Int,
        val openingId: Uuid,
    ) : OpeningError

    data class TallerThanRoom(
        override val wallNumber: Int,
        val openingId: Uuid,
    ) : OpeningError

    /** The openings on a wall cover more than its gross area. */
    data class OpeningsExceedWallArea(
        override val wallNumber: Int,
    ) : OpeningError
}

/** All opening problems in the room; empty when every opening fits its wall. */
fun Room.validateOpenings(): List<OpeningError> =
    buildList {
        walls.forEach { wall ->
            wall.openings.forEach { opening ->
                val size = opening.size
                if (size is OpeningSize.Dimensions) {
                    if (size.width > wall.length) add(OpeningError.WiderThanWall(wall.number, opening.id))
                    if (size.height > height) add(OpeningError.TallerThanRoom(wall.number, opening.id))
                }
            }
            if (wall.openings.sumOf { it.area } >
                wallGrossArea(wall)
            ) {
                add(OpeningError.OpeningsExceedWallArea(wall.number))
            }
        }
    }
