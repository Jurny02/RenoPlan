package dev.jurczyk.renoplan.data.repository

import dev.jurczyk.renoplan.data.local.RoomDao
import dev.jurczyk.renoplan.data.mapper.toDomain
import dev.jurczyk.renoplan.data.mapper.toEntities
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** [RoomRepository] backed by the local Room database. */
class LocalRoomRepository(
    private val dao: RoomDao,
) : RoomRepository {
    override fun observeRooms(levelId: Uuid): Flow<List<Room>> =
        dao.observeByLevel(levelId.toString()).map { rooms -> rooms.map { it.toDomain() } }

    override fun observeRoom(id: Uuid): Flow<Room?> = dao.observe(id.toString()).map { it?.toDomain() }

    override suspend fun getRoom(id: Uuid): Room? = dao.get(id.toString())?.toDomain()

    override suspend fun saveRoom(
        levelId: Uuid,
        room: Room,
    ) {
        val entities = room.toEntities(levelId)
        dao.save(entities.room, entities.walls, entities.openings)
    }

    override suspend fun deleteRoom(id: Uuid) = dao.delete(id.toString())
}
