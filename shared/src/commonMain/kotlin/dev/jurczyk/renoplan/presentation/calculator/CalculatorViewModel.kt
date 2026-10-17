package dev.jurczyk.renoplan.presentation.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.jurczyk.renoplan.domain.calculator.PackageUnit
import dev.jurczyk.renoplan.domain.calculator.PricePerSquareMetre
import dev.jurczyk.renoplan.domain.calculator.pricePerMetre
import dev.jurczyk.renoplan.domain.calculator.pricePerSquareMetre
import dev.jurczyk.renoplan.domain.model.Money

private const val DEFAULT_COATS = 2

/** What the price is per: m² (paint, glue: coverage and coats) or running metre (skirting, trims). */
enum class CalculatorMode {
    SQUARE_METRE,
    METRE,
}

/** The calculator form as typed: the price in złoty, amounts and coverage with a comma or a dot. */
data class CalculatorUiState(
    val mode: CalculatorMode = CalculatorMode.SQUARE_METRE,
    val packagePrice: String = "",
    /** Metres in one package, used in [CalculatorMode.METRE]. */
    val metres: String = "",
    val amount: String = "",
    val unit: PackageUnit = PackageUnit.LITRE,
    val coverage: String = "",
    val coats: String = DEFAULT_COATS.toString(),
) {
    val packagePriceError: Boolean get() = packagePrice.isNotBlank() && parsePrice(packagePrice) == null
    val amountError: Boolean get() = amount.isNotBlank() && parsePositiveDecimal(amount) == null
    val coverageError: Boolean get() = coverage.isNotBlank() && parsePositiveDecimal(coverage) == null
    val coatsError: Boolean get() = coats.isNotBlank() && parseCoats(coats) == null
    val metresError: Boolean get() = metres.isNotBlank() && parsePositiveDecimal(metres) == null

    /** Price per running metre in [CalculatorMode.METRE], or `null` until both fields are valid. */
    val pricePerMetre: Money?
        get() {
            val price = parsePrice(packagePrice)
            val metres = parsePositiveDecimal(metres)
            val ready = mode == CalculatorMode.METRE && price != null && metres != null
            return if (ready) pricePerMetre(price, metres) else null
        }

    /** Live price per m² in [CalculatorMode.SQUARE_METRE], or `null` until every field is valid. */
    val result: PricePerSquareMetre?
        get() {
            if (mode != CalculatorMode.SQUARE_METRE) return null
            val price = parsePrice(packagePrice) ?: return null
            val amount = parsePositiveDecimal(amount) ?: return null
            val coverage = parsePositiveDecimal(coverage) ?: return null
            val coats = parseCoats(coats) ?: return null
            return pricePerSquareMetre(price, amount, coverage, coats)
        }
}

/** Price per m² or per metre calculator (screen 9). Nothing is stored; the user copies the result into an operation. */
class CalculatorViewModel : ViewModel() {
    /**
     * The inputs. Compose state rather than a flow, so text fields get every change synchronously;
     * asynchronous updates move the cursor (seen on iOS).
     */
    var state: CalculatorUiState by mutableStateOf(CalculatorUiState())
        private set

    fun onModeChange(mode: CalculatorMode) = update { it.copy(mode = mode) }

    fun onPackagePriceChange(text: String) = update { it.copy(packagePrice = text) }

    fun onMetresChange(text: String) = update { it.copy(metres = text) }

    fun onAmountChange(text: String) = update { it.copy(amount = text) }

    fun onUnitChange(unit: PackageUnit) = update { it.copy(unit = unit) }

    fun onCoverageChange(text: String) = update { it.copy(coverage = text) }

    fun onCoatsChange(text: String) = update { it.copy(coats = text) }

    private fun update(change: (CalculatorUiState) -> CalculatorUiState) {
        state = change(state)
    }
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
