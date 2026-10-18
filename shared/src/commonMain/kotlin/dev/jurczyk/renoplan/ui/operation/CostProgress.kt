package dev.jurczyk.renoplan.ui.operation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.ui.components.AppProgressBar
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.percent_value
import kotlin.math.roundToInt

private const val PERCENT = 100

/** Overall status from the share of the cost that is done. */
val CostSummary.status: OperationStatus
    get() =
        when {
            total.grosze > 0L && done == total -> OperationStatus.DONE
            done.grosze > 0L -> OperationStatus.IN_PROGRESS
            else -> OperationStatus.TODO
        }

/** Share of the cost that is done, in whole percent. */
val CostSummary.percentDone: Int get() = (progress * PERCENT).roundToInt()

/** The total cost in large type, a status pill with the share done, and a progress bar. */
@Composable
fun CostProgress(
    summary: CostSummary,
    modifier: Modifier = Modifier,
) {
    val status = summary.status
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = moneyLabel(summary.total), style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(status)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.percent_value, summary.percentDone),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppProgressBar(progress = summary.progress.toFloat(), color = status.textColor())
    }
}
