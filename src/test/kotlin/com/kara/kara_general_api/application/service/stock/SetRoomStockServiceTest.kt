package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockItem
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockResult
import com.kara.kara_general_api.domain.port.output.ProductRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SetRoomStockServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val roomStockRepository = mockk<RoomStockRepository>(relaxed = true)
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = SetRoomStockService(roomRepository, productRepository, roomStockRepository, serverShiftRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())
    private val product =
        Product(ProductId(UUID.randomUUID()), "Coca-Cola 33cl", "Canette 33cl", BigDecimal("2.50"), Currency.EUR)

    @Test
    fun `should upsert the stock and return the entry when the caller is admin`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { productRepository.findById(product.id) } returns product
        val captured = slot<RoomStockItem>()
        every { roomStockRepository.upsert(capture(captured)) } returns Unit

        val result =
            sut.setRoomStock(
                SetRoomStockCommand(roomId, product.id, quantity = 30, currentUserId = UserId(UUID.randomUUID()), isAdmin = true),
            )

        val success = assertIs<SetRoomStockResult.Success>(result)
        assertEquals(product, success.entry.product)
        assertEquals(30, success.entry.quantity)
        assertEquals(30, captured.captured.quantity)
        verify(exactly = 1) { roomStockRepository.upsert(any()) }
    }

    @Test
    fun `should upsert when the server is on duty in the room`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverId)
        every { productRepository.findById(product.id) } returns product

        val result =
            sut.setRoomStock(SetRoomStockCommand(roomId, product.id, 5, serverId, isAdmin = false))

        assertIs<SetRoomStockResult.Success>(result)
    }

    @Test
    fun `should return NotAuthorized when the server is not on duty`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns emptySet()

        val result =
            sut.setRoomStock(SetRoomStockCommand(roomId, product.id, 5, serverId, isAdmin = false))

        assertEquals(SetRoomStockResult.NotAuthorized, result)
        verify(exactly = 0) { roomStockRepository.upsert(any()) }
    }

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result =
            sut.setRoomStock(SetRoomStockCommand(roomId, product.id, 5, serverId, isAdmin = true))

        assertEquals(SetRoomStockResult.RoomNotFound, result)
    }

    @Test
    fun `should return ProductNotFound when the product does not exist`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { productRepository.findById(product.id) } returns null

        val result =
            sut.setRoomStock(SetRoomStockCommand(roomId, product.id, 5, serverId, isAdmin = true))

        assertEquals(SetRoomStockResult.ProductNotFound, result)
    }
}
