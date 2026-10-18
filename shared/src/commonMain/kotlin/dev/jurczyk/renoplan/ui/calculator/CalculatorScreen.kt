package dev.jurczyk.renoplan.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.calculator.PackageUnit
import dev.jurczyk.renoplan.domain.calculator.PricePerSquareMetre
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.presentation.calculator.CalculatorMode
import dev.jurczyk.renoplan.presentation.calculator.CalculatorUiState
import dev.jurczyk.renoplan.presentation.calculator.CalculatorViewModel
import dev.jurczyk.renoplan.ui.components.AppCard
import dev.jurczyk.renoplan.ui.components.FieldLabel
import dev.jurczyk.renoplan.ui.components.HeroCard
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
import dev.jurczyk.renoplan.ui.components.LabeledTextField
import dev.jurczyk.renoplan.ui.components.PillToggle
import dev.jurczyk.renoplan.ui.components.ScreenHeader
import dev.jurczyk.renoplan.ui.components.SectionLabel
import dev.jurczyk.renoplan.ui.components.UnitTextField
import dev.jurczyk.renoplan.ui.components.plainTextClipEntry
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.calculator_amount_label
import renoplan.shared.generated.resources.calculator_coats_error
import renoplan.shared.generated.resources.calculator_coats_label
import renoplan.shared.generated.resources.calculator_copied
import renoplan.shared.generated.resources.calculator_copied_metre
import renoplan.shared.generated.resources.calculator_copy
import renoplan.shared.generated.resources.calculator_coverage_label
import renoplan.shared.generated.resources.calculator_coverage_unit
import renoplan.shared.generated.resources.calculator_currency
import renoplan.shared.generated.resources.calculator_hint
import renoplan.shared.generated.resources.calculator_hint_metre
import renoplan.shared.generated.resources.calculator_metres_label
import renoplan.shared.generated.resources.calculator_mode_metre
import renoplan.shared.generated.resources.calculator_mode_square_metre
import renoplan.shared.generated.resources.calculator_number_error
import renoplan.shared.generated.resources.calculator_package_price_label
import renoplan.shared.generated.resources.calculator_price_per_metre
import renoplan.shared.generated.resources.calculator_price_per_square_metre
import renoplan.shared.generated.resources.calculator_result_area
import renoplan.shared.generated.resources.calculator_result_one_coat
import renoplan.shared.generated.resources.calculator_result_price
import renoplan.shared.generated.resources.calculator_result_price_metre
import renoplan.shared.generated.resources.calculator_subtitle
import renoplan.shared.generated.resources.calculator_unit_kilogram
import renoplan.shared.generated.resources.calculator_unit_label
import renoplan.shared.generated.resources.calculator_unit_litre
import renoplan.shared.generated.resources.nav_calculator
import renoplan.shared.generated.resources.unit_metres_unit
import renoplan.shared.generated.resources.unit_square_metres

/** Price-per-m² calculator (screen 9), the start screen of the Calculator tab. */
@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = koinViewModel(),
) {
    val state = viewModel.state
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    CalculatorContent(
        state = state,
        callbacks =
            CalculatorCallbacks(
                onModeChange = viewModel::onModeChange,
                onPackagePriceChange = viewModel::onPackagePriceChange,
                onMetresChange = viewModel::onMetresChange,
                onAmountChange = viewModel::onAmountChange,
                onUnitChange = viewModel::onUnitChange,
                onCoverageChange = viewModel::onCoverageChange,
                onCoatsChange = viewModel::onCoatsChange,
                onCopyPrice = { price ->
                    scope.launch {
                        clipboard.setClipEntry(plainTextClipEntry(price))
                        val message =
                            if (state.mode ==
                                CalculatorMode.METRE
                            ) {
                                Res.string.calculator_copied_metre
                            } else {
                                Res.string.calculator_copied
                            }
                        snackbarHostState.showSnackbar(getString(message, price))
                    }
                },
            ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Form actions, grouped to keep [CalculatorContent]'s signature readable. */
class CalculatorCallbacks(
    val onModeChange: (CalculatorMode) -> Unit = {},
    val onPackagePriceChange: (String) -> Unit = {},
    val onMetresChange: (String) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onUnitChange: (PackageUnit) -> Unit = {},
    val onCoverageChange: (String) -> Unit = {},
    val onCoatsChange: (String) -> Unit = {},
    /** Called with the price per m² or per metre formatted for pasting, e.g. `"11,43"`. */
    val onCopyPrice: (String) -> Unit = {},
)

@Composable
fun CalculatorContent(
    state: CalculatorUiState,
    callbacks: CalculatorCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        bottomBar = { KeyboardDoneBar() },
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.nav_calculator),
                subtitle = stringResource(Res.string.calculator_subtitle),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PillToggle(
                options = CalculatorMode.entries,
                selected = state.mode,
                optionLabel = { stringResource(it.label) },
                onSelect = callbacks.onModeChange,
            )
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    UnitTextField(
                        value = state.packagePrice,
                        onValueChange = callbacks.onPackagePriceChange,
                        label = stringResource(Res.string.calculator_package_price_label),
                        unit = stringResource(Res.string.calculator_currency),
                        isError = state.packagePriceError,
                        errorText = stringResource(Res.string.calculator_number_error),
                    )
                    when (state.mode) {
                        CalculatorMode.SQUARE_METRE -> SquareMetreFields(state, callbacks)
                        CalculatorMode.METRE -> MetreFields(state, callbacks)
                    }
                }
            }
            CalculatorResult(state, onCopyPrice = callbacks.onCopyPrice)
        }
    }
}

