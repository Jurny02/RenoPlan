package dev.jurczyk.renoplan.presentation.roomeditor

import dev.jurczyk.renoplan.domain.geometry.OutlineError
import dev.jurczyk.renoplan.domain.geometry.Point
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
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

class RoomEditorOutlineTest : MainDispatcherTest() {
    private val store = FakeStore().apply { levels.value = listOf(Level(LEVEL_ID, "Parter")) }
    private val editor = RoomEditorViewModel(LEVEL_ID, roomId = null, roomRepository = store.roomRepository)

    private val state get() = editor.state.value

    /** Types rows like `"3,2 R 2,5 R 2,8 R"` into the outline editor, replacing the current rows. */
    private fun typeOutline(outline: String) {
        while (state.rows.isNotEmpty()) editor.removeRow(0)
        outline.trim().split(" ").chunked(2).forEachIndexed { index, (length, turn) ->
            editor.addRow()
            editor.onRowLengthChange(index, length)
            editor.onRowTurnChange(index, if (turn == "R") Turn.RIGHT else Turn.LEFT)
        }
    }

    private fun startOutline() {
        editor.onNameChange("Kuchnia")
        editor.onHeightChange("2,5")
        editor.onShapeModeChange(ShapeMode.OUTLINE)
    }

    @Test
    fun emptyRectangleBecomesFourEmptyRows() {
        startOutline()

        assertEquals(List(4) { WallRow() }, state.rows)
        assertNull(state.outlineProblem)
        assertFalse(state.canSave)
    }

    @Test
    fun filledRectangleBecomesFourRows() {
        editor.onLengthChange("3,2")
        editor.onWidthChange("2,5")

        editor.onShapeModeChange(ShapeMode.OUTLINE)

        assertEquals(listOf("3,2", "2,5", "3,2", "2,5"), state.rows.map { it.length })
        assertTrue(state.rows.all { it.turn == Turn.RIGHT })
    }

    @Test
    fun chimneyKitchenFromDocs() {
        startOutline()
        typeOutline("3,2 R 2,5 R 2,8 R 0,4 L 0,4 R 2,1 R")

        assertNull(state.outlineProblem)
        assertEquals(7.84, state.floorArea!!, 1e-9)
        assertEquals(11_400.mm, state.perimeter)
        assertEquals(19.6, state.volume!!, 1e-9)
        assertTrue(state.canSave)
    }

    @Test
    fun outlineThatDoesNotCloseIsReported() {
        startOutline()
        typeOutline("3 R 2 R 3 R 1,9 R")

        assertEquals(OutlineProblem.Invalid(listOf(OutlineError.NotClosed(Point(0, -100)))), state.outlineProblem)
        assertNull(state.floorArea)
        assertFalse(state.canSave)
    }

    @Test
    fun problemsWaitUntilEveryLengthIsTyped() {
        startOutline()
        typeOutline("3 R 2 R 3 R")
        editor.addRow()

        assertNull(state.outlineProblem)
        assertTrue(
            state.rows
                .last()
                .length
                .isEmpty(),
        )
    }

    @Test
    fun autoCloseComputesTheLastWall() {
        startOutline()
        typeOutline("3,2 R 2,5 R 2,8 R 0,4 L 0,4 R")

        editor.onAutoCloseChange(true)

        assertEquals(5, state.rows.size)
        assertEquals(Wall(6, 2_100.mm, Turn.RIGHT), state.walls?.last())
        assertEquals(7.84, state.floorArea!!, 1e-9)
    }

    @Test
    fun autoCloseThatIsImpossibleIsReported() {
        startOutline()
        editor.onAutoCloseChange(true)
        typeOutline("3 R 2 R 1 R")

        assertEquals(OutlineProblem.CannotAutoClose, state.outlineProblem)
        assertFalse(state.canSave)
    }

    @Test
    fun turningAutoCloseOnForAClosedOutlineDropsTheTypedLastWall() {
        startOutline()
        typeOutline("3 R 2 R 3 R 2 R")

        editor.onAutoCloseChange(true)

        assertEquals(3, state.rows.size)
        assertEquals(Wall(4, 2_000.mm, Turn.RIGHT), state.walls?.last())
    }

    @Test
    fun turningAutoCloseOffKeepsTheComputedWallAsARow() {
        startOutline()
        editor.onAutoCloseChange(true)
        typeOutline("3 R 2 R 3 R")

        editor.onAutoCloseChange(false)

        assertEquals(WallRow("2", Turn.RIGHT), state.rows.last())
        assertTrue(state.canSave)
    }

    @Test
    fun backToRectangleOnlyForAClosedFourWallOutline() {
        startOutline()
        typeOutline("3,2 R 2,5 R 2,8 R 0,4 L 0,4 R 2,1 R")
        assertFalse(state.canSwitchToRectangle)
        editor.onShapeModeChange(ShapeMode.RECTANGLE)
        assertEquals(ShapeMode.OUTLINE, state.shapeMode)

        typeOutline("4 L 3 L 4 L 3 L")
        editor.onShapeModeChange(ShapeMode.RECTANGLE)

        assertEquals(ShapeMode.RECTANGLE, state.shapeMode)
        assertEquals("4" to "3", state.length to state.width)
    }

    @Test
    fun savingAnOutlineRoom() =
        runTest {
            startOutline()
            typeOutline("3,2 R 2,5 R 2,8 R 0,4 L 0,4 R 2,1 R")

            editor.save()

            val room = store.rooms().single()
            assertEquals(ShapeInput.Outline, room.shapeInput)
            assertEquals(listOf(3_200.mm, 2_500.mm, 2_800.mm, 400.mm, 400.mm, 2_100.mm), room.walls.map { it.length })
            assertEquals(Turn.LEFT, room.walls[3].turn)
        }

    @Test
    fun editingAnOutlineRoomWarnsAboutOpeningsOnRemovedWalls() =
        runTest {
            val niche = Opening(Uuid.random(), OpeningType.NICHE, OpeningSize.Area(0.5))
            val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
            val walls =
                listOf(3_200, 2_500, 2_800, 400, 400, 2_100).mapIndexed { index, length ->
                    val turn = if (index == 3) Turn.LEFT else Turn.RIGHT
                    val openings =
                        when (index) {
                            0 -> listOf(door)
                            4 -> listOf(niche)
                            else -> emptyList()
                        }
                    Wall(index + 1, length.mm, turn, openings)
                }
            val kitchen = Room(Uuid.random(), "Kuchnia", 2_600.mm, walls, ShapeInput.Outline)
            store.roomRepository.saveRoom(LEVEL_ID, kitchen)

            val edit = RoomEditorViewModel(LEVEL_ID, kitchen.id, store.roomRepository)
            assertEquals(ShapeMode.OUTLINE, edit.state.value.shapeMode)
            assertEquals(
                "0,4",
                edit.state.value.rows[3]
                    .length,
            )

            // Remove the chimney box: walls 4 and 5 go, wall 6 becomes the 4th wall.
            edit.removeRow(3)
            edit.removeRow(3)
            edit.onRowLengthChange(2, "3,2")
            edit.onRowLengthChange(3, "2,5")
            assertEquals(listOf(5), edit.state.value.wallsLosingOpenings)

            edit.save()
            val saved = store.rooms().single()
            assertEquals(4, saved.walls.size)
            assertEquals(listOf(door), saved.walls[0].openings)
            assertTrue(saved.walls.none { niche in it.openings })
        }

    private companion object {
        val LEVEL_ID = Uuid.random()
    }
}
