package dev.jurczyk.renoplan.ui.levels

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.presentation.levels.LevelCard
import dev.jurczyk.renoplan.presentation.levels.LevelsUiState
import dev.jurczyk.renoplan.presentation.levels.LevelsViewModel
import dev.jurczyk.renoplan.ui.components.ConfirmDeleteDialog
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.components.MenuAction
import dev.jurczyk.renoplan.ui.components.NameDialog
import dev.jurczyk.renoplan.ui.components.OverflowMenu
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.operation.CostProgress
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_rename
import renoplan.shared.generated.resources.ic_add
import renoplan.shared.generated.resources.ic_help
import renoplan.shared.generated.resources.level_add
import renoplan.shared.generated.resources.level_card_details
import renoplan.shared.generated.resources.level_delete_text
import renoplan.shared.generated.resources.level_delete_title
import renoplan.shared.generated.resources.level_name_label
import renoplan.shared.generated.resources.level_rename_title
import renoplan.shared.generated.resources.level_room_count
import renoplan.shared.generated.resources.levels_empty_action
import renoplan.shared.generated.resources.levels_empty_text
import renoplan.shared.generated.resources.levels_empty_title
import renoplan.shared.generated.resources.onboarding_open
import renoplan.shared.generated.resources.screen_levels
import renoplan.shared.generated.resources.unit_square_metres
import kotlin.uuid.Uuid

@Composable
fun LevelsScreen(
    onOpenLevel: (Uuid) -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LevelsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LevelsContent(
        state = state,
        onOpenLevel = onOpenLevel,
        onAddLevel = viewModel::addLevel,
        onRenameLevel = viewModel::renameLevel,
        onDeleteLevel = viewModel::deleteLevel,
        modifier = modifier,
        onOpenHelp = onOpenHelp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelsTopBar(
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(stringResource(Res.string.screen_levels)) },
        actions = {
            IconButton(onClick = onOpenHelp) {
                Icon(painterResource(Res.drawable.ic_help), stringResource(Res.string.onboarding_open))
            }
        },
        modifier = modifier,
    )
}

private sealed interface LevelDialog {
    data object Add : LevelDialog

    data class Rename(
        val level: LevelCard,
    ) : LevelDialog

    data class Delete(
        val level: LevelCard,
    ) : LevelDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsContent(
    state: LevelsUiState,
    onOpenLevel: (Uuid) -> Unit,
    onAddLevel: (String) -> Unit,
    onRenameLevel: (Uuid, String) -> Unit,
    onDeleteLevel: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHelp: () -> Unit = {},
) {
    var dialog by remember { mutableStateOf<LevelDialog?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = { LevelsTopBar(onOpenHelp) },
        floatingActionButton = {
            if (state.levels.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = LevelDialog.Add },
                    icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(Res.string.level_add)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Unit
            }

            state.levels.isEmpty() -> {
                EmptyState(
                    title = stringResource(Res.string.levels_empty_title),
                    text = stringResource(Res.string.levels_empty_text),
                    actionLabel = stringResource(Res.string.levels_empty_action),
                    onAction = { dialog = LevelDialog.Add },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.levels, key = { it.id.toString() }) { level ->
                        LevelCardItem(
                            level = level,
                            onClick = { onOpenLevel(level.id) },
                            onRename = { dialog = LevelDialog.Rename(level) },
                            onDelete = { dialog = LevelDialog.Delete(level) },
                        )
                    }
                }
            }
        }
    }
    LevelDialogs(
        dialog = dialog,
        onDismiss = { dialog = null },
        onAddLevel = onAddLevel,
        onRenameLevel = onRenameLevel,
        onDeleteLevel = onDeleteLevel,
    )
}

@Composable
private fun LevelDialogs(
    dialog: LevelDialog?,
    onDismiss: () -> Unit,
    onAddLevel: (String) -> Unit,
    onRenameLevel: (Uuid, String) -> Unit,
    onDeleteLevel: (Uuid) -> Unit,
) {
    when (dialog) {
        LevelDialog.Add -> {
            NameDialog(
                title = stringResource(Res.string.level_add),
                label = stringResource(Res.string.level_name_label),
                onConfirm = {
                    onAddLevel(it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        }

        is LevelDialog.Rename -> {
            NameDialog(
                title = stringResource(Res.string.level_rename_title),
                label = stringResource(Res.string.level_name_label),
                initialName = dialog.level.name,
                onConfirm = {
                    onRenameLevel(dialog.level.id, it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        }

        is LevelDialog.Delete -> {
            ConfirmDeleteDialog(
                title = stringResource(Res.string.level_delete_title),
                text = stringResource(Res.string.level_delete_text, dialog.level.name),
                onConfirm = {
                    onDeleteLevel(dialog.level.id)
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
private fun LevelCardItem(
    level: LevelCard,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = level.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        stringResource(
                            Res.string.level_card_details,
                            pluralStringResource(Res.plurals.level_room_count, level.roomCount, level.roomCount),
                            stringResource(Res.string.unit_square_metres, formatDecimal(level.floorArea)),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CostProgress(level.cost, modifier = Modifier.padding(top = 4.dp, end = 12.dp))
            }
            OverflowMenu(
                MenuAction(Res.string.action_rename, onRename),
                MenuAction(Res.string.action_delete, onDelete),
            )
        }
    }
}

@Preview
@Composable
private fun LevelsContentPreview() {
    RenoPlanTheme {
        LevelsContent(
            state =
                LevelsUiState(
                    levels =
                        listOf(
                            LevelCard(Uuid.random(), "Parter", roomCount = 5, floorArea = 62.4),
                            LevelCard(Uuid.random(), "Piętro", roomCount = 1, floorArea = 14.0),
                        ),
                    isLoading = false,
                ),
            onOpenLevel = {},
            onAddLevel = {},
            onRenameLevel = { _, _ -> },
            onDeleteLevel = {},
        )
    }
}

@Preview
@Composable
private fun LevelsContentEmptyPreview() {
    RenoPlanTheme {
        LevelsContent(
            state = LevelsUiState(isLoading = false),
            onOpenLevel = {},
            onAddLevel = {},
            onRenameLevel = { _, _ -> },
            onDeleteLevel = {},
        )
    }
}
