package dev.jurczyk.renoplan.data.mapper

import dev.jurczyk.renoplan.domain.model.Labour
import dev.jurczyk.renoplan.domain.model.Money
import dev.jurczyk.renoplan.domain.model.Operation
import dev.jurczyk.renoplan.domain.model.OperationStatus
import dev.jurczyk.renoplan.domain.model.OperationTemplate
import dev.jurczyk.renoplan.domain.model.OperationType
import dev.jurczyk.renoplan.domain.model.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class OperationMappersTest {
    private val types =
        listOf(
            OperationType.Painting("biały mat"),
            OperationType.SkimCoat,
            OperationType.Priming,
            OperationType.Tiling,
            OperationType.WallPanels,
            OperationType.Skirting,
            OperationType.CrownMoulding,
            OperationType.Custom("Tapeta"),
        )
    private val labours = listOf(Labour.PerUnit(Money(1_500)), Labour.LumpSum(Money(120_000)), Labour.SelfDone)

    @Test
    fun everyTypeLabourAndStatusSurvivesARoundTrip() {
        types.forEach { type ->
            labours.forEach { labour ->
                OperationStatus.entries.forEach { status ->
                    val operation =
                        Operation(
                            Uuid.random(),
                            type,
                            setOf(Surface.Wall(3), Surface.Wall(1), Surface.Ceiling),
                            Money(650),
                            labour,
                            status,
                        )
                    assertEquals(operation, operation.toEntity(roomId = "room", position = 0).toDomain())
                }
            }
        }
    }

    @Test
    fun surfacesAreStoredAsReadableText() {
        val operation =
            Operation(
                Uuid.random(),
                OperationType.Tiling,
                setOf(Surface.Wall(12), Surface.Floor),
                Money(8_000),
                Labour.SelfDone,
            )

        val entity = operation.toEntity(roomId = "room", position = 2)

        assertEquals("WALL:12,FLOOR", entity.surfaces)
        assertEquals("TILING", entity.typeKind)
        assertEquals("SELF_DONE", entity.labourKind)
        assertEquals(null, entity.labourGrosze)
        assertEquals(listOf(Surface.Wall(12), Surface.Floor), entity.toDomain().surfaces.toList())
    }

    @Test
    fun linearSurfacesSurviveARoundTrip() {
        val skirting =
            Operation(
                Uuid.random(),
                OperationType.Skirting,
                setOf(Surface.Skirting, Surface.CrownMoulding),
                Money(2_500),
                Labour.SelfDone,
            )

        assertEquals(skirting, skirting.toEntity("room", 0).toDomain())
    }

    @Test
    fun templatesSurviveARoundTrip() {
        val template =
            OperationTemplate(
                Uuid.random(),
                OperationType.Painting("biały mat"),
                Money(600),
                Labour.PerUnit(Money(1_500)),
            )

        assertEquals(template, template.toEntity().toDomain())
    }
}
