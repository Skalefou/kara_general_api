package com.kara.kara_general_api.infrastructure.adapter.output.persistence.notification

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderKind
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.infrastructure.adapter.output.persistence.user.UserRepositoryAdapter
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class BookingEndReminderRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: BookingEndReminderRepositoryAdapter

    @Autowired
    private lateinit var userRepositoryAdapter: UserRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private val now = Instant.parse("2026-08-01T20:50:00Z")

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM booking_end_reminders", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId, fcmToken = "device-token")
        insertRoom(roomId)
    }

    @Test
    fun `findConfirmedDue returns a confirmed booking ending within the window`() {
        insertBooking(bookingId, status = "CONFIRMED", endAt = now.plusSeconds(300))

        val due = adapter.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))

        assertEquals(1, due.size)
        assertEquals(bookingId, due[0].bookingId)
        assertEquals(userId, due[0].userId)
        assertEquals("device-token", due[0].fcmToken)
        assertEquals("Salle Bleue", due[0].roomName)
    }

    @Test
    fun `findConfirmedDue excludes a booking ending outside the window`() {
        insertBooking(bookingId, status = "CONFIRMED", endAt = now.plusSeconds(20 * 60))

        val due = adapter.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))

        assertTrue(due.isEmpty())
    }

    @Test
    fun `findConfirmedDue excludes a non-confirmed booking`() {
        insertBooking(bookingId, status = "PENDING", endAt = now.plusSeconds(300))

        val due = adapter.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))

        assertTrue(due.isEmpty())
    }

    @Test
    fun `findConfirmedDue excludes a booking already marked sent for the kind`() {
        // Fin dans 90 s : dans la fenêtre des deux types de rappel.
        insertBooking(bookingId, status = "CONFIRMED", endAt = now.plusSeconds(90))
        adapter.markSent(bookingId, BookingEndReminderKind.TEN_MINUTES)

        val ten = adapter.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        val two = adapter.findConfirmedDue(BookingEndReminderKind.TWO_MINUTES, now, now.plus(BookingEndReminderKind.TWO_MINUTES.lead))

        assertTrue(ten.isEmpty())
        // Un rappel TWO_MINUTES reste dû : il vise une autre fenêtre et n'a pas été marqué.
        assertEquals(1, two.size)
    }

    @Test
    fun `markSent is idempotent thanks to the on conflict clause`() {
        insertBooking(bookingId, status = "CONFIRMED", endAt = now.plusSeconds(300))

        adapter.markSent(bookingId, BookingEndReminderKind.TEN_MINUTES)
        adapter.markSent(bookingId, BookingEndReminderKind.TEN_MINUTES)

        val count =
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_end_reminders WHERE booking_id = :id AND kind = :kind",
                mapOf("id" to bookingId.value, "kind" to "TEN_MINUTES"),
                Int::class.java,
            )
        assertEquals(1, count)
    }

    @Test
    fun `updateFcmToken persists the new device token`() {
        userRepositoryAdapter.updateFcmToken(userId, "fresh-token")

        val stored =
            jdbc.queryForObject(
                "SELECT fcm_token FROM users WHERE id = :id",
                mapOf("id" to userId.value),
                String::class.java,
            )
        assertEquals("fresh-token", stored)
    }

    private fun insertUser(
        id: UserId,
        fcmToken: String?,
    ) {
        val sql =
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified, fcm_token)
            VALUES (:id, :email, 'hash', 'Jane', 'Doe', '+33612345678',
                    '1990-01-01', 'CLIENT', :firebaseUid, NOW(), true, :fcmToken)
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "email" to "user_${id.value}@example.com",
                "firebaseUid" to "uid_${id.value}",
                "fcmToken" to fcmToken,
            ),
        )
    }

    private fun insertRoom(id: RoomId) {
        val sql =
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country,
                               price_per_person_per_hour, currency, max_capacity,
                               is_there_wifi, is_there_sono_pro, is_there_air_conditioning, status, created_at)
            VALUES (:id, 'Salle Bleue', 'desc', 'rue', 'Paris', '75002', 'France',
                    10.00, 'EUR', 50, true, false, false, 'OPEN', NOW())
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value))
    }

    private fun insertBooking(
        id: BookingId,
        status: String,
        endAt: Instant,
    ) {
        val sql =
            """
            INSERT INTO bookings (id, room_id, user_id, start_at, end_at, number_of_people,
                                  total_price, currency, status, payment_mode, created_at, expires_at)
            VALUES (:id, :roomId, :userId, :startAt, :endAt, 8,
                    435.00, 'EUR', :status, 'PAY_ALL', :createdAt, :expiresAt)
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf(
                "id" to id.value,
                "roomId" to roomId.value,
                "userId" to userId.value,
                "startAt" to Timestamp.from(endAt.minusSeconds(3 * 3600)),
                "endAt" to Timestamp.from(endAt),
                "status" to status,
                "createdAt" to Timestamp.from(Instant.parse("2026-07-20T10:00:00Z")),
                "expiresAt" to Timestamp.from(Instant.parse("2026-07-20T10:15:00Z")),
            ),
        )
    }
}
