package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.uuid.Uuid

/** Parses an outline written as in docs/03, in millimetres: `"3200 R 2500 R 2800 R ..."`. */
fun walls(outline: String): List<Wall> =
    if (outline.isBlank()) {
        emptyList()
    } else {
        outline
            .trim()
            .split(Regex("\\s+"))
            .chunked(2)
            .mapIndexed { index, (length, turn) ->
                Wall(
                    number = index + 1,
                    length = Millimetres(length.toLong()),
                    turn = if (turn == "R") Turn.RIGHT else Turn.LEFT,
                )
            }
    }

fun room(
    outline: String,
    height: Millimetres = 2_500.mm,
): Room =
    Room(id = Uuid.random(), name = "Test", height = height, walls = walls(outline), shapeInput = ShapeInput.Outline)

fun opening(
    type: OpeningType,
    width: Int,
    height: Int,
    startsAtFloor: Boolean = type.startsAtFloorByDefault,
): Opening = Opening(Uuid.random(), type, OpeningSize.Dimensions(width.mm, height.mm), startsAtFloor)

fun Room.withOpenings(vararg openingsByWall: Pair<Int, Opening>): Room =
    copy(
        walls =
            walls.map { wall ->
                val added = openingsByWall.filter { it.first == wall.number }.map { it.second }
                wall.copy(openings = wall.openings + added)
            },
    )

const val TOLERANCE = 1e-9

/** The kitchen-with-a-chimney example from docs/03. */
const val CHIMNEY_KITCHEN = "3200 R 2500 R 2800 R 400 L 400 R 2100 R"

/** A 4 × 3 m rectangle with a 2 × 1 m corner cut out. */
const val L_SHAPE = "4000 R 2000 R 2000 L 1000 R 2000 R 3000 R"
