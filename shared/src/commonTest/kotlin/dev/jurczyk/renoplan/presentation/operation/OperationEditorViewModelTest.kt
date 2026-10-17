package dev.jurczyk.renoplan.presentation.operation

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class OperationEditorViewModelTest : MainDispatcherTest() {
    /** 4 × 3 m, 2.5 m high: walls 10 / 7.5 / 10 / 7.5 m², floor 12 m², skirting 14 m. */
    private val room = Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm)
    private val store = FakeStore().apply { levels.value = listOf(Level(LEVEL_ID, "Parter", listOf(room))) }

    private fun editor(operationId: Uuid? = null) =
        OperationEditorViewModel(LEVEL_ID, room.id, operationId, store.roomRepository, store.templateRepository)

    private suspend fun OperationEditorViewModel.loaded() = state.first { !it.isLoading }

    private fun savedOperations() = store.rooms().single().operations

    @Test
    fun paintingAllWallsWithPerUnitLabour() =
        runTest {
            val editor = editor()
            editor.onPricingChange(PricingForm(OperationKind.PAINTING, "biały", "6", LabourMode.PER_UNIT, "15"))
            editor.onAllWallsToggle()

            val state = editor.loaded()
            assertEquals(MeasureUnit.SQUARE_METRE, state.unit)
            assertEquals(35.0, state.quantity, 1e-9)
            assertEquals(Money.fromZloty(210.0), state.cost?.material)
            assertEquals(Money.fromZloty(525.0), state.cost?.labour)
            assertEquals(Money.fromZloty(735.0), state.cost?.total)

            editor.save()

            val saved = savedOperations().single()
            assertEquals(OperationType.Painting("biały"), saved.type)
            assertEquals((1..4).map { Surface.Wall(it) }.toSet(), saved.surfaces)
            assertTrue(editor.loaded().form.isSaved)
        }

    @Test
    fun paintingNeedsAColour() =
        runTest {
            val editor = editor()
            editor.onSurfaceToggle(Surface.Ceiling)

            assertFalse(editor.loaded().canSave)

            editor.onPricingChange(PricingForm(OperationKind.PAINTING, text = "biały"))
            assertTrue(editor.loaded().canSave)
        }

    @Test
    fun blankPricesCountAsZeroAndInvalidOnesBlockSaving() =
        runTest {
            val editor = editor()
            editor.onSurfaceToggle(Surface.Floor)
            editor.onKindChange(OperationKind.PRIMING)

            assertEquals(Money.Zero, editor.loaded().cost?.total)

            editor.onPricingChange(
                editor
                    .loaded()
                    .form.pricing
                    .copy(materialPrice = "-2"),
            )
            assertTrue(
                editor
                    .loaded()
                    .form.pricing.materialPriceError,
            )
            assertFalse(editor.loaded().canSave)
        }

    @Test
    fun areaAndLinearSurfacesAreNotMixed() =
        runTest {
            val editor = editor()
            editor.onSurfaceToggle(Surface.Wall(1))
            editor.onSurfaceToggle(Surface.Floor)
            editor.onSurfaceToggle(Surface.Skirting)

            assertEquals(setOf<Surface>(Surface.Skirting), editor.loaded().form.surfaces)

            editor.onAllWallsToggle()
            assertEquals((1..4).map { Surface.Wall(it) }.toSet(), editor.loaded().form.surfaces)

            editor.onAllWallsToggle()
            assertTrue(
                editor
                    .loaded()
                    .form.surfaces
                    .isEmpty(),
            )
        }

    @Test
    fun skirtingTypePreselectsTheSkirtingSurface() =
        runTest {
            val editor = editor()
            editor.onKindChange(OperationKind.SKIRTING)
            editor.onPricingChange(
                editor
                    .loaded()
                    .form.pricing
                    .copy(materialPrice = "25", labourMode = LabourMode.SELF_DONE),
            )

            val state = editor.loaded()
            assertEquals(MeasureUnit.METRE, state.unit)
            assertEquals(14.0, state.quantity, 1e-9)
            assertEquals(Money.fromZloty(350.0), state.cost?.total)
        }

    @Test
    fun switchingBetweenMouldingTypesSwitchesTheSurface() =
        runTest {
            val editor = editor()
            editor.onKindChange(OperationKind.SKIRTING)
            editor.onKindChange(OperationKind.CROWN_MOULDING)
            assertEquals(setOf<Surface>(Surface.CrownMoulding), editor.loaded().form.surfaces)

            editor.onKindChange(OperationKind.PAINTING)
            assertTrue(
                editor
                    .loaded()
                    .form.surfaces
                    .isEmpty(),
            )
        }

    @Test
    fun mouldingTypeReplacesAnAreaSelection() =
        runTest {
            val editor = editor()
            editor.onSurfaceToggle(Surface.Wall(1))
            editor.onKindChange(OperationKind.CROWN_MOULDING)

            assertEquals(setOf<Surface>(Surface.CrownMoulding), editor.loaded().form.surfaces)
        }

    @Test
    fun mouldingTypeKeepsMouldingsChosenByHand() =
        runTest {
            val editor = editor()
            editor.onKindChange(OperationKind.CUSTOM)
            editor.onSurfaceToggle(Surface.Skirting)
            editor.onSurfaceToggle(Surface.CrownMoulding)
            editor.onKindChange(OperationKind.SKIRTING)

            assertEquals(setOf(Surface.Skirting, Surface.CrownMoulding), editor.loaded().form.surfaces)
        }

    @Test
    fun customQuantityAloneUsesItsOwnUnit() =
        runTest {
            val editor = editor()
            editor.onKindChange(OperationKind.CROWN_MOULDING)
            editor.onSurfaceToggle(Surface.CrownMoulding)
            editor.onCustomUnitChange(MeasureUnit.METRE)
            editor.onCustomQuantityChange("4")
            editor.onPricingChange(
                editor
                    .loaded()
                    .form.pricing
                    .copy(materialPrice = "10", labourMode = LabourMode.SELF_DONE),
            )

            val state = editor.loaded()
            assertEquals(MeasureUnit.METRE, state.unit)
            assertEquals(4.0, state.quantity, 1e-9)
            assertEquals(Money.fromZloty(40.0), state.cost?.total)
        }

    @Test
    fun customQuantityIsAddedToTheSelectedSurfacesInTheirUnit() =
        runTest {
            val editor = editor()
            editor.onPricingChange(PricingForm(OperationKind.PAINTING, "zielony", "10", LabourMode.SELF_DONE))
            editor.onSurfaceToggle(Surface.Ceiling)
            editor.onCustomUnitChange(MeasureUnit.METRE)
            editor.onCustomQuantityChange("5")

            val state = editor.loaded()
            assertEquals(MeasureUnit.SQUARE_METRE, state.customUnit)
            assertEquals(17.0, state.quantity, 1e-9)

            editor.save()
            assertEquals(
                setOf(Surface.Ceiling, Surface.Custom(5.0, MeasureUnit.SQUARE_METRE)),
                savedOperations().single().surfaces,
            )
        }

    @Test
    fun invalidCustomQuantityBlocksSaving() =
        runTest {
            val editor = editor()
            editor.onPricingChange(PricingForm(OperationKind.PRIMING))
            editor.onCustomQuantityChange("abc")

            assertTrue(editor.loaded().customQuantityError)
            assertFalse(editor.loaded().canSave)
        }

    @Test
    fun editingLoadsTheCustomQuantityBack() =
        runTest {
            val half =
                Operation(
                    Uuid.random(),
                    OperationType.Painting("zielony"),
                    setOf(Surface.Custom(4.5, MeasureUnit.SQUARE_METRE)),
                    Money(600),
                    Labour.SelfDone,
                )
            store.roomRepository.saveRoom(LEVEL_ID, room.copy(operations = listOf(half)))

            val form = editor(half.id).loaded().form
            assertEquals("4,5", form.customQuantity)
            assertTrue(form.surfaces.isEmpty())
        }

    @Test
    fun lumpSumLabour() =
        runTest {
            val editor = editor()
            editor.onKindChange(OperationKind.TILING)
            editor.onSurfaceToggle(Surface.Floor)
            editor.onPricingChange(
                editor.loaded().form.pricing.copy(
                    materialPrice = "80",
                    labourMode = LabourMode.LUMP_SUM,
                    labourPrice = "1500",
                ),
            )

            assertEquals(Money.fromZloty(2_460.0), editor.loaded().cost?.total)
        }

    @Test
    fun templateFillsTypeAndPricesAndCanBeSavedFromAnOperation() =
        runTest {
            val white =
                OperationTemplate(
                    Uuid.random(),
                    OperationType.Painting("biały mat"),
                    Money(600),
                    Labour.PerUnit(Money(1_500)),
                )
            store.templates.value = listOf(white)
            val editor = editor()
            assertEquals(listOf(white), editor.loaded().templates)

            editor.applyTemplate(white)
            editor.onSurfaceToggle(Surface.Ceiling)
            editor.onSaveAsTemplateChange(true)
            editor.save()

            assertEquals(white.type, savedOperations().single().type)
            assertEquals(2, store.templates.value.size)
        }

    @Test
    fun editingAndDeletingAnExistingOperation() =
        runTest {
            val priming =
                Operation(Uuid.random(), OperationType.Priming, setOf(Surface.Ceiling), Money(300), Labour.SelfDone)
            store.roomRepository.saveRoom(LEVEL_ID, room.copy(operations = listOf(priming)))

            val editor = editor(priming.id)
            val loaded = editor.loaded()
            assertFalse(loaded.isNew)
            assertEquals(OperationKind.PRIMING, loaded.form.pricing.kind)
            assertEquals("3", loaded.form.pricing.materialPrice)

            editor.onStatusChange(OperationStatus.DONE)
            editor.save()
            assertEquals(listOf(priming.copy(status = OperationStatus.DONE)), savedOperations())

            editor(priming.id).delete()
            assertTrue(savedOperations().isEmpty())
        }

    @Test
    fun missingOperationOpensAnEmptyForm() =
        runTest {
            assertNull(editor(Uuid.random()).loaded().cost)
        }

    private companion object {
        val LEVEL_ID = Uuid.random()
    }
}
