package com.kara.kara_general_api.infrastructure.adapter.output.persistence.order

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.order.OrderId
import com.kara.kara_general_api.domain.model.order.OrderStatus
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class OrderRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: OrderRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM orders", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM room_products", emptyMap<String, Any>())
        jdbc.update("DELETE FROM products", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        insertRoom(roomId)
        insertProduct(productId)
        insertBooking(bookingId)
    }

    private fun order(
        id: OrderId = OrderId.generate(),
        quantity: Int = 2,
        createdAt: Instant = Instant.parse("2026-08-01T19:00:00Z"),
    ) = Order(
        id = id,
        bookingId = bookingId,
        userId = userId,
        productId = productId,
        quantity = quantity,
        unitPrice = BigDecimal("2.50"),
        currency = Currency.EUR,
        totalPrice = BigDecimal("2.50").multiply(BigDecimal(quantity)),
        status = OrderStatus.PLACED,
        createdAt = createdAt,
    )

    @Test
    fun `save then findByBookingId returns the persisted order`() {
        val saved = adapter.save(order(quantity = 3))

        val found = adapter.findByBookingId(bookingId)

        assertEquals(1, found.size)
        assertEquals(saved.id, found[0].id)
        assertEquals(3, found[0].quantity)
        assertEquals(BigDecimal("2.50"), found[0].unitPrice)
        assertEquals(BigDecimal("7.50"), found[0].totalPrice)
        assertEquals(OrderStatus.PLACED, found[0].status)
        assertEquals(Currency.EUR, found[0].currency)
    }

    @Test
    fun `findByBookingId returns the orders ordered by creation date`() {
        val first = order(quantity = 1, createdAt = Instant.parse("2026-08-01T19:00:00Z"))
        val second = order(quantity = 2, createdAt = Instant.parse("2026-08-01T19:30:00Z"))
        adapter.save(second)
        adapter.save(first)

        val found = adapter.findByBookingId(bookingId)

        assertEquals(listOf(first.id, second.id), found.map { it.id })
    }

    @Test
    fun `findByBookingId returns an empty list for a booking without orders`() {
        assertEquals(emptyList<Order>(), adapter.findByBookingId(BookingId(UUID.randomUUID())))
    }

    private fun insertUser(id: UserId) {
        val sql =
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, 'hash', 'Jane', 'Doe', '+33612345678',
                    '1990-01-01', 'CLIENT', :firebaseUid, NOW(), true)
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "email" to "user_${id.value}@example.com",
                "firebaseUid" to "uid_${id.value}",
            ),
        )
    }

    private fun insertRoom(id: RoomId) {
        val sql =
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, 'Salle', 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 50, true, false, false, 'OPEN', NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value))
    }

    private fun insertProduct(id: ProductId) {
        val sql =
            """
            INSERT INTO products (id, name, description, price, currency, created_at)
            VALUES (:id, 'Coca-Cola 33cl', NULL, 2.50, 'EUR', NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value))
    }

    private fun insertBooking(id: BookingId) {
        val sql =
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, payment_mode, created_at, expires_at)
            VALUES (:id, :roomId, :userId, :startAt, :endAt, 8,
                    435.00, 'EUR', 'CONFIRMED', 'PAY_ALL', :createdAt, :expiresAt)
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "roomId" to roomId.value,
                "userId" to userId.value,
                "startAt" to Timestamp.from(Instant.parse("2026-08-01T18:00:00Z")),
                "endAt" to Timestamp.from(Instant.parse("2026-08-01T21:00:00Z")),
                "createdAt" to Timestamp.from(Instant.parse("2026-07-20T10:00:00Z")),
                "expiresAt" to Timestamp.from(Instant.parse("2026-07-20T10:15:00Z")),
            ),
        )
    }
}
