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

    /** The room's current position, or the next free one in level [levelId] for a new room. */
    @Query(
        """
        SELECT COALESCE(
            (SELECT position FROM rooms WHERE id = :id),
            (SELECT COALESCE(MAX(position) + 1, 0) FROM rooms WHERE levelId = :levelId)
        )
        """,
    )
    suspend fun positionFor(
        id: String,
        levelId: String,
    ): Int

    /** An update, unlike REPLACE, does not cascade-delete the room's walls. */
    @Upsert
    suspend fun upsert(room: RoomEntity)

    @Query("DELETE FROM walls WHERE roomId = :roomId")
    suspend fun deleteWalls(roomId: String)

    @Query("DELETE FROM operations WHERE roomId = :roomId")
    suspend fun deleteOperations(roomId: String)

    @Insert
    suspend fun insertChildren(
        walls: List<WallEntity>,
        openings: List<OpeningEntity>,
        operations: List<OperationEntity>,
    )

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Saves the room and replaces its walls, openings and operations. [room]'s position is kept for an
     * existing room.
     */
    @Transaction
    suspend fun save(
        room: RoomEntity,
        walls: List<WallEntity>,
        openings: List<OpeningEntity>,
        operations: List<OperationEntity>,
    ) {
        upsert(room.copy(position = positionFor(room.id, room.levelId)))
        // Deleting the walls also deletes their openings.
        deleteWalls(room.id)
        deleteOperations(room.id)
        insertChildren(walls, openings, operations)
    }
}
