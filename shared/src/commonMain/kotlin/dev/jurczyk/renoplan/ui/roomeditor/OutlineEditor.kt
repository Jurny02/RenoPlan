package dev.jurczyk.renoplan.ui.roomeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.presentation.roomeditor.RoomEditorUiState
import dev.jurczyk.renoplan.presentation.roomeditor.ShapeMode
import dev.jurczyk.renoplan.presentation.roomeditor.WallRow
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_add
import renoplan.shared.generated.resources.ic_close
import renoplan.shared.generated.resources.room_auto_close
import renoplan.shared.generated.resources.room_auto_close_wall
import renoplan.shared.generated.resources.room_length_error
import renoplan.shared.generated.resources.room_shape_outline_hint
import renoplan.shared.generated.resources.room_turn_left
import renoplan.shared.generated.resources.room_turn_left_description
import renoplan.shared.generated.resources.room_turn_right
import renoplan.shared.generated.resources.room_turn_right_description
import renoplan.shared.generated.resources.room_wall_add
import renoplan.shared.generated.resources.room_wall_length_label
import renoplan.shared.generated.resources.room_wall_remove
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_metres_short

/** Rows `#, length, turn` with add / remove and the "close automatically" switch. */
@Composable
fun OutlineEditor(
    state: RoomEditorUiState,
    onRowLengthChange: (Int, String) -> Unit,
    onRowTurnChange: (Int, Turn) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onAddRow: () -> Unit,
    onAutoCloseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.room_shape_outline_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.rows.forEachIndexed { index, row ->
            WallRowEditor(
                number = index + 1,
                row = row,
                onLengthChange = { onRowLengthChange(index, it) },
                onTurnChange = { onRowTurnChange(index, it) },
                onRemove = { onRemoveRow(index) },
            )
        }
        state.closingWall?.let { closing ->
            Text(
                text =
                    stringResource(
                        Res.string.room_auto_close_wall,
                        state.rows.size + 1,
                        stringResource(Res.string.unit_metres, formatDecimal(closing.length.metres)),
                        stringResource(closing.turn.label),
                    ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        TextButton(onClick = onAddRow) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Text(stringResource(Res.string.room_wall_add), modifier = Modifier.padding(start = 8.dp))
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = state.autoClose, role = Role.Switch, onValueChange = onAutoCloseChange)
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(Res.string.room_auto_close), modifier = Modifier.weight(1f))
            Switch(checked = state.autoClose, onCheckedChange = null)
        }
    }
}

@Composable
private fun WallRowEditor(
    number: Int,
    row: WallRow,
    onLengthChange: (String) -> Unit,
    onTurnChange: (Turn) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = row.length,
            onValueChange = onLengthChange,
            label = { Text(stringResource(Res.string.room_wall_length_label, number)) },
            suffix = { Text(stringResource(Res.string.unit_metres_short)) },
            isError = row.lengthError,
            supportingText = if (row.lengthError) ({ Text(stringResource(Res.string.room_length_error)) }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        TurnToggle(turn = row.turn, onTurnChange = onTurnChange)
        IconButton(onClick = onRemove) {
            Icon(painterResource(Res.drawable.ic_close), stringResource(Res.string.room_wall_remove, number))
        }
    }
}

@Composable
private fun TurnToggle(
    turn: Turn,
    onTurnChange: (Turn) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(112.dp)) {
        Turn.entries.forEachIndexed { index, option ->
            val description = stringResource(option.description)
            SegmentedButton(
                selected = turn == option,
                onClick = { onTurnChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Turn.entries.size),
                icon = {},
                modifier = Modifier.semantics { contentDescription = description },
            ) { Text(stringResource(option.label)) }
        }
    }
}

private val Turn.label
    get() =
        when (this) {
            Turn.LEFT -> Res.string.room_turn_left
            Turn.RIGHT -> Res.string.room_turn_right
        }

private val Turn.description
    get() =
        when (this) {
            Turn.LEFT -> Res.string.room_turn_left_description
            Turn.RIGHT -> Res.string.room_turn_right_description
        }

@Preview
@Composable
private fun OutlineEditorPreview() {
    RenoPlanTheme {
        OutlineEditor(
            state =
                RoomEditorUiState(
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
            onRowLengthChange = { _, _ -> },
            onRowTurnChange = { _, _ -> },
            onRemoveRow = {},
            onAddRow = {},
            onAutoCloseChange = {},
        )
    }
}
