package dev.jurczyk.renoplan.ui.roomdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.domain.geometry.area
import dev.jurczyk.renoplan.domain.geometry.wallNetArea
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsUiState
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsViewModel
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_add
import renoplan.shared.generated.resources.opening_add
import renoplan.shared.generated.resources.opening_add_on_wall
import renoplan.shared.generated.resources.room_details_ceiling
import renoplan.shared.generated.resources.room_details_edit_shape
import renoplan.shared.generated.resources.room_details_floor
import renoplan.shared.generated.resources.room_details_no_openings
import renoplan.shared.generated.resources.room_details_opening_count
import renoplan.shared.generated.resources.room_details_perimeter
import renoplan.shared.generated.resources.room_details_skirting
import renoplan.shared.generated.resources.room_details_volume
import renoplan.shared.generated.resources.room_details_wall
import renoplan.shared.generated.resources.room_details_wall_summary
import renoplan.shared.generated.resources.room_details_walls
import renoplan.shared.generated.resources.room_details_walls_net
import renoplan.shared.generated.resources.unit_cubic_metres
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_square_metres
import kotlin.uuid.Uuid

@Composable
fun RoomDetailsScreen(
    levelId: Uuid,
    roomId: Uuid,
    onBack: () -> Unit,
    onEditShape: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoomDetailsViewModel = koinViewModel { parametersOf(levelId, roomId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isMissing) { if (state.isMissing) currentOnBack() }
    RoomDetailsContent(
        state = state,
        onBack = onBack,
        onEditShape = onEditShape,
        onAddOpening = viewModel::addOpening,
        onEditOpening = viewModel::editOpening,
        modifier = modifier,
    )
    state.form?.let { form ->
        OpeningSheet(
            state = state,
            form = form,
            onFormChange = viewModel::onFormChange,
            onSave = viewModel::saveOpening,
            onDelete = viewModel::deleteOpening,
            onDismiss = viewModel::dismissForm,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailsContent(
    state: RoomDetailsUiState,
    onBack: () -> Unit,
    onEditShape: () -> Unit,
    onAddOpening: (Int) -> Unit,
    onEditOpening: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedWall by rememberSaveable { mutableStateOf<Int?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.room?.name.orEmpty()) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    TextButton(
                        onClick = onEditShape,
                    ) { Text(stringResource(Res.string.room_details_edit_shape)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddOpening(expandedWall ?: 1) },
                icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(Res.string.opening_add)) },
            )
        },
    ) { padding ->
        val room = state.room ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { RoomMetrics(state) }
            item {
                Text(
                    text = stringResource(Res.string.room_details_walls),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(room.walls, key = { it.number }) { wall ->
                WallCard(
                    room = room,
                    wall = wall,
                    expanded = expandedWall == wall.number,
                    onToggle = { expandedWall = if (expandedWall == wall.number) null else wall.number },
                    onAddOpening = { onAddOpening(wall.number) },
                    onEditOpening = onEditOpening,
                )
            }
        }
    }
}

@Composable
private fun RoomMetrics(
    state: RoomDetailsUiState,
    modifier: Modifier = Modifier,
) {
    val squareMetres = @Composable { value: Double ->
        stringResource(Res.string.unit_square_metres, formatDecimal(value))
    }
    val metres = @Composable { value: Double -> stringResource(Res.string.unit_metres, formatDecimal(value)) }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricRow(
                stringResource(Res.string.room_details_floor) to squareMetres(state.floorArea),
                stringResource(Res.string.room_details_ceiling) to squareMetres(state.ceilingArea),
            )
            MetricRow(
                stringResource(Res.string.room_details_walls_net) to squareMetres(state.wallsNetArea),
                stringResource(Res.string.room_details_perimeter) to metres(state.perimeter.metres),
            )
            MetricRow(
                stringResource(Res.string.room_details_skirting) to metres(state.skirtingLength.metres),
                stringResource(Res.string.room_details_volume) to
                    stringResource(Res.string.unit_cubic_metres, formatDecimal(state.volume)),
            )
        }
    }
}

@Composable
private fun MetricRow(
    first: Pair<String, String>,
    second: Pair<String, String>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        listOf(first, second).forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun WallCard(
    room: Room,
    wall: Wall,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddOpening: () -> Unit,
    onEditOpening: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.clickable(onClick = onToggle).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(Res.string.room_details_wall, wall.number),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    stringResource(
                        Res.string.room_details_wall_summary,
                        stringResource(Res.string.unit_metres, formatDecimal(wall.length.metres)),
                        stringResource(Res.string.unit_square_metres, formatDecimal(room.wallNetArea(wall))),
                        pluralStringResource(
                            Res.plurals.room_details_opening_count,
                            wall.openings.size,
                            wall.openings.size,
                        ),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) WallOpenings(wall, onAddOpening, onEditOpening)
    }
}

@Composable
private fun WallOpenings(
    wall: Wall,
    onAddOpening: () -> Unit,
    onEditOpening: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        if (wall.openings.isEmpty()) {
            Text(
                text = stringResource(Res.string.room_details_no_openings),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        wall.openings.forEach { opening -> OpeningRow(opening, onClick = { onEditOpening(opening.id) }) }
        TextButton(onClick = onAddOpening, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Text(stringResource(Res.string.opening_add_on_wall, wall.number), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun OpeningRow(
    opening: Opening,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(openingDescription(opening), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            stringResource(Res.string.unit_square_metres, formatDecimal(opening.area)),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
private fun RoomDetailsContentPreview() {
    val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Dimensions(800.mm, 2_000.mm))
    val window = Opening(Uuid.random(), OpeningType.WINDOW, OpeningSize.Area(1.5))
    val room =
        Room
            .rectangle(Uuid.random(), "Kuchnia", 3_200.mm, 2_500.mm, 2_600.mm)
            .withOpening(1, door)
            .withOpening(2, window)
    RenoPlanTheme {
        RoomDetailsContent(
            state = RoomDetailsUiState(room = room, isLoading = false),
            onBack = {},
            onEditShape = {},
            onAddOpening = {},
            onEditOpening = {},
        )
    }
}
