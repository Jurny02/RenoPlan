package dev.jurczyk.renoplan.domain.calculator

import dev.jurczyk.renoplan.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PricePerSquareMetreTest {
    @Test
    fun documentedExample() {
        // 200 PLN, 5 L, 7 m²/L, 2 coats → 17.5 m² per package → 11.43 PLN/m² (5.71 for one coat).
        val result = pricePerSquareMetre(packagePrice = Money(20_000), amount = 5.0, coverage = 7.0, coats = 2)

        assertEquals(17.5, result.areaPerPackage, 1e-9)
        assertEquals(Money(1_143), result.price)
        assertEquals(Money(571), result.oneCoatPrice)
    }

    @Test
    fun oneCoatPriceEqualsPriceForOneCoat() {
        val result = pricePerSquareMetre(packagePrice = Money(9_999), amount = 2.5, coverage = 10.0, coats = 1)

        assertEquals(25.0, result.areaPerPackage, 1e-9)
        assertEquals(Money(400), result.price)
        assertEquals(result.price, result.oneCoatPrice)
    }

    @Test
    fun moreCoatsCostMorePerSquareMetre() {
        val result = pricePerSquareMetre(packagePrice = Money(12_000), amount = 25.0, coverage = 1.2, coats = 3)

        assertEquals(10.0, result.areaPerPackage, 1e-9)
        assertEquals(Money(1_200), result.price)
        assertEquals(Money(400), result.oneCoatPrice)
    }

    @Test
    fun rejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> { pricePerSquareMetre(Money(-1), 5.0, 7.0, 2) }
        assertFailsWith<IllegalArgumentException> { pricePerSquareMetre(Money(100), 0.0, 7.0, 2) }
        assertFailsWith<IllegalArgumentException> { pricePerSquareMetre(Money(100), 5.0, 0.0, 2) }
        assertFailsWith<IllegalArgumentException> { pricePerSquareMetre(Money(100), 5.0, 7.0, 0) }
    }
}
