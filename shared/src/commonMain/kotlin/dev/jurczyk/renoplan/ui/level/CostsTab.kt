package dev.jurczyk.renoplan.ui.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.sum
import dev.jurczyk.renoplan.presentation.level.LevelUiState
import dev.jurczyk.renoplan.presentation.level.OperationEntry
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.operation.StatusLabel
import dev.jurczyk.renoplan.ui.operation.labourLabel
import dev.jurczyk.renoplan.ui.operation.moneyLabel
import dev.jurczyk.renoplan.ui.operation.operationTypeLabel
import dev.jurczyk.renoplan.ui.operation.quantityLabel
import dev.jurczyk.renoplan.ui.operation.unitPriceLabel
import dev.jurczyk.renoplan.ui.roomdetails.RoomCostCard
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.costs_by_type
import renoplan.shared.generated.resources.costs_labour
import renoplan.shared.generated.resources.costs_material
import renoplan.shared.generated.resources.costs_no_operations
import renoplan.shared.generated.resources.costs_row_labour
import renoplan.shared.generated.resources.costs_row_material
import renoplan.shared.generated.resources.list_separator
import renoplan.shared.generated.resources.summary_by_room
import kotlin.uuid.Uuid

/** Money: level totals, then every operation grouped by room or by type. Tapping one opens it. */
@Composable
fun CostsTab(
    state: LevelUiState,
    onOpenOperation: (roomId: Uuid, operationId: Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    var byType by rememberSaveable { mutableStateOf(false) }
    if (state.operations.isEmpty()) {
        EmptyState(title = stringResource(Res.string.costs_no_operations), text = "", modifier = modifier)
        return
    }
    val groups =
        if (byType) {
            state.operations
                .groupBy { it.operation.type to it.operation.unit }
                .values
                .toList()
        } else {
            state.operations
                .groupBy { it.roomId }
                .values
                .toList()
        }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { LevelCostCard(state.costSummary) }
        item {
            GroupingToggle(
                options = listOf(Res.string.summary_by_room, Res.string.costs_by_type),
                selected = if (byType) 1 else 0,
                onSelect = { byType = it == 1 },
            )
        }
        groups.forEach { entries -> costGroup(entries, byType, onOpenOperation) }
    }
}

@Composable
private fun LevelCostCard(
    summary: CostSummary,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoomCostCard(summary)
        ValueRow(
            stringResource(Res.string.costs_material),
            moneyLabel(summary.material),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        ValueRow(
            stringResource(Res.string.costs_labour),
            moneyLabel(summary.labour),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun LazyListScope.costGroup(
    entries: List<OperationEntry>,
    byType: Boolean,
    onOpenOperation: (Uuid, Uuid) -> Unit,
) {
    val first = entries.first()
    item(key = if (byType) "type-${first.operation.id}" else "room-${first.roomId}") {
        val title = if (byType) operationTypeLabel(first.operation.type) else first.roomName
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ValueRow(title, moneyLabel(entries.map { it.cost.total }.sum()), emphasized = true)
            Card(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider()
                    CostRow(entry, byType, onClick = { onOpenOperation(entry.roomId, entry.operation.id) })
                }
            }
        }
    }
}

@Composable
private fun CostRow(
    entry: OperationEntry,
    byType: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val operation = entry.operation
    val separator = stringResource(Res.string.list_separator)
    Column(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ValueRow(if (byType) entry.roomName else operationTypeLabel(operation.type), moneyLabel(entry.cost.total))
        Text(
            text =
                listOf(
                    quantityLabel(entry.cost.quantity, entry.cost.unit),
                    stringResource(
                        Res.string.costs_row_material,
                        unitPriceLabel(operation.materialPrice, operation.unit),
                    ),
                    if (operation.labour == Labour.SelfDone) {
                        labourLabel(operation.labour, operation.unit)
                    } else {
                        stringResource(Res.string.costs_row_labour, labourLabel(operation.labour, operation.unit))
                    },
                ).joinToString(separator),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusLabel(operation.status)
    }
}
