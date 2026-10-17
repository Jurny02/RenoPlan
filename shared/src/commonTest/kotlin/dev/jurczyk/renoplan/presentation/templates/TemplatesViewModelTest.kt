package dev.jurczyk.renoplan.presentation.templates

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class TemplatesViewModelTest : MainDispatcherTest() {
    private val store = FakeStore()
    private val tiles =
        OperationTemplate(Uuid.random(), OperationType.Tiling, Money(8_000), Labour.LumpSum(Money(150_000)))

    @Test
    fun listAndDelete() =
        runTest {
            store.templates.value = listOf(tiles)
            val viewModel = TemplatesViewModel(store.templateRepository)

            assertEquals(listOf(tiles), viewModel.state.first { !it.isLoading }.templates)

            viewModel.deleteTemplate(tiles.id)
            assertTrue(store.templates.value.isEmpty())
        }

    @Test
    fun createATemplate() =
        runTest {
            val editor = TemplateEditorViewModel(templateId = null, templateRepository = store.templateRepository)
            assertFalse(editor.state.canSave)

            editor.onPricingChange(PricingForm(OperationKind.CUSTOM, "Tapeta", "35,5", LabourMode.PER_UNIT, "20"))
            editor.save()

            val saved = store.templates.value.single()
            assertEquals(OperationType.Custom("Tapeta"), saved.type)
            assertEquals(Money(3_550), saved.materialPrice)
            assertEquals(Labour.PerUnit(Money(2_000)), saved.labour)
            assertTrue(editor.state.isSaved)
        }

    @Test
    fun editAndDeleteATemplate() =
        runTest {
            store.templates.value = listOf(tiles)
            val editor = TemplateEditorViewModel(tiles.id, store.templateRepository)
            assertEquals("80", editor.state.pricing.materialPrice)
            assertEquals(LabourMode.LUMP_SUM, editor.state.pricing.labourMode)

            editor.onPricingChange(
                editor.state.pricing
                    .copy(materialPrice = "90"),
            )
            editor.save()
            assertEquals(listOf(tiles.copy(materialPrice = Money(9_000))), store.templates.value)

            TemplateEditorViewModel(tiles.id, store.templateRepository).delete()
            assertTrue(store.templates.value.isEmpty())
        }
}
