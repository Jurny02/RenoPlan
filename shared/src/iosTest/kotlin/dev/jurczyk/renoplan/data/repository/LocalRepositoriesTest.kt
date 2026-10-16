package dev.jurczyk.renoplan.data.repository

import androidx.room.Room
import androidx.room.useReaderConnection
import app.cash.turbine.test
import dev.jurczyk.renoplan.data.local.RenoPlanDatabase
import dev.jurczyk.renoplan.data.local.buildDatabase
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.model.mm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import dev.jurczyk.renoplan.domain.model.Room as RenoRoom

// Runs on the iOS simulator only: the Android database builder needs a Context, which the JVM host tests lack.
class LocalRepositoriesTest {
    private val database = buildDatabase(Room.inMemoryDatabaseBuilder<RenoPlanDatabase>())
    private val levels = LocalLevelRepository(database.levelDao())
    private val rooms = LocalRoomRepository(database.roomDao())

    private val groundFloor = Level(Uuid.random(), "Parter")
    private val bathroom = RenoRoom.rectangle(Uuid.random(), "Łazienka", 2_500.mm, 2_000.mm, 2_600.mm)
    private val kitchen =
        RenoRoom(
            id = Uuid.random(),
            name = "Kuchnia",
            height = 2_600.mm,
            walls =
                listOf(
                    Wall(
                        1,
                        3_200.mm,
                        Turn.RIGHT,
                        listOf(Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))),
                    ),
                    Wall(
                        2,
                        2_500.mm,
                        Turn.RIGHT,
                        listOf(Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(1.2))),
                    ),
                    Wall(3, 2_800.mm, Turn.RIGHT),
                    Wall(4, 400.mm, Turn.LEFT),
                    Wall(5, 400.mm, Turn.RIGHT),
                    Wall(6, 2_100.mm, Turn.RIGHT),
                ),
            shapeInput = ShapeInput.Outline,
        )

    private suspend fun count(table: String): Long =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM $table") { statement ->
                statement.step()
                statement.getLong(0)
            }
        }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedLevelIsObserved() =
        runTest {
            levels.observeLevels().test {
                assertEquals(emptyList(), awaitItem())

                levels.saveLevel(groundFloor)

                assertEquals(listOf(groundFloor), awaitItem())
            }
        }

    @Test
    fun levelsKeepCreationOrderAndCanBeRenamed() =
        runTest {
            val firstFloor = Level(Uuid.random(), "Piętro")
            levels.saveLevel(groundFloor)
            levels.saveLevel(firstFloor)
            levels.saveLevel(groundFloor.copy(name = "Parter po zmianie"))

            assertEquals(listOf("Parter po zmianie", "Piętro"), levels.observeLevels().first().map { it.name })
        }

    @Test
    fun roomWithWallsAndOpeningsSurvivesARoundTrip() =
        runTest {
            levels.saveLevel(groundFloor)
            rooms.saveRoom(groundFloor.id, kitchen)

            assertEquals(kitchen, rooms.getRoom(kitchen.id))
        }

    @Test
    fun savingARoomAgainReplacesItsWallsAndKeepsItsPosition() =
        runTest {
            levels.saveLevel(groundFloor)
            rooms.saveRoom(groundFloor.id, kitchen)
            rooms.saveRoom(groundFloor.id, bathroom)
            val smallerKitchen = kitchen.copy(walls = ShapeInput.Rectangle(3_000.mm, 2_000.mm).toWalls())

            rooms.saveRoom(groundFloor.id, smallerKitchen)

            assertEquals(listOf(smallerKitchen, bathroom), levels.getLevel(groundFloor.id)?.rooms)
        }

    @Test
    fun observedRoomsFollowChanges() =
        runTest {
            levels.saveLevel(groundFloor)
            rooms.observeRooms(groundFloor.id).test {
                assertEquals(emptyList(), awaitItem())

                rooms.saveRoom(groundFloor.id, bathroom)
                assertEquals(listOf(bathroom), awaitItem())

                rooms.deleteRoom(bathroom.id)
                assertEquals(emptyList(), awaitItem())
            }
        }

    @Test
    fun deletingALevelDeletesItsRoomsWallsAndOpenings() =
        runTest {
            levels.saveLevel(groundFloor)
            rooms.saveRoom(groundFloor.id, kitchen)

            levels.deleteLevel(groundFloor.id)

            assertNull(levels.getLevel(groundFloor.id))
            assertNull(rooms.getRoom(kitchen.id))
            assertEquals(0L, count("walls"))
            assertEquals(0L, count("openings"))
        }
}
