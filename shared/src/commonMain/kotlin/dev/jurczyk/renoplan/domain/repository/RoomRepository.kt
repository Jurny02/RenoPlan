package dev.jurczyk.renoplan.domain.repository

import dev.jurczyk.renoplan.domain.model.Room
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/** Rooms with their walls and openings. */
interface RoomRepository {
    /** Rooms of a level, in the order they were added. */
    fun observeRooms(levelId: Uuid): Flow<List<Room>>

    fun observeRoom(id: Uuid): Flow<Room?>

    suspend fun getRoom(id: Uuid): Room?

    /** Inserts or replaces the room with all its walls and openings. A new room goes to the end of the level. */
    suspend fun saveRoom(
        levelId: Uuid,
        room: Room,
    )

    suspend fun deleteRoom(id: Uuid)
}
