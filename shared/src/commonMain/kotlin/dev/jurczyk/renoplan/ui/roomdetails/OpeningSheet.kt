package dev.jurczyk.renoplan.ui.roomdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.geometry.OpeningError
import dev.jurczyk.renoplan.domain.geometry.area
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.roomdetails.OpeningForm
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsUiState
import dev.jurczyk.renoplan.presentation.roomdetails.SizeMode
import dev.jurczyk.renoplan.ui.components.ChoiceChips
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
import dev.jurczyk.renoplan.ui.components.UnitTextField
import dev.jurczyk.renoplan.ui.components.hideKeyboardOnTapOrDrag
import dev.jurczyk.renoplan.ui.format.formatDecimal
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_save
import renoplan.shared.generated.resources.opening_area_error
import renoplan.shared.generated.resources.opening_area_label
import renoplan.shared.generated.resources.opening_edit_title
import renoplan.shared.generated.resources.opening_error_exceeds
import renoplan.shared.generated.resources.opening_error_taller
import renoplan.shared.generated.resources.opening_error_wider
import renoplan.shared.generated.resources.opening_height_label
import renoplan.shared.generated.resources.opening_new_title
import renoplan.shared.generated.resources.opening_result_area
import renoplan.shared.generated.resources.opening_result_wall_net
import renoplan.shared.generated.resources.opening_size_area
import renoplan.shared.generated.resources.opening_size_dimensions
import renoplan.shared.generated.resources.opening_starts_at_floor
import renoplan.shared.generated.resources.opening_starts_at_floor_hint
import renoplan.shared.generated.resources.opening_type_label
import renoplan.shared.generated.resources.opening_wall_label
import renoplan.shared.generated.resources.opening_width_label
import renoplan.shared.generated.resources.room_length_error
import renoplan.shared.generated.resources.unit_metres
import renoplan.shared.generated.resources.unit_metres_short
import renoplan.shared.generated.resources.unit_square_metres
import renoplan.shared.generated.resources.unit_square_metres_short
import renoplan.shared.generated.resources.value_missing
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningSheet(
    state: RoomDetailsUiState,
    form: OpeningForm,
    onFormChange: (OpeningForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column {
            OpeningSheetContent(
                state = state,
                form = form,
                onFormChange = onFormChange,
                onSave = onSave,
                onDelete = onDelete,
                modifier = Modifier.weight(1f, fill = false),
            )
            KeyboardDoneBar()
        }
    }
}

@Composable
fun OpeningSheetContent(
    state: RoomDetailsUiState,
    form: OpeningForm,
    onFormChange: (OpeningForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The sheet is a separate window, so the app-wide keyboard handling does not reach it.
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .hideKeyboardOnTapOrDrag(focusManager)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val title = if (form.isNew) Res.string.opening_new_title else Res.string.opening_edit_title
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
        ChoiceChips(
            title = stringResource(Res.string.opening_type_label),
            options = OpeningType.entries,
            selected = form.type,
            optionLabel = { stringResource(it.label) },
            onSelect = { onFormChange(form.withType(it)) },
        )
        ChoiceChips(
            title = stringResource(Res.string.opening_wall_label),
            options =
                state.room
                    ?.walls
                    .orEmpty()
                    .map { it.number },
            selected = form.wallNumber,
            optionLabel = { it.toString() },
            onSelect = { onFormChange(form.copy(wallNumber = it)) },
        )
        StartsAtFloorRow(form, onFormChange)
        SizeFields(form, onFormChange)
        state.room?.let { room -> FormErrors(room, form, state.formErrors) }
        FormResults(state, form)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!form.isNew) TextButton(onClick = onDelete) { Text(stringResource(Res.string.action_delete)) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSave, enabled = state.canSaveForm) { Text(stringResource(Res.string.action_save)) }
        }
    }
}

