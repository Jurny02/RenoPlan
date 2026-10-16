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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorUiState
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorViewModel
import dev.jurczyk.renoplan.ui.components.BackButton
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
import renoplan.shared.generated.resources.room_results_floor_area
import renoplan.shared.generated.resources.room_results_perimeter
import renoplan.shared.generated.resources.room_results_volume
import renoplan.shared.generated.resources.room_shape_outline
import renoplan.shared.generated.resources.room_shape_outline_soon
import renoplan.shared.generated.resources.room_shape_rectangle
import renoplan.shared.generated.resources.room_width_label
import renoplan.shared.generated.resources.unit_cubic_metres
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_metres_short
import renoplan.shared.generated.resources.unit_square_metres
import renoplan.shared.generated.resources.value_missing
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isSaved) { if (state.isSaved) currentOnBack() }
    RoomEditorContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onHeightChange = viewModel::onHeightChange,
        onLengthChange = viewModel::onLengthChange,
        onWidthChange = viewModel::onWidthChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEditorContent(
    state: RoomEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.isNew) Res.string.room_editor_new_title else Res.string.room_editor_edit_title
    Scaffold(
        modifier = modifier,
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
            ShapeToggle()
            MetresField(state.length, onLengthChange, stringResource(Res.string.room_length_label), state.lengthError)
            MetresField(
                value = state.width,
                onValueChange = onWidthChange,
                label = stringResource(Res.string.room_width_label),
                isError = state.widthError,
                imeAction = ImeAction.Done,
            )
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(stringResource(Res.string.unit_metres_short)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(Res.string.room_length_error)) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Outline mode comes in the next stage; the toggle shows it is planned. */
@Composable
private fun ShapeToggle(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(Res.string.room_shape_rectangle)) }
            SegmentedButton(
                selected = false,
                onClick = {},
                enabled = false,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(Res.string.room_shape_outline)) }
        }
        Text(
            text = stringResource(Res.string.room_shape_outline_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
            onSave = {},
        )
    }
}
