package dev.jurczyk.renoplan.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// Lengths are Long millimetres, ids are Uuid strings, enums are stored by name.

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("levelId")],
)
data class RoomEntity(
    @PrimaryKey val id: String,
    val levelId: String,
    val name: String,
    val heightMm: Long,
    /** `RECTANGLE` or `OUTLINE`. */
    val shape: String,
    val rectangleLengthMm: Long?,
    val rectangleWidthMm: Long?,
    val position: Int,
)

@Entity(
    tableName = "walls",
    primaryKeys = ["roomId", "number"],
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WallEntity(
    val roomId: String,
    val number: Int,
    val lengthMm: Long,
    val turn: String,
)

/** Either [widthMm] and [heightMm] or [areaSquareMetres] is set. */
@Entity(
    tableName = "openings",
    foreignKeys = [
        ForeignKey(
            entity = WallEntity::class,
            parentColumns = ["roomId", "number"],
            childColumns = ["roomId", "wallNumber"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("roomId", "wallNumber")],
)
data class OpeningEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val wallNumber: Int,
    val type: String,
    val widthMm: Long?,
    val heightMm: Long?,
    val areaSquareMetres: Double?,
    val startsAtFloor: Boolean,
    val position: Int,
)

/** Type, surfaces and labour are encoded as text, see `data/mapper/OperationMappers.kt`. */
@Entity(
    tableName = "operations",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("roomId")],
)
data class OperationEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val position: Int,
    val typeKind: String,
    /** Painting colour or custom name. */
    val typeText: String?,
    val surfaces: String,
    val materialPriceGrosze: Long,
    val labourKind: String,
    val labourGrosze: Long?,
    val status: String,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val typeKind: String,
    val typeText: String?,
    val materialPriceGrosze: Long,
    val labourKind: String,
    val labourGrosze: Long?,
)

data class RoomWithWalls(
    @Embedded val room: RoomEntity,
    @Relation(parentColumn = "id", entityColumn = "roomId")
    val walls: List<WallEntity>,
    @Relation(parentColumn = "id", entityColumn = "roomId")
    val openings: List<OpeningEntity>,
    @Relation(parentColumn = "id", entityColumn = "roomId")
    val operations: List<OperationEntity> = emptyList(),
)

data class LevelWithRooms(
    @Embedded val level: LevelEntity,
    @Relation(entity = RoomEntity::class, parentColumn = "id", entityColumn = "levelId")
    val rooms: List<RoomWithWalls>,
)
