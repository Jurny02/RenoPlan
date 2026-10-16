package dev.jurczyk.renoplan.ui.format

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Formats [value] with a decimal comma (the UI is Polish), e.g. `7.84` → `"7,84"`. */
fun formatDecimal(
    value: Double,
    decimals: Int = 2,
): String {
    val factor = 10.0.pow(decimals).toLong()
    val scaled = (abs(value) * factor).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val whole = scaled / factor
    if (decimals == 0) return "$sign$whole"
    val fraction = (scaled % factor).toString().padStart(decimals, '0')
    return "$sign$whole,$fraction"
}