/** The price with a copy button once the inputs are valid, otherwise a hint what to enter. */
@Composable
private fun CalculatorResult(
    state: CalculatorUiState,
    onCopyPrice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        CalculatorMode.SQUARE_METRE -> {
            val result = state.result
            if (result == null) {
                Hint(stringResource(Res.string.calculator_hint), modifier = modifier)
            } else {
                Results(result = result, onCopyPrice = onCopyPrice, modifier = modifier)
            }
        }

        CalculatorMode.METRE -> {
            val price = state.pricePerMetre
            if (price == null) {
                Hint(stringResource(Res.string.calculator_hint_metre), modifier = modifier)
            } else {
                MetreResult(price = price, onCopyPrice = onCopyPrice, modifier = modifier)
            }
        }
    }
}

private val CalculatorMode.label: StringResource
    get() =
        when (this) {
            CalculatorMode.SQUARE_METRE -> Res.string.calculator_mode_square_metre
            CalculatorMode.METRE -> Res.string.calculator_mode_metre
        }

/** Paint, glue and the like: amount, coverage and coats give the price per m². */
@Composable
private fun SquareMetreFields(
    state: CalculatorUiState,
    callbacks: CalculatorCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val unit = stringResource(state.unit.label)
        UnitToggle(selected = state.unit, onUnitChange = callbacks.onUnitChange)
        UnitTextField(
            value = state.amount,
            onValueChange = callbacks.onAmountChange,
            label = stringResource(Res.string.calculator_amount_label),
            unit = unit,
            isError = state.amountError,
            errorText = stringResource(Res.string.calculator_number_error),
        )
        UnitTextField(
            value = state.coverage,
            onValueChange = callbacks.onCoverageChange,
            label = stringResource(Res.string.calculator_coverage_label),
            unit = stringResource(Res.string.calculator_coverage_unit, unit),
            isError = state.coverageError,
            errorText = stringResource(Res.string.calculator_number_error),
        )
        CoatsField(value = state.coats, onValueChange = callbacks.onCoatsChange, isError = state.coatsError)
    }
}

/** Skirting, trims and the like: the length in the package gives the price per metre. */
@Composable
private fun MetreFields(
    state: CalculatorUiState,
    callbacks: CalculatorCallbacks,
    modifier: Modifier = Modifier,
) {
    UnitTextField(
        value = state.metres,
        onValueChange = callbacks.onMetresChange,
        label = stringResource(Res.string.calculator_metres_label),
        unit = stringResource(Res.string.unit_metres_unit),
        isError = state.metresError,
        errorText = stringResource(Res.string.calculator_number_error),
        imeAction = ImeAction.Done,
        modifier = modifier,
    )
}

@Composable
private fun MetreResult(
    price: Money,
    onCopyPrice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatted = formatPrice(price)
    HeroCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(Res.string.calculator_result_price_metre),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(Res.string.calculator_price_per_metre, formatted),
            style = MaterialTheme.typography.displaySmall,
        )
        CopyButton(onClick = { onCopyPrice(formatted) })
    }
}

@Composable
private fun CopyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(stringResource(Res.string.calculator_copy))
    }
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

private val PackageUnit.label: StringResource
    get() =
        when (this) {
            PackageUnit.LITRE -> Res.string.calculator_unit_litre
            PackageUnit.KILOGRAM -> Res.string.calculator_unit_kilogram
        }

@Composable
private fun UnitToggle(
    selected: PackageUnit,
    onUnitChange: (PackageUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(Res.string.calculator_unit_label))
        PillToggle(
            options = PackageUnit.entries,
            selected = selected,
            optionLabel = { stringResource(it.label) },
            onSelect = onUnitChange,
        )
    }
}

@Composable
private fun CoatsField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    LabeledTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(Res.string.calculator_coats_label),
        isError = isError,
        supportingText = if (isError) stringResource(Res.string.calculator_coats_error) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun Results(
    result: PricePerSquareMetre,
    onCopyPrice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val price = formatPrice(result.price)
    HeroCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(Res.string.calculator_result_price),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(Res.string.calculator_price_per_square_metre, price),
            style = MaterialTheme.typography.displaySmall,
        )
        ResultRow(
            label = stringResource(Res.string.calculator_result_area),
            value = stringResource(Res.string.unit_square_metres, formatDecimal(result.areaPerPackage)),
        )
        ResultRow(
            label = stringResource(Res.string.calculator_result_one_coat),
            value = stringResource(Res.string.calculator_price_per_square_metre, formatPrice(result.oneCoatPrice)),
        )
        CopyButton(onClick = { onCopyPrice(price) })
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

/** The amount without a currency, with a decimal comma, e.g. 1143 gr → `"11,43"`. */
private fun formatPrice(money: Money): String = formatDecimal(money.zloty)

@Preview
@Composable
private fun CalculatorContentMetrePreview() {
    RenoPlanTheme {
        CalculatorContent(
            state = CalculatorUiState(mode = CalculatorMode.METRE, packagePrice = "50", metres = "2,5"),
            callbacks = CalculatorCallbacks(),
        )
    }
}

@Preview
@Composable
private fun CalculatorContentPreview() {
    RenoPlanTheme {
        CalculatorContent(
            state = CalculatorUiState(packagePrice = "200", amount = "5", coverage = "7"),
            callbacks = CalculatorCallbacks(),
        )
    }
}
