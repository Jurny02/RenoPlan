package dev.jurczyk.renoplan.presentation.common

import dev.jurczyk.renoplan.domain.model.Millimetres
import kotlin.math.round

private const val MILLIMETRES_PER_METRE = 1_000L
private const val MILLIMETRE_DIGITS = 3

/**
 * Parses a length typed in metres, with a comma or a dot, e.g. `"2,65"` → 2650 mm.
 * Returns `null` for blank, malformed, zero or negative input.
 */
fun parseMetres(text: String): Millimetres? {
    val metres =
        text
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() } ?: return null
    return Millimetres.fromMetres(metres).takeIf { it.value > 0 }
}

/** Formats a length for an input field in metres, without trailing zeros, e.g. 2600 mm → `"2,6"`. */
fun Millimetres.toMetresInput(): String {
    val whole = value / MILLIMETRES_PER_METRE
    val fraction =
        (value % MILLIMETRES_PER_METRE)
            .toString()
            .padStart(MILLIMETRE_DIGITS, '0')
            .trimEnd('0')
    return if (fraction.isEmpty()) "$whole" else "$whole,$fraction"
}

private const val AREA_INPUT_DECIMALS = 100.0

/** Parses an area typed in m², with a comma or a dot. Returns `null` for blank, malformed or non-positive input. */
fun parseSquareMetres(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }

/** Formats an area for an input field, to 0.01 m² and without trailing zeros, e.g. `1.5` → `"1,5"`. */
fun Double.toSquareMetresInput(): String {
    val hundredths = round(this * AREA_INPUT_DECIMALS).toLong()
    val fraction = (hundredths % AREA_INPUT_DECIMALS.toLong()).toString().padStart(2, '0').trimEnd('0')
    val whole = hundredths / AREA_INPUT_DECIMALS.toLong()
    return if (fraction.isEmpty()) "$whole" else "$whole,$fraction"
}
