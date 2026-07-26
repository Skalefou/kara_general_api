package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.order.OrderId
import com.kara.kara_general_api.domain.model.order.OrderStatus
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderResult
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderUseCase
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val BOOKING_ID = "99999999-8888-7777-6666-555555555555"
private const val PRODUCT_ID = "44444444-4444-4444-4444-444444444441"
private const val REQUEST_BODY = """{"productId": "$PRODUCT_ID", "quantity": 2}"""

@WebMvcTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    private fun sampleOrder() =
        Order(
            id = OrderId(UUID.randomUUID()),
            bookingId = BookingId(UUID.fromString(BOOKING_ID)),
            userId = UserId(UUID.fromString(USER_ID)),
            productId = ProductId(UUID.fromString(PRODUCT_ID)),
            quantity = 2,
            unitPrice = BigDecimal("2.50"),
            currency = Currency.EUR,
            totalPrice = BigDecimal("5.00"),
            status = OrderStatus.PLACED,
            createdAt = Instant.parse("2026-08-01T19:00:00Z"),
        )

    private fun performOrder(body: String = REQUEST_BODY) =
        mockMvc.perform(
            post("/api/v1/bookings/$BOOKING_ID/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    @Test
    fun `should return 401 when placing an order without authentication`() {
        performOrder().andExpect(status().isUnauthorized)

        verify(exactly = 0) { placeOrderUseCase.placeOrder(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 201 with the created order`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.Success(sampleOrder())

        performOrder()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.bookingId").value(BOOKING_ID))
            .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.unitPrice").value(2.50))
            .andExpect(jsonPath("$.totalPrice").value(5.00))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.status").value("PLACED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 402 when no payment method is registered`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.PaymentMethodRequired

        performOrder()
            .andExpect(status().isPaymentRequired)
            .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_REQUIRED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when the booking is not owned by the user`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.NotOwner

        performOrder()
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when the booking is not found`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.BookingNotFound

        performOrder()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when the product is not found`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.ProductNotFound

        performOrder()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the booking is not active`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.BookingNotActive

        performOrder()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_ACTIVE"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the stock is insufficient`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.InsufficientStock

        performOrder()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the quantity is invalid`() {
        every { placeOrderUseCase.placeOrder(any()) } returns PlaceOrderResult.Success(sampleOrder())

        performOrder("""{"productId": "$PRODUCT_ID", "quantity": 0}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { placeOrderUseCase.placeOrder(any()) }
    }
}
