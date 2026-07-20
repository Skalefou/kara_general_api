package com.kara.kara_general_api.infrastructure.adapter.input.rest.payment

import com.kara.kara_general_api.domain.port.input.payment.HandleStripeWebhookUseCase
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentUseCase
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

private const val BOOKING_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val USER_ID = "11111111-2222-3333-4444-555555555555"

@WebMvcTest(PaymentController::class)
@Import(SecurityConfig::class)
class PaymentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var initiateBookingPaymentUseCase: InitiateBookingPaymentUseCase

    @MockkBean
    private lateinit var handleStripeWebhookUseCase: HandleStripeWebhookUseCase

    private fun performInitiate() =
        mockMvc.perform(
            post("/api/v1/bookings/$BOOKING_ID/payments")
                .contentType(MediaType.APPLICATION_JSON),
        )

    @Test
    fun `should return 401 when initiating a payment without authentication`() {
        performInitiate().andExpect(status().isUnauthorized)

        verify(exactly = 0) { initiateBookingPaymentUseCase.initiate(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the PaymentSheet secrets`() {
        every { initiateBookingPaymentUseCase.initiate(any()) } returns
            InitiateBookingPaymentResult.Ready(
                clientSecret = "pi_secret",
                ephemeralKeySecret = "ek_secret",
                customerId = "cus_123",
                publishableKey = "pk_test",
                paymentId = UUID.fromString(BOOKING_ID),
            )

        performInitiate()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.clientSecret").value("pi_secret"))
            .andExpect(jsonPath("$.ephemeralKeySecret").value("ek_secret"))
            .andExpect(jsonPath("$.customerId").value("cus_123"))
            .andExpect(jsonPath("$.publishableKey").value("pk_test"))
            .andExpect(jsonPath("$.paymentId").value(BOOKING_ID))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when the booking is not found`() {
        every { initiateBookingPaymentUseCase.initiate(any()) } returns
            InitiateBookingPaymentResult.BookingNotFound

        performInitiate()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PAYMENT_BOOKING_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when the caller is not the booking owner`() {
        every { initiateBookingPaymentUseCase.initiate(any()) } returns InitiateBookingPaymentResult.NotOwner

        performInitiate()
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("PAYMENT_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the booking is already paid`() {
        every { initiateBookingPaymentUseCase.initiate(any()) } returns InitiateBookingPaymentResult.AlreadyPaid

        performInitiate()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_PAID"))
    }

    @Test
    fun `should return 200 when the webhook is handled without authentication`() {
        every { handleStripeWebhookUseCase.handle(any()) } returns StripeWebhookResult.Handled

        mockMvc.perform(
            post("/api/v1/stripe/webhook")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `should return 200 when the webhook event is ignored`() {
        every { handleStripeWebhookUseCase.handle(any()) } returns StripeWebhookResult.Ignored

        mockMvc.perform(
            post("/api/v1/stripe/webhook")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `should return 400 when the webhook signature is invalid`() {
        every { handleStripeWebhookUseCase.handle(any()) } returns StripeWebhookResult.InvalidSignature

        mockMvc.perform(
            post("/api/v1/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("STRIPE_INVALID_SIGNATURE"))
    }
}
