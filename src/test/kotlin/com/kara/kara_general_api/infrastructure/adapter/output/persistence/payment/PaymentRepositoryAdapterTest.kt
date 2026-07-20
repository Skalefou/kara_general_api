package com.kara.kara_general_api.infrastructure.adapter.output.persistence.payment

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class PaymentRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: PaymentRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val bookingId = BookingId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM payments", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        val roomId = RoomId(UUID.randomUUID())
        insertRoom(roomId)
        insertBooking(bookingId, roomId, userId)
    }

    private fun payment(intentId: String = "pi_1", status: PaymentStatus = PaymentStatus.PENDING) =
        Payment(
            id = com.kara.kara_general_api.domain.model.payment.PaymentId(UUID.randomUUID()),
            bookingId = bookingId,
            userId = userId,
            amount = BigDecimal("435.00"),
            currency = Currency.EUR,
            status = status,
            stripePaymentIntentId = intentId,
            createdAt = Instant.now(),
        )

    @Test
    fun `save then findById returns the payment`() {
        val saved = payment()

        adapter.save(saved)
        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(PaymentStatus.PENDING, found!!.status)
        assertEquals("pi_1", found.stripePaymentIntentId)
        assertEquals(BigDecimal("435.00"), found.amount)
    }

    @Test
    fun `findByStripePaymentIntentId returns the matching payment`() {
        adapter.save(payment(intentId = "pi_lookup"))

        val found = adapter.findByStripePaymentIntentId("pi_lookup")

        assertNotNull(found)
        assertEquals("pi_lookup", found!!.stripePaymentIntentId)
    }

    @Test
    fun `findByStripePaymentIntentId returns null when unknown`() {
        assertNull(adapter.findByStripePaymentIntentId("pi_absent"))
    }

    @Test
    fun `save upserts the status on conflict`() {
        val saved = payment()
        adapter.save(saved)

        adapter.save(saved.markPaid())

        assertEquals(PaymentStatus.PAID, adapter.findById(saved.id)!!.status)
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

    private fun insertBooking(id: BookingId, roomId: RoomId, userId: UserId) {
        val sql =
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, created_at, expires_at)
            VALUES (:id, :roomId, :userId, '2026-08-01T18:00:00Z', '2026-08-01T21:00:00Z', 8,
                    435.00, 'EUR', 'PENDING', NOW(), NOW() + INTERVAL '15 minutes')
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf("id" to id.value, "roomId" to roomId.value, "userId" to userId.value),
        )
    }
}
