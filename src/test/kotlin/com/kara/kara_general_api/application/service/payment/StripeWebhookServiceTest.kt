package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.application.service.pool.PoolSettlementService
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookCommand
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.StripeWebhookEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class StripeWebhookServiceTest {
    private val paymentGateway = mockk<PaymentGateway>()
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val poolSettlementService = mockk<PoolSettlementService>(relaxed = true)
    private val applyBookingExtensionService = mockk<ApplyBookingExtensionService>(relaxed = true)
    private val sut =
        StripeWebhookService(
            paymentGateway,
            paymentRepository,
            bookingRepository,
            poolSettlementService,
            applyBookingExtensionService,
        )

    private val bookingId = BookingId(UUID.randomUUID())

    private fun payment(status: PaymentStatus = PaymentStatus.PENDING) =
        Payment(
            id = PaymentId(UUID.randomUUID()),
            bookingId = bookingId,
            userId = UserId(UUID.randomUUID()),
            amount = BigDecimal("435.00"),
            currency = Currency.EUR,
            status = status,
            stripePaymentIntentId = "pi_1",
            createdAt = Instant.now(),
        )

    @Test
    fun `should return InvalidSignature when the signature header is missing`() {
        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = null))

        assertEquals(StripeWebhookResult.InvalidSignature, result)
        verify(exactly = 0) { paymentGateway.verifyAndParseWebhook(any(), any()) }
    }

    @Test
    fun `should return InvalidSignature when Stripe rejects the signature`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "bad") } returns null

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "bad"))

        assertEquals(StripeWebhookResult.InvalidSignature, result)
    }

    @Test
    fun `should ignore events other than payment_intent succeeded`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.created", paymentIntentId = "pi_1")

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Ignored, result)
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `should ignore when no matching payment is found`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.succeeded", paymentIntentId = "pi_unknown")
        every { paymentRepository.findByStripePaymentIntentId("pi_unknown") } returns null

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Ignored, result)
    }

    @Test
    fun `should mark the payment PAID and confirm the booking on payment_intent succeeded`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.succeeded", paymentIntentId = "pi_1")
        every { paymentRepository.findByStripePaymentIntentId("pi_1") } returns payment()

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Handled, result)
        verify { paymentRepository.save(match { it.status == PaymentStatus.PAID }) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
    }

    @Test
    fun `should delegate amount_capturable_updated to the pool settlement service`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.amount_capturable_updated", paymentIntentId = "pi_pool")
        every { poolSettlementService.onShareAuthorized("pi_pool") } returns StripeWebhookResult.Handled

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Handled, result)
        verify(exactly = 1) { poolSettlementService.onShareAuthorized("pi_pool") }
        verify(exactly = 0) { paymentRepository.findByStripePaymentIntentId(any()) }
    }

    @Test
    fun `should delegate payment_intent canceled to the pool settlement service`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.canceled", paymentIntentId = "pi_pool")
        every { poolSettlementService.onShareCanceled("pi_pool") } returns StripeWebhookResult.Handled

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Handled, result)
        verify(exactly = 1) { poolSettlementService.onShareCanceled("pi_pool") }
    }

    @Test
    fun `should be idempotent when the payment is already PAID`() {
        every { paymentGateway.verifyAndParseWebhook("{}", "sig") } returns
            StripeWebhookEvent(type = "payment_intent.succeeded", paymentIntentId = "pi_1")
        every { paymentRepository.findByStripePaymentIntentId("pi_1") } returns payment(status = PaymentStatus.PAID)

        val result = sut.handle(StripeWebhookCommand(payload = "{}", signature = "sig"))

        assertEquals(StripeWebhookResult.Handled, result)
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }
}
