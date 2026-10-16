package dev.jurczyk.renoplan.presentation.roomeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.perimeter
import dev.jurczyk.renoplan.domain.geometry.volume
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.common.parseMetres
import dev.jurczyk.renoplan.presentation.common.toMetresInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Room editor form in rectangle mode. Lengths are typed in metres; results are derived, never stored. */
data class RoomEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    val height: String = "",
    val length: String = "",
    val width: String = "",
    val isSaved: Boolean = false,
) {
    val heightMm: Millimetres? get() = parseMetres(height)
    val lengthMm: Millimetres? get() = parseMetres(length)
    val widthMm: Millimetres? get() = parseMetres(width)

    /** Errors are shown only for fields the user has typed into. */
    val heightError: Boolean get() = height.isNotBlank() && heightMm == null
    val lengthError: Boolean get() = length.isNotBlank() && lengthMm == null
    val widthError: Boolean get() = width.isNotBlank() && widthMm == null

    /** The room as it would be saved, or `null` while the form is incomplete. */
    val preview: Room? get() = heightMm?.let { height -> rectangle()?.copy(name = name.trim(), height = height) }

    /** m², or `null` until length and width are valid. */
    val floorArea: Double? get() = rectangle()?.floorArea
    val perimeter: Millimetres? get() = rectangle()?.perimeter
    val volume: Double? get() = preview?.volume

    val canSave: Boolean get() = !isLoading && name.isNotBlank() && preview != null

    private fun rectangle(): Room? {
        val length = lengthMm
        val width = widthMm
        return if (length != null && width != null) {
            Room.rectangle(id = PREVIEW_ID, name = "", length = length, width = width, height = Millimetres.Zero)
        } else {
            null
        }
    }

    private companion object {
        val PREVIEW_ID = Uuid.NIL
    }
}

class RoomEditorViewModel(
    private val levelId: Uuid,
    private val roomId: Uuid?,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RoomEditorUiState(isNew = roomId == null, isLoading = roomId != null))
    val state: StateFlow<RoomEditorUiState> = _state.asStateFlow()

    /** The stored room being edited; its openings are kept when the walls change. */
    private var original: Room? = null

    init {
        if (roomId != null) viewModelScope.launch { load(roomId) }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    fun onHeightChange(height: String) = _state.update { it.copy(height = height) }

    fun onLengthChange(length: String) = _state.update { it.copy(length = length) }

    fun onWidthChange(width: String) = _state.update { it.copy(width = width) }

    fun save() {
        val current = _state.value
        val preview = current.preview ?: return
        if (!current.canSave) return
        val room =
            original?.let { stored ->
                stored
                    .copy(name = preview.name, height = preview.height, shapeInput = preview.shapeInput)
                    .withWalls(preview.walls)
            } ?: preview.copy(id = Uuid.random())
        viewModelScope.launch {
            roomRepository.saveRoom(levelId, room)
            _state.update { it.copy(isSaved = true) }
        }
    }

    private suspend fun load(id: Uuid) {
        val room = roomRepository.getRoom(id)
        original = room
        val rectangle = room?.shapeInput as? ShapeInput.Rectangle
        _state.update {
            it.copy(
                isLoading = false,
                name = room?.name.orEmpty(),
                height = room?.height?.toMetresInput().orEmpty(),
                length = rectangle?.length?.toMetresInput().orEmpty(),
                width = rectangle?.width?.toMetresInput().orEmpty(),
            )
        }
    }
}
