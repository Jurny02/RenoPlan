package dev.jurczyk.renoplan.ui.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.cost.OperationGroupKey
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.sum
import dev.jurczyk.renoplan.presentation.level.LevelUiState
import dev.jurczyk.renoplan.presentation.level.OperationEntry
import dev.jurczyk.renoplan.ui.components.ConfirmDialog
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.components.MenuAction
import dev.jurczyk.renoplan.ui.components.OverflowMenu
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
import renoplan.shared.generated.resources.costs_edit_price_all
import renoplan.shared.generated.resources.costs_labour
import renoplan.shared.generated.resources.costs_material
import renoplan.shared.generated.resources.costs_no_operations
import renoplan.shared.generated.resources.costs_row_labour
import renoplan.shared.generated.resources.costs_row_material
import renoplan.shared.generated.resources.costs_self_done_all
import renoplan.shared.generated.resources.costs_self_done_confirm
import renoplan.shared.generated.resources.costs_self_done_text
import renoplan.shared.generated.resources.costs_self_done_title
import renoplan.shared.generated.resources.list_separator
import renoplan.shared.generated.resources.summary_by_room
import kotlin.uuid.Uuid

/** What the Costs tab can do, grouped to keep its signature short. */
class CostsActions(
    val onOpenOperation: (roomId: Uuid, operationId: Uuid) -> Unit = { _, _ -> },
    /** "Edit price for all" for a type group. */
    val onEditPrices: (OperationGroupKey) -> Unit = {},
    /** "I do it myself" for a type group; the tab asks for confirmation first. */
    val onSelfDone: (OperationGroupKey) -> Unit = {},
)

/**
 * Money: level totals, then every operation grouped by room or by type. Tapping one opens it; a type
 * group's menu changes the prices of the whole group.
 */
@Composable
fun CostsTab(
    state: LevelUiState,
    actions: CostsActions,
    modifier: Modifier = Modifier,
) {
    var confirmSelfDone by remember { mutableStateOf<OperationGroupKey?>(null) }
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
        groups.forEach { entries ->
            costGroup(
                entries,
                byType,
                CostsActions(actions.onOpenOperation, actions.onEditPrices, onSelfDone = {
                    confirmSelfDone =
                        it
                }),
            )
        }
    }
    confirmSelfDone?.let { group ->
        SelfDoneDialog(
            state = state,
            group = group,
            onConfirm = {
                actions.onSelfDone(group)
                confirmSelfDone = null
            },
            onDismiss = { confirmSelfDone = null },
        )
    }
}

@Composable
private fun SelfDoneDialog(
    state: LevelUiState,
    group: OperationGroupKey,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmDialog(
        title = stringResource(Res.string.costs_self_done_title),
        text =
            stringResource(
                Res.string.costs_self_done_text,
                operationTypeLabel(group.type),
                moneyLabel(state.selfDoneSaving(group)),
            ),
        confirmLabel = stringResource(Res.string.costs_self_done_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
    )
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
    actions: CostsActions,
) {
    val first = entries.first()
    item(key = if (byType) "type-${first.operation.id}" else "room-${first.roomId}") {
        val title = if (byType) operationTypeLabel(first.operation.type) else first.roomName
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ValueRow(
                    title,
                    moneyLabel(entries.map { it.cost.total }.sum()),
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                )
                if (byType) {
                    val group = OperationGroupKey(first.operation.type, first.operation.unit)
                    OverflowMenu(
                        MenuAction(Res.string.costs_edit_price_all) { actions.onEditPrices(group) },
                        MenuAction(Res.string.costs_self_done_all) { actions.onSelfDone(group) },
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider()
                    CostRow(entry, byType, onClick = { actions.onOpenOperation(entry.roomId, entry.operation.id) })
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
