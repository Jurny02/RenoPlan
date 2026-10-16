package dev.jurczyk.renoplan.domain.calculator

import dev.jurczyk.renoplan.domain.model.Money
import kotlin.math.roundToLong

/** The unit a package of material is sold in. */
enum class PackageUnit {
    LITRE,
    KILOGRAM,
}

/**
 * Result of the price-per-m² calculator (docs/04-operations-and-costs.md).
 * Prices are rounded to the nearest grosz.
 */
data class PricePerSquareMetre(
    /** m² one package covers with the given number of coats. */
    val areaPerPackage: Double,
    val price: Money,
    /** Price per m² for a single coat, for reference. */
    val oneCoatPrice: Money,
)

/**
 * Converts a package of material into a price per m²:
 * `area per package = amount · coverage / coats`, `price per m² = package price / area per package`.
 *
 * @param amount of material in one package, in L or kg
 * @param coverage m² per L or kg, for one coat
 */
fun pricePerSquareMetre(
    packagePrice: Money,
    amount: Double,
    coverage: Double,
    coats: Int,
): PricePerSquareMetre {
    require(packagePrice >= Money.Zero) { "Package price must not be negative: $packagePrice" }
    require(amount > 0 && coverage > 0) { "Amount and coverage must be positive: $amount, $coverage" }
    require(coats > 0) { "Coats must be positive: $coats" }
    val oneCoatArea = amount * coverage
    val areaPerPackage = oneCoatArea / coats
    return PricePerSquareMetre(
        areaPerPackage = areaPerPackage,
        price = Money((packagePrice.grosze / areaPerPackage).roundToLong()),
        oneCoatPrice = Money((packagePrice.grosze / oneCoatArea).roundToLong()),
    )
}
