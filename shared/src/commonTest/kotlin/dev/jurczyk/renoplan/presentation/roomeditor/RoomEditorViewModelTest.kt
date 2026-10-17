package dev.jurczyk.renoplan.presentation.roomeditor

import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class RoomEditorViewModelTest : MainDispatcherTest() {
    private val store = FakeStore().apply { levels.value = listOf(Level(LEVEL_ID, "Parter")) }

    private fun newRoomEditor() = RoomEditorViewModel(LEVEL_ID, roomId = null, roomRepository = store.roomRepository)

    private fun RoomEditorViewModel.fill(
        name: String = "Kuchnia",
        height: String = "2,6",
        length: String = "3,2",
        width: String = "2,5",
    ) {
        onNameChange(name)
        onHeightChange(height)
        onLengthChange(length)
        onWidthChange(width)
    }

    @Test
    fun newRoomStartsEmptyAndCannotBeSaved() {
        val state = newRoomEditor().state

        assertTrue(state.isNew)
        assertFalse(state.canSave)
        assertNull(state.floorArea)
    }

    @Test
    fun liveResults() {
        val editor = newRoomEditor().apply { fill() }
        val state = editor.state

        assertEquals(8.0, state.floorArea!!, 1e-9)
        assertEquals(11_400.mm, state.perimeter)
        assertEquals(20.8, state.volume!!, 1e-9)
        assertTrue(state.canSave)
    }

    @Test
    fun floorAreaAndPerimeterNeedNoHeight() {
        val state = newRoomEditor().apply { fill(height = "") }.state

        assertEquals(8.0, state.floorArea!!, 1e-9)
        assertNull(state.volume)
        assertFalse(state.canSave)
    }

    @Test
    fun invalidInputIsReportedOnlyOnceTyped() {
        val editor = newRoomEditor()
        assertFalse(editor.state.lengthError)

        editor.onLengthChange("0")
        assertTrue(editor.state.lengthError)

        editor.onLengthChange("3,2")
        assertFalse(editor.state.lengthError)
    }

    @Test
    fun blankNameCannotBeSaved() {
        assertFalse(
            newRoomEditor()
                .apply { fill(name = "  ") }
                .state.canSave,
        )
    }

    @Test
    fun savingANewRoomAddsARectangle() =
        runTest {
            val editor = newRoomEditor().apply { fill(name = " Kuchnia ") }

            editor.save()

            val room = store.rooms().single()
            assertEquals("Kuchnia", room.name)
            assertEquals(2_600.mm, room.height)
            assertEquals(ShapeInput.Rectangle(3_200.mm, 2_500.mm), room.shapeInput)
            assertTrue(editor.state.isSaved)
        }

    @Test
    fun editingLoadsTheRoomAndKeepsItsOpenings() =
        runTest {
            val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
            val room =
                Room.rectangle(Uuid.random(), "Kuchnia", 3_200.mm, 2_500.mm, 2_650.mm).let {
                    it.copy(
                        walls =
                            it.walls.map { wall ->
                                if (wall.number ==
                                    2
                                ) {
                                    wall.copy(openings = listOf(door))
                                } else {
                                    wall
                                }
                            },
                    )
                }
            store.roomRepository.saveRoom(LEVEL_ID, room)

            val editor = RoomEditorViewModel(LEVEL_ID, room.id, store.roomRepository)
            val loaded = editor.state
            assertFalse(loaded.isNew)
            assertEquals(
                listOf("Kuchnia", "2,65", "3,2", "2,5"),
                listOf(loaded.name, loaded.height, loaded.length, loaded.width),
            )

            editor.onLengthChange("4")
            editor.save()

            val saved = store.rooms().single()
            assertEquals(room.id, saved.id)
            assertEquals(4_000.mm, saved.walls[0].length)
            assertEquals(listOf(door), saved.walls[1].openings)
        }

    private companion object {
        val LEVEL_ID = Uuid.random()
    }
}
