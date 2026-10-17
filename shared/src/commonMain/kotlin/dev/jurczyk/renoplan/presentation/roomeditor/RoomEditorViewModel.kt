package dev.jurczyk.renoplan.presentation.roomeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.common.toMetresInput
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

private const val NEW_OUTLINE_ROWS = 4

class RoomEditorViewModel(
    private val levelId: Uuid,
    private val roomId: Uuid?,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    /**
     * The form. Compose state rather than a flow, so text fields get every change synchronously;
     * asynchronous updates move the cursor (seen on iOS).
     */
    var state: RoomEditorUiState by mutableStateOf(
        RoomEditorUiState(isNew = roomId == null, isLoading = roomId != null),
    )
        private set

    /** The stored room being edited; its openings are kept on walls that still exist. */
    private var original: Room? = null

    init {
        if (roomId != null) viewModelScope.launch { load(roomId) }
    }

    fun onNameChange(name: String) = update { it.copy(name = name) }

    fun onHeightChange(height: String) = update { it.copy(height = height) }

    fun onLengthChange(length: String) = update { it.copy(length = length) }

    fun onWidthChange(width: String) = update { it.copy(width = width) }

    /**
     * Rectangle → Outline turns the rectangle into four rows (or four empty rows).
     * Outline → Rectangle works only for a closed 4-wall outline.
     */
    fun onShapeModeChange(mode: ShapeMode) =
        update { state ->
            when {
                mode == state.shapeMode -> {
                    state
                }

                mode == ShapeMode.OUTLINE -> {
                    state.copy(
                        shapeMode = ShapeMode.OUTLINE,
                        rows = state.walls?.toRows() ?: List(NEW_OUTLINE_ROWS) { WallRow() },
                        autoClose = false,
                    )
                }

                state.canSwitchToRectangle -> {
                    val walls = state.closedWalls.orEmpty()
                    state.copy(
                        shapeMode = ShapeMode.RECTANGLE,
                        length = walls[0].length.toMetresInput(),
                        width = walls[1].length.toMetresInput(),
                    )
                }

                else -> {
                    state
                }
            }
        }

    fun onRowLengthChange(
        index: Int,
        length: String,
    ) = updateRow(index) { it.copy(length = length) }

    fun onRowTurnChange(
        index: Int,
        turn: Turn,
    ) = updateRow(index) { it.copy(turn = turn) }

    fun addRow() = update { it.copy(rows = it.rows + WallRow()) }

    fun removeRow(index: Int) = update { state -> state.copy(rows = state.rows.filterIndexed { i, _ -> i != index }) }

    /**
     * Turning it on adds a computed wall after the typed ones; if the typed walls already close the
     * outline, their last wall is dropped instead, because it is the one that gets computed.
     * Turning it off keeps the computed wall as an editable row.
     */
    fun onAutoCloseChange(autoClose: Boolean) =
        update { state ->
            when {
                autoClose == state.autoClose -> {
                    state
                }

                autoClose -> {
                    val alreadyClosed = state.closedWalls != null
                    state.copy(autoClose = true, rows = if (alreadyClosed) state.rows.dropLast(1) else state.rows)
                }

                else -> {
                    val closing = state.closingWall
                    val row = closing?.let { WallRow(it.length.toMetresInput(), it.turn) } ?: WallRow()
                    state.copy(autoClose = false, rows = state.rows + row)
                }
            }
        }

    fun save() {
        val room = state.takeIf { it.canSave }?.let(::toRoom) ?: return
        viewModelScope.launch {
            roomRepository.saveRoom(levelId, room)
            update { it.copy(isSaved = true) }
        }
    }

    private fun toRoom(state: RoomEditorUiState): Room? {
        val walls = state.closedWalls
        val height = state.heightMm
        val shapeInput = state.shapeInput
        if (walls == null || height == null || shapeInput == null) return null
        val name = state.name.trim()
        return original
            ?.copy(name = name, height = height, shapeInput = shapeInput)
            ?.withWalls(walls)
            ?: Room(id = Uuid.random(), name = name, height = height, walls = walls, shapeInput = shapeInput)
    }

    private fun updateRow(
        index: Int,
        change: (WallRow) -> WallRow,
    ) = update { state ->
        state.copy(rows = state.rows.mapIndexed { i, row -> if (i == index) change(row) else row })
    }

    private suspend fun load(id: Uuid) {
        val room = roomRepository.getRoom(id)
        original = room
        val rectangle = room?.shapeInput as? ShapeInput.Rectangle
        update {
            it.copy(
                isLoading = false,
                name = room?.name.orEmpty(),
                height = room?.height?.toMetresInput().orEmpty(),
                shapeMode = if (room != null && rectangle == null) ShapeMode.OUTLINE else ShapeMode.RECTANGLE,
                length = rectangle?.length?.toMetresInput().orEmpty(),
                width = rectangle?.width?.toMetresInput().orEmpty(),
                rows = if (rectangle == null) room?.walls?.toRows().orEmpty() else emptyList(),
                wallsWithOpenings =
                    room
                        ?.walls
                        ?.filter { wall ->
                            wall.openings.isNotEmpty()
                        }?.map { wall -> wall.number }
                        ?.toSet()
                        .orEmpty(),
            )
        }
    }

    private fun update(change: (RoomEditorUiState) -> RoomEditorUiState) {
        state = change(state)
    }
}

private fun List<Wall>.toRows(): List<WallRow> = map { WallRow(it.length.toMetresInput(), it.turn) }
