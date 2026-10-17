package dev.jurczyk.renoplan.ui.roomeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.geometry.OutlineError
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.presentation.roomeditor.OutlineProblem
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorUiState
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorViewModel
import dev.jurczyk.renoplan.presentation.roomeditor.ShapeMode
import dev.jurczyk.renoplan.presentation.roomeditor.WallRow
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
import dev.jurczyk.renoplan.ui.components.UnitTextField
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_save
import renoplan.shared.generated.resources.room_editor_edit_title
import renoplan.shared.generated.resources.room_editor_new_title
import renoplan.shared.generated.resources.room_height_label
import renoplan.shared.generated.resources.room_length_error
import renoplan.shared.generated.resources.room_length_label
import renoplan.shared.generated.resources.room_name_label
import renoplan.shared.generated.resources.room_openings_lost
import renoplan.shared.generated.resources.room_outline_cannot_auto_close
import renoplan.shared.generated.resources.room_outline_not_closed
import renoplan.shared.generated.resources.room_outline_too_few_walls
import renoplan.shared.generated.resources.room_outline_wrong_rotation
import renoplan.shared.generated.resources.room_results_floor_area
import renoplan.shared.generated.resources.room_results_perimeter
import renoplan.shared.generated.resources.room_results_volume
import renoplan.shared.generated.resources.room_shape_outline
import renoplan.shared.generated.resources.room_shape_rectangle
import renoplan.shared.generated.resources.room_shape_rectangle_disabled
import renoplan.shared.generated.resources.room_width_label
import renoplan.shared.generated.resources.unit_cubic_metres
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_metres_short
import renoplan.shared.generated.resources.unit_square_metres
import renoplan.shared.generated.resources.value_missing
import kotlin.math.hypot
import kotlin.uuid.Uuid

/** Creates a room in [levelId], or edits room [roomId] when it is given. */
@Composable
fun RoomEditorScreen(
    levelId: Uuid,
    roomId: Uuid?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoomEditorViewModel = koinViewModel { parametersOf(levelId, roomId) },
) {
    val state = viewModel.state
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isSaved) { if (state.isSaved) currentOnBack() }
    RoomEditorContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onHeightChange = viewModel::onHeightChange,
        onLengthChange = viewModel::onLengthChange,
        onWidthChange = viewModel::onWidthChange,
        onShapeModeChange = viewModel::onShapeModeChange,
        outlineCallbacks =
            OutlineCallbacks(
                onRowLengthChange = viewModel::onRowLengthChange,
                onRowTurnChange = viewModel::onRowTurnChange,
                onRemoveRow = viewModel::removeRow,
                onAddRow = viewModel::addRow,
                onAutoCloseChange = viewModel::onAutoCloseChange,
            ),
        onSave = viewModel::save,
        modifier = modifier,
    )
}

/** Outline editor actions, grouped to keep [RoomEditorContent]'s signature readable. */
class OutlineCallbacks(
    val onRowLengthChange: (Int, String) -> Unit = { _, _ -> },
    val onRowTurnChange: (Int, Turn) -> Unit = { _, _ -> },
    val onRemoveRow: (Int) -> Unit = {},
    val onAddRow: () -> Unit = {},
    val onAutoCloseChange: (Boolean) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEditorContent(
    state: RoomEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onShapeModeChange: (ShapeMode) -> Unit,
    outlineCallbacks: OutlineCallbacks,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.isNew) Res.string.room_editor_new_title else Res.string.room_editor_edit_title
    Scaffold(
        modifier = modifier,
        bottomBar = { KeyboardDoneBar() },
        topBar = {
            TopAppBar(title = { Text(stringResource(title)) }, navigationIcon = { BackButton(onClick = onBack) })
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    // Keeps the form above the keyboard so Save can be scrolled to.
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(Res.string.room_name_label)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            MetresField(state.height, onHeightChange, stringResource(Res.string.room_height_label), state.heightError)
            ShapeToggle(state = state, onShapeModeChange = onShapeModeChange)
            ShapeFields(state, onLengthChange, onWidthChange, outlineCallbacks)
            state.outlineProblem?.let { WarningText(outlineProblemText(it), color = MaterialTheme.colorScheme.error) }
            state.walls?.let { walls -> Card { OutlinePreview(walls = walls) } }
            if (state.wallsLosingOpenings.isNotEmpty()) {
                WarningText(
                    stringResource(Res.string.room_openings_lost, state.wallsLosingOpenings.joinToString()),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Results(state)
            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
}

@Composable
private fun MetresField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    UnitTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        unit = stringResource(Res.string.unit_metres_short),
        isError = isError,
        errorText = stringResource(Res.string.room_length_error),
        modifier = modifier,
        imeAction = imeAction,
    )
}

@Composable
private fun ShapeFields(
    state: RoomEditorUiState,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    outlineCallbacks: OutlineCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.shapeMode) {
            ShapeMode.RECTANGLE -> {
                MetresField(
                    state.length,
                    onLengthChange,
                    stringResource(Res.string.room_length_label),
                    state.lengthError,
                )
                MetresField(
                    value = state.width,
                    onValueChange = onWidthChange,
                    label = stringResource(Res.string.room_width_label),
                    isError = state.widthError,
                    imeAction = ImeAction.Done,
                )
            }

            ShapeMode.OUTLINE -> {
                OutlineEditor(
                    state = state,
                    onRowLengthChange = outlineCallbacks.onRowLengthChange,
                    onRowTurnChange = outlineCallbacks.onRowTurnChange,
                    onRemoveRow = outlineCallbacks.onRemoveRow,
                    onAddRow = outlineCallbacks.onAddRow,
                    onAutoCloseChange = outlineCallbacks.onAutoCloseChange,
                )
            }
        }
    }
}

