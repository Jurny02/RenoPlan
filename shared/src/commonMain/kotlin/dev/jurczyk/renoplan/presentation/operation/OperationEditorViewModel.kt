package dev.jurczyk.renoplan.presentation.operation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dev.jurczyk.renoplan.presentation.common.parseSquareMetres
import dev.jurczyk.renoplan.presentation.common.toSquareMetresInput
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Where an operation's quantity comes from: measured surfaces, or a number the user types. */
enum class QuantityMode {
    SURFACES,
    CUSTOM,
}

/** What the user has entered in the operation editor. */
data class OperationForm(
    val pricing: PricingForm = PricingForm(),
    val quantityMode: QuantityMode = QuantityMode.SURFACES,
    /** Measured surfaces, used in [QuantityMode.SURFACES]. */
    val surfaces: Set<Surface> = emptySet(),
    /** The typed quantity (e.g. half a wall), used in [QuantityMode.CUSTOM]. */
    val customQuantity: String = "",
    val customUnit: MeasureUnit = MeasureUnit.SQUARE_METRE,
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

    val customQuantityError: Boolean get() =
        form.quantityMode == QuantityMode.CUSTOM &&
            form.customQuantity.isNotBlank() &&
            parseSquareMetres(form.customQuantity) == null

    /** The surfaces of the operation: the selected ones, or the typed quantity alone. */
    val allSurfaces: Set<Surface>
        get() =
            when (form.quantityMode) {
                QuantityMode.SURFACES -> {
                    form.surfaces
                }

                QuantityMode.CUSTOM -> {
                    setOfNotNull(parseSquareMetres(form.customQuantity)?.let { Surface.Custom(it, form.customUnit) })
                }
            }

    /** The unit of the operation, or `null` before anything is selected or typed. */
    val unit: MeasureUnit?
        get() =
            when (form.quantityMode) {
                QuantityMode.SURFACES -> form.surfaces.firstOrNull()?.unit
                QuantityMode.CUSTOM -> form.customUnit
            }

    /** m² or m of the selected surfaces, or the typed quantity. */
    val quantity: Double get() = room?.let { room -> allSurfaces.sumOf { room.quantity(it) } } ?: 0.0

    fun operation(id: Uuid): Operation? {
        val pricing = form.pricing
        val type = pricing.type
        val material = pricing.material
        val labour = pricing.labour
        val surfaces = allSurfaces.takeIf { it.isNotEmpty() && !customQuantityError }
        return if (type != null && material != null && labour != null) {
            surfaces?.let { Operation(id, type, it, material, labour, form.status) }
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
    /**
     * What the user has entered. Compose state rather than a flow, so text fields get every change
     * synchronously; asynchronous updates move the cursor (seen on iOS). The screen combines it with
     * [state].
     */
    var form: OperationForm by mutableStateOf(OperationForm(isLoaded = operationId == null))
        private set

    /** The room and templates, without the form; see [form]. */
    val state: StateFlow<OperationEditorUiState> =
        combine(roomRepository.observeRoom(roomId), templateRepository.observeTemplates()) { room, templates ->
            OperationEditorUiState(isNew = operationId == null, room = room, templates = templates)
        }.stateIn(viewModelScope, WhileUiSubscribed, OperationEditorUiState(isNew = operationId == null))

    init {
        if (operationId != null) viewModelScope.launch { load(operationId) }
    }

    fun onPricingChange(pricing: PricingForm) = updateForm { it.copy(pricing = pricing) }

    /**
     * Skirting and crown moulding select their own surface unless other mouldings are already chosen
     * (an area selection or the other moulding is replaced). Leaving a moulding type clears the
     * moulding it selected.
     */
    fun onKindChange(kind: OperationKind) =
        updateForm { current ->
            val moulding = kind.moulding
            val previous = current.pricing.kind.moulding
            val surfaces =
                when {
                    moulding != null && current.surfaces.none { it.unit == MeasureUnit.METRE && it != previous } -> {
                        setOf(moulding)
                    }

                    moulding == null && previous != null && current.surfaces == setOf(previous) -> {
                        emptySet()
                    }

                    else -> {
                        current.surfaces
                    }
                }
            val customUnit = if (moulding != null) MeasureUnit.METRE else current.customUnit
            current.copy(pricing = current.pricing.copy(kind = kind), surfaces = surfaces, customUnit = customUnit)
        }

    /** Switching keeps what was entered in the other mode, but only the chosen one counts. */
    fun onQuantityModeChange(mode: QuantityMode) = updateForm { it.copy(quantityMode = mode) }

    /** Changes the typed quantity, its unit, or both. */
    fun onCustomQuantityChange(
        quantity: String = form.customQuantity,
        unit: MeasureUnit = form.customUnit,
    ) = updateForm { it.copy(customQuantity = quantity, customUnit = unit) }

    /** Selecting a surface of the other unit (m² vs m) starts a new selection, because they cannot be mixed. */
    fun onSurfaceToggle(surface: Surface) =
        updateForm { current ->
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
            updateForm { current ->
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

    fun onStatusChange(status: OperationStatus) = updateForm { it.copy(status = status) }

    fun onSaveAsTemplateChange(saveAsTemplate: Boolean) = updateForm { it.copy(saveAsTemplate = saveAsTemplate) }

    /** Copies the template's type and prices; later edits of the template do not change this operation. */
    fun applyTemplate(template: OperationTemplate) =
        updateForm { it.copy(pricing = PricingForm.from(template.type, template.materialPrice, template.labour)) }

    fun save() {
        val current = form
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
            updateForm { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        val id = operationId ?: return
        viewModelScope.launch {
            val room = roomRepository.getRoom(roomId) ?: return@launch
            roomRepository.saveRoom(levelId, room.copy(operations = room.operations.filterNot { it.id == id }))
            updateForm { it.copy(isSaved = true) }
        }
    }

    private fun updateForm(change: (OperationForm) -> OperationForm) {
        form = change(form)
    }

    private suspend fun load(id: Uuid) {
        val operation = roomRepository.getRoom(roomId)?.operations?.find { it.id == id }
        form =
            if (operation == null) {
                OperationForm(isLoaded = true)
            } else {
                val custom = operation.surfaces.filterIsInstance<Surface.Custom>().firstOrNull()
                OperationForm(
                    pricing = PricingForm.from(operation.type, operation.materialPrice, operation.labour),
                    quantityMode = if (custom != null) QuantityMode.CUSTOM else QuantityMode.SURFACES,
                    surfaces = operation.surfaces.filterNot { it is Surface.Custom }.toSet(),
                    customQuantity = custom?.quantity?.toSquareMetresInput().orEmpty(),
                    customUnit = custom?.unit ?: MeasureUnit.SQUARE_METRE,
                    status = operation.status,
                    isLoaded = true,
                )
            }
    }
}

private val OperationKind.moulding: Surface?
    get() =
        when (this) {
            OperationKind.SKIRTING -> Surface.Skirting
            OperationKind.CROWN_MOULDING -> Surface.CrownMoulding
            else -> null
        }
