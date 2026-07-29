package com.kara.kara_general_api.infrastructure.adapter.output.persistence.favorite

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.infrastructure.adapter.output.persistence.room.RoomRepositoryAdapter
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class RoomFavoriteRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: RoomFavoriteRepositoryAdapter

    @Autowired
    private lateinit var roomAdapter: RoomRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    @BeforeEach
    fun clean() {
        jdbc.update("DELETE FROM room_favorites", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
    }

    @Test
    fun `add stores the favorite once and stays idempotent`() {
        val userId = saveUser()
        val roomId = saveRoom("Salle Une").id

        assertTrue(adapter.add(userId, roomId))
        assertFalse(adapter.add(userId, roomId))
        assertEquals(1L, adapter.countByUser(userId))
    }

    @Test
    fun `remove reports whether a favorite was deleted`() {
        val userId = saveUser()
        val roomId = saveRoom("Salle Une").id
        adapter.add(userId, roomId)

        assertTrue(adapter.remove(userId, roomId))
        assertFalse(adapter.remove(userId, roomId))
        assertEquals(0L, adapter.countByUser(userId))
    }

    @Test
    fun `findAllRoomIdsByUser returns the most recent favorite first`() {
        val userId = saveUser()
        val first = saveRoom("Salle Une").id
        val second = saveRoom("Salle Deux").id
        adapter.add(userId, first)
        Thread.sleep(10)
        adapter.add(userId, second)

        assertEquals(listOf(second, first), adapter.findAllRoomIdsByUser(userId))
    }

    @Test
    fun `findRoomIdsByUser paginates the favorites`() {
        val userId = saveUser()
        val first = saveRoom("Salle Une").id
        Thread.sleep(10)
        val second = saveRoom("Salle Deux").id
        adapter.add(userId, first)
        Thread.sleep(10)
        adapter.add(userId, second)

        assertEquals(listOf(second), adapter.findRoomIdsByUser(userId, page = 0, size = 1))
        assertEquals(listOf(first), adapter.findRoomIdsByUser(userId, page = 1, size = 1))
    }

    @Test
    fun `favorites of another user are never returned`() {
        val userId = saveUser()
        val otherUserId = saveUser()
        val roomId = saveRoom("Salle Une").id
        adapter.add(otherUserId, roomId)

        assertEquals(emptyList<RoomId>(), adapter.findAllRoomIdsByUser(userId))
        assertEquals(0L, adapter.countByUser(userId))
    }

    private fun saveUser(): UserId {
        val userId = UserId.generate()
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
                "id" to userId.value,
                "email" to "user_${userId.value}@example.com",
                "firebaseUid" to "uid_${userId.value}",
            ),
        )
        return userId
    }

    private fun saveRoom(name: String): Room =
        roomAdapter.save(
            Room.create(
                name = name,
                description = "Grande salle lumineuse",
                address = Address(street = "1 rue Test", city = "Paris", postalCode = "75001", country = "France"),
                pricePerPersonPerHour = BigDecimal("12.50"),
                currency = Currency.EUR,
                maxCapacity = 50,
                isThereWifi = true,
                isThereSonoPro = false,
                isThereAirConditioning = true,
                coordinates = Coordinates(48.85, 2.30),
            ),
        )
}
