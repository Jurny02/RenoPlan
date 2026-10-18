package dev.jurczyk.renoplan.presentation.level

import dev.jurczyk.renoplan.domain.cost.OperationGroupKey
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.testing.FakeStore
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class LevelBulkPriceTest : MainDispatcherTest() {
    private val white = OperationGroupKey(OperationType.Painting("biały"), MeasureUnit.SQUARE_METRE)

    private fun painting(surface: Surface) =
        Operation(
            Uuid.random(),
            OperationType.Painting("biały"),
            setOf(surface),
            Money.fromZloty(6.0),
            Labour.PerUnit(Money.fromZloty(15.0)),
        )

    /** Ceilings 12 m² and 4 m². */
    private val living =
        Room
            .rectangle(
                Uuid.random(),
                "Salon",
                4_000.mm,
                3_000.mm,
                2_500.mm,
            ).copy(operations = listOf(painting(Surface.Ceiling)))
    private val bathroom =
        Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 2_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    painting(Surface.Ceiling),
                    Operation(
                        Uuid.random(),
                        OperationType.Tiling,
                        setOf(Surface.Floor),
                        Money.fromZloty(80.0),
                        Labour.SelfDone,
                    ),
                ),
        )
    private val level = Level(Uuid.random(), "Parter", listOf(living, bathroom))
    private val store = FakeStore().apply { levels.value = listOf(level) }
    private val viewModel = LevelViewModel(level.id, store.levelRepository, store.roomRepository)

    private suspend fun loaded() = viewModel.state.first { !it.isLoading }

    private fun operations() = store.rooms().flatMap { it.operations }

    @Test
    fun editPriceForAllIsPrefilledAndPreviewed() =
        runTest {
            viewModel.openBulkPrice(white)
            val form = viewModel.bulkPriceForm!!
            assertEquals("6", form.pricing.materialPrice)
            assertEquals("15", form.pricing.labourPrice)

            viewModel.onBulkPricingChange(form.pricing.copy(materialPrice = "5", labourPrice = "10"))
            val preview = loaded().bulkPreview(viewModel.bulkPriceForm!!)

            assertEquals(2, preview.count)
            // 16 m² · (6 + 15) → 16 m² · (5 + 10).
            assertEquals(Money.fromZloty(336.0), preview.before)
            assertEquals(Money.fromZloty(240.0), preview.after)
        }

    @Test
    fun applyingChangesEveryOperationOfTheGroupOnly() =
        runTest {
            viewModel.openBulkPrice(white)
            viewModel.onBulkPricingChange(
                viewModel.bulkPriceForm!!.pricing.copy(
                    materialPrice = "5",
                    labourMode = LabourMode.LUMP_SUM,
                    labourPrice = "50",
                ),
            )
            viewModel.applyBulkPrice()

            val (first, second, tiling) = operations()
            assertEquals(
                listOf(Money.fromZloty(5.0), Money.fromZloty(5.0)),
                listOf(first.materialPrice, second.materialPrice),
            )
            assertEquals(Labour.LumpSum(Money.fromZloty(50.0)), first.labour)
            assertEquals(Money.fromZloty(80.0), tiling.materialPrice)
            assertNull(viewModel.bulkPriceForm)
        }

    @Test
    fun invalidPriceIsNotApplied() =
        runTest {
            viewModel.openBulkPrice(white)
            viewModel.onBulkPricingChange(viewModel.bulkPriceForm!!.pricing.copy(materialPrice = "-1"))

            assertNull(loaded().bulkPreview(viewModel.bulkPriceForm!!).after)
            viewModel.applyBulkPrice()
            assertEquals(Money.fromZloty(6.0), operations().first().materialPrice)
        }

    @Test
    fun selfDoneForAGroupShowsTheSavingAndApplies() =
        runTest {
            assertEquals(Money.fromZloty(240.0), loaded().selfDoneSaving(white))

            viewModel.applySelfDone(white)

            assertEquals(
                listOf(Labour.SelfDone, Labour.SelfDone),
                operations().filter { white.matches(it) }.map { it.labour },
            )
            assertEquals(Money.Zero, loaded().selfDoneSaving(white))
        }
}
