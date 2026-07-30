package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
import com.kara.kara_general_api.domain.port.input.pool.SyncPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SyncPoolShareResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SyncPoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val poolSettlementService = mockk<PoolSettlementService>(relaxed = true)
    private val poolRecapAssembler = mockk<PoolRecapAssembler>(relaxed = true)
    private val sut =
        SyncPoolShareService(
            poolRepository,
            poolShareRepository,
            bookingRepository,
            paymentGateway,
            poolSettlementService,
            poolRecapAssembler,
        )

    private val poolId = PoolId(UUID.randomUUID())
    private val shareId = PoolShareId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())
    private val payerId = UserId(UUID.randomUUID())
    private val creatorId = UserId(UUID.randomUUID())
    private val strangerId = UserId(UUID.randomUUID())

    private val pool =
        Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, PoolStatus.OPEN, Instant.now().plusSeconds(3600), "g", Instant.now())

    private fun share(
        status: PoolShareStatus = PoolShareStatus.PENDING,
        intent: String? = "pi_1",
        payer: UserId? = payerId,
    ) = PoolShare(shareId, poolId, "Thomas", null, BigDecimal("50.00"), status, intent, null, payer, false)

    private fun command(requesterId: UserId = payerId) = SyncPoolShareCommand(poolId = poolId, shareId = shareId, requesterId = requesterId)

    private fun recapView(shareStatus: PoolShareStatus) =
        PoolRecapView(
            poolId = poolId.value,
            status = PoolStatus.OPEN,
            roomName = "Salle",
            startAt = Instant.now(),
            endAt = Instant.now().plusSeconds(3600),
            numberOfPeople = 4,
            targetAmount = BigDecimal("100.00"),
            collectedAmount = BigDecimal("50.00"),
            currency = Currency.EUR,
            percentage = 50,
            deadline = Instant.now().plusSeconds(3600),
            shareId = shareId.value,
            shareParticipantName = "Thomas",
            shareAmount = BigDecimal("50.00"),
            shareStatus = shareStatus,
        )

    private fun givenPoolAndShare(
        status: PoolShareStatus = PoolShareStatus.PENDING,
        intent: String? = "pi_1",
        payer: UserId? = payerId,
    ) {
        every { poolRepository.findById(poolId) } returns pool
        every { poolShareRepository.findById(shareId) } returns share(status, intent, payer)
        every { poolRecapAssembler.assemble(any(), any()) } returns recapView(status)
    }

    @Test
    fun `authorizes the share when the gateway reports the funds are held`() {
        givenPoolAndShare()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.REQUIRES_CAPTURE, "cs")

        val result = sut.sync(command())

        assertIs<SyncPoolShareResult.Synced>(result)
        // La transition n'est pas réimplémentée ici : elle est déléguée au chemin d'écriture unique.
        verify(exactly = 1) { poolSettlementService.onShareAuthorized("pi_1") }
    }

    @Test
    fun `leaves the share untouched while the payment is still pending`() {
        givenPoolAndShare()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.REQUIRES_PAYMENT_METHOD, "cs")

        val result = sut.sync(command())

        assertIs<SyncPoolShareResult.Synced>(result)
        verify(exactly = 0) { poolSettlementService.onShareAuthorized(any()) }
        verify(exactly = 0) { poolSettlementService.onShareCanceled(any()) }
        verify(exactly = 0) { poolShareRepository.save(any()) }
    }

    @Test
    fun `cancels the share when the gateway reports the intent was canceled`() {
        givenPoolAndShare()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.CANCELED, null)

        sut.sync(command())

        verify(exactly = 1) { poolSettlementService.onShareCanceled("pi_1") }
        verify(exactly = 0) { poolSettlementService.onShareAuthorized(any()) }
    }

    @Test
    fun `is idempotent for an already authorized share and returns its current state`() {
        givenPoolAndShare(status = PoolShareStatus.AUTHORIZED)
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.REQUIRES_CAPTURE, "cs")

        val result = sut.sync(command())

        // La délégation a bien lieu, mais PoolSettlementService ressort sans effet de bord (part non PENDING).
        assertIs<SyncPoolShareResult.Synced>(result)
        assertEquals(PoolShareStatus.AUTHORIZED, result.view.shareStatus)
        verify(exactly = 0) { poolShareRepository.save(any()) }
        verify(exactly = 0) { paymentGateway.capturePaymentIntent(any(), any()) }
    }

    @Test
    fun `is idempotent for an already captured share`() {
        givenPoolAndShare(status = PoolShareStatus.CAPTURED)
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.SUCCEEDED, null)

        val result = sut.sync(command())

        assertIs<SyncPoolShareResult.Synced>(result)
        assertEquals(PoolShareStatus.CAPTURED, result.view.shareStatus)
        verify(exactly = 0) { poolSettlementService.onShareAuthorized(any()) }
        verify(exactly = 0) { poolSettlementService.onShareCanceled(any()) }
    }

    @Test
    fun `allows the pool creator to reconcile a share they do not own`() {
        givenPoolAndShare()
        every { bookingRepository.findById(bookingId) } returns
            mockk<Booking> { every { userId } returns creatorId }
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            PaymentIntentSnapshot("pi_1", PaymentIntentStatus.REQUIRES_CAPTURE, "cs")

        assertIs<SyncPoolShareResult.Synced>(sut.sync(command(requesterId = creatorId)))
    }

    @Test
    fun `rejects a caller who is neither the payer nor the pool creator`() {
        givenPoolAndShare()
        every { bookingRepository.findById(bookingId) } returns
            mockk<Booking> { every { userId } returns creatorId }

        assertEquals(SyncPoolShareResult.NotAllowed, sut.sync(command(requesterId = strangerId)))
        verify(exactly = 0) { paymentGateway.retrievePaymentIntent(any()) }
        verify(exactly = 0) { poolSettlementService.onShareAuthorized(any()) }
    }

    @Test
    fun `returns not found for an unknown pool`() {
        every { poolRepository.findById(poolId) } returns null

        assertEquals(SyncPoolShareResult.PoolNotFound, sut.sync(command()))
    }

    @Test
    fun `returns not found when the share belongs to another pool`() {
        every { poolRepository.findById(poolId) } returns pool
        every { poolShareRepository.findById(shareId) } returns
            share().copy(poolId = PoolId(UUID.randomUUID()))

        assertEquals(SyncPoolShareResult.ShareNotFound, sut.sync(command()))
    }

    @Test
    fun `skips the gateway call for a share that was never presented for payment`() {
        givenPoolAndShare(intent = null)

        assertIs<SyncPoolShareResult.Synced>(sut.sync(command()))
        verify(exactly = 0) { paymentGateway.retrievePaymentIntent(any()) }
    }

    @Test
    fun `leaves everything untouched when the gateway is unreachable`() {
        givenPoolAndShare()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns null

        assertIs<SyncPoolShareResult.Synced>(sut.sync(command()))
        verify(exactly = 0) { poolSettlementService.onShareAuthorized(any()) }
        verify(exactly = 0) { poolSettlementService.onShareCanceled(any()) }
    }
}
