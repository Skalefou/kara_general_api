package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
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
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class UserRecoveryCodeRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: UserRecoveryCodeRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val userId = UserId(UUID.randomUUID())
    private val otherUserId = UserId(UUID.randomUUID())
    private val hashes = (1..10).map { "hash-$it" }

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM user_recovery_codes", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())
        insertUser(userId)
        insertUser(otherUserId)
    }

    @Test
    fun `findUnusedByUserId returns an empty list when nothing was ever generated`() {
        assertTrue(adapter.findUnusedByUserId(userId).isEmpty())
    }

    @Test
    fun `replaceAll then findUnusedByUserId returns every stored hash`() {
        adapter.replaceAll(userId, hashes)

        val found = adapter.findUnusedByUserId(userId)

        assertEquals(10, found.size)
        assertEquals(hashes.toSet(), found.map { it.codeHash }.toSet())
        assertTrue(found.all { it.usedAt == null })
        assertTrue(found.all { it.userId == userId })
    }

    @Test
    fun `replaceAll drops the previous series instead of appending to it`() {
        adapter.replaceAll(userId, hashes)

        adapter.replaceAll(userId, listOf("brand-new-hash"))

        val found = adapter.findUnusedByUserId(userId)
        assertEquals(1, found.size)
        assertEquals("brand-new-hash", found.first().codeHash)
    }

    @Test
    fun `replaceAll with an empty list clears the series`() {
        adapter.replaceAll(userId, hashes)

        adapter.replaceAll(userId, emptyList())

        assertTrue(adapter.findUnusedByUserId(userId).isEmpty())
    }

    @Test
    fun `markUsed removes the code from the unused series`() {
        adapter.replaceAll(userId, hashes)
        val target = adapter.findUnusedByUserId(userId).first()

        adapter.markUsed(target.id)

        val remaining = adapter.findUnusedByUserId(userId)
        assertEquals(9, remaining.size)
        assertNull(remaining.firstOrNull { it.id == target.id })
    }

    @Test
    fun `markUsed keeps the original timestamp when replayed on an already used code`() {
        adapter.replaceAll(userId, hashes)
        val target = adapter.findUnusedByUserId(userId).first()
        adapter.markUsed(target.id)
        val firstUsedAt = usedAtOf(target.id.value)

        adapter.markUsed(target.id)

        assertNotNull(firstUsedAt)
        assertEquals(firstUsedAt, usedAtOf(target.id.value))
    }

    @Test
    fun `countUnused counts only the codes still available for the account`() {
        adapter.replaceAll(userId, hashes)
        adapter.replaceAll(otherUserId, listOf("other-hash"))
        adapter.markUsed(adapter.findUnusedByUserId(userId).first().id)

        assertEquals(9, adapter.countUnused(userId))
        assertEquals(1, adapter.countUnused(otherUserId))
    }

    @Test
    fun `deleteByUserId wipes the series of the account and leaves the others untouched`() {
        adapter.replaceAll(userId, hashes)
        adapter.replaceAll(otherUserId, listOf("other-hash"))

        adapter.deleteByUserId(userId)

        assertEquals(0, adapter.countUnused(userId))
        assertEquals(1, adapter.countUnused(otherUserId))
    }

    private fun usedAtOf(id: UUID): Any? =
        jdbc.queryForObject(
            "SELECT used_at FROM user_recovery_codes WHERE id = :id",
            mapOf("id" to id),
            java.sql.Timestamp::class.java,
        )

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
