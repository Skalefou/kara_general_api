package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckIn
import com.kara.kara_general_api.domain.model.booking.BookingId
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class BookingAccessCheckInRepositoryAdapterTest {

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: BookingAccessCheckInRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())
    private val clientId = UserId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())
    private val otherServerId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM booking_access_check_ins", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(clientId, "CLIENT")
        insertUser(serverId, "SERVER")
        insertUser(otherServerId, "SERVER")
        insertRoom(roomId)
        insertBooking(bookingId)
    }

    @Test
    fun `findByBookingId returns null when the ticket was never presented`() {
        assertNull(adapter.findByBookingId(bookingId))
    }

    @Test
    fun `save then findByBookingId returns the recorded check-in`() {
        val checkedInAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        adapter.recordIfAbsent(BookingAccessCheckIn.record(bookingId, serverId, checkedInAt))

        val found = adapter.findByBookingId(bookingId)

        assertNotNull(found)
        assertEquals(bookingId, found!!.bookingId)
        assertEquals(serverId, found.serverId)
        assertEquals(checkedInAt, found.checkedInAt)
    }

    @Test
    fun `recordIfAbsent returns the first check-in instead of writing a second one`() {
        val first = BookingAccessCheckIn.record(bookingId, serverId, Instant.now().truncatedTo(ChronoUnit.MILLIS))
        adapter.recordIfAbsent(first)

        val second = BookingAccessCheckIn.record(bookingId, otherServerId, Instant.now())
        val effective = adapter.recordIfAbsent(second)

        assertEquals(first.id, effective.id)
        assertEquals(serverId, effective.serverId)
        assertEquals(first.checkedInAt, effective.checkedInAt)
        assertEquals(1, countCheckIns())
    }

    @Test
    fun `check-ins of other bookings are not returned`() {
        val otherBookingId = BookingId(UUID.randomUUID())
        insertBooking(otherBookingId)
        adapter.recordIfAbsent(BookingAccessCheckIn.record(otherBookingId, serverId, Instant.now()))

        assertNull(adapter.findByBookingId(bookingId))
        assertNotNull(adapter.findByBookingId(otherBookingId))
    }

    private fun countCheckIns(): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM booking_access_check_ins WHERE booking_id = :bookingId",
            mapOf("bookingId" to bookingId.value),
            Int::class.java,
        ) ?: 0

    private fun insertUser(id: UserId, role: String) {
        val sql =
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, 'hash', 'Jane', 'Doe', '+33612345678',
                    '1990-01-01', :role, :firebaseUid, NOW(), true)
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "email" to "user_${id.value}@example.com",
                "role" to role,
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

    private fun insertBooking(id: BookingId) {
        val sql =
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, created_at, expires_at, payment_mode)
            VALUES (:id, :roomId, :userId, :startAt, :endAt, 8,
                    435.00, 'EUR', 'CONFIRMED', NOW(), :expiresAt, 'PAY_ALL')
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "roomId" to roomId.value,
                "userId" to clientId.value,
                "startAt" to java.sql.Timestamp.from(Instant.parse("2026-08-01T18:00:00Z")),
                "endAt" to java.sql.Timestamp.from(Instant.parse("2026-08-01T21:00:00Z")),
                "expiresAt" to java.sql.Timestamp.from(Instant.parse("2026-08-01T17:00:00Z")),
            ),
        )
    }
}
