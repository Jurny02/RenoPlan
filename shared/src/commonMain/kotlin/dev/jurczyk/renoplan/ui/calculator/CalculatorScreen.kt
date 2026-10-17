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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
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
import renoplan.shared.generated.resources.calculator_unit_kilogram
import renoplan.shared.generated.resources.calculator_unit_label
import renoplan.shared.generated.resources.calculator_unit_litre
import renoplan.shared.generated.resources.screen_calculator
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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.screen_calculator)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeToggle(selected = state.mode, onModeChange = callbacks.onModeChange)
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
}

@Composable
private fun ModeToggle(
    selected: CalculatorMode,
    onModeChange: (CalculatorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        CalculatorMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = CalculatorMode.entries.size),
            ) {
                Text(
                    stringResource(
                        if (mode ==
                            CalculatorMode.SQUARE_METRE
                        ) {
                            Res.string.calculator_mode_square_metre
                        } else {
                            Res.string.calculator_mode_metre
                        },
                    ),
                )
            }
        }
    }
}

/** Paint, glue and the like: amount, coverage and coats give the price per m². */
@Composable
private fun SquareMetreFields(
    state: CalculatorUiState,
    callbacks: CalculatorCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        val result = state.result
        if (result == null) {
            Hint(stringResource(Res.string.calculator_hint))
        } else {
            Results(result = result, onCopyPrice = callbacks.onCopyPrice)
        }
    }
}

/** Skirting, trims and the like: the length in the package gives the price per metre. */
@Composable
private fun MetreFields(
    state: CalculatorUiState,
    callbacks: CalculatorCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitTextField(
            value = state.metres,
            onValueChange = callbacks.onMetresChange,
            label = stringResource(Res.string.calculator_metres_label),
            unit = stringResource(Res.string.unit_metres_unit),
            isError = state.metresError,
            errorText = stringResource(Res.string.calculator_number_error),
            imeAction = ImeAction.Done,
        )
        val price = state.pricePerMetre
        if (price == null) {
            Hint(stringResource(Res.string.calculator_hint_metre))
        } else {
            MetreResult(price = price, onCopyPrice = callbacks.onCopyPrice)
        }
    }
}

@Composable
private fun MetreResult(
    price: Money,
    onCopyPrice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatted = formatPrice(price)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.calculator_result_price_metre),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(Res.string.calculator_price_per_metre, formatted),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = { onCopyPrice(formatted) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.calculator_copy))
            }
        }
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
        modifier = modifier,
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.calculator_unit_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PackageUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = selected == unit,
                    onClick = { onUnitChange(unit) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PackageUnit.entries.size),
                ) { Text(stringResource(unit.label)) }
            }
        }
    }
}

@Composable
private fun CoatsField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.calculator_coats_label)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(Res.string.calculator_coats_error)) }) else null,
        singleLine = true,
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.calculator_result_price),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(Res.string.calculator_price_per_square_metre, price),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ResultRow(
                label = stringResource(Res.string.calculator_result_area),
                value = stringResource(Res.string.unit_square_metres, formatDecimal(result.areaPerPackage)),
            )
            ResultRow(
                label = stringResource(Res.string.calculator_result_one_coat),
                value = stringResource(Res.string.calculator_price_per_square_metre, formatPrice(result.oneCoatPrice)),
            )
            Button(onClick = { onCopyPrice(price) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.calculator_copy))
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
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
