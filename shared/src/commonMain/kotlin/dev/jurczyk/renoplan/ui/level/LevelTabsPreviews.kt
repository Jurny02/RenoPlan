package dev.jurczyk.renoplan.ui.level

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Surface
import dev.jurczyk.renoplan.domain.model.mm
import dev.jurczyk.renoplan.presentation.level.LevelUiState
import dev.jurczyk.renoplan.presentation.level.RoomCard
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import kotlin.uuid.Uuid

private val previewState: LevelUiState
    get() {
        val walls = (1..4).map { Surface.Wall(it) }.toSet()

        fun op(
            type: OperationType,
            surfaces: Set<Surface>,
            price: Long,
            status: OperationStatus = OperationStatus.TODO,
        ) = Operation(Uuid.random(), type, surfaces, Money(price), Labour.PerUnit(Money(1_500)), status)
        val living =
            Room.rectangle(Uuid.random(), "Salon", 4_000.mm, 3_000.mm, 2_500.mm).copy(
                operations =
                    listOf(
                        op(OperationType.SkimCoat, walls, 200, OperationStatus.DONE),
                        op(OperationType.Painting("biały"), walls + Surface.Ceiling, 600),
                    ),
            )
        val bathroom =
            Room.rectangle(Uuid.random(), "Łazienka", 2_000.mm, 2_000.mm, 2_500.mm).copy(
                operations = listOf(op(OperationType.Tiling, setOf(Surface.Floor), 8_000, OperationStatus.IN_PROGRESS)),
            )
        return LevelUiState(
            name = "Parter",
            isLoading = false,
            level = Level(Uuid.random(), "Parter", listOf(living, bathroom)),
        )
    }

@Preview
@Composable
private fun SummaryTabPreview() {
    RenoPlanTheme {
        val state = previewState
        val cards =
            state.level
                ?.rooms
                .orEmpty()
                .map { RoomCard(it.id, it.name, floorArea = 0.0, netWallArea = 0.0) }
        SummaryTab(state = state.copy(rooms = cards))
    }
}

@Preview
@Composable
private fun CostsTabPreview() {
    RenoPlanTheme {
        CostsTab(state = previewState, actions = CostsActions())
    }
}
