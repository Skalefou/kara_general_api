package com.kara.kara_general_api.domain.model.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class OrderTest {
    private fun order(
        quantity: Int = 2,
        unitPrice: BigDecimal = BigDecimal("2.50"),
        totalPrice: BigDecimal = BigDecimal("5.00"),
    ) = Order(
        id = OrderId.generate(),
        bookingId = BookingId(UUID.randomUUID()),
        userId = UserId(UUID.randomUUID()),
        productId = ProductId(UUID.randomUUID()),
        quantity = quantity,
        unitPrice = unitPrice,
        currency = Currency.EUR,
        totalPrice = totalPrice,
        status = OrderStatus.PLACED,
        createdAt = Instant.parse("2026-08-01T19:00:00Z"),
    )

    @Test
    fun `creates an order when quantity is positive and total is coherent`() {
        val created = order(quantity = 3, unitPrice = BigDecimal("2.50"), totalPrice = BigDecimal("7.50"))

        assertEquals(3, created.quantity)
        assertEquals(BigDecimal("7.50"), created.totalPrice)
    }

    @Test
    fun `rejects a quantity of zero`() {
        assertThrows<IllegalArgumentException> {
            order(quantity = 0, totalPrice = BigDecimal("0.00"))
        }
    }

    @Test
    fun `rejects a negative quantity`() {
        assertThrows<IllegalArgumentException> {
            order(quantity = -1, totalPrice = BigDecimal("-2.50"))
        }
    }

    @Test
    fun `rejects an incoherent total price`() {
        assertThrows<IllegalArgumentException> {
            order(quantity = 2, unitPrice = BigDecimal("2.50"), totalPrice = BigDecimal("4.00"))
        }
    }
}
