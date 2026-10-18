package dev.jurczyk.renoplan.presentation.roomdetails

import dev.jurczyk.renoplan.domain.geometry.OpeningError
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class RoomDetailsViewModelTest : MainDispatcherTest() {
    /** 3.2 × 2.5 m, 2.6 m high: walls 1 and 3 are 3.2 m (8.32 m²), walls 2 and 4 are 2.5 m (6.5 m²). */
    private val kitchen = Room.rectangle(Uuid.random(), "Kuchnia", 3_200.mm, 2_500.mm, 2_600.mm)
    private val store = FakeStore().apply { levels.value = listOf(Level(LEVEL_ID, "Parter", listOf(kitchen))) }
    private val viewModel = RoomDetailsViewModel(LEVEL_ID, kitchen.id, store.roomRepository)

    /** The state as the screen sees it: stored room plus the open sheet. */
    private suspend fun loaded() = viewModel.state.first { !it.isLoading }.copy(form = viewModel.form)

    private fun room() = store.rooms().single()

    @Test
    fun headerNumbers() =
        runTest {
            val state = loaded()

            assertEquals(8.0, state.floorArea, 1e-9)
            assertEquals(8.0, state.ceilingArea, 1e-9)
            assertEquals(29.64, state.wallsNetArea, 1e-9)
            assertEquals(11_400.mm, state.perimeter)
            assertEquals(11_400.mm, state.skirtingLength)
            assertEquals(20.8, state.volume, 1e-9)
        }

    @Test
    fun addingADoorUpdatesNetAreaAndSkirting() =
        runTest {
            viewModel.addOpening(wallNumber = 2)
            val form = loaded().form!!
            viewModel.onFormChange(form.withType(OpeningType.DOOR).copy(width = "0,8", height = "2"))

            val preview = loaded()
            assertTrue(preview.form!!.startsAtFloor)
            assertEquals(4.9, preview.formWallNetArea!!, 1e-9)
            assertTrue(preview.canSaveForm)

            viewModel.saveOpening()

            val state = loaded()
            assertNull(state.form)
            assertEquals(
                OpeningType.DOOR,
                room()
                    .walls[1]
                    .openings
                    .single()
                    .type,
            )
            assertEquals(28.04, state.wallsNetArea, 1e-9)
            assertEquals(10_600.mm, state.skirtingLength)
        }

    @Test
    fun windowByAreaDoesNotShortenSkirting() =
        runTest {
            viewModel.addOpening(wallNumber = 1)
            viewModel.onFormChange(loaded().form!!.withSizeMode(SizeMode.AREA).copy(area = "1,5"))
            viewModel.saveOpening()

            val state = loaded()
            assertEquals(
                OpeningSize.Area(1.5),
                room()
                    .walls[0]
                    .openings
                    .single()
                    .size,
            )
            assertEquals(28.14, state.wallsNetArea, 1e-9)
            assertEquals(11_400.mm, state.skirtingLength)
        }

    @Test
    fun openingAtTheFloorCannotBeGivenByArea() {
        val form = OpeningForm.new(1).withType(OpeningType.PASSAGE)

        assertFalse(form.canUseArea)
        assertEquals(SizeMode.DIMENSIONS, form.withSizeMode(SizeMode.AREA).sizeMode)
        assertEquals(
            SizeMode.DIMENSIONS,
            form
                .copy(startsAtFloor = false)
                .withSizeMode(SizeMode.AREA)
                .withStartsAtFloor(true)
                .sizeMode,
        )
    }

    @Test
    fun openingWiderThanItsWallCannotBeSaved() =
        runTest {
            viewModel.addOpening(wallNumber = 2)
            viewModel.onFormChange(loaded().form!!.copy(width = "2,6", height = "1"))

            val state = loaded()
            assertEquals(listOf<OpeningError>(OpeningError.WiderThanWall(2, state.form!!.id)), state.formErrors)
            assertFalse(state.canSaveForm)

            viewModel.saveOpening()
            assertTrue(room().walls[1].openings.isEmpty())
        }

    @Test
    fun editingMovesTheOpeningToAnotherWall() =
        runTest {
            val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Dimensions(1_200.mm, 1_500.mm))
            store.roomRepository.saveRoom(LEVEL_ID, kitchen.withOpening(1, window))

            viewModel.editOpening(window.id)
            val form = loaded().form!!
            assertFalse(form.isNew)
            assertEquals(listOf("1,2", "1,5"), listOf(form.width, form.height))

            viewModel.onFormChange(form.copy(wallNumber = 3, width = "1,4"))
            viewModel.saveOpening()

            assertTrue(room().walls[0].openings.isEmpty())
            assertEquals(
                OpeningSize.Dimensions(1_400.mm, 1_500.mm),
                room()
                    .walls[2]
                    .openings
                    .single()
                    .size,
            )
        }

    @Test
    fun deletingAnOpening() =
        runTest {
            val niche = Opening(Uuid.random(), OpeningType.NICHE, OpeningSize.Area(0.5))
            store.roomRepository.saveRoom(LEVEL_ID, kitchen.withOpening(4, niche))

            viewModel.editOpening(niche.id)
            assertEquals("0,5", loaded().form!!.area)
            viewModel.deleteOpening()

            assertTrue(room().walls.all { it.openings.isEmpty() })
            assertNull(loaded().form)
        }

    @Test
    fun dismissClosesTheSheetWithoutSaving() =
        runTest {
            viewModel.addOpening()
            viewModel.onFormChange(loaded().form!!.copy(width = "1", height = "1"))

            viewModel.dismissForm()

            assertNull(loaded().form)
            assertTrue(room().walls.all { it.openings.isEmpty() })
        }

    @Test
    fun operationsShowTheirCostAndStatusCycles() =
        runTest {
            val floor =
                Operation(
                    Uuid.random(),
                    OperationType.Tiling,
                    setOf(Surface.Floor),
                    Money.fromZloty(80.0),
                    Labour.SelfDone,
                )
            store.roomRepository.saveRoom(LEVEL_ID, kitchen.copy(operations = listOf(floor)))

            val (operation, cost) = loaded().operations.single()
            assertEquals(floor, operation)
            assertEquals(Money.fromZloty(640.0), cost.total)
            assertEquals(Money.Zero, loaded().costSummary?.done)

            viewModel.cycleStatus(floor.id)
            assertEquals(OperationStatus.IN_PROGRESS, room().operations.single().status)
            viewModel.cycleStatus(floor.id)
            assertEquals(Money.fromZloty(640.0), loaded().costSummary?.done)
            viewModel.cycleStatus(floor.id)
            assertEquals(OperationStatus.TODO, room().operations.single().status)
        }

    @Test
    fun summaryOfTheRoom() =
        runTest {
            val ceiling =
                Operation(
                    Uuid.random(),
                    OperationType.Painting("biały"),
                    setOf(Surface.Ceiling),
                    Money(600),
                    Labour.SelfDone,
                )
            store.roomRepository.saveRoom(LEVEL_ID, kitchen.copy(operations = listOf(ceiling)))

            val state = loaded()
            assertEquals(8.0, state.geometryTotals.floorArea, 1e-9)
            assertEquals(11_400.mm, state.geometryTotals.skirtingLength)
            assertEquals(listOf(OperationType.Painting("biały")), state.operationGroups.map { it.type })
            assertEquals(8.0, state.operationGroups.single().quantity, 1e-9)
        }

    @Test
    fun deletedRoomIsMissing() =
        runTest {
            store.roomRepository.deleteRoom(kitchen.id)

            assertTrue(loaded().isMissing)
        }

    private companion object {
        val LEVEL_ID = Uuid.random()
    }
}
