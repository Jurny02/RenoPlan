package dev.jurczyk.renoplan.ui.operation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.MeasureUnit
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationEditorUiState
import dev.jurczyk.renoplan.presentation.operation.OperationEditorViewModel
import dev.jurczyk.renoplan.presentation.operation.OperationForm
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.components.ChoiceChips
import dev.jurczyk.renoplan.ui.components.ConfirmDeleteDialog
import dev.jurczyk.renoplan.ui.components.UnitTextField
import dev.jurczyk.renoplan.ui.roomeditor.OutlinePreview
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_save
import renoplan.shared.generated.resources.operation_cost_labour
import renoplan.shared.generated.resources.operation_cost_material
import renoplan.shared.generated.resources.operation_cost_total
import renoplan.shared.generated.resources.operation_custom_quantity_error
import renoplan.shared.generated.resources.operation_custom_quantity_hint
import renoplan.shared.generated.resources.operation_custom_quantity_label
import renoplan.shared.generated.resources.operation_delete_text
import renoplan.shared.generated.resources.operation_delete_title
import renoplan.shared.generated.resources.operation_edit_title
import renoplan.shared.generated.resources.operation_from_template
import renoplan.shared.generated.resources.operation_hide_outline
import renoplan.shared.generated.resources.operation_new_title
import renoplan.shared.generated.resources.operation_quantity
import renoplan.shared.generated.resources.operation_save_as_template
import renoplan.shared.generated.resources.operation_show_outline
import renoplan.shared.generated.resources.operation_status_label
import renoplan.shared.generated.resources.operation_surfaces_hint
import renoplan.shared.generated.resources.operation_surfaces_label
import renoplan.shared.generated.resources.surface_all_walls
import renoplan.shared.generated.resources.unit_metres_unit
import renoplan.shared.generated.resources.unit_square_metres_unit
import renoplan.shared.generated.resources.value_missing
import kotlin.uuid.Uuid

/** Adds an operation to room [roomId], or edits operation [operationId]. */
@Composable
fun OperationEditorScreen(
    levelId: Uuid,
    roomId: Uuid,
    operationId: Uuid?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OperationEditorViewModel = koinViewModel { parametersOf(levelId, roomId, operationId) },
) {
    val stored by viewModel.state.collectAsStateWithLifecycle()
    // The form is read straight from the ViewModel so typing is never delayed.
    val state = stored.copy(form = viewModel.form)
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.form.isSaved) { if (state.form.isSaved) currentOnBack() }
    OperationEditorContent(
        state = state,
        actions =
            OperationEditorActions(
                onBack = onBack,
                onPricingChange = viewModel::onPricingChange,
                onKindChange = viewModel::onKindChange,
                onSurfaceToggle = viewModel::onSurfaceToggle,
                onAllWallsToggle = viewModel::onAllWallsToggle,
                onCustomQuantityChange = viewModel::onCustomQuantityChange,
                onCustomUnitChange = viewModel::onCustomUnitChange,
                onStatusChange = viewModel::onStatusChange,
                onSaveAsTemplateChange = viewModel::onSaveAsTemplateChange,
                onApplyTemplate = viewModel::applyTemplate,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
            ),
        modifier = modifier,
    )
}

