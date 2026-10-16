package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.mm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutlineTest {
    @Test
    fun verticesStartAtOriginAndHeadAlongX() {
        val vertices = outlineVertices(walls("3000 R 2000 R 3000 R 2000 R"))

        assertEquals(
            listOf(Point(0, 0), Point(3000, 0), Point(3000, -2000), Point(0, -2000), Point(0, 0)),
            vertices,
        )
    }

    @Test
    fun leftTurnRotatesCounterClockwise() {
        val vertices = outlineVertices(walls("3000 L 2000 L 3000 L 2000 L"))

        assertEquals(Point(3000, 2000), vertices[2])
    }

    @Test
    fun rectangleIsValid() {
        assertTrue(validateOutline(walls("3000 R 2000 R 3000 R 2000 R")).isEmpty())
        assertTrue(validateOutline(walls("3000 L 2000 L 3000 L 2000 L")).isEmpty())
    }

    @Test
    fun chimneyAndLShapeAreValid() {
        assertTrue(isOutlineClosed(walls(CHIMNEY_KITCHEN)))
        assertTrue(isOutlineClosed(walls(L_SHAPE)))
    }

    @Test
    fun outlineThatDoesNotCloseReportsTheGap() {
        val errors = validateOutline(walls("3000 R 2000 R 3000 R 1900 R"))

        assertEquals(listOf<OutlineError>(OutlineError.NotClosed(Point(0, -100))), errors)
    }

    @Test
    fun gapOfOneMillimetreIsTolerated() {
        assertTrue(isOutlineClosed(walls("3000 R 2000 R 3000 R 1999 R")))
        assertTrue(isOutlineClosed(walls("3000 R 2000 R 3001 R 2000 R")))
    }

    @Test
    fun gapOfTwoMillimetresIsNotTolerated() {
        assertEquals(
            listOf<OutlineError>(OutlineError.NotClosed(Point(0, -2))),
            validateOutline(walls("3000 R 2000 R 3000 R 1998 R")),
        )
    }

    @Test
    fun closedFigureEightHasWrongNetRotation() {
        val errors = validateOutline(walls("2000 R 1000 R 1000 R 2000 L 1000 L 1000 L"))

        assertEquals(listOf<OutlineError>(OutlineError.WrongNetRotation(rightTurns = 3, leftTurns = 3)), errors)
    }

    @Test
    fun fewerThanFourWallsIsAnError() {
        val errors = validateOutline(walls("3000 R 2000 R 3000 R"))

        assertTrue(OutlineError.TooFewWalls(3) in errors)
    }

    @Test
    fun zeroLengthWallIsAnError() {
        val errors = validateOutline(walls("3000 R 0 R 3000 R 2000 R 2000 R"))

        assertTrue(OutlineError.NonPositiveLength(wallNumber = 2) in errors)
    }

    @Test
    fun emptyOutlineIsInvalid() {
        assertTrue(OutlineError.TooFewWalls(0) in validateOutline(emptyList()))
    }

    @Test
    fun closingWallCompletesTheChimneyKitchen() {
        val closing = closingWall(walls("3200 R 2500 R 2800 R 400 L 400 R"))

        assertEquals(ClosingWall(2_100.mm, Turn.RIGHT), closing)
    }

    @Test
    fun closingWallCompletesACounterClockwiseRectangle() {
        assertEquals(ClosingWall(2_000.mm, Turn.LEFT), closingWall(walls("3000 L 2000 L 3000 L")))
    }

    @Test
    fun closingWallIsNullWhenWayBackIsNotAlongTheHeading() {
        assertNull(closingWall(walls("3000 R 2000 R 1000 R")))
    }

    @Test
    fun closingWallIsNullWhenWayBackIsOppositeToTheHeading() {
        assertNull(closingWall(walls("3000 R 2000 R 3000 L")))
    }

    @Test
    fun closingWallIsNullWhenTurnsCannotCompleteAFullTurn() {
        assertNull(closingWall(walls("2000 R 1000 R 1000 R 2000 L 1000 L")))
    }

    @Test
    fun closingWallNeedsAtLeastThreeWalls() {
        assertNull(closingWall(walls("3000 R 2000 R")))
    }
}
