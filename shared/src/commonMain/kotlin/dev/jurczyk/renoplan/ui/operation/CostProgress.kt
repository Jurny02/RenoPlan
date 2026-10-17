package dev.jurczyk.renoplan.ui.operation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.CostSummary
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.cost_progress
import kotlin.math.roundToInt

private const val PERCENT = 100

/** "Koszt 832,45 zł · zrobione 6%" with a progress bar; nothing when there is no cost yet. */
@Composable
fun CostProgress(
    summary: CostSummary,
    modifier: Modifier = Modifier,
) {
    if (summary.total.grosze == 0L) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text =
                stringResource(
                    Res.string.cost_progress,
                    moneyLabel(summary.total),
                    (summary.progress * PERCENT).roundToInt(),
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(progress = { summary.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}
