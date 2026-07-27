package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool.PoolRepositoryAdapter
import com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool.PoolShareRepositoryAdapter
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
class BookingRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: BookingRepositoryAdapter

    @Autowired
    private lateinit var poolAdapter: PoolRepositoryAdapter

    @Autowired
    private lateinit var poolShareAdapter: PoolShareRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = Instant.parse("2026-08-01T21:00:00Z")

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM pool_shares", emptyMap<String, Any>())
        jdbc.update("DELETE FROM pools", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM payments", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM room_services", emptyMap<String, Any>())
        jdbc.update("DELETE FROM services", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        insertRoom(roomId)
    }

    private fun booking(
        id: BookingId = BookingId(UUID.randomUUID()),
        startAt: Instant = start,
        endAt: Instant = end,
        status: BookingStatus = BookingStatus.PENDING,
        optionIds: List<RoomOptionId> = emptyList(),
        expiresAt: Instant = Instant.now().plusSeconds(900),
    ) = Booking(
        id = id,
        roomId = roomId,
        userId = userId,
        startAt = startAt,
        endAt = endAt,
        numberOfPeople = 8,
        selectedOptionIds = optionIds,
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = expiresAt,
    )

    @Test
    fun `save then findById returns the booking with its selected options`() {
        val optionId = insertService("Ménage")
        val saved = booking(optionIds = listOf(optionId))

        adapter.save(saved)
        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(BigDecimal("435.00"), found!!.totalPrice)
        assertEquals(BookingStatus.PENDING, found.status)
        assertEquals(listOf(optionId), found.selectedOptionIds)
    }

    @Test
    fun `existsOverlapping is true when an active booking overlaps the slot`() {
        adapter.save(booking(status = BookingStatus.CONFIRMED))

        // Chevauche partiellement [19:00, 20:00) ⊂ [18:00, 21:00)
        val overlaps =
            adapter.existsOverlapping(
                roomId,
                Instant.parse("2026-08-01T19:00:00Z"),
                Instant.parse("2026-08-01T20:00:00Z"),
            )

        assertTrue(overlaps)
    }

    @Test
    fun `existsOverlapping is false for an adjacent slot`() {
        adapter.save(booking())

        // Créneau adjacent [21:00, 22:00) : start_at < endAt requiert 18:00 < 22:00 vrai,
        // mais end_at > startAt requiert 21:00 > 21:00 faux → pas de chevauchement.
        val overlaps =
            adapter.existsOverlapping(
                roomId,
                Instant.parse("2026-08-01T21:00:00Z"),
                Instant.parse("2026-08-01T22:00:00Z"),
            )

        assertFalse(overlaps)
    }

    @Test
    fun `existsOverlapping ignores cancelled bookings`() {
        adapter.save(booking(status = BookingStatus.CANCELLED))

        val overlaps = adapter.existsOverlapping(roomId, start, end)

        assertFalse(overlaps)
    }

    @Test
    fun `existsOverlapping ignores an expired pending booking`() {
        adapter.save(booking(status = BookingStatus.PENDING, expiresAt = Instant.now().minusSeconds(60)))

        val overlaps = adapter.existsOverlapping(roomId, start, end)

        assertFalse(overlaps)
    }

    @Test
    fun `cancelExpiredPending cancels only expired pending bookings`() {
        val expired =
            booking(
                startAt = Instant.parse("2026-08-02T18:00:00Z"),
                endAt = Instant.parse("2026-08-02T21:00:00Z"),
                status = BookingStatus.PENDING,
                expiresAt = Instant.now().minusSeconds(60),
            )
        val stillPending =
            booking(
                startAt = Instant.parse("2026-08-03T18:00:00Z"),
                endAt = Instant.parse("2026-08-03T21:00:00Z"),
                status = BookingStatus.PENDING,
                expiresAt = Instant.now().plusSeconds(900),
            )
        val confirmed =
            booking(
                startAt = Instant.parse("2026-08-04T18:00:00Z"),
                endAt = Instant.parse("2026-08-04T21:00:00Z"),
                status = BookingStatus.CONFIRMED,
                expiresAt = Instant.now().minusSeconds(60),
            )
        adapter.save(expired)
        adapter.save(stillPending)
        adapter.save(confirmed)

        val cancelledCount = adapter.cancelExpiredPending(Instant.now())

        assertEquals(1, cancelledCount)
        assertEquals(BookingStatus.CANCELLED, adapter.findById(expired.id)!!.status)
        assertEquals(BookingStatus.PENDING, adapter.findById(stillPending.id)!!.status)
        assertEquals(BookingStatus.CONFIRMED, adapter.findById(confirmed.id)!!.status)
    }

    @Test
    fun `findByUserId returns only the bookings of the requested user`() {
        val otherUserId = UserId(UUID.randomUUID())
        insertUser(otherUserId)
        val mine = booking()
        val theirs =
            booking(
                startAt = Instant.parse("2026-09-01T18:00:00Z"),
                endAt = Instant.parse("2026-09-01T21:00:00Z"),
            ).copy(userId = otherUserId)
        adapter.save(mine)
        adapter.save(theirs)

        val found = adapter.findByUserId(userId)

        assertEquals(listOf(mine.id), found.map { it.booking.id })
        assertEquals(listOf(theirs.id), adapter.findByUserId(otherUserId).map { it.booking.id })
    }

    @Test
    fun `findByUserId assembles the room the two options and the pool shares of a booking`() {
        val cleaning = insertService("Ménage")
        val security = insertService("Sécurité")
        val saved = booking(status = BookingStatus.CONFIRMED, optionIds = listOf(cleaning, security))
        adapter.save(saved)
        val poolId = insertPool(saved.id)
        insertPoolShare(poolId, "Jeanne Martin", "jeanne@example.com", "217.50", "AUTHORIZED")
        insertPoolShare(poolId, "Karim Belkacem", null, "217.50", "PENDING")

        val record = adapter.findByUserId(userId).single()
        val shares = poolShareAdapter.findByPoolIds(listOf(poolId))

        assertEquals("Salle", record.roomName)
        assertEquals("rue", record.roomAddress?.street)
        assertEquals("Paris", record.roomAddress?.city)
        assertEquals("75002", record.roomAddress?.postalCode)
        assertEquals(setOf("Ménage", "Sécurité"), record.options.map { it.label }.toSet())
        assertEquals(setOf(cleaning, security), record.options.map { it.optionId }.toSet())
        assertEquals(BigDecimal("25.00"), record.options.first().price)
        assertEquals(setOf(cleaning, security), record.booking.selectedOptionIds.toSet())
        assertEquals(listOf(poolId), poolAdapter.findByBookingIds(listOf(saved.id)).map { it.id })
        assertEquals(setOf("Jeanne Martin", "Karim Belkacem"), shares.map { it.participantName }.toSet())
    }

    @Test
    fun `findByUserId returns every status ordered by start date descending`() {
        val past =
            booking(
                startAt = Instant.parse("2026-06-01T18:00:00Z"),
                endAt = Instant.parse("2026-06-01T21:00:00Z"),
                status = BookingStatus.CANCELLED,
            )
        val future =
            booking(
                startAt = Instant.parse("2026-09-01T18:00:00Z"),
                endAt = Instant.parse("2026-09-01T21:00:00Z"),
                status = BookingStatus.CONFIRMED,
            )
        adapter.save(past)
        adapter.save(booking())
        adapter.save(future)

        val found = adapter.findByUserId(userId)

        assertEquals(3, found.size)
        assertEquals(listOf(future.id, past.id), listOf(found.first().booking.id, found.last().booking.id))
    }

    @Test
    fun `findByUserId returns empty when the user has no booking`() {
        assertTrue(adapter.findByUserId(UserId(UUID.randomUUID())).isEmpty())
    }

    @Test
    fun `updateStatus changes the booking status`() {
        val saved = booking()
        adapter.save(saved)

        adapter.updateStatus(saved.id, BookingStatus.CONFIRMED)

        assertEquals(BookingStatus.CONFIRMED, adapter.findById(saved.id)!!.status)
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

    private fun insertPool(bookingId: BookingId): PoolId {
        val id = UUID.randomUUID()
        val sql =
            """
            INSERT INTO pools (id, booking_id, extension_id, target_amount, currency, status, deadline,
                               global_link_token, created_at)
            VALUES (:id, :bookingId, NULL, 435.00, 'EUR', 'OPEN', NOW() + INTERVAL '1 day',
                    :token, NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id, "bookingId" to bookingId.value, "token" to "tok_$id"))
        return PoolId(id)
    }

    private fun insertPoolShare(
        poolId: PoolId,
        name: String,
        email: String?,
        amount: String,
        status: String,
    ) {
        val sql =
            """
            INSERT INTO pool_shares (id, pool_id, participant_name, email, amount, status,
                                     is_creator_share, created_at)
            VALUES (:id, :poolId, :name, :email, :amount, :status, false, NOW())
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to UUID.randomUUID(),
                "poolId" to poolId.value,
                "name" to name,
                "email" to email,
                "amount" to BigDecimal(amount),
                "status" to status,
            ),
        )
    }

    private fun insertService(label: String): RoomOptionId {
        val id = UUID.randomUUID()
        val sql =
            """
            INSERT INTO services (id, label, description, price, currency, created_at)
            VALUES (:id, :label, 'desc', 25.00, 'EUR', NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id, "label" to label))
        return RoomOptionId(id)
    }
}
