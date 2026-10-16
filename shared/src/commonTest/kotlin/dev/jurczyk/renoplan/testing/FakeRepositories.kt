package dev.jurczyk.renoplan.testing

import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.repository.LevelRepository
import dev.jurczyk.renoplan.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

/** In-memory levels with their rooms, shared by [FakeLevelRepository] and [FakeRoomRepository]. */
class FakeStore {
    val levels = MutableStateFlow<List<Level>>(emptyList())

    val levelRepository = FakeLevelRepository(this)
    val roomRepository = FakeRoomRepository(this)

    fun rooms(): List<Room> = levels.value.flatMap { it.rooms }
}

class FakeLevelRepository(
    private val store: FakeStore,
) : LevelRepository {
    override fun observeLevels(): Flow<List<Level>> = store.levels

    override fun observeLevel(id: Uuid): Flow<Level?> = store.levels.map { levels -> levels.find { it.id == id } }

    override suspend fun getLevel(id: Uuid): Level? = store.levels.value.find { it.id == id }

    override suspend fun saveLevel(level: Level) =
        store.levels.update { levels ->
            if (levels.any { it.id == level.id }) {
                levels.map { if (it.id == level.id) it.copy(name = level.name) else it }
            } else {
                levels + level.copy(rooms = emptyList())
            }
        }

    override suspend fun deleteLevel(id: Uuid) = store.levels.update { levels -> levels.filterNot { it.id == id } }
}

class FakeRoomRepository(
    private val store: FakeStore,
) : RoomRepository {
    override fun observeRooms(levelId: Uuid): Flow<List<Room>> =
        store.levels.map { levels -> levels.find { it.id == levelId }?.rooms.orEmpty() }

    override fun observeRoom(id: Uuid): Flow<Room?> = store.levels.map { store.rooms().find { it.id == id } }

    override suspend fun getRoom(id: Uuid): Room? = store.rooms().find { it.id == id }

    override suspend fun saveRoom(
        levelId: Uuid,
        room: Room,
    ) = store.levels.update { levels ->
        levels.map { level ->
            when {
                level.id != levelId -> {
                    level
                }

                level.rooms.any { it.id == room.id } -> {
                    level.copy(rooms = level.rooms.map { if (it.id == room.id) room else it })
                }

                else -> {
                    level.copy(rooms = level.rooms + room)
                }
            }
        }
    }

    override suspend fun deleteRoom(id: Uuid) =
        store.levels.update { levels ->
            levels.map { level -> level.copy(rooms = level.rooms.filterNot { it.id == id }) }
        }
}
