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
        val priming = Operation(Uuid.random(), OperationType.Priming, setOf(Surface.Floor), Money(500), Labour.SelfDone)
        val copy = room.copy(operations = listOf(priming)).duplicate(newId, "Kuchnia (kopia)")

        val copiedOperation = copy.operations.single()
        assertNotEquals(priming.id, copiedOperation.id)
        assertEquals(priming.copy(id = copiedOperation.id), copiedOperation)

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

    @Test
    fun wallNamesStayWithTheirWallNumber() {
        val named = room.withWallName(1, "  Zlew ").withWallName(4, "Szafa")
        assertEquals("Zlew", named.walls[0].name)

        val changed = named.withWalls(ShapeInput.Rectangle(4_000.mm, 2_000.mm).toWalls().take(3))

        assertEquals(listOf("Zlew", null, null), changed.walls.map { it.name })
    }

    @Test
    fun blankWallNameRemovesIt() {
        val named = room.withWallName(2, "Telewizor")

        assertEquals(null, named.withWallName(2, "   ").walls[1].name)
    }
}