@Composable
private fun StartsAtFloorRow(
    form: OpeningForm,
    onFormChange: (OpeningForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(
                    value = form.startsAtFloor,
                    role = Role.Switch,
                    onValueChange = { onFormChange(form.withStartsAtFloor(it)) },
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.opening_starts_at_floor), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(Res.string.opening_starts_at_floor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = form.startsAtFloor, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SizeFields(
    form: OpeningForm,
    onFormChange: (OpeningForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SizeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = form.sizeMode == mode,
                    onClick = { onFormChange(form.withSizeMode(mode)) },
                    enabled = mode == SizeMode.DIMENSIONS || form.canUseArea,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SizeMode.entries.size),
                ) {
                    Text(
                        stringResource(
                            if (mode ==
                                SizeMode.DIMENSIONS
                            ) {
                                Res.string.opening_size_dimensions
                            } else {
                                Res.string.opening_size_area
                            },
                        ),
                    )
                }
            }
        }
        val metres = stringResource(Res.string.unit_metres_short)
        val lengthError = stringResource(Res.string.room_length_error)
        when (form.sizeMode) {
            SizeMode.DIMENSIONS -> {
                UnitTextField(
                    form.width,
                    { onFormChange(form.copy(width = it)) },
                    stringResource(Res.string.opening_width_label),
                    metres,
                    form.widthError,
                    lengthError,
                )
                UnitTextField(
                    form.height,
                    { onFormChange(form.copy(height = it)) },
                    stringResource(Res.string.opening_height_label),
                    metres,
                    form.heightError,
                    lengthError,
                    imeAction = ImeAction.Done,
                )
            }

            SizeMode.AREA -> {
                UnitTextField(
                    form.area,
                    { onFormChange(form.copy(area = it)) },
                    stringResource(Res.string.opening_area_label),
                    stringResource(Res.string.unit_square_metres_short),
                    form.areaError,
                    stringResource(Res.string.opening_area_error),
                    imeAction = ImeAction.Done,
                )
            }
        }
    }
}

@Composable
private fun FormErrors(
    room: Room,
    form: OpeningForm,
    errors: List<OpeningError>,
    modifier: Modifier = Modifier,
) {
    val wallLength =
        room.walls
            .find { it.number == form.wallNumber }
            ?.length
            ?.metres ?: 0.0
    errors.forEach { error ->
        val text =
            when (error) {
                is OpeningError.WiderThanWall -> {
                    stringResource(
                        Res.string.opening_error_wider,
                        stringResource(Res.string.unit_metres, formatDecimal(wallLength)),
                    )
                }

                is OpeningError.TallerThanRoom -> {
                    stringResource(
                        Res.string.opening_error_taller,
                        stringResource(Res.string.unit_metres, formatDecimal(room.height.metres)),
                    )
                }

                is OpeningError.OpeningsExceedWallArea -> {
                    stringResource(Res.string.opening_error_exceeds)
                }
            }
        Text(
            text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}

@Composable
private fun FormResults(
    state: RoomDetailsUiState,
    form: OpeningForm,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(Res.string.value_missing)
    val squareMetres = @Composable { value: Double? ->
        value?.let { stringResource(Res.string.unit_square_metres, formatDecimal(it)) } ?: missing
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultRow(stringResource(Res.string.opening_result_area), squareMetres(form.opening?.area))
            ResultRow(
                stringResource(Res.string.opening_result_wall_net, form.wallNumber),
                squareMetres(state.formWallNetArea),
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
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview
@Composable
private fun OpeningSheetContentPreview() {
    val room = Room.rectangle(Uuid.random(), "Kuchnia", 3_200.mm, 2_500.mm, 2_600.mm)
    val form = OpeningForm.new(2).withType(OpeningType.DOOR).copy(width = "0,8", height = "2")
    RenoPlanTheme {
        OpeningSheetContent(
            state = RoomDetailsUiState(room = room, isLoading = false, form = form),
            form = form,
            onFormChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
