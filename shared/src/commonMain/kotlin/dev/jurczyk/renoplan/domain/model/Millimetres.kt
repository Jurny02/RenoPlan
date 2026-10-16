package dev.jurczyk.renoplan.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.roundToLong

private const val MILLIMETRES_PER_METRE = 1_000.0
private const val SQUARE_MILLIMETRES_PER_SQUARE_METRE = 1_000_000.0

/** A length in whole millimetres (see ADR 0002). */
@JvmInline
value class Millimetres(
    val value: Long,
) : Comparable<Millimetres> {
    val metres: Double get() = value / MILLIMETRES_PER_METRE

    operator fun plus(other: Millimetres): Millimetres = Millimetres(value + other.value)

    operator fun minus(other: Millimetres): Millimetres = Millimetres(value - other.value)

    override fun compareTo(other: Millimetres): Int = value.compareTo(other.value)

    override fun toString(): String = "$value mm"

    companion object {
        val Zero = Millimetres(0)

        fun fromMetres(metres: Double): Millimetres = Millimetres((metres * MILLIMETRES_PER_METRE).roundToLong())
    }
}

val Int.mm: Millimetres get() = Millimetres(toLong())

fun Iterable<Millimetres>.sum(): Millimetres = Millimetres(sumOf { it.value })

/** Area of a [width] × [height] rectangle in m². */
fun squareMetres(
    width: Millimetres,
    height: Millimetres,
): Double = squareMillimetresToSquareMetres(width.value * height.value)

internal fun squareMillimetresToSquareMetres(squareMillimetres: Long): Double =
    squareMillimetres / SQUARE_MILLIMETRES_PER_SQUARE_METRE
