package dev.jurczyk.renoplan.presentation.calculator

import androidx.lifecycle.ViewModel
import dev.jurczyk.renoplan.domain.calculator.PackageUnit
import dev.jurczyk.renoplan.domain.calculator.PricePerSquareMetre
import dev.jurczyk.renoplan.domain.calculator.pricePerSquareMetre
import dev.jurczyk.renoplan.domain.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val DEFAULT_COATS = 2

/** The calculator form as typed: the price in złoty, amounts and coverage with a comma or a dot. */
data class CalculatorUiState(
    val packagePrice: String = "",
    val amount: String = "",
    val unit: PackageUnit = PackageUnit.LITRE,
    val coverage: String = "",
    val coats: String = DEFAULT_COATS.toString(),
) {
    val packagePriceError: Boolean get() = packagePrice.isNotBlank() && parsePrice(packagePrice) == null
    val amountError: Boolean get() = amount.isNotBlank() && parsePositiveDecimal(amount) == null
    val coverageError: Boolean get() = coverage.isNotBlank() && parsePositiveDecimal(coverage) == null
    val coatsError: Boolean get() = coats.isNotBlank() && parseCoats(coats) == null

    /** Live result, or `null` until every field is valid. */
    val result: PricePerSquareMetre?
        get() {
            val price = parsePrice(packagePrice) ?: return null
            val amount = parsePositiveDecimal(amount) ?: return null
            val coverage = parsePositiveDecimal(coverage) ?: return null
            val coats = parseCoats(coats) ?: return null
            return pricePerSquareMetre(price, amount, coverage, coats)
        }
}

/** Price-per-m² calculator (screen 9). Nothing is stored; the user copies the result into an operation. */
class CalculatorViewModel : ViewModel() {
    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    fun onPackagePriceChange(text: String) = _state.update { it.copy(packagePrice = text) }

    fun onAmountChange(text: String) = _state.update { it.copy(amount = text) }

    fun onUnitChange(unit: PackageUnit) = _state.update { it.copy(unit = unit) }

    fun onCoverageChange(text: String) = _state.update { it.copy(coverage = text) }

    fun onCoatsChange(text: String) = _state.update { it.copy(coats = text) }
}

private fun parsePositiveDecimal(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }

/** A price typed in złoty, e.g. `"199,99"`, rounded to the grosz. Must be at least 1 grosz. */
private fun parsePrice(text: String): Money? =
    parsePositiveDecimal(text)?.let(Money::fromZloty)?.takeIf { it > Money.Zero }

private fun parseCoats(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }
