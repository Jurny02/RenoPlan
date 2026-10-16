package dev.jurczyk.renoplan.ui.roomdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.cost.OperationCost
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.ui.operation.label
import dev.jurczyk.renoplan.ui.operation.moneyLabel
import dev.jurczyk.renoplan.ui.operation.operationTypeLabel
import dev.jurczyk.renoplan.ui.operation.quantityLabel
import dev.jurczyk.renoplan.ui.operation.surfacesLabel
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.list_separator
import renoplan.shared.generated.resources.room_cost_done
import renoplan.shared.generated.resources.room_cost_remaining
import renoplan.shared.generated.resources.room_cost_total
import renoplan.shared.generated.resources.room_operations_empty
import kotlin.uuid.Uuid

/** Room totals: cost, done, remaining and a progress bar. */
@Composable
fun RoomCostCard(
    summary: CostSummary,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    Res.string.room_cost_total to summary.total,
                    Res.string.room_cost_done to summary.done,
                    Res.string.room_cost_remaining to summary.remaining,
                ).forEach { (label, amount) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(moneyLabel(amount), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            LinearProgressIndicator(progress = { summary.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun NoOperations(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.room_operations_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Type, surfaces and quantity, the cost, and a status chip that moves the status on when tapped. */
@Composable
fun OperationCard(
    operation: Operation,
    cost: OperationCost,
    onClick: () -> Unit,
    onCycleStatus: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .clickable(
                        onClick = onClick,
                    ).padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(operationTypeLabel(operation.type), style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        surfacesLabel(operation.surfaces) + stringResource(Res.string.list_separator) +
                            quantityLabel(cost.quantity, cost.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(moneyLabel(cost.total), style = MaterialTheme.typography.bodyLarge)
            }
            AssistChip(
                onClick = { onCycleStatus(operation.id) },
                label = { Text(stringResource(operation.status.label)) },
            )
        }
    }
}
