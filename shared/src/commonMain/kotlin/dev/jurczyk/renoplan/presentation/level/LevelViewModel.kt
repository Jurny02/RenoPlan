package dev.jurczyk.renoplan.presentation.level

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.cost.GeometryTotals
import dev.jurczyk.renoplan.domain.cost.OperationCost
import dev.jurczyk.renoplan.domain.cost.OperationGroup
import dev.jurczyk.renoplan.domain.cost.OperationGroupKey
import dev.jurczyk.renoplan.domain.cost.cost
import dev.jurczyk.renoplan.domain.cost.costSummary
import dev.jurczyk.renoplan.domain.cost.geometryTotals
import dev.jurczyk.renoplan.domain.cost.groupCost
import dev.jurczyk.renoplan.domain.cost.operationCount
import dev.jurczyk.renoplan.domain.cost.operationGroups
import dev.jurczyk.renoplan.domain.cost.withPrices
import dev.jurczyk.renoplan.domain.cost.withSelfDoneLabour
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.wallsNetArea
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class RoomCard(
    val id: Uuid,
    val name: String,
    /** m² */
    val floorArea: Double,
    /** m² */
    val netWallArea: Double,
    val cost: CostSummary = CostSummary.Zero,
)

/** One operation of the level with its room and live cost, for the Costs tab. */
data class OperationEntry(
    val roomId: Uuid,
    val roomName: String,
    val operation: Operation,
    val cost: OperationCost,
)

/** A room's part of the Summary tab. */
data class RoomSummary(
    val id: Uuid,
    val name: String,
    val geometry: GeometryTotals,
    val groups: List<OperationGroup>,
)

data class LevelUiState(
    val name: String = "",
    val rooms: List<RoomCard> = emptyList(),
    val isLoading: Boolean = true,
    /** The level was deleted or never existed. */
    val isMissing: Boolean = false,
    val level: Level? = null,
) {
    val geometry: GeometryTotals get() = geometryTotals(level?.rooms.orEmpty())

    /** Whole level: one group per operation type. */
    val operationGroups: List<OperationGroup> get() = level?.operationGroups.orEmpty()

    /** By room: each room's geometry and operation groups. */
    val roomSummaries: List<RoomSummary>
        get() =
            level?.rooms.orEmpty().map { room ->
                RoomSummary(room.id, room.name, geometryTotals(listOf(room)), room.operationGroups)
            }

    val costSummary: CostSummary get() = level?.costSummary ?: CostSummary.Zero

    /** What "edit price for all" with [form] would change. */
    fun bulkPreview(form: BulkPriceForm): BulkPricePreview {
        val level = level ?: return BulkPricePreview(count = 0, before = Money.Zero, after = null)
        val material = form.pricing.material
        val labour = form.pricing.labour
        val after =
            if (material != null &&
                labour != null
            ) {
                level.withPrices(form.group, material, labour).groupCost(form.group).total
            } else {
                null
            }
        return BulkPricePreview(level.operationCount(form.group), level.groupCost(form.group).total, after)
    }

    /** How much "I do it myself" would save on [group]: its current labour cost. */
    fun selfDoneSaving(group: OperationGroupKey): Money = level?.groupCost(group)?.labour ?: Money.Zero

    /** Every operation of the level, in room order. */
    val operations: List<OperationEntry>
        get() =
            level?.rooms.orEmpty().flatMap { room ->
                room.operations.map { OperationEntry(room.id, room.name, it, room.cost(it)) }
            }
}

/** The "edit price for all" sheet for one operation group. */
data class BulkPriceForm(
    val group: OperationGroupKey,
    val pricing: PricingForm,
)

/** What "edit price for all" would change: how many operations and the group's cost before and after. */
data class BulkPricePreview(
    val count: Int,
    val before: Money,
    /** `null` while a price is invalid. */
    val after: Money?,
)

class LevelViewModel(
    val levelId: Uuid,
    private val levelRepository: LevelRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    val state: StateFlow<LevelUiState> =
        levelRepository
            .observeLevel(levelId)
            .map { level ->
                if (level == null) {
                    LevelUiState(isLoading = false, isMissing = true)
                } else {
                    LevelUiState(
                        name = level.name,
                        rooms = level.rooms.map { it.toCard() },
                        isLoading = false,
                        level = level,
                    )
                }
            }.stateIn(viewModelScope, WhileUiSubscribed, LevelUiState())

    /**
     * The open "edit price for all" sheet, or `null`. Compose state rather than a flow, so its text
     * fields get every change synchronously.
     */
    var bulkPriceForm: BulkPriceForm? by mutableStateOf(null)
        private set

    /** Opens "edit price for all" for [group], prefilled with the prices of its first operation. */
    fun openBulkPrice(group: OperationGroupKey) {
        viewModelScope.launch {
            val first =
                levelRepository
                    .getLevel(levelId)
                    ?.rooms
                    ?.flatMap { it.operations }
                    ?.firstOrNull(group::matches)
            bulkPriceForm =
                BulkPriceForm(
                    group = group,
                    pricing = first?.let { PricingForm.from(it.type, it.materialPrice, it.labour) } ?: PricingForm(),
                )
        }
    }

    fun onBulkPricingChange(pricing: PricingForm) {
        bulkPriceForm = bulkPriceForm?.copy(pricing = pricing)
    }

    fun dismissBulkPrice() {
        bulkPriceForm = null
    }

    /** Sets the sheet's material price and labour on every operation of its group on this level. */
    fun applyBulkPrice() {
        val form = bulkPriceForm
        val material = form?.pricing?.material
        val labour = form?.pricing?.labour
        if (form == null || material == null || labour == null) return
        updateLevel { it.withPrices(form.group, material, labour) }
        bulkPriceForm = null
    }

    /** Sets "I do it myself" labour on every operation of [group] on this level. */
    fun applySelfDone(group: OperationGroupKey) = updateLevel { it.withSelfDoneLabour(group) }

    fun renameRoom(
        id: Uuid,
        name: String,
    ) = updateRoom(id) { room ->
        name.trim().takeIf { it.isNotEmpty() }?.let { room.copy(name = it) }
    }

    /** Copies geometry and openings; [name] is the copy's name, e.g. "Kuchnia (kopia)". */
    fun duplicateRoom(
        id: Uuid,
        name: String,
    ) = updateRoom(id) { room -> room.duplicate(id = Uuid.random(), name = name.trim()) }

    fun deleteRoom(id: Uuid) {
        viewModelScope.launch { roomRepository.deleteRoom(id) }
    }

    /** Saves the rooms that [change] modifies. */
    private fun updateLevel(change: (Level) -> Level) {
        viewModelScope.launch {
            val level = levelRepository.getLevel(levelId) ?: return@launch
            change(level).rooms.zip(level.rooms).filter { (new, old) -> new != old }.forEach { (room, _) ->
                roomRepository.saveRoom(levelId, room)
            }
        }
    }

    private fun updateRoom(
        id: Uuid,
        change: (Room) -> Room?,
    ) {
        viewModelScope.launch {
            val room = roomRepository.getRoom(id) ?: return@launch
            change(room)?.let { roomRepository.saveRoom(levelId, it) }
        }
    }
}

private fun Room.toCard(): RoomCard =
    RoomCard(id = id, name = name, floorArea = floorArea, netWallArea = wallsNetArea, cost = costSummary)
