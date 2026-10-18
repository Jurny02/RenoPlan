package dev.jurczyk.renoplan.data.mapper

import dev.jurczyk.renoplan.data.local.LevelWithRooms
import dev.jurczyk.renoplan.data.local.OpeningEntity
import dev.jurczyk.renoplan.data.local.OperationEntity
import dev.jurczyk.renoplan.data.local.RoomEntity
import dev.jurczyk.renoplan.data.local.RoomWithWalls
import dev.jurczyk.renoplan.data.local.WallEntity
import dev.jurczyk.renoplan.domain.model.Level
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.ShapeInput
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import kotlin.uuid.Uuid

private const val SHAPE_RECTANGLE = "RECTANGLE"
private const val SHAPE_OUTLINE = "OUTLINE"

fun LevelWithRooms.toDomain(): Level =
    Level(
        id = Uuid.parse(level.id),
        name = level.name,
        rooms = rooms.sortedBy { it.room.position }.map { it.toDomain() },
    )

fun RoomWithWalls.toDomain(): Room {
    val openingsByWall = openings.sortedBy { it.position }.groupBy { it.wallNumber }
    return Room(
        id = Uuid.parse(room.id),
        name = room.name,
        height = Millimetres(room.heightMm),
        walls =
            walls.sortedBy { it.number }.map { wall ->
                Wall(
                    number = wall.number,
                    length = Millimetres(wall.lengthMm),
                    turn = Turn.valueOf(wall.turn),
                    openings = openingsByWall[wall.number].orEmpty().map { it.toDomain() },
                    name = wall.name,
                )
            },
        shapeInput = room.toShapeInput(),
        operations = operations.sortedBy { it.position }.map { it.toDomain() },
    )
}

/** Entities for saving [this] room in level [levelId]; the room's position is set by the DAO. */
data class RoomEntities(
    val room: RoomEntity,
    val walls: List<WallEntity>,
    val openings: List<OpeningEntity>,
    val operations: List<OperationEntity>,
)

fun Room.toEntities(levelId: Uuid): RoomEntities {
    val roomId = id.toString()
    val rectangle = shapeInput as? ShapeInput.Rectangle
    return RoomEntities(
        room =
            RoomEntity(
                id = roomId,
                levelId = levelId.toString(),
                name = name,
                heightMm = height.value,
                shape = if (rectangle != null) SHAPE_RECTANGLE else SHAPE_OUTLINE,
                rectangleLengthMm = rectangle?.length?.value,
                rectangleWidthMm = rectangle?.width?.value,
                position = 0,
            ),
        walls =
            walls.map {
                WallEntity(
                    roomId = roomId,
                    number = it.number,
                    lengthMm = it.length.value,
                    turn = it.turn.name,
                    name = it.name,
                )
            },
        openings =
            walls
                .flatMap { wall -> wall.openings.map { wall.number to it } }
                .mapIndexed { index, (wallNumber, opening) -> opening.toEntity(roomId, wallNumber, index) },
        operations = operations.mapIndexed { index, operation -> operation.toEntity(roomId, index) },
    )
}

private fun RoomEntity.toShapeInput(): ShapeInput =
    if (shape == SHAPE_RECTANGLE && rectangleLengthMm != null && rectangleWidthMm != null) {
        ShapeInput.Rectangle(Millimetres(rectangleLengthMm), Millimetres(rectangleWidthMm))
    } else {
        ShapeInput.Outline
    }

private fun OpeningEntity.toDomain(): Opening {
    val size =
        if (widthMm != null && heightMm != null) {
            OpeningSize.Dimensions(Millimetres(widthMm), Millimetres(heightMm))
        } else {
            OpeningSize.Area(checkNotNull(areaSquareMetres) { "Opening $id has neither dimensions nor area" })
        }
    return Opening(id = Uuid.parse(id), type = OpeningType.valueOf(type), size = size, startsAtFloor = startsAtFloor)
}

private fun Opening.toEntity(
    roomId: String,
    wallNumber: Int,
    position: Int,
): OpeningEntity {
    val dimensions = size as? OpeningSize.Dimensions
    return OpeningEntity(
        id = id.toString(),
        roomId = roomId,
        wallNumber = wallNumber,
        type = type.name,
        widthMm = dimensions?.width?.value,
        heightMm = dimensions?.height?.value,
        areaSquareMetres = (size as? OpeningSize.Area)?.squareMetres,
        startsAtFloor = startsAtFloor,
        position = position,
    )
}
