package dev.jurczyk.renoplan.presentation.roomdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.cost.OperationCost
import dev.jurczyk.renoplan.domain.cost.cost
import dev.jurczyk.renoplan.domain.cost.costSummary
import dev.jurczyk.renoplan.domain.geometry.OpeningError
import dev.jurczyk.renoplan.domain.geometry.ceilingArea
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.perimeter
import dev.jurczyk.renoplan.domain.geometry.skirtingLength
import dev.jurczyk.renoplan.domain.geometry.validateOpenings
import dev.jurczyk.renoplan.domain.geometry.volume
import dev.jurczyk.renoplan.domain.geometry.wallNetArea
import dev.jurczyk.renoplan.domain.geometry.wallsNetArea
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Room details with the opening sheet. Every number is derived from [room]. */
data class RoomDetailsUiState(
    val room: Room? = null,
    val isLoading: Boolean = true,
    /** The open opening sheet, or `null` when it is closed. */
    val form: OpeningForm? = null,
) {
    /** The room was deleted or never existed. */
    val isMissing: Boolean get() = !isLoading && room == null

    /** m² */
    val floorArea: Double get() = room?.floorArea ?: 0.0
    val ceilingArea: Double get() = room?.ceilingArea ?: 0.0
    val wallsNetArea: Double get() = room?.wallsNetArea ?: 0.0
    val perimeter: Millimetres get() = room?.perimeter ?: Millimetres.Zero
    val skirtingLength: Millimetres get() = room?.skirtingLength ?: Millimetres.Zero

    /** m³ */
    val volume: Double get() = room?.volume ?: 0.0

    /** The room with the sheet's opening applied, or `null` while the sheet is incomplete. */
    val formRoom: Room?
        get() {
            val opening = form?.opening
            return if (room != null && opening != null) room.withOpening(form.wallNumber, opening) else null
        }

    /** Problems of the sheet's opening and of its wall. */
    val formErrors: List<OpeningError>
        get() {
            val form = form ?: return emptyList()
            return formRoom?.validateOpenings().orEmpty().filter { error ->
                error.wallNumber == form.wallNumber &&
                    when (error) {
                        is OpeningError.WiderThanWall -> error.openingId == form.id
                        is OpeningError.TallerThanRoom -> error.openingId == form.id
                        is OpeningError.OpeningsExceedWallArea -> true
                    }
            }
        }

    /** Net area in m² of the sheet's wall after the change. */
    val formWallNetArea: Double?
        get() {
            val changed = formRoom ?: return null
            return changed.walls.find { it.number == form?.wallNumber }?.let { changed.wallNetArea(it) }
        }

    val canSaveForm: Boolean get() = formRoom != null && formErrors.isEmpty()

    /** Each operation with its live cost. */
    val operations: List<Pair<Operation, OperationCost>>
        get() = room?.let { room -> room.operations.map { it to room.cost(it) } }.orEmpty()

    val costSummary: CostSummary? get() = room?.costSummary
}

class RoomDetailsViewModel(
    private val levelId: Uuid,
    private val roomId: Uuid,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val form = MutableStateFlow<OpeningForm?>(null)

    val state: StateFlow<RoomDetailsUiState> =
        combine(roomRepository.observeRoom(roomId), form) { room, form ->
            RoomDetailsUiState(room = room, isLoading = false, form = form)
        }.stateIn(viewModelScope, WhileUiSubscribed, RoomDetailsUiState())

    /** Opens the sheet for a new opening, on [wallNumber] when opened from a wall. */
    fun addOpening(wallNumber: Int = 1) {
        form.value = OpeningForm.new(wallNumber)
    }

    fun editOpening(id: Uuid) {
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            val wall = room.walls.find { wall -> wall.openings.any { it.id == id } } ?: return@launch
            form.value = OpeningForm.edit(wall.openings.first { it.id == id }, wall.number)
        }
    }

    fun onFormChange(changed: OpeningForm) {
        form.value = changed
    }

    fun dismissForm() {
        form.value = null
    }

    fun saveOpening() {
        val current = form.value ?: return
        viewModelScope.launch {
            val state = RoomDetailsUiState(room = roomRepository.getRoom(roomId), isLoading = false, form = current)
            val changed = state.formRoom?.takeIf { state.canSaveForm } ?: return@launch
            roomRepository.saveRoom(levelId, changed)
            form.value = null
        }
    }

    /** Moves the operation's status on: to do → in progress → done → to do. */
    fun cycleStatus(operationId: Uuid) {
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            val operations =
                room.operations.map { operation ->
                    if (operation.id == operationId) operation.copy(status = operation.status.next()) else operation
                }
            roomRepository.saveRoom(levelId, room.copy(operations = operations))
        }
    }

    fun deleteOpening() {
        val current = form.value ?: return
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            roomRepository.saveRoom(levelId, room.withoutOpening(current.id))
            form.value = null
        }
    }
}

private fun OperationStatus.next(): OperationStatus =
    OperationStatus.entries[
        (ordinal + 1) %
            OperationStatus.entries.size,
    ]
