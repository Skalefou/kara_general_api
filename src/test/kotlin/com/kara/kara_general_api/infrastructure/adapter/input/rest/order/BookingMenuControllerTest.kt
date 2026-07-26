package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuResult
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val BOOKING_ID = "99999999-8888-7777-6666-555555555555"

@WebMvcTest(BookingMenuController::class)
@Import(SecurityConfig::class)
class BookingMenuControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getBookingMenuUseCase: GetBookingMenuUseCase

    private val entry =
        RoomStockEntry(
            Product(ProductId(UUID.randomUUID()), "Coca-Cola 33cl", "Canette 33cl", BigDecimal("2.50"), Currency.EUR),
            12,
        )

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the orderable products for the owner`() {
        every { getBookingMenuUseCase.getBookingMenu(any()) } returns GetBookingMenuResult.Success(listOf(entry))

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID/available-products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Coca-Cola 33cl"))
            .andExpect(jsonPath("$[0].quantity").value(12))
    }

    @Test
    fun `should return 401 when unauthenticated`() {
        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID/available-products"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when the booking belongs to someone else`() {
        every { getBookingMenuUseCase.getBookingMenu(any()) } returns GetBookingMenuResult.NotOwner

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID/available-products"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when the booking is unknown`() {
        every { getBookingMenuUseCase.getBookingMenu(any()) } returns GetBookingMenuResult.BookingNotFound

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID/available-products"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
    }
}
