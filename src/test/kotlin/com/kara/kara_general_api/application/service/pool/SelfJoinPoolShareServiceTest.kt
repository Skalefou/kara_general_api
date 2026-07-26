package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareResult
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class SelfJoinPoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val sut = SelfJoinPoolShareService(poolRepository, poolShareRepository, userRepository, paymentGateway)

    private val token = "global-token"
    private val callerId = UserId(UUID.randomUUID())
    private val poolId = PoolId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private fun pool(
        status: PoolStatus = PoolStatus.OPEN,
        deadline: Instant = Instant.now().plusSeconds(3600),
    ) = Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, status, deadline, token, Instant.now())

    private fun creatorShare(
        amount: String = "100.00",
        status: PoolShareStatus = PoolShareStatus.PENDING,
    ) = PoolShare(PoolShareId(UUID.randomUUID()), poolId, "Créateur", null, BigDecimal(amount), status, null, null, null, true)

    private fun user(): User {
        val u = mockk<User>(relaxed = true)
        every { u.id } returns callerId
        every { u.firstName } returns "Jane"
        every { u.lastName } returns "Doe"
        every { u.email } returns Email("jane@example.com")
        every { u.stripeCustomerId } returns null
        return u
    }

    private fun stubStripe() {
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek_secret"
        every { paymentGateway.publishableKey() } returns "pk_test"
        every { paymentGateway.createManualCapturePaymentIntent(any(), any(), "cus_1") } returns
            PaymentIntentResult("cs_1", "pi_1")
    }

    private fun command(amount: String) = SelfJoinPoolShareCommand(globalToken = token, callerId = callerId, amount = BigDecimal(amount))

    @Test
    fun `happy path carves the remainder and authorizes the self-share`() {
        val creator = creatorShare("100.00")
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator)
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns creator
        stubStripe()

        val result = assertInstanceOf<SelfJoinPoolShareResult.Ready>(sut.selfJoin(command("30.00")))

        assertEquals("cs_1", result.clientSecret)
        assertEquals("ek_secret", result.ephemeralKeySecret)
        assertEquals("cus_1", result.customerId)
        // creator remainder carved down to 70
        verify { poolShareRepository.save(match { it.isCreatorShare && it.amount.compareTo(BigDecimal("70.00")) == 0 }) }
        // self-share authorized (PaymentIntent + payer bound), amount 30
        verify {
            poolShareRepository.save(
                match {
                    !it.isCreatorShare &&
                        it.payerUserId == callerId &&
                        it.stripePaymentIntentId == "pi_1" &&
                        it.amount.compareTo(BigDecimal("30.00")) == 0
                },
            )
        }
    }

    @Test
    fun `locks the creator remainder before reading its amount`() {
        val creator = creatorShare("100.00")
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator)
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns creator
        stubStripe()

        sut.selfJoin(command("30.00"))

        verify(exactly = 1) { poolShareRepository.findCreatorShareForUpdate(poolId) }
    }

    @Test
    fun `returns PoolNotFound for an unknown global token`() {
        every { poolRepository.findByGlobalLinkToken(token) } returns null

        assertEquals(SelfJoinPoolShareResult.PoolNotFound, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns PoolClosed when the pool is not open`() {
        every { poolRepository.findByGlobalLinkToken(token) } returns pool(status = PoolStatus.SETTLED)

        assertEquals(SelfJoinPoolShareResult.PoolClosed, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns PoolExpired when the deadline has passed`() {
        every { poolRepository.findByGlobalLinkToken(token) } returns pool(deadline = Instant.now().minusSeconds(1))

        assertEquals(SelfJoinPoolShareResult.PoolExpired, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns PayerNotFound when the caller is unknown`() {
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns null

        assertEquals(SelfJoinPoolShareResult.PayerNotFound, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns AlreadyJoined when the caller already holds a share`() {
        val creator = creatorShare("100.00")
        val existing =
            PoolShare(
                PoolShareId(UUID.randomUUID()),
                poolId,
                "Jane",
                null,
                BigDecimal("20.00"),
                PoolShareStatus.PENDING,
                "pi_x",
                null,
                callerId,
                false,
            )
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator, existing)

        assertEquals(SelfJoinPoolShareResult.AlreadyJoined, sut.selfJoin(command("30.00")))
        verify(exactly = 0) { poolShareRepository.findCreatorShareForUpdate(any()) }
    }

    @Test
    fun `returns RemainderLocked when the creator share is no longer pending`() {
        val creator = creatorShare("100.00", status = PoolShareStatus.AUTHORIZED)
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator)
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns creator

        assertEquals(SelfJoinPoolShareResult.RemainderLocked, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns NoCreatorRemainder when there is no creator share`() {
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns emptyList()
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns null

        assertEquals(SelfJoinPoolShareResult.NoCreatorRemainder, sut.selfJoin(command("30.00")))
    }

    @Test
    fun `returns InvalidAmount when the amount is not positive`() {
        val creator = creatorShare("100.00")
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator)
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns creator

        assertEquals(SelfJoinPoolShareResult.InvalidAmount, sut.selfJoin(command("0.00")))
    }

    @Test
    fun `returns InsufficientRemainder when the amount would exhaust the remainder`() {
        val creator = creatorShare("30.00")
        every { poolRepository.findByGlobalLinkToken(token) } returns pool()
        every { userRepository.findById(callerId) } returns user()
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creator)
        every { poolShareRepository.findCreatorShareForUpdate(poolId) } returns creator

        assertEquals(SelfJoinPoolShareResult.InsufficientRemainder, sut.selfJoin(command("30.00")))
        verify(exactly = 0) { poolShareRepository.save(any()) }
    }
}
