package dev.jurczyk.renoplan.ui.operation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import dev.jurczyk.renoplan.ui.components.ChoiceChips
import dev.jurczyk.renoplan.ui.components.UnitTextField
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.operation_colour_label
import renoplan.shared.generated.resources.operation_custom_name_label
import renoplan.shared.generated.resources.operation_labour_amount_label
import renoplan.shared.generated.resources.operation_labour_label
import renoplan.shared.generated.resources.operation_labour_price_label
import renoplan.shared.generated.resources.operation_material_price_label
import renoplan.shared.generated.resources.operation_price_error
import renoplan.shared.generated.resources.operation_text_required
import renoplan.shared.generated.resources.operation_type_label
import renoplan.shared.generated.resources.unit_zloty_per_metre
import renoplan.shared.generated.resources.unit_zloty_per_square_metre
import renoplan.shared.generated.resources.unit_zloty_per_unit
import renoplan.shared.generated.resources.unit_zloty_short

/**
 * Type, material price and labour, shared by the operation and template editors. Prices are per
 * [unit] (per unit of the operation when it is `null`, as in templates).
 */
@Composable
fun PricingFields(
    pricing: PricingForm,
    unit: MeasureUnit?,
    onPricingChange: (PricingForm) -> Unit,
    modifier: Modifier = Modifier,
    onKindChange: (OperationKind) -> Unit = { onPricingChange(pricing.copy(kind = it)) },
    /** `false` when the type is fixed, e.g. when changing prices for a whole group. */
    showType: Boolean = true,
) {
    val unitPrice =
        stringResource(
            when (unit) {
                MeasureUnit.SQUARE_METRE -> Res.string.unit_zloty_per_square_metre
                MeasureUnit.METRE -> Res.string.unit_zloty_per_metre
                null -> Res.string.unit_zloty_per_unit
            },
        )
    val priceError = stringResource(Res.string.operation_price_error)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showType) {
            ChoiceChips(
                title = stringResource(Res.string.operation_type_label),
                options = OperationKind.entries,
                selected = pricing.kind,
                optionLabel = { stringResource(it.label) },
                onSelect = onKindChange,
            )
            if (pricing.needsText) TypeTextField(pricing, onPricingChange)
        }
        UnitTextField(
            value = pricing.materialPrice,
            onValueChange = { onPricingChange(pricing.copy(materialPrice = it)) },
            label = stringResource(Res.string.operation_material_price_label),
            unit = unitPrice,
            isError = pricing.materialPriceError,
            errorText = priceError,
        )
        LabourFields(pricing, unitPrice, priceError, onPricingChange)
    }
}

@Composable
private fun TypeTextField(
    pricing: PricingForm,
    onPricingChange: (PricingForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when (pricing.kind) {
            OperationKind.PAINTING -> Res.string.operation_colour_label
            else -> Res.string.operation_custom_name_label
        }
    val required = stringResource(Res.string.operation_text_required)
    OutlinedTextField(
        value = pricing.text,
        onValueChange = { onPricingChange(pricing.copy(text = it)) },
        label = { Text(stringResource(label)) },
        supportingText = if (pricing.text.isBlank()) ({ Text(required) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LabourFields(
    pricing: PricingForm,
    unitPrice: String,
    priceError: String,
    onPricingChange: (PricingForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.operation_labour_label), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LabourMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = pricing.labourMode == mode,
                    onClick = { onPricingChange(pricing.copy(labourMode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = LabourMode.entries.size),
                    // No check mark: three labels need the room at large font sizes.
                    icon = {},
                ) { Text(stringResource(mode.label)) }
            }
        }
        if (pricing.labourMode != LabourMode.SELF_DONE) {
            val lumpSum = pricing.labourMode == LabourMode.LUMP_SUM
            val label =
                if (lumpSum) Res.string.operation_labour_amount_label else Res.string.operation_labour_price_label
            UnitTextField(
                value = pricing.labourPrice,
                onValueChange = { onPricingChange(pricing.copy(labourPrice = it)) },
                label = stringResource(label),
                unit = if (lumpSum) stringResource(Res.string.unit_zloty_short) else unitPrice,
                isError = pricing.labourPriceError,
                errorText = priceError,
                imeAction = ImeAction.Done,
            )
        }
    }
}

@Preview
@Composable
private fun PricingFieldsPreview() {
    RenoPlanTheme {
        PricingFields(
            pricing = PricingForm(OperationKind.PAINTING, "biały mat", "6", LabourMode.PER_UNIT, "15"),
            unit = MeasureUnit.SQUARE_METRE,
            onPricingChange = {},
        )
    }
}
