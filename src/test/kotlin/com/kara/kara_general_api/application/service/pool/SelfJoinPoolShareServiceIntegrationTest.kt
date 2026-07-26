package com.kara.kara_general_api.application.service.pool

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareResult
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class SelfJoinPoolShareServiceIntegrationTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var service: SelfJoinPoolShareService

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val creatorId = UUID.randomUUID()
    private val poolId = UUID.randomUUID()
    private val globalToken = "g-token-selfjoin"
    private val intentCounter = AtomicInteger()

    @BeforeEach
    fun setUp() {
        jdbc.update("DELETE FROM pool_shares", emptyMap<String, Any>())
        jdbc.update("DELETE FROM pools", emptyMap<String, Any>())
        jdbc.update("DELETE FROM booking_options", emptyMap<String, Any>())
        jdbc.update("DELETE FROM bookings", emptyMap<String, Any>())
        jdbc.update("DELETE FROM rooms", emptyMap<String, Any>())
        jdbc.update("DELETE FROM users", emptyMap<String, Any>())

        insertUser(creatorId, "Owner", "One")
        val roomId = UUID.randomUUID()
        insertRoom(roomId)
        val bookingId = UUID.randomUUID()
        insertBooking(bookingId, roomId, creatorId)
        insertPool(poolId, bookingId, globalToken)
        insertCreatorShare(poolId, "100.00")

        intentCounter.set(0)
        every { paymentGateway.ensureCustomer(any()) } answers { "cus_" + firstArg<User>().id.value }
        every { paymentGateway.createEphemeralKey(any()) } returns "ek_secret"
        every { paymentGateway.publishableKey() } returns "pk_test"
        every { paymentGateway.createManualCapturePaymentIntent(any(), any(), any()) } answers {
            val n = intentCounter.incrementAndGet()
            PaymentIntentResult("cs_$n", "pi_$n")
        }
    }

    @Test
    fun `real self-join carves the creator remainder and creates an authorized self-share`() {
        val joiner = UUID.randomUUID()
        insertUser(joiner, "Jane", "Doe")

        val result = service.selfJoin(SelfJoinPoolShareCommand(globalToken, UserId(joiner), BigDecimal("30.00")))

        assertTrue(result is SelfJoinPoolShareResult.Ready)
        assertEquals(BigDecimal("70.00"), creatorRemainderAmount())
        val self = shareOfPayer(joiner)!!
        assertEquals(0, (self["amount"] as BigDecimal).compareTo(BigDecimal("30.00")))
        assertEquals("PENDING", self["status"])
        assertTrue((self["stripe_payment_intent_id"] as String).startsWith("pi_"))
        assertEquals("Jane Doe", self["participant_name"])
        assertEquals(BigDecimal("100.00"), totalShareAmount())
    }

    @Test
    fun `enforces one share per person`() {
        val joiner = UUID.randomUUID()
        insertUser(joiner, "Jane", "Doe")

        val first = service.selfJoin(SelfJoinPoolShareCommand(globalToken, UserId(joiner), BigDecimal("20.00")))
        val second = service.selfJoin(SelfJoinPoolShareCommand(globalToken, UserId(joiner), BigDecimal("20.00")))

        assertTrue(first is SelfJoinPoolShareResult.Ready)
        assertEquals(SelfJoinPoolShareResult.AlreadyJoined, second)
    }

    @Test
    fun `concurrent self-joins exceeding the remainder let exactly one succeed`() {
        val userA = UUID.randomUUID()
        val userB = UUID.randomUUID()
        insertUser(userA, "Alice", "A")
        insertUser(userB, "Bob", "B")

        val executor = Executors.newFixedThreadPool(2)
        val startGate = CountDownLatch(1)
        // Together 60 + 60 = 120 > remainder 100 : the FOR UPDATE lock must let only one through.
        val tasks =
            listOf(userA, userB).map { u ->
                Callable {
                    startGate.await()
                    service.selfJoin(SelfJoinPoolShareCommand(globalToken, UserId(u), BigDecimal("60.00")))
                }
            }
        val futures = tasks.map { executor.submit(it) }
        startGate.countDown()
        val results = futures.map { it.get() }
        executor.shutdown()

        val successes = results.count { it is SelfJoinPoolShareResult.Ready }
        val insufficient = results.count { it == SelfJoinPoolShareResult.InsufficientRemainder }
        assertEquals(1, successes, "exactly one self-join must succeed")
        assertEquals(1, insufficient, "the other must be rejected as insufficient remainder")
        // Invariant preserved: the shares still sum to the pool target.
        assertEquals(BigDecimal("100.00"), totalShareAmount())
    }

    private fun creatorRemainderAmount(): BigDecimal =
        jdbc.queryForObject(
            "SELECT amount FROM pool_shares WHERE pool_id = :poolId AND is_creator_share = TRUE",
            mapOf("poolId" to poolId),
            BigDecimal::class.java,
        )!!

    private fun totalShareAmount(): BigDecimal =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM pool_shares WHERE pool_id = :poolId",
            mapOf("poolId" to poolId),
            BigDecimal::class.java,
        )!!

    private fun shareOfPayer(payerId: UUID): Map<String, Any?>? =
        jdbc
            .queryForList(
                "SELECT amount, status, stripe_payment_intent_id, participant_name FROM pool_shares " +
                    "WHERE pool_id = :poolId AND payer_user_id = :payerId",
                mapOf("poolId" to poolId, "payerId" to payerId),
            ).firstOrNull()

    private fun insertUser(
        id: UUID,
        firstName: String,
        lastName: String,
    ) {
        jdbc.update(
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                               birth_date, role, firebase_uid, created_at, email_verified)
            VALUES (:id, :email, 'hash', :firstName, :lastName, '+33612345678',
                    '1990-01-01', 'CLIENT', :firebaseUid, NOW(), true)
            """.trimIndent(),
            mapOf(
                "id" to id,
                "email" to "u_$id@ex.com",
                "firstName" to firstName,
                "lastName" to lastName,
                "firebaseUid" to "uid_$id",
            ),
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
        token: String,
    ) {
        jdbc.update(
            """
            INSERT INTO pools (id, booking_id, target_amount, currency, status, deadline,
                               global_link_token, created_at)
            VALUES (:id, :bookingId, 100.00, 'EUR', 'OPEN', NOW() + INTERVAL '24 hours', :token, NOW())
            """.trimIndent(),
            mapOf("id" to id, "bookingId" to bookingId, "token" to token),
        )
    }

    private fun insertCreatorShare(
        poolId: UUID,
        amount: String,
    ) {
        jdbc.update(
            """
            INSERT INTO pool_shares (id, pool_id, participant_name, email, amount, status,
                                     stripe_payment_intent_id, unique_link_token, payer_user_id, is_creator_share, created_at)
            VALUES (:id, :poolId, 'Créateur', NULL, :amount, 'PENDING', NULL, NULL, NULL, TRUE, NOW())
            """.trimIndent(),
            mapOf("id" to UUID.randomUUID(), "poolId" to poolId, "amount" to BigDecimal(amount)),
        )
    }
}
