package com.kara.kara_general_api.infrastructure.adapter.input.rest.stock

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockUseCase
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockUseCase
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val ROOM_ID = "99999999-8888-7777-6666-555555555555"
private const val PRODUCT_ID = "44444444-4444-4444-4444-444444444441"

@WebMvcTest(RoomStockController::class)
@Import(SecurityConfig::class)
class RoomStockControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getRoomStockUseCase: GetRoomStockUseCase

    @MockkBean
    private lateinit var setRoomStockUseCase: SetRoomStockUseCase

    @MockkBean
    private lateinit var removeRoomStockUseCase: RemoveRoomStockUseCase

    private val entry =
        RoomStockEntry(
            Product(ProductId(UUID.fromString(PRODUCT_ID)), "Coca-Cola 33cl", "Canette 33cl", BigDecimal("2.50"), Currency.EUR),
            24,
        )

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 200 with the stock when admin lists it`() {
        every { getRoomStockUseCase.getRoomStock(any()) } returns GetRoomStockResult.Success(listOf(entry))

        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].productId").value(PRODUCT_ID))
            .andExpect(jsonPath("$[0].name").value("Coca-Cola 33cl"))
            .andExpect(jsonPath("$[0].quantity").value(24))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["SERVER"])
    fun `should return 200 when the on-duty server lists the stock`() {
        every { getRoomStockUseCase.getRoomStock(any()) } returns GetRoomStockResult.Success(listOf(entry))

        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 403 when a client lists the stock`() {
        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated lists the stock`() {
        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 404 when the room is unknown`() {
        every { getRoomStockUseCase.getRoomStock(any()) } returns GetRoomStockResult.RoomNotFound

        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["SERVER"])
    fun `should return 403 when the server is not on duty in the room`() {
        every { getRoomStockUseCase.getRoomStock(any()) } returns GetRoomStockResult.NotAuthorized

        mockMvc
            .perform(get("/api/v1/rooms/$ROOM_ID/stock"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_AUTHORIZED"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 200 when admin sets a product quantity`() {
        every { setRoomStockUseCase.setRoomStock(any()) } returns SetRoomStockResult.Success(entry)

        mockMvc
            .perform(
                put("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"quantity": 24}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(24))
            .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 400 when the quantity is negative`() {
        mockMvc
            .perform(
                put("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"quantity": -1}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { setRoomStockUseCase.setRoomStock(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 404 when setting stock on an unknown product`() {
        every { setRoomStockUseCase.setRoomStock(any()) } returns SetRoomStockResult.ProductNotFound

        mockMvc
            .perform(
                put("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"quantity": 24}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["SERVER"])
    fun `should return 403 when an off-duty server sets stock`() {
        every { setRoomStockUseCase.setRoomStock(any()) } returns SetRoomStockResult.NotAuthorized

        mockMvc
            .perform(
                put("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"quantity": 24}"""),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_AUTHORIZED"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 403 when a client sets stock`() {
        mockMvc
            .perform(
                put("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"quantity": 24}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 204 when admin removes a product from stock`() {
        every { removeRoomStockUseCase.removeRoomStock(any()) } returns RemoveRoomStockResult.Success

        mockMvc
            .perform(delete("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["SERVER"])
    fun `should return 404 when removing a product absent from stock`() {
        every { removeRoomStockUseCase.removeRoomStock(any()) } returns RemoveRoomStockResult.NotInStock

        mockMvc
            .perform(delete("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STOCK_ITEM_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 403 when a client removes stock`() {
        mockMvc
            .perform(delete("/api/v1/rooms/$ROOM_ID/stock/$PRODUCT_ID"))
            .andExpect(status().isForbidden)
    }
}
