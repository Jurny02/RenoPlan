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
    val operations: List<Operation> = emptyList(),
) {
    /** A copy under a new [id] (and new opening and operation ids) with the same geometry, openings and operations. */
    fun duplicate(
        id: Uuid,
        name: String,
    ): Room =
        copy(
            id = id,
            name = name,
            walls = walls.map { wall -> wall.copy(openings = wall.openings.map { it.copy(id = Uuid.random()) }) },
            operations = operations.map { it.copy(id = Uuid.random()) },
        )

    /**
     * Replaces the walls, keeping each old wall's openings on the wall with the same number.
     * Openings of walls that no longer exist are dropped.
     */
    fun withWalls(newWalls: List<Wall>): Room {
        val openingsByNumber = walls.associate { it.number to it.openings }
        return copy(walls = newWalls.map { it.copy(openings = openingsByNumber[it.number].orEmpty()) })
    }

    /**
     * Puts [opening] on wall [wallNumber]. An opening with the same id is replaced in place, or moved
     * there from another wall.
     */
    fun withOpening(
        wallNumber: Int,
        opening: Opening,
    ): Room =
        copy(
            walls =
                walls.map { wall ->
                    val index = wall.openings.indexOfFirst { it.id == opening.id }
                    when {
                        wall.number == wallNumber && index >= 0 -> {
                            wall.copy(openings = wall.openings.toMutableList().apply { set(index, opening) })
                        }

                        wall.number == wallNumber -> {
                            wall.copy(openings = wall.openings + opening)
                        }

                        index >= 0 -> {
                            wall.copy(openings = wall.openings.filterNot { it.id == opening.id })
                        }

                        else -> {
                            wall
                        }
                    }
                },
        )

    fun withoutOpening(id: Uuid): Room =
        copy(walls = walls.map { wall -> wall.copy(openings = wall.openings.filterNot { it.id == id }) })

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
