package dev.jurczyk.renoplan.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.presentation.operation.LabourMode
import dev.jurczyk.renoplan.presentation.operation.OperationKind
import dev.jurczyk.renoplan.presentation.operation.PricingForm
import dev.jurczyk.renoplan.presentation.templates.TemplateEditorUiState
import dev.jurczyk.renoplan.presentation.templates.TemplateEditorViewModel
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.components.ConfirmDeleteDialog
import dev.jurczyk.renoplan.ui.components.KeyboardDoneBar
import dev.jurczyk.renoplan.ui.operation.PricingFields
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.action_delete
import renoplan.shared.generated.resources.action_save
import renoplan.shared.generated.resources.template_delete_text
import renoplan.shared.generated.resources.template_delete_title
import renoplan.shared.generated.resources.template_edit_title
import renoplan.shared.generated.resources.template_editor_hint
import renoplan.shared.generated.resources.template_new_title
import kotlin.uuid.Uuid

/** Creates a template, or edits template [templateId]. */
@Composable
fun TemplateEditorScreen(
    templateId: Uuid?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateEditorViewModel = koinViewModel { parametersOf(templateId) },
) {
    val state = viewModel.state
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isSaved) { if (state.isSaved) currentOnBack() }
    TemplateEditorContent(
        state = state,
        onBack = onBack,
        onPricingChange = viewModel::onPricingChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorContent(
    state: TemplateEditorUiState,
    onBack: () -> Unit,
    onPricingChange: (PricingForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val title = if (state.isNew) Res.string.template_new_title else Res.string.template_edit_title
    Scaffold(
        modifier = modifier,
        bottomBar = { KeyboardDoneBar() },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { BackButton(onClick = onBack) },
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
            Text(
                text = stringResource(Res.string.template_editor_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PricingFields(pricing = state.pricing, unit = null, onPricingChange = onPricingChange)
            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = stringResource(Res.string.template_delete_title),
            text = stringResource(Res.string.template_delete_text),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Preview
@Composable
private fun TemplateEditorContentPreview() {
    RenoPlanTheme {
        TemplateEditorContent(
            state =
                TemplateEditorUiState(
                    pricing = PricingForm(OperationKind.TILING, "", "80", LabourMode.LUMP_SUM, "1500"),
                ),
            onBack = {},
            onPricingChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}
