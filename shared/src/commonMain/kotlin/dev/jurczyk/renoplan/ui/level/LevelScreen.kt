package dev.jurczyk.renoplan.ui.level

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.presentation.level.LevelUiState
import dev.jurczyk.renoplan.presentation.level.LevelViewModel
import dev.jurczyk.renoplan.presentation.level.RoomCard
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.components.ConfirmDeleteDialog
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.components.MenuAction
import dev.jurczyk.renoplan.ui.components.NameDialog
import dev.jurczyk.renoplan.ui.components.OverflowMenu
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.operation.CostProgress
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_duplicate
import renoplan.shared.generated.resources.action_rename
import renoplan.shared.generated.resources.ic_add
import renoplan.shared.generated.resources.level_tab_costs
import renoplan.shared.generated.resources.level_tab_rooms
import renoplan.shared.generated.resources.level_tab_summary
import renoplan.shared.generated.resources.room_add
import renoplan.shared.generated.resources.room_card_details
import renoplan.shared.generated.resources.room_copy_name
import renoplan.shared.generated.resources.room_delete_text
import renoplan.shared.generated.resources.room_delete_title
import renoplan.shared.generated.resources.room_name_label
import renoplan.shared.generated.resources.room_rename_title
import renoplan.shared.generated.resources.rooms_empty_text
import renoplan.shared.generated.resources.rooms_empty_title
import renoplan.shared.generated.resources.unit_square_metres
import kotlin.uuid.Uuid

@Composable
fun LevelScreen(
    levelId: Uuid,
    onBack: () -> Unit,
    onAddRoom: () -> Unit,
    onOpenRoom: (Uuid) -> Unit,
    onOpenOperation: (roomId: Uuid, operationId: Uuid) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LevelViewModel = koinViewModel { parametersOf(levelId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isMissing) { if (state.isMissing) currentOnBack() }
    LevelContent(
        state = state,
        onBack = onBack,
        onAddRoom = onAddRoom,
        onOpenRoom = onOpenRoom,
        onOpenOperation = onOpenOperation,
        onRenameRoom = viewModel::renameRoom,
        onDuplicateRoom = viewModel::duplicateRoom,
        onDeleteRoom = viewModel::deleteRoom,
        modifier = modifier,
    )
}

private enum class LevelTab(
    val label: StringResource,
) {
    ROOMS(Res.string.level_tab_rooms),
    SUMMARY(Res.string.level_tab_summary),
    COSTS(Res.string.level_tab_costs),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelContent(
    state: LevelUiState,
    onBack: () -> Unit,
    onAddRoom: () -> Unit,
    onOpenRoom: (Uuid) -> Unit,
    onRenameRoom: (Uuid, String) -> Unit,
    onDuplicateRoom: (Uuid, String) -> Unit,
    onDeleteRoom: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    onOpenOperation: (roomId: Uuid, operationId: Uuid) -> Unit = { _, _ -> },
) {
    var selectedTab by rememberSaveable { mutableStateOf(LevelTab.ROOMS) }
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(title = { Text(state.name) }, navigationIcon = { BackButton(onClick = onBack) })
                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    LevelTab.entries.forEach { tab ->
                        Tab(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            text = { Text(stringResource(tab.label)) },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == LevelTab.ROOMS && !state.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddRoom,
                    icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.room_add)) },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            LevelTab.ROOMS -> {
                RoomsTab(
                    state = state,
                    onOpenRoom = onOpenRoom,
                    onRenameRoom = onRenameRoom,
                    onDuplicateRoom = onDuplicateRoom,
                    onDeleteRoom = onDeleteRoom,
                    modifier = contentModifier,
                )
            }

            LevelTab.SUMMARY -> {
                SummaryTab(state = state, modifier = contentModifier)
            }

            LevelTab.COSTS -> {
                CostsTab(state = state, onOpenOperation = onOpenOperation, modifier = contentModifier)
            }
        }
    }
}

private sealed interface RoomDialog {
    data class Rename(
        val room: RoomCard,
    ) : RoomDialog

