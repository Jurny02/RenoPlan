package dev.jurczyk.renoplan.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY position")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: String): TemplateEntity?

    @Query("SELECT position FROM templates WHERE id = :id")
    suspend fun position(id: String): Int?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM templates")
    suspend fun nextPosition(): Int

    @Upsert
    suspend fun upsert(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: String)

    /** Saves the template, keeping the position of an existing one; a new one goes to the end. */
    @Transaction
    suspend fun save(template: TemplateEntity) {
        upsert(template.copy(position = position(template.id) ?: nextPosition()))
    }
}
