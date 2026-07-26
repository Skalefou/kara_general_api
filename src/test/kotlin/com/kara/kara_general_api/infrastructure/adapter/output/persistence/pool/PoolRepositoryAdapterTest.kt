package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolStatus
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
class PoolRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: PoolRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val bookingId = BookingId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM pool_shares", emptyMap<String, Any>())
        jdbc.update("DELETE FROM pools", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        insertRoom(roomId)
        insertBooking(bookingId, roomId, userId)
    }

    private fun pool(
        id: PoolId = PoolId(UUID.randomUUID()),
        status: PoolStatus = PoolStatus.OPEN,
        deadline: Instant = Instant.now().plusSeconds(3600),
        token: String = "global-${UUID.randomUUID()}",
        booking: BookingId = bookingId,
    ) = Pool(id, booking, BigDecimal("100.00"), Currency.EUR, status, deadline, token, Instant.now())

    @Test
    fun `save then findById returns the pool`() {
        val saved = pool()
        adapter.save(saved)

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(PoolStatus.OPEN, found!!.status)
        assertEquals(BigDecimal("100.00"), found.targetAmount)
    }

    @Test
    fun `findByBookingId and findByGlobalLinkToken locate the pool`() {
        val saved = pool(token = "lookup-token")
        adapter.save(saved)

        assertEquals(saved.id, adapter.findByBookingId(bookingId)!!.id)
        assertEquals(saved.id, adapter.findByGlobalLinkToken("lookup-token")!!.id)
        assertNull(adapter.findByGlobalLinkToken("absent"))
    }

    @Test
    fun `updateStatus and updateGlobalLinkToken mutate the pool`() {
        val saved = pool()
        adapter.save(saved)

        adapter.updateStatus(saved.id, PoolStatus.SETTLED)
        adapter.updateGlobalLinkToken(saved.id, "new-token")

        val found = adapter.findById(saved.id)!!
        assertEquals(PoolStatus.SETTLED, found.status)
        assertEquals("new-token", found.globalLinkToken)
    }

    @Test
    fun `findByUserInvolvement returns pools created by the user and pools where the user holds a share`() {
        // Pool 1: booking owned by userId (the user is the creator).
        val creatorPool = pool()
        adapter.save(creatorPool)

        // Pool 2: booking owned by a stranger, but the user holds an authorized share.
        val stranger = UserId(UUID.randomUUID())
        insertUser(stranger)
        val otherBooking = BookingId(UUID.randomUUID())
        insertBooking(otherBooking, roomId, stranger)
        val shareholderPool = pool(booking = otherBooking)
        adapter.save(shareholderPool)
        insertShare(shareholderPool.id.value, payer = userId.value)

        val pools = adapter.findByUserInvolvement(userId)

        assertEquals(setOf(creatorPool.id, shareholderPool.id), pools.map { it.id }.toSet())
    }

    @Test
    fun `findByUserInvolvement returns empty when the user is unrelated`() {
        adapter.save(pool())

        assertEquals(0, adapter.findByUserInvolvement(UserId(UUID.randomUUID())).size)
    }

    @Test
    fun `findExpiredOpen returns only open pools past their deadline`() {
        val secondBooking = BookingId(UUID.randomUUID())
        insertBooking(secondBooking, roomId, userId)
        adapter.save(pool(status = PoolStatus.OPEN, deadline = Instant.now().minusSeconds(60)))
        adapter.save(
            pool(status = PoolStatus.OPEN, deadline = Instant.now().plusSeconds(3600), booking = secondBooking),
        )

        val expired = adapter.findExpiredOpen(Instant.now())

        assertEquals(1, expired.size)
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

    private fun insertRoom(id: RoomId) {
        jdbc.update(
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, 'Salle', 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 50, true, false, false, 'OPEN', NOW())
            """.trimIndent(),
            mapOf("id" to id.value),
        )
    }

    private fun insertShare(
        poolId: UUID,
        payer: UUID,
    ) {
        jdbc.update(
            """
            INSERT INTO pool_shares (id, pool_id, participant_name, amount, status,
                                     payer_user_id, is_creator_share, created_at)
            VALUES (:id, :poolId, 'P', 50.00, 'AUTHORIZED', :payer, false, NOW())
            """.trimIndent(),
            mapOf("id" to UUID.randomUUID(), "poolId" to poolId, "payer" to payer),
        )
    }

    private fun insertBooking(
        id: BookingId,
        roomId: RoomId,
        userId: UserId,
    ) {
        jdbc.update(
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, payment_mode, created_at, expires_at)
            VALUES (:id, :roomId, :userId, '2026-08-01T18:00:00Z', '2026-08-01T21:00:00Z', 8,
                    100.00, 'EUR', 'PENDING', 'SHARED_POT', NOW(), NOW() + INTERVAL '24 hours')
            """.trimIndent(),
            mapOf("id" to id.value, "roomId" to roomId.value, "userId" to userId.value),
        )
    }
}