    data class Delete(
        val room: RoomCard,
    ) : RoomDialog
}

@Composable
private fun RoomsTab(
    state: LevelUiState,
    onOpenRoom: (Uuid) -> Unit,
    onRenameRoom: (Uuid, String) -> Unit,
    onDuplicateRoom: (Uuid, String) -> Unit,
    onDeleteRoom: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<RoomDialog?>(null) }
    when {
        state.isLoading -> {
            Unit
        }

        state.rooms.isEmpty() -> {
            EmptyState(
                title = stringResource(Res.string.rooms_empty_title),
                text = stringResource(Res.string.rooms_empty_text),
                modifier = modifier,
            )
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.rooms, key = { it.id.toString() }) { room ->
                    val copyName = stringResource(Res.string.room_copy_name, room.name)
                    RoomCardItem(
                        room = room,
                        onClick = { onOpenRoom(room.id) },
                        onRename = { dialog = RoomDialog.Rename(room) },
                        onDuplicate = { onDuplicateRoom(room.id, copyName) },
                        onDelete = { dialog = RoomDialog.Delete(room) },
                    )
                }
            }
        }
    }
    RoomDialogs(
        dialog = dialog,
        onDismiss = { dialog = null },
        onRenameRoom = onRenameRoom,
        onDeleteRoom = onDeleteRoom,
    )
}

@Composable
private fun RoomDialogs(
    dialog: RoomDialog?,
    onDismiss: () -> Unit,
    onRenameRoom: (Uuid, String) -> Unit,
    onDeleteRoom: (Uuid) -> Unit,
) {
    when (dialog) {
        is RoomDialog.Rename -> {
            NameDialog(
                title = stringResource(Res.string.room_rename_title),
                label = stringResource(Res.string.room_name_label),
                initialName = dialog.room.name,
                onConfirm = {
                    onRenameRoom(dialog.room.id, it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        }

        is RoomDialog.Delete -> {
            ConfirmDeleteDialog(
                title = stringResource(Res.string.room_delete_title),
                text = stringResource(Res.string.room_delete_text, dialog.room.name),
                onConfirm = {
                    onDeleteRoom(dialog.room.id)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        }

        null -> {
            Unit
        }
    }
}

@Composable
private fun RoomCardItem(
    room: RoomCard,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = room.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        stringResource(
                            Res.string.room_card_details,
                            stringResource(Res.string.unit_square_metres, formatDecimal(room.floorArea)),
                            stringResource(Res.string.unit_square_metres, formatDecimal(room.netWallArea)),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CostProgress(room.cost, modifier = Modifier.padding(top = 4.dp, end = 12.dp))
            }
            OverflowMenu(
                MenuAction(Res.string.action_rename, onRename),
                MenuAction(Res.string.action_duplicate, onDuplicate),
                MenuAction(Res.string.action_delete, onDelete),
            )
        }
    }
}

@Preview
@Composable
private fun LevelContentPreview() {
    RenoPlanTheme {
        LevelContent(
            state =
                LevelUiState(
                    name = "Parter",
                    rooms =
                        listOf(
                            RoomCard(Uuid.random(), "Kuchnia", floorArea = 7.84, netWallArea = 26.1),
                            RoomCard(Uuid.random(), "Łazienka", floorArea = 5.0, netWallArea = 21.8),
                        ),
                    isLoading = false,
                ),
            onBack = {},
            onAddRoom = {},
            onOpenRoom = {},
            onRenameRoom = { _, _ -> },
            onDuplicateRoom = { _, _ -> },
            onDeleteRoom = {},
        )
    }
}

@Preview
@Composable
private fun LevelContentEmptyPreview() {
    RenoPlanTheme {
        LevelContent(
            state = LevelUiState(name = "Piętro", isLoading = false),
            onBack = {},
            onAddRoom = {},
            onOpenRoom = {},
            onRenameRoom = { _, _ -> },
            onDuplicateRoom = { _, _ -> },
            onDeleteRoom = {},
        )
    }
}
