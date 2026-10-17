package dev.jurczyk.renoplan.presentation.levels

import app.cash.turbine.test
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
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
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class LevelsViewModelTest : MainDispatcherTest() {
    private val store = FakeStore()
    private val viewModel = LevelsViewModel(store.levelRepository)

    private suspend fun loaded(): LevelsUiState = viewModel.state.first { !it.isLoading }

    @Test
    fun emptyStateAfterLoading() =
        runTest {
            assertEquals(emptyList(), loaded().levels)
        }

    @Test
    fun cardsShowRoomCountAndTotalFloorArea() =
        runTest {
            val rooms =
                listOf(
                    Room.rectangle(Uuid.random(), "Kuchnia", 3_000.mm, 2_000.mm, 2_600.mm),
                    Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 1_500.mm, 2_600.mm),
                )
            val level = Level(Uuid.random(), "Parter", rooms)
            store.levels.value = listOf(level)

            assertEquals(listOf(LevelCard(level.id, "Parter", roomCount = 2, floorArea = 9.0)), loaded().levels)
        }

    @Test
    fun cardsShowTheLevelCost() =
        runTest {
            val painting =
                Operation(
                    Uuid.random(),
                    OperationType.Painting("biały"),
                    setOf(Surface.Ceiling),
                    Money.fromZloty(10.0),
                    Labour.SelfDone,
                )
            val room =
                Room
                    .rectangle(
                        Uuid.random(),
                        "Salon",
                        4_000.mm,
                        3_000.mm,
                        2_500.mm,
                    ).copy(operations = listOf(painting))
            store.levels.value = listOf(Level(Uuid.random(), "Parter", listOf(room)))

            assertEquals(
                Money.fromZloty(120.0),
                loaded()
                    .levels
                    .single()
                    .cost.total,
            )
        }

    @Test
    fun addLevelTrimsTheName() =
        runTest {
            viewModel.state.test {
                skipItems(1)
                viewModel.addLevel("  Parter ")
                assertEquals(listOf("Parter"), awaitItem().levels.map { it.name })
            }
        }

    @Test
    fun blankNameIsIgnored() =
        runTest {
            viewModel.addLevel("   ")

            assertTrue(store.levels.value.isEmpty())
        }

    @Test
    fun renameAndDelete() =
        runTest {
            val level = Level(Uuid.random(), "Parter")
            store.levels.value = listOf(level)

            viewModel.renameLevel(level.id, "Piętro")
            assertEquals(
                "Piętro",
                store.levels.value
                    .single()
                    .name,
            )

            viewModel.deleteLevel(level.id)
            assertTrue(store.levels.value.isEmpty())
        }
}
