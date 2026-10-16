package dev.jurczyk.renoplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Transaction
    @Query("SELECT * FROM rooms WHERE levelId = :levelId ORDER BY position")
    fun observeByLevel(levelId: String): Flow<List<RoomWithWalls>>

    @Transaction
    @Query("SELECT * FROM rooms WHERE id = :id")
    fun observe(id: String): Flow<RoomWithWalls?>

    @Transaction
    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun get(id: String): RoomWithWalls?

    @Query("SELECT position FROM rooms WHERE id = :id")
    suspend fun position(id: String): Int?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM rooms WHERE levelId = :levelId")
    suspend fun nextPosition(levelId: String): Int

    /** An update, unlike REPLACE, does not cascade-delete the room's walls. */
    @Upsert
    suspend fun upsert(room: RoomEntity)

    @Query("DELETE FROM walls WHERE roomId = :roomId")
    suspend fun deleteWalls(roomId: String)

    @Insert
    suspend fun insertWalls(walls: List<WallEntity>)

    @Insert
    suspend fun insertOpenings(openings: List<OpeningEntity>)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun delete(id: String)

    /** Saves the room and replaces its walls and openings. [room]'s position is kept for an existing room. */
    @Transaction
    suspend fun save(
        room: RoomEntity,
        walls: List<WallEntity>,
        openings: List<OpeningEntity>,
    ) {
        val position = position(room.id) ?: nextPosition(room.levelId)
        upsert(room.copy(position = position))
        deleteWalls(room.id)
        insertWalls(walls)
        insertOpenings(openings)
    }
}
