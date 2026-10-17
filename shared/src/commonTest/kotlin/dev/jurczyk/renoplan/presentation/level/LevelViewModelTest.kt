package dev.jurczyk.renoplan.presentation.level

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class LevelViewModelTest : MainDispatcherTest() {
    private val store = FakeStore()
    private val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
    private val kitchen =
        Room.rectangle(Uuid.random(), "Kuchnia", 3_000.mm, 2_000.mm, 2_500.mm).let { room ->
            room.copy(
                walls =
                    room.walls.mapIndexed { i, wall ->
                        if (i ==
                            0
                        ) {
                            wall.copy(openings = listOf(door))
                        } else {
                            wall
                        }
                    },
            )
        }
    private val level = Level(Uuid.random(), "Parter", listOf(kitchen))

    private fun viewModel(levelId: Uuid = level.id): LevelViewModel {
        store.levels.value = listOf(level)
        return LevelViewModel(levelId, store.levelRepository, store.roomRepository)
    }

    @Test
    fun roomCardsShowFloorAndNetWallArea() =
        runTest {
            val state = viewModel().state.first { !it.isLoading }

            assertEquals("Parter", state.name)
            // Walls: 10 m perimeter × 2.5 m = 25 m², minus the 1.6 m² door.
            assertEquals(listOf(RoomCard(kitchen.id, "Kuchnia", floorArea = 6.0, netWallArea = 23.4)), state.rooms)
        }

    @Test
    fun summaryAndCostsCoverEveryRoom() =
        runTest {
            val floor =
                Operation(
                    Uuid.random(),
                    OperationType.Tiling,
                    setOf(Surface.Floor),
                    Money.fromZloty(80.0),
                    Labour.SelfDone,
                    OperationStatus.DONE,
                )
            val bathroom =
                Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 2_000.mm, 2_500.mm).copy(operations = listOf(floor))
            store.levels.value = listOf(level.copy(rooms = listOf(kitchen, bathroom)))
            val state =
                LevelViewModel(
                    level.id,
                    store.levelRepository,
                    store.roomRepository,
                ).state.first { !it.isLoading }

            assertEquals(10.0, state.geometry.floorArea, 1e-9)
            assertEquals(listOf("Kuchnia", "Łazienka"), state.roomSummaries.map { it.name })
            assertEquals(listOf(OperationType.Tiling), state.operationGroups.map { it.type })
            assertEquals(Money.fromZloty(320.0), state.costSummary.total)
            assertEquals(Money.fromZloty(320.0), state.costSummary.done)
            assertEquals(listOf(Money.Zero, Money.fromZloty(320.0)), state.rooms.map { it.cost.total })
            val entry = state.operations.single()
            assertEquals(bathroom.id to "Łazienka", entry.roomId to entry.roomName)
            assertEquals(4.0, entry.cost.quantity, 1e-9)
        }

    @Test
    fun missingLevel() =
        runTest {
            assertTrue(viewModel(levelId = Uuid.random()).state.first { !it.isLoading }.isMissing)
        }

    @Test
    fun renameRoom() =
        runTest {
            viewModel().renameRoom(kitchen.id, " Aneks ")

            assertEquals("Aneks", store.rooms().single().name)
        }

    @Test
    fun duplicateRoomCopiesGeometryAndOpeningsUnderNewIds() =
        runTest {
            viewModel().duplicateRoom(kitchen.id, "Kuchnia (kopia)")

            val (original, copy) = store.rooms()
            assertEquals(kitchen, original)
            assertEquals("Kuchnia (kopia)", copy.name)
            assertNotEquals(kitchen.id, copy.id)
            assertEquals(kitchen.walls.map { it.length }, copy.walls.map { it.length })
            val copiedDoor =
                copy.walls
                    .first()
                    .openings
                    .single()
            assertEquals(door.size, copiedDoor.size)
            assertNotEquals(door.id, copiedDoor.id)
        }

    @Test
    fun deleteRoom() =
        runTest {
            viewModel().deleteRoom(kitchen.id)

            assertTrue(store.rooms().isEmpty())
        }
}
