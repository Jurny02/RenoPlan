package dev.jurczyk.renoplan.presentation.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.repository.TemplateRepository
import dev.jurczyk.renoplan.presentation.common.WhileUiSubscribed
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class TemplatesUiState(
    val templates: List<OperationTemplate> = emptyList(),
    val isLoading: Boolean = true,
)

class TemplatesViewModel(
    private val templateRepository: TemplateRepository,
) : ViewModel() {
    val state: StateFlow<TemplatesUiState> =
        templateRepository
            .observeTemplates()
            .map { TemplatesUiState(templates = it, isLoading = false) }
            .stateIn(viewModelScope, WhileUiSubscribed, TemplatesUiState())

    fun deleteTemplate(id: Uuid) {
        viewModelScope.launch { templateRepository.deleteTemplate(id) }
    }
}

data class TemplateEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val pricing: PricingForm = PricingForm(),
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && pricing.isValid
}

/** Edits template [templateId], or creates one when it is `null`. */
class TemplateEditorViewModel(
    private val templateId: Uuid?,
    private val templateRepository: TemplateRepository,
) : ViewModel() {
    /**
     * The form. Compose state rather than a flow, so text fields get every change synchronously;
     * asynchronous updates move the cursor (seen on iOS).
     */
    var state: TemplateEditorUiState by mutableStateOf(
        TemplateEditorUiState(
            isNew = templateId == null,
            isLoading =
                templateId != null,
        ),
    )
        private set

    init {
        if (templateId != null) {
            viewModelScope.launch {
                val template = templateRepository.getTemplate(templateId)
                update { state ->
                    state.copy(
                        isLoading = false,
                        pricing =
                            template?.let { PricingForm.from(it.type, it.materialPrice, it.labour) } ?: state.pricing,
                    )
                }
            }
        }
    }

    fun onPricingChange(pricing: PricingForm) = update { it.copy(pricing = pricing) }

    fun save() {
        val pricing = state.pricing
        val type = pricing.type
        val material = pricing.material
        val labour = pricing.labour
        if (type == null || material == null || labour == null) return
        viewModelScope.launch {
            templateRepository.saveTemplate(OperationTemplate(templateId ?: Uuid.random(), type, material, labour))
            update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        val id = templateId ?: return
        viewModelScope.launch {
            templateRepository.deleteTemplate(id)
            update { it.copy(isSaved = true) }
        }
    }

    private fun update(change: (TemplateEditorUiState) -> TemplateEditorUiState) {
        state = change(state)
    }
}
