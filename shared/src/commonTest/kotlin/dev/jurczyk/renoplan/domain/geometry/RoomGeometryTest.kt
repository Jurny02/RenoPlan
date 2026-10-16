package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class RoomGeometryTest {
    private val rectangle =
        Room.rectangle(id = Uuid.random(), name = "Bathroom", length = 2_500.mm, width = 2_000.mm, height = 2_600.mm)

    @Test
    fun rectangleAreasPerimeterAndVolume() {
        assertEquals(5.0, rectangle.floorArea, TOLERANCE)
        assertEquals(5.0, rectangle.ceilingArea, TOLERANCE)
        assertEquals(9_000.mm, rectangle.perimeter)
        assertEquals(13.0, rectangle.volume, TOLERANCE)
        assertEquals(23.4, rectangle.wallsGrossArea, TOLERANCE)
        assertEquals(23.4, rectangle.wallsNetArea, TOLERANCE)
        assertEquals(9_000.mm, rectangle.skirtingLength)
        assertEquals(9_000.mm, rectangle.crownMouldingLength)
    }

    @Test
    fun counterClockwiseOutlineHasPositiveArea() {
        assertEquals(6.0, room("3000 L 2000 L 3000 L 2000 L").floorArea, TOLERANCE)
    }

    @Test
    fun chimneyKitchenMatchesTheDocsExample() {
        val kitchen = room(CHIMNEY_KITCHEN)

        assertEquals(7.84, kitchen.floorArea, TOLERANCE)
        assertEquals(11_400.mm, kitchen.perimeter)
    }

    @Test
    fun lShape() {
        val room = room(L_SHAPE, height = 2_700.mm)

        assertEquals(10.0, room.floorArea, TOLERANCE)
        assertEquals(14_000.mm, room.perimeter)
        assertEquals(27.0, room.volume, TOLERANCE)
        assertEquals(37.8, room.wallsGrossArea, TOLERANCE)
    }

    @Test
    fun wallGrossAndNetArea() {
        val room = rectangle.withOpenings(1 to opening(OpeningType.DOOR, width = 800, height = 2_000))
        val wall1 = room.walls[0]

        assertEquals(6.5, room.wallGrossArea(wall1), TOLERANCE)
        assertEquals(4.9, room.wallNetArea(wall1), TOLERANCE)
        assertEquals(21.8, room.wallsNetArea, TOLERANCE)
    }

    @Test
    fun openingGivenByArea() {
        val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(squareMetres = 1.2))
        val room = rectangle.withOpenings(2 to window)

        assertEquals(1.2, window.area, TOLERANCE)
        assertEquals(5.2 - 1.2, room.wallNetArea(room.walls[1]), TOLERANCE)
        assertEquals(9_000.mm, room.skirtingLength)
    }

    @Test
    fun openingGivenByDimensions() {
        assertEquals(1.68, opening(OpeningType.PASSAGE, width = 800, height = 2_100).area, TOLERANCE)
    }

    @Test
    fun skirtingSubtractsDoorAndPassageButNotWindowOrNiche() {
        val room =
            rectangle.withOpenings(
                1 to opening(OpeningType.DOOR, width = 800, height = 2_000),
                3 to opening(OpeningType.PASSAGE, width = 900, height = 2_100),
                2 to opening(OpeningType.WINDOW, width = 1_200, height = 1_500),
                4 to opening(OpeningType.NICHE, width = 600, height = 1_000),
            )

        assertEquals(7_300.mm, room.skirtingLength)
        assertEquals(9_000.mm, room.crownMouldingLength)
    }

    @Test
    fun nicheStartingAtTheFloorShortensSkirting() {
        val room = rectangle.withOpenings(4 to opening(OpeningType.NICHE, 600, 1_000, startsAtFloor = true))

        assertEquals(8_400.mm, room.skirtingLength)
    }

    @Test
    fun emptyRoomHasNoArea() {
        assertEquals(0.0, room("").floorArea, TOLERANCE)
    }
}
