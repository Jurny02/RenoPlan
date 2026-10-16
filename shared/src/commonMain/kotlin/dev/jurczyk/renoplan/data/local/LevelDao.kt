package dev.jurczyk.renoplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {
    @Transaction
    @Query("SELECT * FROM levels ORDER BY position")
    fun observeAll(): Flow<List<LevelWithRooms>>

    @Transaction
    @Query("SELECT * FROM levels WHERE id = :id")
    fun observe(id: String): Flow<LevelWithRooms?>

    @Transaction
    @Query("SELECT * FROM levels WHERE id = :id")
    suspend fun get(id: String): LevelWithRooms?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM levels")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(level: LevelEntity)

    /** Returns the number of renamed rows. */
    @Query("UPDATE levels SET name = :name WHERE id = :id")
    suspend fun rename(
        id: String,
        name: String,
    ): Int

    @Query("DELETE FROM levels WHERE id = :id")
    suspend fun delete(id: String)

    /** Renames the level, or inserts it at the end of the list. */
    @Transaction
    suspend fun save(
        id: String,
        name: String,
    ) {
        if (rename(id, name) == 0) insert(LevelEntity(id = id, name = name, position = nextPosition()))
    }
}
