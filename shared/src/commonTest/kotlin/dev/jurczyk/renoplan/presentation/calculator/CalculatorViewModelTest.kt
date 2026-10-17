package dev.jurczyk.renoplan.presentation.calculator

import dev.jurczyk.renoplan.domain.calculator.PackageUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorViewModelTest : MainDispatcherTest() {
    private val calculator = CalculatorViewModel()

    private val price: Money? get() =
        calculator.state.result
            ?.price

    private fun fill(
        price: String = "200",
        amount: String = "5",
        coverage: String = "7",
    ) {
        calculator.onPackagePriceChange(price)
        calculator.onAmountChange(amount)
        calculator.onCoverageChange(coverage)
    }

    @Test
    fun startsEmptyWithTwoCoatsAndLitres() {
        val state = calculator.state

        assertEquals("2", state.coats)
        assertEquals(PackageUnit.LITRE, state.unit)
        assertNull(state.result)
        assertFalse(state.packagePriceError)
    }

    @Test
    fun liveResult() {
        fill()
        val result = assertNotNull(calculator.state.result)

        assertEquals(17.5, result.areaPerPackage, 1e-9)
        assertEquals(Money(1_143), result.price)
        assertEquals(Money(571), result.oneCoatPrice)
    }

    @Test
    fun acceptsCommaAndDot() {
        fill(price = "199,99", amount = "2.5", coverage = "10")
        calculator.onCoatsChange("1")

        assertEquals(Money(800), price)
    }

    @Test
    fun changingCoatsRecalculates() {
        fill()
        calculator.onCoatsChange("1")

        assertEquals(Money(571), price)
    }

    @Test
    fun unitDoesNotChangeTheResult() {
        fill()
        calculator.onUnitChange(PackageUnit.KILOGRAM)

        assertEquals(PackageUnit.KILOGRAM, calculator.state.unit)
        assertEquals(Money(1_143), price)
    }

    @Test
    fun invalidInputShowsErrorsAndNoResult() {
        fill(price = "abc", amount = "0", coverage = "-3")
        calculator.onCoatsChange("1,5")
        val state = calculator.state

        assertTrue(state.packagePriceError)
        assertTrue(state.amountError)
        assertTrue(state.coverageError)
        assertTrue(state.coatsError)
        assertNull(state.result)
    }

    @Test
    fun blankFieldsShowNoErrors() {
        fill(price = "", amount = "", coverage = "")
        calculator.onCoatsChange("")
        val state = calculator.state

        assertFalse(state.packagePriceError || state.amountError || state.coverageError || state.coatsError)
        assertNull(state.result)
    }

    @Test
    fun metreModeGivesThePricePerMetre() {
        val viewModel = CalculatorViewModel()
        viewModel.onModeChange(CalculatorMode.METRE)
        viewModel.onPackagePriceChange("50")
        viewModel.onMetresChange("2,5")

        assertEquals(Money.fromZloty(20.0), viewModel.state.pricePerMetre)
        assertNull(viewModel.state.result)
    }

    @Test
    fun metreModeWaitsForValidMetres() {
        val viewModel = CalculatorViewModel()
        viewModel.onModeChange(CalculatorMode.METRE)
        viewModel.onPackagePriceChange("50")
        viewModel.onMetresChange("0")

        assertTrue(viewModel.state.metresError)
        assertNull(viewModel.state.pricePerMetre)
    }

    @Test
    fun squareMetreModeIgnoresTheMetresField() {
        val viewModel = CalculatorViewModel()
        viewModel.onPackagePriceChange("50")
        viewModel.onMetresChange("2,5")

        assertNull(viewModel.state.pricePerMetre)
    }
}
