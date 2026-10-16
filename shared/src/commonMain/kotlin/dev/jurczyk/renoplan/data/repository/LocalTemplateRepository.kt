package dev.jurczyk.renoplan.data.repository

import dev.jurczyk.renoplan.data.local.TemplateDao
import dev.jurczyk.renoplan.data.mapper.toDomain
import dev.jurczyk.renoplan.data.mapper.toEntity
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/** [TemplateRepository] backed by the local Room database. */
class LocalTemplateRepository(
    private val dao: TemplateDao,
) : TemplateRepository {
    override fun observeTemplates(): Flow<List<OperationTemplate>> =
        dao.observeAll().map { templates -> templates.map { it.toDomain() } }

    override suspend fun getTemplate(id: Uuid): OperationTemplate? = dao.get(id.toString())?.toDomain()

    override suspend fun saveTemplate(template: OperationTemplate) = dao.save(template.toEntity())

    override suspend fun deleteTemplate(id: Uuid) = dao.delete(id.toString())
}
