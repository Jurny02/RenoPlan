package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class OpeningValidationTest {
    private val room =
        Room.rectangle(id = Uuid.random(), name = "Bedroom", length = 2_500.mm, width = 2_000.mm, height = 2_600.mm)

    @Test
    fun openingsThatFitAreValid() {
        val valid =
            room.withOpenings(
                1 to opening(OpeningType.DOOR, width = 2_500, height = 2_600),
                2 to opening(OpeningType.WINDOW, width = 1_200, height = 1_500),
            )

        assertTrue(valid.validateOpenings().isEmpty())
    }

    @Test
    fun openingWiderThanItsWall() {
        val door = opening(OpeningType.DOOR, width = 2_100, height = 2_000)

        assertEquals(
            listOf<OpeningError>(OpeningError.WiderThanWall(wallNumber = 2, openingId = door.id)),
            room.withOpenings(2 to door).validateOpenings(),
        )
    }

    @Test
    fun openingTallerThanTheRoom() {
        val passage = opening(OpeningType.PASSAGE, width = 900, height = 2_700)

        assertEquals(
            listOf<OpeningError>(OpeningError.TallerThanRoom(wallNumber = 1, openingId = passage.id)),
            room.withOpenings(1 to passage).validateOpenings(),
        )
    }

    @Test
    fun openingsLargerThanTheWall() {
        val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(squareMetres = 5.0))
        val niche = Opening(Uuid.random(), OpeningType.NICHE, OpeningSize.Area(squareMetres = 2.0))

        assertEquals(
            listOf<OpeningError>(OpeningError.OpeningsExceedWallArea(wallNumber = 1)),
            room.withOpenings(1 to window, 1 to niche).validateOpenings(),
        )
    }
}
