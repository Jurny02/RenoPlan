package dev.jurczyk.renoplan.ui.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.cost.GeometryTotals
import dev.jurczyk.renoplan.domain.cost.OperationGroup
import dev.jurczyk.renoplan.domain.cost.SurfaceCounts
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.presentation.level.LevelUiState
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.operation.operationTypeLabel
import dev.jurczyk.renoplan.ui.operation.quantityLabel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.list_separator
import renoplan.shared.generated.resources.room_details_ceiling
import renoplan.shared.generated.resources.room_details_floor
import renoplan.shared.generated.resources.room_details_skirting
import renoplan.shared.generated.resources.room_details_walls_net
import renoplan.shared.generated.resources.status_count_done
import renoplan.shared.generated.resources.status_count_in_progress
import renoplan.shared.generated.resources.status_count_todo
import renoplan.shared.generated.resources.summary_by_room
import renoplan.shared.generated.resources.summary_geometry
import renoplan.shared.generated.resources.summary_no_operations
import renoplan.shared.generated.resources.summary_no_rooms
import renoplan.shared.generated.resources.summary_whole_level
import renoplan.shared.generated.resources.surface_count_ceilings
import renoplan.shared.generated.resources.surface_count_crown_mouldings
import renoplan.shared.generated.resources.surface_count_customs
import renoplan.shared.generated.resources.surface_count_floors
import renoplan.shared.generated.resources.surface_count_skirtings
import renoplan.shared.generated.resources.surface_count_walls
import renoplan.shared.generated.resources.surface_crown_moulding
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_square_metres

/** A two-way toggle used by the Summary and Costs tabs. */
@Composable
fun GroupingToggle(
    options: List<StringResource>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/** Quantities only: geometry and operation groups, by room or for the whole level. */
@Composable
fun SummaryTab(
    state: LevelUiState,
    modifier: Modifier = Modifier,
) {
    var byRoom by rememberSaveable { mutableStateOf(false) }
    if (state.rooms.isEmpty()) {
        EmptyState(title = stringResource(Res.string.summary_no_rooms), text = "", modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GroupingToggle(
                options = listOf(Res.string.summary_whole_level, Res.string.summary_by_room),
                selected = if (byRoom) 1 else 0,
                onSelect = { byRoom = it == 1 },
            )
        }
        if (byRoom) {
            state.roomSummaries.forEach { room ->
                item(key = room.id.toString()) {
                    Text(
                        room.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                summaryItems(room.geometry, room.groups, key = room.id.toString())
            }
        } else {
            summaryItems(state.geometry, state.operationGroups, key = "level")
        }
    }
}

private fun LazyListScope.summaryItems(
    geometry: GeometryTotals,
    groups: List<OperationGroup>,
    key: String,
) {
    item(key = "$key-geometry") { GeometryCard(geometry) }
    if (groups.isEmpty()) {
        item(key = "$key-empty") {
            Text(
                stringResource(Res.string.summary_no_operations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        item(key = "$key-groups") {
            Card(modifier = Modifier.fillMaxWidth()) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider()
                    GroupRow(group)
                }
            }
        }
    }
}

@Composable
private fun GeometryCard(
    geometry: GeometryTotals,
    modifier: Modifier = Modifier,
) {
    val area = @Composable { value: Double -> stringResource(Res.string.unit_square_metres, formatDecimal(value)) }
    val length = @Composable { value: Double -> stringResource(Res.string.unit_metres, formatDecimal(value)) }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.summary_geometry), style = MaterialTheme.typography.labelLarge)
            ValueRow(stringResource(Res.string.room_details_floor), area(geometry.floorArea))
            ValueRow(stringResource(Res.string.room_details_ceiling), area(geometry.ceilingArea))
            ValueRow(stringResource(Res.string.room_details_walls_net), area(geometry.wallsNetArea))
            ValueRow(stringResource(Res.string.room_details_skirting), length(geometry.skirtingLength.metres))
            ValueRow(stringResource(Res.string.surface_crown_moulding), length(geometry.crownMouldingLength.metres))
        }
    }
}

@Composable
fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = style, modifier = Modifier.weight(1f))
        Text(value, style = style)
    }
}

@Composable
private fun GroupRow(
    group: OperationGroup,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ValueRow(operationTypeLabel(group.type), quantityLabel(group.quantity, group.unit), emphasized = true)
        Text(surfaceCountsLabel(group.surfaces), style = MaterialTheme.typography.bodyMedium)
        Text(
            statusCountsLabel(group.statusCounts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** E.g. "8 ścian · 2 sufity". */
@Composable
fun surfaceCountsLabel(counts: SurfaceCounts): String =
    listOf(
        counts.walls to Res.plurals.surface_count_walls,
        counts.floors to Res.plurals.surface_count_floors,
        counts.ceilings to Res.plurals.surface_count_ceilings,
        counts.skirtings to Res.plurals.surface_count_skirtings,
        counts.crownMouldings to Res.plurals.surface_count_crown_mouldings,
        counts.customs to Res.plurals.surface_count_customs,
    ).filter { (count, _) -> count > 0 }
        .map { (count, label) -> pluralStringResource(label, count, count) }
        .joinToString(stringResource(Res.string.list_separator))

/** E.g. "1 zrobione · 2 do zrobienia". */
@Composable
fun statusCountsLabel(counts: Map<OperationStatus, Int>): String =
    listOf(
        OperationStatus.DONE to Res.string.status_count_done,
        OperationStatus.IN_PROGRESS to Res.string.status_count_in_progress,
        OperationStatus.TODO to Res.string.status_count_todo,
    ).mapNotNull { (status, label) -> counts[status]?.takeIf { it > 0 }?.let { stringResource(label, it) } }
        .joinToString(stringResource(Res.string.list_separator))
