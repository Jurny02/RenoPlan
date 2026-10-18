package dev.jurczyk.renoplan.ui.roomdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsUiState
import dev.jurczyk.renoplan.presentation.roomdetails.RoomDetailsViewModel
import dev.jurczyk.renoplan.ui.components.BackButton
import dev.jurczyk.renoplan.ui.level.summaryItems
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.room_summary_title
import kotlin.uuid.Uuid

/** The Summary (quantities only) filtered to one room, opened from Room details. */
@Composable
fun RoomSummaryScreen(
    levelId: Uuid,
    roomId: Uuid,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoomDetailsViewModel = koinViewModel { parametersOf(levelId, roomId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isMissing) { if (state.isMissing) currentOnBack() }
    RoomSummaryContent(state = state, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSummaryContent(
    state: RoomDetailsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.room_summary_title, state.room?.name.orEmpty()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        if (state.room == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            summaryItems(state.geometryTotals, state.operationGroups, key = "room")
        }
    }
}

@Preview
@Composable
private fun RoomSummaryContentPreview() {
    val walls = (1..4).map { Surface.Wall(it) }.toSet()
    val room =
        Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm).copy(
            operations =
                listOf(
                    Operation(
                        Uuid.random(),
                        OperationType.SkimCoat,
                        walls,
                        Money(200),
                        Labour.SelfDone,
                        OperationStatus.DONE,
                    ),
                    Operation(
                        Uuid.random(),
                        OperationType.Painting("biały"),
                        walls + Surface.Ceiling,
                        Money(600),
                        Labour.SelfDone,
                    ),
                ),
        )
    RenoPlanTheme {
        RoomSummaryContent(state = RoomDetailsUiState(room = room, isLoading = false), onBack = {})
    }
}
