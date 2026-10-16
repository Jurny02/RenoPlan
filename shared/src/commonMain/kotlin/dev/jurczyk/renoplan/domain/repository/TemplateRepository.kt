package dev.jurczyk.renoplan.domain.repository

import dev.jurczyk.renoplan.domain.model.OperationTemplate
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/** Operation templates, in the order they were created. */
interface TemplateRepository {
    fun observeTemplates(): Flow<List<OperationTemplate>>

    suspend fun getTemplate(id: Uuid): OperationTemplate?

    /** Inserts the template or replaces an existing one. */
    suspend fun saveTemplate(template: OperationTemplate)

    suspend fun deleteTemplate(id: Uuid)
}
