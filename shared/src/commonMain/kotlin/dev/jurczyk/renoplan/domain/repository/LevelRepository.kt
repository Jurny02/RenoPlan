package dev.jurczyk.renoplan.domain.repository

import dev.jurczyk.renoplan.domain.model.Level
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/** Levels with their rooms, in the order they were created. */
interface LevelRepository {
    fun observeLevels(): Flow<List<Level>>

    fun observeLevel(id: Uuid): Flow<Level?>

    suspend fun getLevel(id: Uuid): Level?

    /** Inserts the level or renames an existing one. Its rooms are saved through [RoomRepository]. */
    suspend fun saveLevel(level: Level)

    /** Deletes the level with all its rooms. */
    suspend fun deleteLevel(id: Uuid)
}
