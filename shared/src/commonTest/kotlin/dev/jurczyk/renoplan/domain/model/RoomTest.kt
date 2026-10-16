package dev.jurczyk.renoplan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.Uuid

class RoomTest {
    private val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
    private val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(1.2))
    private val room =
        Room(
            id = Uuid.random(),
            name = "Kuchnia",
            height = 2_600.mm,
            walls =
                ShapeInput.Rectangle(3_000.mm, 2_000.mm).toWalls().map { wall ->
                    when (wall.number) {
                        1 -> wall.copy(openings = listOf(door))
                        4 -> wall.copy(openings = listOf(window))
                        else -> wall
                    }
                },
            shapeInput = ShapeInput.Rectangle(3_000.mm, 2_000.mm),
        )

    @Test
    fun duplicateGetsNewIdsAndKeepsEverythingElse() {
        val newId = Uuid.random()
        val copy = room.duplicate(newId, "Kuchnia (kopia)")

        assertEquals(newId, copy.id)
        assertEquals("Kuchnia (kopia)", copy.name)
        assertEquals(room.height, copy.height)
        assertEquals(room.shapeInput, copy.shapeInput)
        assertEquals(room.walls.map { it.length }, copy.walls.map { it.length })
        val copiedDoor = copy.walls[0].openings.single()
        assertNotEquals(door.id, copiedDoor.id)
        assertEquals(door.copy(id = copiedDoor.id), copiedDoor)
    }

    @Test
    fun withWallsKeepsOpeningsByWallNumberAndDropsRemovedWalls() {
        val threeWalls = ShapeInput.Rectangle(4_000.mm, 2_000.mm).toWalls().take(3)

        val changed = room.withWalls(threeWalls)

        assertEquals(listOf(4_000.mm, 2_000.mm, 4_000.mm), changed.walls.map { it.length })
        assertEquals(listOf(door), changed.walls[0].openings)
        assertEquals(emptyList(), changed.walls.flatMap { it.openings }.filter { it == window })
    }

    @Test
    fun withOpeningAddsReplacesAndMoves() {
        val niche = Opening(Uuid.random(), OpeningType.NICHE, OpeningSize.Area(0.5))
        val added = room.withOpening(1, niche)
        assertEquals(listOf(door, niche), added.walls[0].openings)

        val wider = door.copy(size = OpeningSize.Dimensions(900.mm, 2_000.mm))
        assertEquals(listOf(wider, niche), added.withOpening(1, wider).walls[0].openings)

        val moved = added.withOpening(3, door)
        assertEquals(listOf(niche), moved.walls[0].openings)
        assertEquals(listOf(door), moved.walls[2].openings)
    }

    @Test
    fun withoutOpening() {
        val changed = room.withoutOpening(door.id)

        assertEquals(emptyList(), changed.walls[0].openings)
        assertEquals(listOf(window), changed.walls[3].openings)
    }
}
