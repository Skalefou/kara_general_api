package com.kara.kara_general_api.domain.model.product

import com.kara.kara_general_api.domain.model.room.Currency
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ProductTest {

    private val id = ProductId(UUID.randomUUID())

    @Test
    fun `should build a product when name is present and price is positive`() {
        val product =
            Product(
                id = id,
                name = "Coca-Cola 33cl",
                description = "Canette 33cl",
                price = BigDecimal("2.50"),
                currency = Currency.EUR,
            )

        assertEquals("Coca-Cola 33cl", product.name)
        assertEquals(BigDecimal("2.50"), product.price)
    }

    @Test
    fun `should build a product when price is zero`() {
        val product =
            Product(
                id = id,
                name = "Verre d'eau offert",
                description = null,
                price = BigDecimal.ZERO,
                currency = Currency.EUR,
            )

        assertEquals(BigDecimal.ZERO, product.price)
    }

    @Test
    fun `should throw when name is blank`() {
        assertThrows<IllegalArgumentException> {
            Product(
                id = id,
                name = "   ",
                description = null,
                price = BigDecimal("2.50"),
                currency = Currency.EUR,
            )
        }
    }

    @Test
    fun `should throw when price is negative`() {
        assertThrows<IllegalArgumentException> {
            Product(
                id = id,
                name = "Part de pizza",
                description = null,
                price = BigDecimal("-1.00"),
                currency = Currency.EUR,
            )
        }
    }
}
