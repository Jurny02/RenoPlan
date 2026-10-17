package dev.jurczyk.renoplan.domain.calculator

import dev.jurczyk.renoplan.domain.model.Money
import kotlin.math.roundToLong

/**
 * Price per running metre of material sold by length (skirting, crown moulding, trims):
 * `price per m = package price / metres in the package`, rounded to the nearest grosz.
 */
fun pricePerMetre(
    packagePrice: Money,
    metres: Double,
): Money {
    require(packagePrice >= Money.Zero) { "Package price must not be negative: $packagePrice" }
    require(metres > 0) { "Metres must be positive: $metres" }
    return Money((packagePrice.grosze / metres).roundToLong())
}
