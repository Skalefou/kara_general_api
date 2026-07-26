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
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthorizePoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>()
    private val sut = AuthorizePoolShareService(poolRepository, poolShareRepository, userRepository, paymentGateway)

    private val poolId = PoolId(UUID.randomUUID())
    private val shareId = PoolShareId(UUID.randomUUID())
    private val payerId = UserId(UUID.randomUUID())

    private fun pool(
        status: PoolStatus = PoolStatus.OPEN,
        deadline: Instant = Instant.now().plusSeconds(3600),
    ) = Pool(poolId, BookingId(UUID.randomUUID()), BigDecimal("100.00"), Currency.EUR, status, deadline, "g", Instant.now())

    private fun share(status: PoolShareStatus = PoolShareStatus.PENDING) =
        PoolShare(shareId, poolId, "Alice", Email("a@b.com"), BigDecimal("50.00"), status, null, "tok", null, false)

    private fun payer() =
        User(
            id = payerId,
            email = Email("a@b.com"),
            hashedPassword = HashedPassword("h"),
            firstName = "Al",
            lastName = "Ice",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 1),
            role = UserRole.CLIENT,
            firebaseUid = "uid",
            createdAt = Instant.now(),
            stripeCustomerId = "cus_1",
        )

    private fun command() = AuthorizePoolShareCommand(poolId, shareId, payerId)

    @Test
    fun `returns PoolNotFound when the pool is missing`() {
        every { poolRepository.findById(poolId) } returns null
        assertEquals(AuthorizePoolShareResult.PoolNotFound, sut.authorize(command()))
    }

    @Test
    fun `returns PoolClosed when the pool is not open`() {
        every { poolRepository.findById(poolId) } returns pool(status = PoolStatus.SETTLED)
        assertEquals(AuthorizePoolShareResult.PoolClosed, sut.authorize(command()))
    }

    @Test
    fun `returns PoolExpired when the deadline has passed`() {
        every { poolRepository.findById(poolId) } returns pool(deadline = Instant.now().minusSeconds(1))
        assertEquals(AuthorizePoolShareResult.PoolExpired, sut.authorize(command()))
    }

    @Test
    fun `returns ShareNotFound when the share does not belong to the pool`() {
        every { poolRepository.findById(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns null
        assertEquals(AuthorizePoolShareResult.ShareNotFound, sut.authorize(command()))
    }

    @Test
    fun `returns ShareAlreadyProcessed when the share is not pending`() {
        every { poolRepository.findById(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns share(status = PoolShareStatus.AUTHORIZED)
        assertEquals(AuthorizePoolShareResult.ShareAlreadyProcessed, sut.authorize(command()))
    }

    @Test
    fun `creates a manual-capture intent and links it to the share`() {
        every { poolRepository.findById(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns share()
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") } returns
            PaymentIntentResult(clientSecret = "cs", paymentIntentId = "pi_1")
        every { paymentGateway.publishableKey() } returns "pk"

        val result = sut.authorize(command())

        val ready = assertIs<AuthorizePoolShareResult.Ready>(result)
        assertEquals("cs", ready.clientSecret)
        assertEquals(shareId.value, ready.shareId)
        verify {
            poolShareRepository.save(
                match { it.stripePaymentIntentId == "pi_1" && it.payerUserId == payerId && it.status == PoolShareStatus.PENDING },
            )
        }
        // A manual-capture intent must be used (never the auto-capture pay-all variant).
        verify(exactly = 0) { paymentGateway.createPaymentIntent(any(), any(), any()) }
    }
}