/** Everything the editor can do, grouped to keep the content's signature short. */
class OperationEditorActions(
    val onBack: () -> Unit = {},
    val onPricingChange: (PricingForm) -> Unit = {},
    val onKindChange: (OperationKind) -> Unit = {},
    val onSurfaceToggle: (Surface) -> Unit = {},
    val onAllWallsToggle: () -> Unit = {},
    val onCustomQuantityChange: (String) -> Unit = {},
    val onCustomUnitChange: (MeasureUnit) -> Unit = {},
    val onStatusChange: (OperationStatus) -> Unit = {},
    val onSaveAsTemplateChange: (Boolean) -> Unit = {},
    val onApplyTemplate: (OperationTemplate) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationEditorContent(
    state: OperationEditorUiState,
    actions: OperationEditorActions,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val title = if (state.isNew) Res.string.operation_new_title else Res.string.operation_edit_title
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { BackButton(onClick = actions.onBack) },
                actions = {
                    if (!state.isNew) {
                        TextButton(
                            onClick = { confirmDelete = true },
                        ) { Text(stringResource(Res.string.action_delete)) }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OperationFields(state, actions)
            Button(onClick = actions.onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = stringResource(Res.string.operation_delete_title),
            text = stringResource(Res.string.operation_delete_text),
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Template picker, type and prices, surfaces, status and the live cost. */
@Composable
private fun OperationFields(
    state: OperationEditorUiState,
    actions: OperationEditorActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.templates.isNotEmpty()) TemplatePicker(state.templates, actions.onApplyTemplate)
        PricingFields(
            pricing = state.form.pricing,
            unit = state.unit,
            onPricingChange = actions.onPricingChange,
            onKindChange = actions.onKindChange,
        )
        SurfacePicker(state, actions)
        ChoiceChips(
            title = stringResource(Res.string.operation_status_label),
            options = OperationStatus.entries,
            selected = state.form.status,
            optionLabel = { stringResource(it.label) },
            onSelect = actions.onStatusChange,
        )
        SaveAsTemplateRow(state.form.saveAsTemplate, actions.onSaveAsTemplateChange)
        CostCard(state)
    }
}

@Composable
private fun TemplatePicker(
    templates: List<OperationTemplate>,
    onApplyTemplate: (OperationTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceChips(
        title = stringResource(Res.string.operation_from_template),
        options = templates,
        isSelected = { false },
        optionLabel = { operationTypeLabel(it.type) },
        onSelect = onApplyTemplate,
        modifier = modifier,
    )
}

/** Surface chips ("all walls" is `null` among them), the room's outline on demand and a typed quantity. */
@Composable
private fun SurfacePicker(
    state: OperationEditorUiState,
    actions: OperationEditorActions,
    modifier: Modifier = Modifier,
) {
    val walls = state.room?.walls.orEmpty()
    val wallSurfaces = walls.map { Surface.Wall(it.number) }
    val options: List<Surface?> =
        listOf(null) + wallSurfaces + listOf(Surface.Floor, Surface.Ceiling, Surface.Skirting, Surface.CrownMoulding)
    val allWalls = wallSurfaces.isNotEmpty() && state.form.surfaces.containsAll(wallSurfaces)
    val allWallsLabel = stringResource(Res.string.surface_all_walls)
    var showOutline by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ChoiceChips(
            title = stringResource(Res.string.operation_surfaces_label),
            options = options,
            isSelected = { surface -> if (surface == null) allWalls else surface in state.form.surfaces },
            optionLabel = { surface -> surface?.let { surfaceLabel(it) } ?: allWallsLabel },
            onSelect = { surface -> surface?.let(actions.onSurfaceToggle) ?: actions.onAllWallsToggle() },
        )
        TextButton(onClick = { showOutline = !showOutline }) {
            Text(
                stringResource(
                    if (showOutline) Res.string.operation_hide_outline else Res.string.operation_show_outline,
                ),
            )
        }
        if (showOutline && walls.isNotEmpty()) Card { OutlinePreview(walls = walls) }
        Text(
            text = stringResource(Res.string.operation_surfaces_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CustomQuantityField(state, actions, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CustomQuantityField(
    state: OperationEditorUiState,
    actions: OperationEditorActions,
    modifier: Modifier = Modifier,
) {
    val unitLabels =
        mapOf(
            MeasureUnit.SQUARE_METRE to stringResource(Res.string.unit_square_metres_unit),
            MeasureUnit.METRE to stringResource(Res.string.unit_metres_unit),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        UnitTextField(
            value = state.form.customQuantity,
            onValueChange = actions.onCustomQuantityChange,
            label = stringResource(Res.string.operation_custom_quantity_label),
            unit = unitLabels.getValue(state.customUnit),
            isError = state.customQuantityError,
            errorText = stringResource(Res.string.operation_custom_quantity_error),
        )
        // With surfaces selected, the typed quantity uses their unit.
        if (state.form.surfaces.isEmpty()) {
            ChoiceChips(
                title = "",
                options = MeasureUnit.entries,
                selected = state.form.customUnit,
                optionLabel = { unitLabels.getValue(it) },
                onSelect = actions.onCustomUnitChange,
            )
        }
        Text(
            text = stringResource(Res.string.operation_custom_quantity_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SaveAsTemplateRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.fillMaxWidth().toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(stringResource(Res.string.operation_save_as_template), modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CostCard(
    state: OperationEditorUiState,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(Res.string.value_missing)
    val cost = state.cost
    val unit = state.unit
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CostRow(
                stringResource(Res.string.operation_quantity),
                unit?.let { quantityLabel(state.quantity, it) } ?: missing,
            )
            CostRow(
                stringResource(Res.string.operation_cost_material),
                cost?.let { moneyLabel(it.material) } ?: missing,
            )
            CostRow(stringResource(Res.string.operation_cost_labour), cost?.let { moneyLabel(it.labour) } ?: missing)
            CostRow(
                stringResource(Res.string.operation_cost_total),
                cost?.let {
                    moneyLabel(it.total)
                } ?: missing,
                emphasized = true,
            )
        }
    }
}

@Composable
private fun CostRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = style)
        Text(value, style = style)
    }
}

@Preview
@Composable
private fun OperationEditorContentPreview() {
    val room = Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm)
    val template =
        OperationTemplate(Uuid.random(), OperationType.Painting("biały mat"), Money(600), Labour.PerUnit(Money(1_500)))
    RenoPlanTheme {
        OperationEditorContent(
            state =
                OperationEditorUiState(
                    room = room,
                    templates = listOf(template),
                    form =
                        OperationForm(
                            pricing = PricingForm(OperationKind.PAINTING, "biały", "6", LabourMode.PER_UNIT, "15"),
                            surfaces = (1..4).map { Surface.Wall(it) }.toSet(),
                            isLoaded = true,
                        ),
                ),
            actions = OperationEditorActions(),
        )
    }
}
