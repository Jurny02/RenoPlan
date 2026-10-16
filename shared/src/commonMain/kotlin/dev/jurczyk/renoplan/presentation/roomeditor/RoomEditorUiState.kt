package dev.jurczyk.renoplan.presentation.roomeditor

import dev.jurczyk.renoplan.domain.geometry.ClosingWall
import dev.jurczyk.renoplan.domain.geometry.OutlineError
import dev.jurczyk.renoplan.domain.geometry.closingWall
import dev.jurczyk.renoplan.domain.geometry.floorArea
import dev.jurczyk.renoplan.domain.geometry.perimeter
import dev.jurczyk.renoplan.domain.geometry.validateOutline
import dev.jurczyk.renoplan.domain.geometry.volume
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.presentation.common.parseMetres
import kotlin.uuid.Uuid

enum class ShapeMode {
    RECTANGLE,
    OUTLINE,
}

/** One row of the outline editor: the wall's length in metres and the turn at its end. */
data class WallRow(
    val length: String = "",
    val turn: Turn = Turn.RIGHT,
) {
    val lengthMm: Millimetres? get() = parseMetres(length)
    val lengthError: Boolean get() = length.isNotBlank() && lengthMm == null
}

/** Why the typed outline cannot be saved; reported only once every wall has a valid length. */
sealed interface OutlineProblem {
    data class Invalid(
        val errors: List<OutlineError>,
    ) : OutlineProblem

    /** "Close automatically" is on, but no single wall closes the outline. */
    data object CannotAutoClose : OutlineProblem
}

/** Room editor form. Lengths are typed in metres; every result is derived, never stored. */
data class RoomEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    val height: String = "",
    val shapeMode: ShapeMode = ShapeMode.RECTANGLE,
    val length: String = "",
    val width: String = "",
    val rows: List<WallRow> = emptyList(),
    /** The last wall is computed from [rows] instead of typed. */
    val autoClose: Boolean = false,
    /** Numbers of the stored room's walls that have openings. */
    val wallsWithOpenings: Set<Int> = emptySet(),
    val isSaved: Boolean = false,
) {
    val heightMm: Millimetres? get() = parseMetres(height)
    val lengthMm: Millimetres? get() = parseMetres(length)
    val widthMm: Millimetres? get() = parseMetres(width)

    /** Errors are shown only for fields the user has typed into. */
    val heightError: Boolean get() = height.isNotBlank() && heightMm == null
    val lengthError: Boolean get() = length.isNotBlank() && lengthMm == null
    val widthError: Boolean get() = width.isNotBlank() && widthMm == null

    /** The computed last wall when [autoClose] is on and the outline can be closed. */
    val closingWall: ClosingWall? get() = if (autoClose) typedWalls()?.let(::closingWall) else null

    /** The walls as typed (plus the computed one), or `null` while a length is missing or invalid. */
    val walls: List<Wall>?
        get() =
            when (shapeMode) {
                ShapeMode.RECTANGLE -> rectangle()?.toWalls()
                ShapeMode.OUTLINE -> outlineWalls()
            }

    val outlineProblem: OutlineProblem?
        get() {
            val typed = if (shapeMode == ShapeMode.OUTLINE) typedWalls() else null
            return when {
                typed == null -> null
                autoClose && closingWall == null -> OutlineProblem.CannotAutoClose
                else -> validateOutline(walls.orEmpty()).takeIf { it.isNotEmpty() }?.let(OutlineProblem::Invalid)
            }
        }

    /** Walls of a closed outline, the only ones results are computed from. */
    val closedWalls: List<Wall>? get() = walls?.takeIf { validateOutline(it).isEmpty() }

    /** m² */
    val floorArea: Double? get() = closedRoom(Millimetres.Zero)?.floorArea
    val perimeter: Millimetres? get() = closedRoom(Millimetres.Zero)?.perimeter

    /** m³ */
    val volume: Double? get() = heightMm?.let { closedRoom(it)?.volume }

    /** Wall numbers whose openings would be lost because the wall no longer exists. */
    val wallsLosingOpenings: List<Int>
        get() {
            val numbers = closedWalls?.map { it.number }?.toSet() ?: return emptyList()
            return wallsWithOpenings.filterNot { it in numbers }.sorted()
        }

    /** Outline → Rectangle is allowed only for a closed 4-wall outline. */
    val canSwitchToRectangle: Boolean get() = shapeMode == ShapeMode.RECTANGLE || closedWalls?.size == RECTANGLE_WALLS

    val canSave: Boolean get() = !isLoading && name.isNotBlank() && heightMm != null && closedWalls != null

    /** The shape to store with the room. */
    val shapeInput: ShapeInput?
        get() =
            when (shapeMode) {
                ShapeMode.RECTANGLE -> rectangle()
                ShapeMode.OUTLINE -> ShapeInput.Outline
            }

    private fun rectangle(): ShapeInput.Rectangle? {
        val length = lengthMm
        val width = widthMm
        return if (length != null && width != null) ShapeInput.Rectangle(length, width) else null
    }

    private fun typedWalls(): List<Wall>? {
        val lengths = rows.map { it.lengthMm ?: return null }
        return rows.mapIndexed { index, row -> Wall(number = index + 1, length = lengths[index], turn = row.turn) }
    }

    private fun outlineWalls(): List<Wall>? {
        val typed = typedWalls()
        return when {
            typed == null -> null
            !autoClose -> typed
            else -> closingWall?.let { typed + Wall(number = typed.size + 1, length = it.length, turn = it.turn) }
        }
    }

    private fun closedRoom(height: Millimetres): Room? =
        closedWalls?.let {
            Room(
                id = Uuid.NIL,
                name = "",
                height = height,
                walls = it,
                shapeInput = ShapeInput.Outline,
            )
        }

    private companion object {
        const val RECTANGLE_WALLS = 4
    }
}
