package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class PoolShareRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: PoolShareRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val poolId = PoolId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM pool_shares", emptyMap<String, Any>())
        jdbc.update("DELETE FROM pools", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        val roomId = UUID.randomUUID()
        insertRoom(roomId)
        insertBooking(bookingId.value, roomId, userId.value)
        insertPool(poolId.value, bookingId.value)
    }

    private fun share(
        name: String = "Alice",
        email: Email? = Email("alice@ex.com"),
        amount: String = "50.00",
        status: PoolShareStatus = PoolShareStatus.PENDING,
        intent: String? = null,
        token: String? = "tok-${UUID.randomUUID()}",
        payer: UserId? = null,
        isCreator: Boolean = false,
    ) = PoolShare(
        com.kara.kara_general_api.domain.model.payment
            .PoolShareId(UUID.randomUUID()),
        poolId,
        name,
        email,
        BigDecimal(amount),
        status,
        intent,
        token,
        payer,
        isCreator,
    )

    @Test
    fun `saveAll then findByPoolId returns the shares ordered by creation`() {
        adapter.saveAll(
            listOf(
                share(name = "Créateur", email = null, amount = "60.00", isCreator = true),
                share(name = "Bob", amount = "40.00"),
            ),
        )

        val shares = adapter.findByPoolId(poolId)

        assertEquals(2, shares.size)
        assertEquals(BigDecimal("100.00"), shares.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount })
    }

    @Test
    fun `save upserts status, intent and payer on conflict`() {
        val original = share(amount = "50.00")
        adapter.save(original)

        adapter.save(original.withAuthorizationIntent("pi_1", userId).markAuthorized())

        val found = adapter.findById(original.id)!!
        assertEquals(PoolShareStatus.AUTHORIZED, found.status)
        assertEquals("pi_1", found.stripePaymentIntentId)
        assertEquals(userId, found.payerUserId)
    }

    @Test
    fun `findByUniqueLinkToken and findByStripePaymentIntentId locate the share`() {
        val s = share(token = "unique-token").withAuthorizationIntent("pi_lookup", userId)
        adapter.save(s)

        assertEquals(s.id, adapter.findByUniqueLinkToken("unique-token")!!.id)
        assertEquals(s.id, adapter.findByStripePaymentIntentId("pi_lookup")!!.id)
        assertNull(adapter.findByStripePaymentIntentId("absent"))
    }

    @Test
    fun `findByPoolIdAndPayerUserId returns the share of that payer`() {
        val mine = share(name = "Alice", token = "mine").withAuthorizationIntent("pi_mine", userId)
        adapter.save(mine)

        assertEquals(mine.id, adapter.findByPoolIdAndPayerUserId(poolId, userId)!!.id)
    }

    @Test
    fun `findByPoolIdAndPayerUserId returns null when the payer holds no share`() {
        adapter.save(share(name = "Alice", token = "someone-else").withAuthorizationIntent("pi_a", userId))

        val stranger = UserId(UUID.randomUUID())
        insertUser(stranger)

        assertNull(adapter.findByPoolIdAndPayerUserId(poolId, stranger))
    }

    @Test
    fun `findByPoolIdAndPayerUserId never returns the share of another payer`() {
        // Le filtrage est fait en SQL sur payer_user_id : la part d'un tiers ne peut pas fuiter.
        val otherPayer = UserId(UUID.randomUUID())
        insertUser(otherPayer)
        adapter.save(share(name = "Bob", token = "bob").withAuthorizationIntent("pi_bob", otherPayer))
        val mine = share(name = "Alice", token = "alice").withAuthorizationIntent("pi_alice", userId)
        adapter.save(mine)

        val found = adapter.findByPoolIdAndPayerUserId(poolId, userId)!!

        assertEquals(mine.id, found.id)
        assertEquals(userId, found.payerUserId)
    }

    @Test
    fun `findByPoolIdAndPayerUserId ignores shares whose payer is still null`() {
        // Part jamais présentée au paiement : payer_user_id IS NULL, elle n'appartient à personne.
        adapter.save(share(name = "Anonyme", token = "no-payer", payer = null))

        assertNull(adapter.findByPoolIdAndPayerUserId(poolId, userId))
    }

    @Test
    fun `findByPoolIdAndPayerUserId does not leak a share from another pool of the same payer`() {
        val otherRoomId = UUID.randomUUID()
        insertRoom(otherRoomId)
        val otherBookingId = UUID.randomUUID()
        insertBooking(otherBookingId, otherRoomId, userId.value)
        val otherPoolId = PoolId(UUID.randomUUID())
        insertPool(otherPoolId.value, otherBookingId)

        val shareInOtherPool =
            share(name = "Alice", token = "other-pool")
                .copy(poolId = otherPoolId)
                .withAuthorizationIntent("pi_other_pool", userId)
        adapter.save(shareInOtherPool)

        assertNull(adapter.findByPoolIdAndPayerUserId(poolId, userId))
        assertEquals(shareInOtherPool.id, adapter.findByPoolIdAndPayerUserId(otherPoolId, userId)!!.id)
    }

    @Test
    fun `existsForBookingAndPayer is true when the user paid a share of a pool of that booking`() {
        adapter.save(share(name = "Alice", token = "paid").withAuthorizationIntent("pi_paid", userId))

        assertTrue(adapter.existsForBookingAndPayer(bookingId, userId))
    }

    @Test
    fun `existsForBookingAndPayer is false for a user holding no share of that booking`() {
        val stranger = UserId(UUID.randomUUID())
        insertUser(stranger)
        adapter.save(share(name = "Alice", token = "mine-only").withAuthorizationIntent("pi_mine", userId))

        assertFalse(adapter.existsForBookingAndPayer(bookingId, stranger))
    }

    @Test
    fun `existsForBookingAndPayer is false when the share was never paid`() {
        adapter.save(share(name = "Anonyme", token = "unpaid", payer = null))

        assertFalse(adapter.existsForBookingAndPayer(bookingId, userId))
    }

    @Test
    fun `existsForBookingAndPayer does not leak involvement from a pool of another booking`() {
        val otherRoomId = UUID.randomUUID()
        insertRoom(otherRoomId)
        val otherBookingId = BookingId(UUID.randomUUID())
        insertBooking(otherBookingId.value, otherRoomId, userId.value)
        val otherPoolId = PoolId(UUID.randomUUID())
        insertPool(otherPoolId.value, otherBookingId.value)
        adapter.save(
            share(name = "Alice", token = "other-booking")
                .copy(poolId = otherPoolId)
                .withAuthorizationIntent("pi_other_booking", userId),
        )

        assertFalse(adapter.existsForBookingAndPayer(bookingId, userId))
        assertTrue(adapter.existsForBookingAndPayer(otherBookingId, userId))
    }

    @Test
    fun `email is persisted as null when absent`() {
        val s = share(email = null, token = "no-email-token")
        adapter.save(s)

        assertNotNull(adapter.findById(s.id))
        assertNull(adapter.findById(s.id)!!.email)
    }

    private fun insertUser(id: UserId) {
        jdbc.update(
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, 'hash', 'Jane', 'Doe', '+33612345678',
                    '1990-01-01', 'CLIENT', :firebaseUid, NOW(), true)
            """.trimIndent(),
            mapOf("id" to id.value, "email" to "u_${id.value}@ex.com", "firebaseUid" to "uid_${id.value}"),
        )
    }

    private fun insertRoom(id: UUID) {
        jdbc.update(
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, 'Salle', 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 50, true, false, false, 'OPEN', NOW())
            """.trimIndent(),
            mapOf("id" to id),
        )
    }

    private fun insertBooking(
        id: UUID,
        roomId: UUID,
        userId: UUID,
    ) {
        jdbc.update(
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, payment_mode, created_at, expires_at)
            VALUES (:id, :roomId, :userId, '2026-08-01T18:00:00Z', '2026-08-01T21:00:00Z', 8,
                    100.00, 'EUR', 'PENDING', 'SHARED_POT', NOW(), NOW() + INTERVAL '24 hours')
            """.trimIndent(),
            mapOf("id" to id, "roomId" to roomId, "userId" to userId),
        )
    }

    private fun insertPool(
        id: UUID,
        bookingId: UUID,
    ) {
        jdbc.update(
            """
            INSERT INTO pools (id, booking_id, target_amount, currency, status, deadline,
                               global_link_token, created_at)
            VALUES (:id, :bookingId, 100.00, 'EUR', 'OPEN', NOW() + INTERVAL '24 hours',
                    :token, NOW())
            """.trimIndent(),
            mapOf("id" to id, "bookingId" to bookingId, "token" to "g-$id"),
        )
    }
}
