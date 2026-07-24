package com.kara.kara_general_api.infrastructure.adapter.output.persistence.invoice

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
class InvoiceRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: InvoiceRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val userId = UserId(UUID.randomUUID())
    private val roomId = UUID.randomUUID()

    // Reservation (pay-all) — PAID payment
    private val paidPaymentId = PaymentId(UUID.randomUUID())
    private val pendingPaymentId = PaymentId(UUID.randomUUID())
    private val reservationBookingId = UUID.randomUUID()

    // Cagnotte — CAPTURED share
    private val capturedShareId = PoolShareId(UUID.randomUUID())
    private val authorizedShareId = PoolShareId(UUID.randomUUID())
    private val cagnotteBookingId = UUID.randomUUID()
    private val poolId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM pool_shares", emptyMap<String, Any>())
        jdbc.update("DELETE FROM pools", emptyMap<String, Any>())
        jdbc.update("DELETE FROM payments", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())

        insertUser(userId, "Jane", "Doe")
        insertRoom(roomId, "Salle Étoile")
        insertBooking(reservationBookingId, roomId, userId)
        insertBooking(cagnotteBookingId, roomId, userId)

        // PAID payment → appears ; PENDING payment → excluded.
        insertPayment(paidPaymentId, reservationBookingId, userId, "PAID", "435.00", "2026-07-10T10:00:00Z")
        insertPayment(pendingPaymentId, reservationBookingId, userId, "PENDING", "435.00", "2026-07-11T10:00:00Z")

        // CAPTURED share → appears ; AUTHORIZED share → excluded.
        insertPool(poolId, cagnotteBookingId)
        insertShare(capturedShareId, poolId, userId, "CAPTURED", "50.00", "2026-07-05T10:00:00Z")
        insertShare(authorizedShareId, poolId, userId, "AUTHORIZED", "50.00", "2026-07-06T10:00:00Z")
    }

    @Test
    fun `findByUser returns the union of PAID payments and CAPTURED shares, newest first`() {
        val invoices = adapter.findByUser(userId)

        assertEquals(2, invoices.size)
        // reservation (2026-07-10) is newer than the captured share (2026-07-05) → first
        assertEquals(InvoiceType.RESERVATION, invoices[0].type)
        assertEquals("PAY-${paidPaymentId.value}", invoices[0].id.value)
        assertEquals(InvoiceType.CAGNOTTE, invoices[1].type)
        assertEquals("SHR-${capturedShareId.value}", invoices[1].id.value)
        assertEquals("Salle Étoile", invoices[0].label)
    }

    @Test
    fun `findByUser returns empty when the user has no settled source`() {
        assertTrue(adapter.findByUser(UserId(UUID.randomUUID())).isEmpty())
    }

    @Test
    fun `findReservationDetail returns the receipt and buyer only when PAID`() {
        val detail = adapter.findReservationDetail(paidPaymentId)

        assertNotNull(detail)
        assertEquals(userId, detail!!.ownerId)
        assertEquals("Jane Doe", detail.buyer.fullName)
        assertEquals(InvoiceType.RESERVATION, detail.invoice.type)
        assertNull(adapter.findReservationDetail(pendingPaymentId))
    }

    @Test
    fun `findCagnotteDetail returns the receipt and buyer only when CAPTURED`() {
        val detail = adapter.findCagnotteDetail(capturedShareId)

        assertNotNull(detail)
        assertEquals(userId, detail!!.ownerId)
        assertEquals(InvoiceType.CAGNOTTE, detail.invoice.type)
        assertNull(adapter.findCagnotteDetail(authorizedShareId))
    }

    private fun insertUser(id: UserId, firstName: String, lastName: String) {
        jdbc.update(
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, 'hash', :firstName, :lastName, '+33612345678',
                    '1990-01-01', 'CLIENT', :firebaseUid, NOW(), true)
            """.trimIndent(),
            mapOf(
                "id" to id.value,
                "email" to "user_${id.value}@example.com",
                "firstName" to firstName,
                "lastName" to lastName,
                "firebaseUid" to "uid_${id.value}",
            ),
        )
    }

    private fun insertRoom(id: UUID, name: String) {
        jdbc.update(
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, :name, 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 50, true, false, false, 'OPEN', NOW())
            """.trimIndent(),
            mapOf("id" to id, "name" to name),
        )
    }

    private fun insertBooking(id: UUID, roomId: UUID, userId: UserId) {
        jdbc.update(
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, created_at, expires_at)
            VALUES (:id, :roomId, :userId, '2026-08-01T18:00:00Z', '2026-08-01T21:00:00Z', 8,
                    435.00, 'EUR', 'CONFIRMED', NOW(), NOW() + INTERVAL '15 minutes')
            """.trimIndent(),
            mapOf("id" to id, "roomId" to roomId, "userId" to userId.value),
        )
    }

    private fun insertPayment(id: PaymentId, bookingId: UUID, userId: UserId, status: String, amount: String, createdAt: String) {
        jdbc.update(
            """
            INSERT INTO payments (id, booking_id, user_id, amount, currency, status,
                                  stripe_payment_intent_id, created_at)
            VALUES (:id, :bookingId, :userId, :amount, 'EUR', :status, :intent, :createdAt)
            """.trimIndent(),
            mapOf(
                "id" to id.value,
                "bookingId" to bookingId,
                "userId" to userId.value,
                "amount" to BigDecimal(amount),
                "status" to status,
                "intent" to "pi_${id.value}",
                "createdAt" to Timestamp.from(Instant.parse(createdAt)),
            ),
        )
    }

    private fun insertPool(id: UUID, bookingId: UUID) {
        jdbc.update(
            """
            INSERT INTO pools (id, booking_id, target_amount, currency, status, deadline, global_link_token, created_at)
            VALUES (:id, :bookingId, 100.00, 'EUR', 'SETTLED', NOW() + INTERVAL '1 day', :token, NOW())
            """.trimIndent(),
            mapOf("id" to id, "bookingId" to bookingId, "token" to "tok_$id"),
        )
    }

    private fun insertShare(id: PoolShareId, poolId: UUID, payerUserId: UserId, status: String, amount: String, createdAt: String) {
        jdbc.update(
            """
            INSERT INTO pool_shares (id, pool_id, participant_name, email, amount, status,
                                     stripe_payment_intent_id, unique_link_token, payer_user_id, is_creator_share, created_at)
            VALUES (:id, :poolId, 'Jane', 'jane@example.com', :amount, :status,
                    :intent, :linkToken, :payerUserId, false, :createdAt)
            """.trimIndent(),
            mapOf(
                "id" to id.value,
                "poolId" to poolId,
                "amount" to BigDecimal(amount),
                "status" to status,
                "intent" to "pi_share_${id.value}",
                "linkToken" to "link_${id.value}",
                "payerUserId" to payerUserId.value,
                "createdAt" to Timestamp.from(Instant.parse(createdAt)),
            ),
        )
    }
}
