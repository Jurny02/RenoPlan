package dev.jurczyk.renoplan.data.repository

import dev.jurczyk.renoplan.data.local.LevelDao
import dev.jurczyk.renoplan.data.mapper.toDomain
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** [LevelRepository] backed by the local Room database. */
class LocalLevelRepository(
    private val dao: LevelDao,
) : LevelRepository {
    override fun observeLevels(): Flow<List<Level>> = dao.observeAll().map { levels -> levels.map { it.toDomain() } }

    override fun observeLevel(id: Uuid): Flow<Level?> = dao.observe(id.toString()).map { it?.toDomain() }

    override suspend fun getLevel(id: Uuid): Level? = dao.get(id.toString())?.toDomain()

    override suspend fun saveLevel(level: Level) = dao.save(level.id.toString(), level.name)

    override suspend fun deleteLevel(id: Uuid) = dao.delete(id.toString())
}
