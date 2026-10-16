package dev.jurczyk.renoplan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class OpeningTest {
    private val dimensions = OpeningSize.Dimensions(800.mm, 2_000.mm)

    @Test
    fun startsAtFloorDefaultsByType() {
        assertTrue(Opening(Uuid.random(), OpeningType.DOOR, dimensions).startsAtFloor)
        assertTrue(Opening(Uuid.random(), OpeningType.PASSAGE, dimensions).startsAtFloor)
        assertFalse(Opening(Uuid.random(), OpeningType.WINDOW, dimensions).startsAtFloor)
        assertFalse(Opening(Uuid.random(), OpeningType.NICHE, dimensions).startsAtFloor)
    }

    @Test
    fun openingAtTheFloorMustHaveDimensions() {
        assertFailsWith<IllegalArgumentException> {
            Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Area(1.6))
        }
    }

    @Test
    fun doorGivenByAreaIsAllowedWhenNotAtTheFloor() {
        val door = Opening(Uuid.random(), OpeningType.DOOR, OpeningSize.Area(1.6), startsAtFloor = false)

        assertEquals(OpeningSize.Area(1.6), door.size)
    }
}
