package dev.jurczyk.renoplan.presentation.operation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.cost.OperationCost
import dev.jurczyk.renoplan.domain.cost.cost
import dev.jurczyk.renoplan.domain.cost.quantity
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.unit
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.domain.repository.TemplateRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** What the user has entered in the operation editor. */
data class OperationForm(
    val pricing: PricingForm = PricingForm(),
    val surfaces: Set<Surface> = emptySet(),
    val status: OperationStatus = OperationStatus.TODO,
    val saveAsTemplate: Boolean = false,
    val isLoaded: Boolean = false,
    val isSaved: Boolean = false,
)

data class OperationEditorUiState(
    val isNew: Boolean = true,
    val room: Room? = null,
    val templates: List<OperationTemplate> = emptyList(),
    val form: OperationForm = OperationForm(),
) {
    val isLoading: Boolean get() = room == null || !form.isLoaded

    /** The unit of the selected surfaces, or `null` before any is selected. */
    val unit: MeasureUnit? get() = form.surfaces.firstOrNull()?.unit

    /** m² or m of the selected surfaces. */
    val quantity: Double get() = room?.let { room -> form.surfaces.sumOf { room.quantity(it) } } ?: 0.0

    fun operation(id: Uuid): Operation? {
        val pricing = form.pricing
        val type = pricing.type
        val material = pricing.material
        val labour = pricing.labour
        return if (type != null && material != null && labour != null) {
            form.surfaces.takeIf { it.isNotEmpty() }?.let { Operation(id, type, it, material, labour, form.status) }
        } else {
            null
        }
    }

    /** Live cost, or `null` while the form is incomplete. */
    val cost: OperationCost? get() = room?.let { room -> operation(Uuid.NIL)?.let { room.cost(it) } }

    val canSave: Boolean get() = !isLoading && operation(Uuid.NIL) != null
}

class OperationEditorViewModel(
    private val levelId: Uuid,
    private val roomId: Uuid,
    private val operationId: Uuid?,
    private val roomRepository: RoomRepository,
    private val templateRepository: TemplateRepository,
) : ViewModel() {
    private val form = MutableStateFlow(OperationForm(isLoaded = operationId == null))

    val state: StateFlow<OperationEditorUiState> =
        combine(
            roomRepository.observeRoom(roomId),
            templateRepository.observeTemplates(),
            form,
        ) { room, templates, form ->
            OperationEditorUiState(isNew = operationId == null, room = room, templates = templates, form = form)
        }.stateIn(viewModelScope, WhileUiSubscribed, OperationEditorUiState(isNew = operationId == null))

    init {
        if (operationId != null) viewModelScope.launch { load(operationId) }
    }

    fun onPricingChange(pricing: PricingForm) = form.update { it.copy(pricing = pricing) }

    /** Skirting and crown moulding preselect their own surface when nothing is selected yet. */
    fun onKindChange(kind: OperationKind) =
        form.update { current ->
            val surfaces =
                when {
                    current.surfaces.isNotEmpty() -> current.surfaces
                    kind == OperationKind.SKIRTING -> setOf(Surface.Skirting)
                    kind == OperationKind.CROWN_MOULDING -> setOf(Surface.CrownMoulding)
                    else -> current.surfaces
                }
            current.copy(pricing = current.pricing.copy(kind = kind), surfaces = surfaces)
        }

    /** Selecting a surface of the other unit (m² vs m) starts a new selection, because they cannot be mixed. */
    fun onSurfaceToggle(surface: Surface) =
        form.update { current ->
            val surfaces =
                when {
                    surface in current.surfaces -> current.surfaces - surface
                    current.surfaces.any { it.unit != surface.unit } -> setOf(surface)
                    else -> current.surfaces + surface
                }
            current.copy(surfaces = surfaces)
        }

    /** Selects every wall of the room, or clears them when all are selected. */
    fun onAllWallsToggle() {
        viewModelScope.launch {
            val walls =
                roomRepository
                    .getRoom(roomId)
                    ?.walls
                    .orEmpty()
                    .map { Surface.Wall(it.number) }
                    .toSet()
            form.update { current ->
                val surfaces =
                    when {
                        current.surfaces.containsAll(walls) -> current.surfaces - walls
                        current.surfaces.any { it.unit != MeasureUnit.SQUARE_METRE } -> walls
                        else -> current.surfaces + walls
                    }
                current.copy(surfaces = surfaces)
            }
        }
    }

    fun onStatusChange(status: OperationStatus) = form.update { it.copy(status = status) }

    fun onSaveAsTemplateChange(saveAsTemplate: Boolean) = form.update { it.copy(saveAsTemplate = saveAsTemplate) }

    /** Copies the template's type and prices; later edits of the template do not change this operation. */
    fun applyTemplate(template: OperationTemplate) =
        form.update { it.copy(pricing = PricingForm.from(template.type, template.materialPrice, template.labour)) }

    fun save() {
        val current = form.value
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            val ui = OperationEditorUiState(isNew = operationId == null, room = room, form = current)
            val operation = ui.operation(operationId ?: Uuid.random()) ?: return@launch
            val operations =
                if (room.operations.any { it.id == operation.id }) {
                    room.operations.map { if (it.id == operation.id) operation else it }
                } else {
                    room.operations + operation
                }
            roomRepository.saveRoom(levelId, room.copy(operations = operations))
            if (current.saveAsTemplate) {
                templateRepository.saveTemplate(
                    OperationTemplate(Uuid.random(), operation.type, operation.materialPrice, operation.labour),
                )
            }
            form.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        val id = operationId ?: return
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            roomRepository.saveRoom(levelId, room.copy(operations = room.operations.filterNot { it.id == id }))
            form.update { it.copy(isSaved = true) }
        }
    }

    private suspend fun load(id: Uuid) {
        val operation = roomRepository.getRoom(roomId)?.operations?.find { it.id == id }
        form.value =
            if (operation == null) {
                OperationForm(isLoaded = true)
            } else {
                OperationForm(
                    pricing = PricingForm.from(operation.type, operation.materialPrice, operation.labour),
                    surfaces = operation.surfaces,
                    status = operation.status,
                    isLoaded = true,
                )
            }
    }
}
