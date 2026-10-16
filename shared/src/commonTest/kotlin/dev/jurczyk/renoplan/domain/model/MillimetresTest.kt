package dev.jurczyk.renoplan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class MillimetresTest {
    @Test
    fun convertsToAndFromMetres() {
        assertEquals(3.2, 3_200.mm.metres)
        assertEquals(3_200.mm, Millimetres.fromMetres(3.2))
        assertEquals(300.mm, Millimetres.fromMetres(0.1 + 0.2))
    }

    @Test
    fun arithmeticAndComparison() {
        assertEquals(1_500.mm, 1_000.mm + 500.mm)
        assertEquals(500.mm, 1_000.mm - 500.mm)
        assertTrue(500.mm < 1_000.mm)
        assertEquals(1_800.mm, listOf(1_000.mm, 800.mm).sum())
        assertEquals(Millimetres.Zero, emptyList<Millimetres>().sum())
    }

    @Test
    fun squareMetresOfARectangle() {
        assertEquals(8.0, squareMetres(3_200.mm, 2_500.mm), 1e-9)
    }

    @Test
    fun rectangleShapeGeneratesFourRightTurningWalls() {
        val walls = ShapeInput.Rectangle(length = 3_000.mm, width = 2_000.mm).toWalls()

        assertEquals(listOf(1, 2, 3, 4), walls.map { it.number })
        assertEquals(listOf(3_000.mm, 2_000.mm, 3_000.mm, 2_000.mm), walls.map { it.length })
        assertTrue(walls.all { it.turn == Turn.RIGHT })
    }

    @Test
    fun levelHoldsRoomsInOrder() {
        val kitchen = Room.rectangle(Uuid.random(), "Kitchen", 3_000.mm, 2_000.mm, 2_600.mm)
        val bathroom = Room.rectangle(Uuid.random(), "Bathroom", 2_000.mm, 1_500.mm, 2_600.mm)

        assertEquals(
            listOf(
                "Kitchen",
                "Bathroom",
            ),
            Level(Uuid.random(), "Ground floor", listOf(kitchen, bathroom)).rooms.map {
                it.name
            },
        )
    }
}
