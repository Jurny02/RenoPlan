package dev.jurczyk.renoplan.domain.model

import kotlin.uuid.Uuid

/**
 * A room with a flat ceiling at [height]. Geometry always comes from [walls]; [shapeInput] only
 * remembers how the user entered the shape.
 */
data class Room(
    val id: Uuid,
    val name: String,
    val height: Millimetres,
    val walls: List<Wall>,
    val shapeInput: ShapeInput,
) {
    companion object {
        fun rectangle(
            id: Uuid,
            name: String,
            length: Millimetres,
            width: Millimetres,
            height: Millimetres,
        ): Room {
            val shape = ShapeInput.Rectangle(length, width)
            return Room(id = id, name = name, height = height, walls = shape.toWalls(), shapeInput = shape)
        }
    }
}

/** How the user entered the room shape in the editor. */
sealed interface ShapeInput {
    data class Rectangle(
        val length: Millimetres,
        val width: Millimetres,
    ) : ShapeInput {
        /** Four walls `length R, width R, length R, width R`. */
        fun toWalls(): List<Wall> =
            listOf(length, width, length, width).mapIndexed { index, wallLength ->
                Wall(number = index + 1, length = wallLength, turn = Turn.RIGHT)
            }
    }

    data object Outline : ShapeInput
}
