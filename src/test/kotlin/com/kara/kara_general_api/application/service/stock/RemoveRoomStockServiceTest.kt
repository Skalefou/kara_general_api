package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class RemoveRoomStockServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val roomStockRepository = mockk<RoomStockRepository>()
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = RemoveRoomStockService(roomRepository, roomStockRepository, serverShiftRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())

    @Test
    fun `should return Success when the item was removed by an admin`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { roomStockRepository.deleteByRoomIdAndProductId(roomId, productId) } returns true

        val result =
            sut.removeRoomStock(RemoveRoomStockCommand(roomId, productId, UserId(UUID.randomUUID()), isAdmin = true))

        assertEquals(RemoveRoomStockResult.Success, result)
    }

    @Test
    fun `should return NotInStock when no row was removed`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { roomStockRepository.deleteByRoomIdAndProductId(roomId, productId) } returns false

        val result =
            sut.removeRoomStock(RemoveRoomStockCommand(roomId, productId, UserId(UUID.randomUUID()), isAdmin = true))

        assertEquals(RemoveRoomStockResult.NotInStock, result)
    }

    @Test
    fun `should return NotAuthorized when the server is not on duty`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns emptySet()

        val result =
            sut.removeRoomStock(RemoveRoomStockCommand(roomId, productId, serverId, isAdmin = false))

        assertEquals(RemoveRoomStockResult.NotAuthorized, result)
    }

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result =
            sut.removeRoomStock(RemoveRoomStockCommand(roomId, productId, serverId, isAdmin = true))

        assertEquals(RemoveRoomStockResult.RoomNotFound, result)
    }
}
