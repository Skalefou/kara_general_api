package com.kara.kara_general_api.infrastructure.adapter.output.persistence.stock

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockItem
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class RoomStockRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: RoomStockRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM room_products", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM products", emptyMap<String, Any>())
        insertRoom(roomId)
    }

    private fun insertRoom(id: RoomId) {
        jdbc.update(
            """
            INSERT INTO rooms (id, name, street, city, postal_code, country, status, created_at)
            VALUES (:id, 'Salle démo', '1 rue', 'Paris', '75001', 'France', 'OPEN', NOW())
            """.trimIndent(),
            mapOf("id" to id.value),
        )
    }

    private fun insertProduct(
        name: String,
        price: String,
    ): ProductId {
        val id = ProductId(UUID.randomUUID())
        jdbc.update(
            """
            INSERT INTO products (id, name, description, price, currency, created_at)
            VALUES (:id, :name, NULL, :price, 'EUR', NOW())
            """.trimIndent(),
            mapOf("id" to id.value, "name" to name, "price" to java.math.BigDecimal(price)),
        )
        return id
    }

    @Test
    fun `upsert then findByRoomId returns the product with its quantity`() {
        val productId = insertProduct("Coca-Cola 33cl", "2.50")

        adapter.upsert(RoomStockItem(roomId, productId, 24))
        val stock = adapter.findByRoomId(roomId)

        assertEquals(1, stock.size)
        assertEquals(productId, stock[0].product.id)
        assertEquals("Coca-Cola 33cl", stock[0].product.name)
        assertEquals(24, stock[0].quantity)
    }

    @Test
    fun `upsert updates the quantity of an existing room-product pair`() {
        val productId = insertProduct("Eau minérale 50cl", "1.50")

        adapter.upsert(RoomStockItem(roomId, productId, 10))
        adapter.upsert(RoomStockItem(roomId, productId, 42))
        val stock = adapter.findByRoomId(roomId)

        assertEquals(1, stock.size)
        assertEquals(42, stock[0].quantity)
    }

    @Test
    fun `findByRoomId returns the stock ordered by product name`() {
        val pizza = insertProduct("Part de pizza", "4.00")
        val coca = insertProduct("Coca-Cola 33cl", "2.50")
        val eau = insertProduct("Eau minérale 50cl", "1.50")
        adapter.upsert(RoomStockItem(roomId, pizza, 5))
        adapter.upsert(RoomStockItem(roomId, coca, 5))
        adapter.upsert(RoomStockItem(roomId, eau, 5))

        val stock = adapter.findByRoomId(roomId)

        assertEquals(
            listOf("Coca-Cola 33cl", "Eau minérale 50cl", "Part de pizza"),
            stock.map { it.product.name },
        )
    }

    @Test
    fun `findQuantity returns the stored quantity when present and null when absent`() {
        val productId = insertProduct("Coca-Cola 33cl", "2.50")
        adapter.upsert(RoomStockItem(roomId, productId, 24))

        assertEquals(24, adapter.findQuantity(roomId, productId))
        assertNull(adapter.findQuantity(roomId, ProductId(UUID.randomUUID())))
    }

    @Test
    fun `tryDecrement lowers the quantity and returns true when the stock covers the request`() {
        val productId = insertProduct("Coca-Cola 33cl", "2.50")
        adapter.upsert(RoomStockItem(roomId, productId, 10))

        assertTrue(adapter.tryDecrement(roomId, productId, 4))

        assertEquals(6, adapter.findQuantity(roomId, productId))
    }

    @Test
    fun `tryDecrement leaves the quantity untouched and returns false when the stock is too low`() {
        val productId = insertProduct("Coca-Cola 33cl", "2.50")
        adapter.upsert(RoomStockItem(roomId, productId, 3))

        assertFalse(adapter.tryDecrement(roomId, productId, 4))

        assertEquals(3, adapter.findQuantity(roomId, productId))
    }

    @Test
    fun `tryDecrement returns false when the product is not in the room stock`() {
        assertFalse(adapter.tryDecrement(roomId, ProductId(UUID.randomUUID()), 1))
    }

    @Test
    fun `deleteByRoomIdAndProductId removes the row and returns true, false when absent`() {
        val productId = insertProduct("Coca-Cola 33cl", "2.50")
        adapter.upsert(RoomStockItem(roomId, productId, 24))

        assertTrue(adapter.deleteByRoomIdAndProductId(roomId, productId))
        assertTrue(adapter.findByRoomId(roomId).isEmpty())
        assertFalse(adapter.deleteByRoomIdAndProductId(roomId, productId))
    }
}
