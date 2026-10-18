package dev.jurczyk.renoplan.data.mapper

import dev.jurczyk.renoplan.data.local.LevelEntity
import dev.jurczyk.renoplan.data.local.LevelWithRooms
import dev.jurczyk.renoplan.data.local.RoomWithWalls
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class MappersTest {
    private val levelId = Uuid.random()

    private val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
    private val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(1.2))
    private val niche =
        Opening(Uuid.random(), OpeningType.NICHE, OpeningSize.Dimensions(600.mm, 1_000.mm), startsAtFloor = true)

    private val kitchen =
        Room(
            id = Uuid.random(),
            name = "Kitchen",
            height = 2_600.mm,
            walls =
                listOf(
                    Wall(1, 3_200.mm, Turn.RIGHT, listOf(door, niche)),
                    Wall(2, 2_500.mm, Turn.RIGHT, listOf(window)),
                    Wall(3, 2_800.mm, Turn.RIGHT),
                    Wall(4, 400.mm, Turn.LEFT),
                    Wall(5, 400.mm, Turn.RIGHT),
                    Wall(6, 2_100.mm, Turn.RIGHT),
                ),
            shapeInput = ShapeInput.Outline,
        )

    @Test
    fun outlineRoomWithOpeningsSurvivesARoundTrip() {
        assertEquals(kitchen, roundTrip(kitchen))
    }

    @Test
    fun operationsKeepTheirOrderInARoundTrip() {
        val operations =
            listOf(
                Operation(Uuid.random(), OperationType.Priming, setOf(Surface.Ceiling), Money(300), Labour.SelfDone),
                Operation(
                    Uuid.random(),
                    OperationType.Painting("biały"),
                    setOf(Surface.Wall(1)),
                    Money(600),
                    Labour.SelfDone,
                ),
            )
        val withOperations = kitchen.copy(operations = operations)

        assertEquals(withOperations, roundTrip(withOperations))
    }

    @Test
    fun wallNamesSurviveARoundTrip() {
        val named = kitchen.withWallName(1, "Zlew").withWallName(3, "Lodówka")

        assertEquals(named, roundTrip(named))
        assertEquals(
            "Zlew",
            named
                .toEntities(levelId)
                .walls
                .first()
                .name,
        )
    }

    @Test
    fun rectangleShapeSurvivesARoundTrip() {
        val bathroom = Room.rectangle(Uuid.random(), "Bathroom", 2_500.mm, 2_000.mm, 2_600.mm)

        assertEquals(bathroom, roundTrip(bathroom))
    }

    @Test
    fun entitiesStoreMillimetresAndNames() {
        val entities = kitchen.toEntities(levelId)

        assertEquals(levelId.toString(), entities.room.levelId)
        assertEquals("OUTLINE", entities.room.shape)
        assertEquals(listOf(3_200L, 2_500L, 2_800L, 400L, 400L, 2_100L), entities.walls.map { it.lengthMm })
        assertEquals("LEFT", entities.walls[3].turn)
        assertEquals(listOf(1, 1, 2), entities.openings.map { it.wallNumber })
        assertEquals(listOf(0, 1, 2), entities.openings.map { it.position })
    }

    @Test
    fun levelRoomsAreOrderedByPosition() {
        val first = Room.rectangle(Uuid.random(), "First", 1_000.mm, 1_000.mm, 2_500.mm)
        val second = Room.rectangle(Uuid.random(), "Second", 1_000.mm, 1_000.mm, 2_500.mm)
        val level =
            LevelWithRooms(
                level = LevelEntity(levelId.toString(), "Ground floor", position = 0),
                rooms = listOf(second.withPosition(1), first.withPosition(0)),
            ).toDomain()

        assertEquals(listOf("First", "Second"), level.rooms.map { it.name })
    }

    private fun Room.withPosition(position: Int): RoomWithWalls {
        val entities = toEntities(levelId)
        return RoomWithWalls(
            entities.room.copy(position = position),
            entities.walls,
            entities.openings,
            entities.operations,
        )
    }

    private fun roundTrip(room: Room): Room {
        val entities = room.toEntities(levelId)
        // The database returns rows in no particular order.
        return RoomWithWalls(
            entities.room,
            entities.walls.reversed(),
            entities.openings.reversed(),
            entities.operations.reversed(),
        ).toDomain()
    }
}
