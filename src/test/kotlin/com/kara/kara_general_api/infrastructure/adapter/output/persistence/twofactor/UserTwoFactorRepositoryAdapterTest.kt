package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
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
class UserTwoFactorRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: UserTwoFactorRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val userId = UserId(UUID.randomUUID())
    private val otherUserId = UserId(UUID.randomUUID())
    private val createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM user_two_factor", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        insertUser(otherUserId)
    }

    @Test
    fun `findByUserId returns null when the account never enrolled`() {
        assertNull(adapter.findByUserId(userId))
    }

    @Test
    fun `save then findByUserId returns the pending secret`() {
        adapter.save(TwoFactorSecret.pending(userId, "encrypted-secret", createdAt))

        val found = adapter.findByUserId(userId)

        assertNotNull(found)
        assertEquals("encrypted-secret", found!!.secretCipher)
        assertEquals(TwoFactorStatus.PENDING, found.status)
        assertEquals(createdAt, found.createdAt)
        assertNull(found.activatedAt)
        assertNull(found.lastUsedStep)
    }

    @Test
    fun `save then findByUserId returns the activation timestamp and the consumed step`() {
        val activatedAt = createdAt.plusSeconds(30)
        adapter.save(
            TwoFactorSecret.pending(userId, "encrypted-secret", createdAt).activate(activatedAt, 1_000L),
        )

        val found = adapter.findByUserId(userId)

        assertNotNull(found)
        assertEquals(TwoFactorStatus.ACTIVE, found!!.status)
        assertEquals(activatedAt, found.activatedAt)
        assertEquals(1_000L, found.lastUsedStep)
    }

    @Test
    fun `save overwrites a previous pending secret instead of inserting a second row`() {
        adapter.save(TwoFactorSecret.pending(userId, "first-secret", createdAt))
        adapter.save(TwoFactorSecret.pending(userId, "second-secret", createdAt))

        assertEquals("second-secret", adapter.findByUserId(userId)?.secretCipher)
        assertEquals(1, countRows(userId))
    }

    @Test
    fun `updateLastUsedStep consumes the time step of the stored secret`() {
        adapter.save(TwoFactorSecret.pending(userId, "encrypted-secret", createdAt).activate(createdAt, 900L))

        adapter.updateLastUsedStep(userId, 1_001L)

        assertEquals(1_001L, adapter.findByUserId(userId)?.lastUsedStep)
    }

    @Test
    fun `deleteByUserId drops the secret of the account and leaves the others untouched`() {
        adapter.save(TwoFactorSecret.pending(userId, "mine", createdAt))
        adapter.save(TwoFactorSecret.pending(otherUserId, "theirs", createdAt))

        adapter.deleteByUserId(userId)

        assertNull(adapter.findByUserId(userId))
        assertNotNull(adapter.findByUserId(otherUserId))
    }

    private fun countRows(id: UserId): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM user_two_factor WHERE user_id = :userId",
            mapOf("userId" to id.value),
            Int::class.java,
        ) ?: 0

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
}
