package dev.jurczyk.renoplan.presentation.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class LevelCard(
    val id: Uuid,
    val name: String,
    val roomCount: Int,
    /** Total floor area in m². */
    val floorArea: Double,
)

data class LevelsUiState(
    val levels: List<LevelCard> = emptyList(),
    val isLoading: Boolean = true,
)

class LevelsViewModel(
    private val levelRepository: LevelRepository,
) : ViewModel() {
    val state: StateFlow<LevelsUiState> =
        levelRepository
            .observeLevels()
            .map { levels -> LevelsUiState(levels = levels.map { it.toCard() }, isLoading = false) }
            .stateIn(viewModelScope, WhileUiSubscribed, LevelsUiState())

    fun addLevel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { levelRepository.saveLevel(Level(id = Uuid.random(), name = trimmed)) }
    }

    fun renameLevel(
        id: Uuid,
        name: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            levelRepository.getLevel(id)?.let { levelRepository.saveLevel(it.copy(name = trimmed)) }
        }
    }

    fun deleteLevel(id: Uuid) {
        viewModelScope.launch { levelRepository.deleteLevel(id) }
    }
}

private fun Level.toCard(): LevelCard =
    LevelCard(id = id, name = name, roomCount = rooms.size, floorArea = rooms.sumOf { it.floorArea })
