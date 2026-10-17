package dev.jurczyk.renoplan.presentation.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.cost.GeometryTotals
import dev.jurczyk.renoplan.domain.cost.OperationCost
import dev.jurczyk.renoplan.domain.cost.OperationGroup
import dev.jurczyk.renoplan.domain.cost.cost
import dev.jurczyk.renoplan.domain.cost.costSummary
import dev.jurczyk.renoplan.domain.cost.geometryTotals
import dev.jurczyk.renoplan.domain.cost.operationGroups
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.wallsNetArea
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
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

    /** Every operation of the level, in room order. */
    val operations: List<OperationEntry>
        get() =
            level?.rooms.orEmpty().flatMap { room ->
                room.operations.map { OperationEntry(room.id, room.name, it, room.cost(it)) }
            }
}

class LevelViewModel(
    val levelId: Uuid,
    levelRepository: LevelRepository,
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
