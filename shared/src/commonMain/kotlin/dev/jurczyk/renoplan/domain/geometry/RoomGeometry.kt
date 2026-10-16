package dev.jurczyk.renoplan.domain.geometry

import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.Room
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.domain.model.squareMetres
import dev.jurczyk.renoplan.domain.model.squareMillimetresToSquareMetres
import dev.jurczyk.renoplan.domain.model.sum
import kotlin.math.abs

// Formulas from docs/03-calculations.md. Results for a room whose outline is not closed are
// meaningless; check validateOutline first.

/** Floor area in m² (shoelace formula). */
val Room.floorArea: Double
    get() {
        val points = outlineVertices(walls).dropLast(1)
        if (points.isEmpty()) return 0.0
        val twiceArea =
            points.indices.sumOf { i ->
                val a = points[i]
                val b = points[(i + 1) % points.size]
                a.x * b.y - b.x * a.y
            }
        return squareMillimetresToSquareMetres(abs(twiceArea)) / 2
    }

/** Ceiling area in m²; equal to the floor for a flat ceiling. */
val Room.ceilingArea: Double get() = floorArea

val Room.perimeter: Millimetres get() = walls.map { it.length }.sum()

/** Volume in m³. */
val Room.volume: Double get() = floorArea * height.metres

/** Opening area in m². */
val Opening.area: Double
    get() =
        when (val size = size) {
            is OpeningSize.Dimensions -> squareMetres(size.width, size.height)
            is OpeningSize.Area -> size.squareMetres
        }

/** Wall area in m² before subtracting openings. */
fun Room.wallGrossArea(wall: Wall): Double = squareMetres(wall.length, height)

/** Wall area in m² minus its openings. Reveals and niche depths are ignored. */
fun Room.wallNetArea(wall: Wall): Double = wallGrossArea(wall) - wall.openings.sumOf { it.area }

val Room.wallsGrossArea: Double get() = walls.sumOf { wallGrossArea(it) }

val Room.wallsNetArea: Double get() = walls.sumOf { wallNetArea(it) }

/** Perimeter minus the widths of openings that start at the floor. */
val Room.skirtingLength: Millimetres
    get() {
        val floorOpeningWidths =
            walls
                .flatMap { it.openings }
                .filter { it.startsAtFloor }
                .mapNotNull { (it.size as? OpeningSize.Dimensions)?.width }
        return perimeter - floorOpeningWidths.sum()
    }

val Room.crownMouldingLength: Millimetres get() = perimeter
