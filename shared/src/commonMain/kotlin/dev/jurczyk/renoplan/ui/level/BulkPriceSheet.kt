package dev.jurczyk.renoplan.ui.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.OperationGroupKey
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.presentation.level.BulkPriceForm
import dev.jurczyk.renoplan.presentation.level.BulkPricePreview
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import dev.jurczyk.renoplan.ui.components.AppCard
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
import dev.jurczyk.renoplan.ui.components.hideKeyboardOnTapOrDrag
import dev.jurczyk.renoplan.ui.operation.PricingFields
import dev.jurczyk.renoplan.ui.operation.moneyLabel
import dev.jurczyk.renoplan.ui.operation.operationTypeLabel
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.bulk_price_apply
import renoplan.shared.generated.resources.bulk_price_cost
import renoplan.shared.generated.resources.bulk_price_count
import renoplan.shared.generated.resources.bulk_price_title
import renoplan.shared.generated.resources.value_missing

/** "Edit price for all": one material price and labour for every operation of a type group. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPriceSheet(
    form: BulkPriceForm,
    preview: BulkPricePreview,
    onPricingChange: (PricingForm) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column {
            BulkPriceContent(form, preview, onPricingChange, onApply, modifier = Modifier.weight(1f, fill = false))
            KeyboardDoneBar()
        }
    }
}

@Composable
fun BulkPriceContent(
    form: BulkPriceForm,
    preview: BulkPricePreview,
    onPricingChange: (PricingForm) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The sheet is a separate window, so the app-wide keyboard handling does not reach it.
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .hideKeyboardOnTapOrDrag(focusManager)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.bulk_price_title), style = MaterialTheme.typography.titleLarge)
        Text(operationTypeLabel(form.group.type), style = MaterialTheme.typography.titleMedium)
        PricingFields(
            pricing = form.pricing,
            unit = form.group.unit,
            onPricingChange = onPricingChange,
            showType = false,
        )
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pluralStringResource(Res.plurals.bulk_price_count, preview.count, preview.count))
                Text(
                    text =
                        stringResource(
                            Res.string.bulk_price_cost,
                            moneyLabel(preview.before),
                            preview.after?.let { moneyLabel(it) } ?: stringResource(Res.string.value_missing),
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Button(
            onClick = onApply,
            enabled = preview.after != null && preview.count > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.bulk_price_apply))
        }
    }
}

@Preview
@Composable
private fun BulkPriceContentPreview() {
    RenoPlanTheme {
        BulkPriceContent(
            form =
                BulkPriceForm(
                    group = OperationGroupKey(OperationType.Painting("biały"), MeasureUnit.SQUARE_METRE),
                    pricing = PricingForm(OperationKind.PAINTING, "biały", "5", LabourMode.PER_UNIT, "10"),
                ),
            preview = BulkPricePreview(count = 3, before = Money.fromZloty(538.65), after = Money.fromZloty(384.75)),
            onPricingChange = {},
            onApply = {},
        )
    }
}
