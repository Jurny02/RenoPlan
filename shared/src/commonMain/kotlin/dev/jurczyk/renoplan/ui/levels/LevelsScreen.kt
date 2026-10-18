package dev.jurczyk.renoplan.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import dev.jurczyk.renoplan.domain.cost.CostSummary
import dev.jurczyk.renoplan.presentation.levels.LevelCard
import dev.jurczyk.renoplan.presentation.levels.LevelsUiState
import dev.jurczyk.renoplan.presentation.levels.LevelsViewModel
import dev.jurczyk.renoplan.ui.components.AppCard
import dev.jurczyk.renoplan.ui.components.AppProgressBar
import dev.jurczyk.renoplan.ui.components.ConfirmDeleteDialog
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.components.MenuAction
import dev.jurczyk.renoplan.ui.components.NameDialog
import dev.jurczyk.renoplan.ui.components.OverflowMenu
import dev.jurczyk.renoplan.ui.components.ScreenHeader
import dev.jurczyk.renoplan.ui.components.SectionLabel
import dev.jurczyk.renoplan.ui.components.StatValue
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.operation.CostProgress
import dev.jurczyk.renoplan.ui.operation.moneyLabel
import dev.jurczyk.renoplan.ui.operation.percentDone
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_rename
import renoplan.shared.generated.resources.app_name
import renoplan.shared.generated.resources.app_tagline
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
import renoplan.shared.generated.resources.levels_total_area
import renoplan.shared.generated.resources.levels_total_cost
import renoplan.shared.generated.resources.levels_total_progress
import renoplan.shared.generated.resources.onboarding_open
import renoplan.shared.generated.resources.percent_value
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

@Composable
private fun LevelsHeader(
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenHeader(
        title = stringResource(Res.string.app_name),
        subtitle = stringResource(Res.string.app_tagline),
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
        topBar = { LevelsHeader(onOpenHelp) },
        floatingActionButton = {
            if (state.levels.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { dialog = LevelDialog.Add },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.level_add),
                    )
                }
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
                LevelsList(
                    levels = state.levels,
                    onOpenLevel = onOpenLevel,
                    onRename = { dialog = LevelDialog.Rename(it) },
                    onDelete = { dialog = LevelDialog.Delete(it) },
                    modifier = Modifier.padding(padding),
                )
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
private fun LevelsList(
    levels: List<LevelCard>,
    onOpenLevel: (Uuid) -> Unit,
    onRename: (LevelCard) -> Unit,
    onDelete: (LevelCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "overview") { ApartmentOverviewCard(levels) }
        item(key = "levels-label") {
            SectionLabel(
                stringResource(Res.string.screen_levels),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }
        items(levels, key = { it.id.toString() }) { level ->
            LevelCardItem(
                level = level,
                onClick = { onOpenLevel(level.id) },
                onRename = { onRename(level) },
                onDelete = { onDelete(level) },
            )
        }
    }
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

/** Totals of all levels: cost, floor area and progress of the whole apartment. */
@Composable
private fun ApartmentOverviewCard(
    levels: List<LevelCard>,
    modifier: Modifier = Modifier,
) {
    val cost = levels.fold(CostSummary.Zero) { total, level -> total + level.cost }
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatValue(
                    label = stringResource(Res.string.levels_total_cost),
                    value = moneyLabel(cost.total),
                    modifier = Modifier.weight(1f),
                )
                StatValue(
                    label = stringResource(Res.string.levels_total_area),
                    value =
                        stringResource(Res.string.unit_square_metres, formatDecimal(levels.sumOf { it.floorArea })),
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(Res.string.levels_total_progress), modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(Res.string.percent_value, cost.percentDone),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                AppProgressBar(progress = cost.progress.toFloat())
            }
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
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = level.name, style = MaterialTheme.typography.titleLarge)
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
                }
                OverflowMenu(
                    MenuAction(Res.string.action_rename, onRename),
                    MenuAction(Res.string.action_delete, onDelete),
                )
            }
            CostProgress(level.cost, modifier = Modifier.padding(top = 12.dp))
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
