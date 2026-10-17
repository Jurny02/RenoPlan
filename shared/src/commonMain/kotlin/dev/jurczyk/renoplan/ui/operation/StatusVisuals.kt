package dev.jurczyk.renoplan.ui.operation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.ui.theme.DoneContainerDark
import dev.jurczyk.renoplan.ui.theme.DoneContainerLight
import dev.jurczyk.renoplan.ui.theme.OnDoneContainerDark
import dev.jurczyk.renoplan.ui.theme.OnDoneContainerLight
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_status_done
import renoplan.shared.generated.resources.ic_status_in_progress
import renoplan.shared.generated.resources.ic_status_todo

// Each status has its own icon and colour, so it can be told apart without relying on colour alone.

private const val DARK_BACKGROUND_LUMINANCE = 0.5f

val OperationStatus.icon: DrawableResource
    get() =
        when (this) {
            OperationStatus.TODO -> Res.drawable.ic_status_todo
            OperationStatus.IN_PROGRESS -> Res.drawable.ic_status_in_progress
            OperationStatus.DONE -> Res.drawable.ic_status_done
        }

@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE

@Composable
fun OperationStatus.containerColor(): Color =
    when (this) {
        OperationStatus.TODO -> MaterialTheme.colorScheme.surfaceVariant
        OperationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
        OperationStatus.DONE -> if (isDarkScheme()) DoneContainerDark else DoneContainerLight
    }

@Composable
fun OperationStatus.contentColor(): Color =
    when (this) {
        OperationStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
        OperationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
        OperationStatus.DONE -> if (isDarkScheme()) OnDoneContainerDark else OnDoneContainerLight
    }

/** A tappable status chip, e.g. to move the status on. */
@Composable
fun StatusChip(
    status: OperationStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(status.label)) },
        leadingIcon = {
            Icon(
                painterResource(status.icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = status.containerColor(),
                labelColor = status.contentColor(),
                leadingIconContentColor = status.contentColor(),
            ),
        // "To do" shares the card's grey, so it keeps an outline to read as a button.
        border = if (status == OperationStatus.TODO) AssistChipDefaults.assistChipBorder(enabled = true) else null,
        modifier = modifier,
    )
}

/** Icon and status name in the status colour, for rows and lists. */
@Composable
fun StatusLabel(
    status: OperationStatus,
    modifier: Modifier = Modifier,
) {
    val color = if (status == OperationStatus.TODO) MaterialTheme.colorScheme.onSurfaceVariant else status.textColor()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(status.icon), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(stringResource(status.label), style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** On a plain surface the container colours are too pale for text, so labels use stronger tones. */
@Composable
private fun OperationStatus.textColor(): Color =
    when (this) {
        OperationStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
        OperationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        OperationStatus.DONE -> if (isDarkScheme()) DoneContainerLight else DoneContainerDark
    }

/** Choosing a status in the operation editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPicker(
    title: String,
    selected: OperationStatus,
    onSelect: (OperationStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperationStatus.entries.forEach { status ->
                FilterChip(
                    selected = status == selected,
                    onClick = { onSelect(status) },
                    label = { Text(stringResource(status.label)) },
                    leadingIcon = {
                        Icon(
                            painterResource(status.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = status.containerColor(),
                            selectedLabelColor = status.contentColor(),
                            selectedLeadingIconColor = status.contentColor(),
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun StatusVisualsPreview() {
    RenoPlanTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OperationStatus.entries.forEach { status ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(status, onClick = {})
                    StatusLabel(status)
                }
            }
            StatusPicker(title = "Status", selected = OperationStatus.IN_PROGRESS, onSelect = {})
        }
    }
}