@Composable
private fun ShapeToggle(
    state: RoomEditorUiState,
    onShapeModeChange: (ShapeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ShapeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.shapeMode == mode,
                    onClick = { onShapeModeChange(mode) },
                    enabled = mode != ShapeMode.RECTANGLE || state.canSwitchToRectangle,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ShapeMode.entries.size),
                ) { Text(stringResource(mode.label)) }
            }
        }
        if (!state.canSwitchToRectangle) {
            Text(
                text = stringResource(Res.string.room_shape_rectangle_disabled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val ShapeMode.label
    get() =
        when (this) {
            ShapeMode.RECTANGLE -> Res.string.room_shape_rectangle
            ShapeMode.OUTLINE -> Res.string.room_shape_outline
        }

@Composable
private fun outlineProblemText(problem: OutlineProblem): String =
    when (problem) {
        OutlineProblem.CannotAutoClose -> {
            stringResource(Res.string.room_outline_cannot_auto_close)
        }

        is OutlineProblem.Invalid -> {
            problem.errors
                .mapNotNull { error ->
                    when (error) {
                        is OutlineError.TooFewWalls -> {
                            stringResource(Res.string.room_outline_too_few_walls)
                        }

                        is OutlineError.NotClosed -> {
                            val gapMetres =
                                hypot(error.gap.x.toDouble(), error.gap.y.toDouble()) / MILLIMETRES_PER_METRE
                            stringResource(
                                Res.string.room_outline_not_closed,
                                stringResource(Res.string.unit_metres, formatDecimal(gapMetres)),
                            )
                        }

                        is OutlineError.WrongNetRotation -> {
                            stringResource(Res.string.room_outline_wrong_rotation, error.rightTurns, error.leftTurns)
                        }

                        // Rows only accept positive lengths.
                        is OutlineError.NonPositiveLength -> {
                            null
                        }
                    }
                }.joinToString("\n")
        }
    }

private const val MILLIMETRES_PER_METRE = 1_000.0

@Composable
private fun WarningText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Composable
private fun Results(
    state: RoomEditorUiState,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(Res.string.value_missing)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultRow(
                label = stringResource(Res.string.room_results_floor_area),
                value =
                    state.floorArea?.let {
                        stringResource(
                            Res.string.unit_square_metres,
                            formatDecimal(it),
                        )
                    } ?: missing,
            )
            ResultRow(
                label = stringResource(Res.string.room_results_perimeter),
                value =
                    state.perimeter?.let {
                        stringResource(
                            Res.string.unit_metres,
                            formatDecimal(it.metres),
                        )
                    } ?: missing,
            )
            ResultRow(
                label = stringResource(Res.string.room_results_volume),
                value =
                    state.volume?.let {
                        stringResource(
                            Res.string.unit_cubic_metres,
                            formatDecimal(it),
                        )
                    } ?: missing,
            )
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview
@Composable
private fun RoomEditorContentPreview() {
    RenoPlanTheme {
        RoomEditorContent(
            state = RoomEditorUiState(name = "Kuchnia", height = "2,6", length = "3,2", width = "2,5"),
            onBack = {},
            onNameChange = {},
            onHeightChange = {},
            onLengthChange = {},
            onWidthChange = {},
            onShapeModeChange = {},
            outlineCallbacks = OutlineCallbacks(),
            onSave = {},
        )
    }
}

@Preview
@Composable
private fun RoomEditorContentOutlinePreview() {
    RenoPlanTheme {
        RoomEditorContent(
            state =
                RoomEditorUiState(
                    name = "Kuchnia",
                    height = "2,6",
                    shapeMode = ShapeMode.OUTLINE,
                    rows =
                        listOf(
                            WallRow("3,2", Turn.RIGHT),
                            WallRow("2,5", Turn.RIGHT),
                            WallRow("2,8", Turn.RIGHT),
                            WallRow("0,4", Turn.LEFT),
                            WallRow("0,4", Turn.RIGHT),
                        ),
                    autoClose = true,
                ),
            onBack = {},
            onNameChange = {},
            onHeightChange = {},
            onLengthChange = {},
            onWidthChange = {},
            onShapeModeChange = {},
            outlineCallbacks = OutlineCallbacks(),
            onSave = {},
        )
    }
}

@Preview
@Composable
private fun RoomEditorContentErrorPreview() {
    RenoPlanTheme {
        RoomEditorContent(
            state = RoomEditorUiState(name = "Kuchnia", height = "2,6", length = "0"),
            onBack = {},
            onNameChange = {},
            onHeightChange = {},
            onLengthChange = {},
            onWidthChange = {},
            onShapeModeChange = {},
            outlineCallbacks = OutlineCallbacks(),
            onSave = {},
        )
    }
}
