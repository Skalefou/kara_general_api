package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
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
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = Instant.parse("2026-08-01T21:00:00Z")

    @BeforeEach
    fun setUp() {
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
