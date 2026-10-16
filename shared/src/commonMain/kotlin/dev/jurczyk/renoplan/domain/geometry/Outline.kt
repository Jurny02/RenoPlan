package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import kotlin.math.abs

/** Minimum number of walls in a closed right-angled outline. */
const val MIN_WALL_COUNT = 4

/** Allowed gap between the last vertex and the start point, per axis. */
private const val CLOSURE_TOLERANCE_MM = 1L

/** A full turn made of 90° turns. */
private const val FULL_TURN = 4

/** A vertex of the outline in millimetres. */
data class Point(
    val x: Long,
    val y: Long,
) {
    companion object {
        val Origin = Point(0, 0)
    }
}

private enum class Heading(
    val dx: Long,
    val dy: Long,
) {
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0),
    NORTH(0, 1),
    ;

    /** RIGHT rotates clockwise, LEFT counter-clockwise. */
    fun turn(turn: Turn): Heading {
        val step = if (turn == Turn.RIGHT) 1 else -1
        return entries[(ordinal + step).mod(entries.size)]
    }
}

/**
 * Vertices of the outline: starts at [Point.Origin] heading along +X and walks each wall in order.
 * Returns `walls.size + 1` points; for a closed outline the last one equals the first.
 */
fun outlineVertices(walls: List<Wall>): List<Point> = walk(walls).points

sealed interface OutlineError {
    data class TooFewWalls(
        val count: Int,
    ) : OutlineError

    data class NonPositiveLength(
        val wallNumber: Int,
    ) : OutlineError

    /** The last vertex misses the start point by [gap]. */
    data class NotClosed(
        val gap: Point,
    ) : OutlineError

    /** The turns do not add up to one full turn (`|#RIGHT − #LEFT| ≠ 4`). */
    data class WrongNetRotation(
        val rightTurns: Int,
        val leftTurns: Int,
    ) : OutlineError
}

/** All problems that keep [walls] from forming a closed outline; empty when it is valid. */
fun validateOutline(walls: List<Wall>): List<OutlineError> =
    buildList {
        if (walls.size < MIN_WALL_COUNT) add(OutlineError.TooFewWalls(walls.size))
        walls.filter { it.length.value <= 0 }.forEach { add(OutlineError.NonPositiveLength(it.number)) }
        val end = walk(walls).points.last()
        if (!end.isWithinToleranceOfOrigin()) add(OutlineError.NotClosed(end))
        val rightTurns = walls.count { it.turn == Turn.RIGHT }
        val leftTurns = walls.size - rightTurns
        if (abs(rightTurns - leftTurns) != FULL_TURN) add(OutlineError.WrongNetRotation(rightTurns, leftTurns))
    }

fun isOutlineClosed(walls: List<Wall>): Boolean = validateOutline(walls).isEmpty()

/** Length and turn of the wall that closes an outline. */
data class ClosingWall(
    val length: Millimetres,
    val turn: Turn,
)

/**
 * Computes the last wall that closes the outline started by [walls] (walls `1..n−1`), or `null`
 * when one wall cannot close it: the way back to the start point must run along the current
 * heading, and the remaining turns must complete one full turn.
 */
fun closingWall(walls: List<Wall>): ClosingWall? {
    if (walls.size < MIN_WALL_COUNT - 1) return null
    val (points, heading) = walk(walls)
    val end = points.last()
    val length = -(end.x * heading.dx + end.y * heading.dy)
    val gap = Point(end.x + length * heading.dx, end.y + length * heading.dy)
    val rotation = walls.count { it.turn == Turn.RIGHT } - walls.count { it.turn == Turn.LEFT }
    val turn =
        when (rotation) {
            FULL_TURN - 1 -> Turn.RIGHT
            -(FULL_TURN - 1) -> Turn.LEFT
            else -> null
        }
    return if (length > 0 && turn != null && gap.isWithinToleranceOfOrigin()) {
        ClosingWall(Millimetres(length), turn)
    } else {
        null
    }
}

private data class Walk(
    val points: List<Point>,
    val heading: Heading,
)

private fun walk(walls: List<Wall>): Walk {
    var heading = Heading.EAST
    var current = Point.Origin
    val points = mutableListOf(current)
    walls.forEach { wall ->
        current = Point(current.x + wall.length.value * heading.dx, current.y + wall.length.value * heading.dy)
        points += current
        heading = heading.turn(wall.turn)
    }
    return Walk(points, heading)
}

private fun Point.isWithinToleranceOfOrigin(): Boolean =
    abs(x) <= CLOSURE_TOLERANCE_MM && abs(y) <= CLOSURE_TOLERANCE_MM
