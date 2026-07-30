package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
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

class PoolSettlementServiceTest {
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val poolNotifier = mockk<PoolNotifier>(relaxed = true)
    private val applyBookingExtensionService = mockk<ApplyBookingExtensionService>(relaxed = true)
    private val sut =
        PoolSettlementService(
            poolRepository,
            poolShareRepository,
            bookingRepository,
            paymentGateway,
            poolNotifier,
            applyBookingExtensionService,
        )

    private val poolId = PoolId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private fun pool(status: PoolStatus = PoolStatus.OPEN) =
        Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, status, Instant.now().plusSeconds(3600), "g", Instant.now())

    private fun share(
        intent: String,
        status: PoolShareStatus,
        amount: String = "50.00",
    ) = PoolShare(
        PoolShareId(UUID.randomUUID()),
        poolId,
        "P",
        null,
        BigDecimal(amount),
        status,
        intent,
        null,
        null,
        false,
    )

    private fun creatorShare(
        intent: String,
        status: PoolShareStatus,
        amount: String = "50.00",
    ) = share(intent, status, amount).copy(participantName = "Créateur", isCreatorShare = true)

    @Test
    fun `ignores an unknown payment intent`() {
        every { poolShareRepository.findByStripePaymentIntentId("pi_x") } returns null

        assertEquals(StripeWebhookResult.Ignored, sut.onShareAuthorized("pi_x"))
    }

    @Test
    fun `is idempotent when the share is already authorized`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.AUTHORIZED)

        assertEquals(StripeWebhookResult.Handled, sut.onShareAuthorized("pi_b"))
        verify(exactly = 0) { poolShareRepository.save(any()) }
        verify(exactly = 0) { paymentGateway.capturePaymentIntent(any(), any()) }
    }

    @Test
    fun `locks the pool before deciding on a share transition`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.PENDING)
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(share("pi_a", PoolShareStatus.PENDING), share("pi_b", PoolShareStatus.AUTHORIZED))

        sut.onShareAuthorized("pi_b")

        // Le verrou pessimiste est la garantie d'idempotence face à un webhook et un sync concurrents :
        // sans lui, les deux liraient la part PENDING et l'autoriseraient (voire captureraient) deux fois.
        verify(exactly = 1) { poolRepository.findByIdForUpdate(poolId) }
        verify(exactly = 0) { poolRepository.findById(any()) }
    }

    @Test
    fun `re-reads the share after locking so a concurrent transition wins only once`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        // Première lecture (avant verrou) : PENDING. Relecture (après verrou) : une transaction concurrente
        // a commité l'autorisation entre-temps.
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returnsMany
            listOf(
                share("pi_b", PoolShareStatus.PENDING),
                share("pi_b", PoolShareStatus.AUTHORIZED),
            )

        assertEquals(StripeWebhookResult.Handled, sut.onShareAuthorized("pi_b"))
        verify(exactly = 0) { poolShareRepository.save(any()) }
        verify(exactly = 0) { paymentGateway.capturePaymentIntent(any(), any()) }
    }

    @Test
    fun `does not capture twice when the share is already captured`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.CAPTURED)

        assertEquals(StripeWebhookResult.Handled, sut.onShareAuthorized("pi_b"))
        verify(exactly = 0) { poolShareRepository.save(any()) }
        verify(exactly = 0) { paymentGateway.capturePaymentIntent(any(), any()) }
    }

    @Test
    fun `marks the share authorized without capturing while the pool is incomplete`() {
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.PENDING)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(
                share("pi_a", PoolShareStatus.PENDING),
                share("pi_b", PoolShareStatus.AUTHORIZED),
            )

        val result = sut.onShareAuthorized("pi_b")

        assertEquals(StripeWebhookResult.Handled, result)
        verify { poolShareRepository.save(match { it.status == PoolShareStatus.AUTHORIZED }) }
        verify(exactly = 0) { paymentGateway.capturePaymentIntent(any(), any()) }
        verify(exactly = 0) { bookingRepository.updateStatus(any(), BookingStatus.CONFIRMED) }
    }

    @Test
    fun `captures all shares, settles the pool and confirms the booking when complete`() {
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.PENDING)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(
                share("pi_a", PoolShareStatus.AUTHORIZED),
                share("pi_b", PoolShareStatus.AUTHORIZED),
            )
        every { bookingRepository.findById(bookingId) } returns mockk<Booking>()

        val result = sut.onShareAuthorized("pi_b")

        assertEquals(StripeWebhookResult.Handled, result)
        verify(exactly = 1) { paymentGateway.capturePaymentIntent("pi_a", BigDecimal("50.00")) }
        verify(exactly = 1) { paymentGateway.capturePaymentIntent("pi_b", BigDecimal("50.00")) }
        verify { poolRepository.updateStatus(poolId, PoolStatus.SETTLED) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
        verify { poolNotifier.notifyPoolConfirmed(any()) }
    }

    @Test
    fun `settles a pool whose last authorized share is the creator remainder, capturing its intent too`() {
        val creatorRemainder = creatorShare("pi_creator", PoolShareStatus.PENDING, amount = "60.00")
        every { poolShareRepository.findByStripePaymentIntentId("pi_creator") } returns creatorRemainder
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(
                share("pi_guest", PoolShareStatus.AUTHORIZED, amount = "40.00"),
                creatorShare("pi_creator", PoolShareStatus.AUTHORIZED, amount = "60.00"),
            )
        every { bookingRepository.findById(bookingId) } returns mockk<Booking>()

        val result = sut.onShareAuthorized("pi_creator")

        // Le reliquat du créateur est une part comme les autres : elle compte dans la complétude et son
        // autorisation est capturée au même titre que celle des participants.
        assertEquals(StripeWebhookResult.Handled, result)
        verify(exactly = 1) { paymentGateway.capturePaymentIntent("pi_guest", BigDecimal("40.00")) }
        verify(exactly = 1) { paymentGateway.capturePaymentIntent("pi_creator", BigDecimal("60.00")) }
        verify { poolRepository.updateStatus(poolId, PoolStatus.SETTLED) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
    }

    @Test
    fun `captures the amount due by the share, never the whole authorized amount`() {
        // Le reliquat a été découpé (100 -> 60) : quoi qu'il arrive, on ne prélève que les 60 dus. Capturer
        // sans montant prélèverait l'intégralité de l'autorisation, donc plus que le dû.
        every { poolShareRepository.findByStripePaymentIntentId("pi_creator") } returns
            creatorShare("pi_creator", PoolShareStatus.PENDING, amount = "60.00")
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(
                share("pi_guest", PoolShareStatus.AUTHORIZED, amount = "40.00"),
                creatorShare("pi_creator", PoolShareStatus.AUTHORIZED, amount = "60.00"),
            )
        every { bookingRepository.findById(bookingId) } returns mockk<Booking>()

        sut.onShareAuthorized("pi_creator")

        verify(exactly = 0) { paymentGateway.capturePaymentIntent("pi_creator", BigDecimal("100.00")) }
        verify(exactly = 1) { paymentGateway.capturePaymentIntent("pi_creator", BigDecimal("60.00")) }
    }

    @Test
    fun `onShareCanceled marks the share cancelled`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findByStripePaymentIntentId("pi_b") } returns
            share("pi_b", PoolShareStatus.AUTHORIZED)

        assertEquals(StripeWebhookResult.Handled, sut.onShareCanceled("pi_b"))
        verify { poolShareRepository.save(match { it.status == PoolShareStatus.CANCELLED }) }
    }

    @Test
    fun `cancelShareHolds releases only shares with an active hold`() {
        val authorized = share("pi_a", PoolShareStatus.AUTHORIZED)
        val pendingWithIntent = share("pi_b", PoolShareStatus.PENDING)
        val pendingNoIntent =
            PoolShare(
                PoolShareId(UUID.randomUUID()),
                poolId,
                "P",
                null,
                BigDecimal("50.00"),
                PoolShareStatus.PENDING,
                null,
                null,
                null,
                false,
            )
        val captured = share("pi_c", PoolShareStatus.CAPTURED)

        sut.cancelShareHolds(listOf(authorized, pendingWithIntent, pendingNoIntent, captured))

        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_a") }
        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_b") }
        verify(exactly = 0) { paymentGateway.cancelPaymentIntent("pi_c") }
        verify(exactly = 2) { poolShareRepository.save(match { it.status == PoolShareStatus.CANCELLED }) }
    }

    @Test
    fun `refundCapturedShares refunds only captured shares`() {
        val captured = share("pi_a", PoolShareStatus.CAPTURED)
        val authorized = share("pi_b", PoolShareStatus.AUTHORIZED)

        sut.refundCapturedShares(listOf(captured, authorized))

        verify(exactly = 1) { paymentGateway.refundPaymentIntent("pi_a") }
        verify(exactly = 0) { paymentGateway.refundPaymentIntent("pi_b") }
        verify(exactly = 1) { poolShareRepository.save(match { it.status == PoolShareStatus.REFUNDED }) }
    }
}
