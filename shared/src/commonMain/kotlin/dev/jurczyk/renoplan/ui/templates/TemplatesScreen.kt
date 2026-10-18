package dev.jurczyk.renoplan.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.presentation.templates.TemplatesUiState
import dev.jurczyk.renoplan.presentation.templates.TemplatesViewModel
import dev.jurczyk.renoplan.ui.components.AppCard
import dev.jurczyk.renoplan.ui.components.EmptyState
import dev.jurczyk.renoplan.ui.components.ScreenHeader
import dev.jurczyk.renoplan.ui.operation.labourLabel
import dev.jurczyk.renoplan.ui.operation.operationTypeLabel
import dev.jurczyk.renoplan.ui.operation.unitPriceLabel
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.ic_add
import renoplan.shared.generated.resources.nav_templates
import renoplan.shared.generated.resources.template_add
import renoplan.shared.generated.resources.template_details
import renoplan.shared.generated.resources.templates_empty_text
import renoplan.shared.generated.resources.templates_empty_title
import renoplan.shared.generated.resources.templates_subtitle
import kotlin.uuid.Uuid

@Composable
fun TemplatesScreen(
    onAddTemplate: () -> Unit,
    onOpenTemplate: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TemplatesContent(state = state, onAddTemplate = onAddTemplate, onOpenTemplate = onOpenTemplate, modifier = modifier)
}

@Composable
fun TemplatesContent(
    state: TemplatesUiState,
    onAddTemplate: () -> Unit,
    onOpenTemplate: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.nav_templates),
                subtitle = stringResource(Res.string.templates_subtitle),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTemplate,
                icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(Res.string.template_add)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Unit
            }

            state.templates.isEmpty() -> {
                EmptyState(
                    title = stringResource(Res.string.templates_empty_title),
                    text = stringResource(Res.string.templates_empty_text),
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.templates, key = { it.id.toString() }) { template ->
                        TemplateCard(template, onClick = { onOpenTemplate(template.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: OperationTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(operationTypeLabel(template.type), style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    stringResource(
                        Res.string.template_details,
                        unitPriceLabel(template.materialPrice, unit = null),
                        labourLabel(template.labour, unit = null),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun TemplatesContentPreview() {
    RenoPlanTheme {
        TemplatesContent(
            state =
                TemplatesUiState(
                    templates =
                        listOf(
                            OperationTemplate(
                                Uuid.random(),
                                OperationType.Painting("biały mat"),
                                Money(600),
                                Labour.PerUnit(Money(1_500)),
                            ),
                            OperationTemplate(
                                Uuid.random(),
                                OperationType.Tiling,
                                Money(8_000),
                                Labour.LumpSum(Money(150_000)),
                            ),
                        ),
                    isLoading = false,
                ),
            onAddTemplate = {},
            onOpenTemplate = {},
        )
    }
}

@Preview
@Composable
private fun TemplatesContentEmptyPreview() {
    RenoPlanTheme {
        TemplatesContent(state = TemplatesUiState(isLoading = false), onAddTemplate = {}, onOpenTemplate = {})
    }
}
