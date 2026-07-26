package com.kara.kara_general_api.domain.model.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RoomStockItemTest {

    private val roomId = RoomId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())

    @Test
    fun `should build a stock item when quantity is positive`() {
        val item = RoomStockItem(roomId = roomId, productId = productId, quantity = 24)

        assertEquals(24, item.quantity)
    }

    @Test
    fun `should build a stock item when quantity is zero`() {
        val item = RoomStockItem(roomId = roomId, productId = productId, quantity = 0)

        assertEquals(0, item.quantity)
    }

    @Test
    fun `should throw when quantity is negative`() {
        assertThrows<IllegalArgumentException> {
            RoomStockItem(roomId = roomId, productId = productId, quantity = -1)
        }
    }
}
