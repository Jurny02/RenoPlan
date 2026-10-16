package dev.jurczyk.renoplan.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.roundToLong

private const val GROSZE_PER_ZLOTY = 100.0

/** An amount in whole grosze (0.01 PLN), see ADR 0005. */
@JvmInline
value class Money(
    val grosze: Long,
) : Comparable<Money> {
    val zloty: Double get() = grosze / GROSZE_PER_ZLOTY

    operator fun plus(other: Money): Money = Money(grosze + other.grosze)

    operator fun minus(other: Money): Money = Money(grosze - other.grosze)

    /** A price per unit times a quantity, rounded to the nearest grosz. */
    operator fun times(quantity: Double): Money = Money((grosze * quantity).roundToLong())

    override fun compareTo(other: Money): Int = grosze.compareTo(other.grosze)

    override fun toString(): String = "$grosze gr"

    companion object {
        val Zero = Money(0)

        fun fromZloty(zloty: Double): Money = Money((zloty * GROSZE_PER_ZLOTY).roundToLong())
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.grosze })
