package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetRoomStockServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val roomStockRepository = mockk<RoomStockRepository>()
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = GetRoomStockService(roomRepository, roomStockRepository, serverShiftRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())
    private val entries =
        listOf(
            RoomStockEntry(
                Product(ProductId(UUID.randomUUID()), "Coca-Cola 33cl", null, BigDecimal("2.50"), Currency.EUR),
                12,
            ),
        )

    @Test
    fun `should return the stock when the caller is admin`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { roomStockRepository.findByRoomId(roomId) } returns entries

        val result =
            sut.getRoomStock(GetRoomStockCommand(roomId, UserId(UUID.randomUUID()), isAdmin = true))

        val success = assertIs<GetRoomStockResult.Success>(result)
        assertEquals(entries, success.entries)
    }

    @Test
    fun `should return the stock when the server is on duty in the room`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverId)
        every { roomStockRepository.findByRoomId(roomId) } returns entries

        val result = sut.getRoomStock(GetRoomStockCommand(roomId, serverId, isAdmin = false))

        assertIs<GetRoomStockResult.Success>(result)
    }

    @Test
    fun `should return NotAuthorized when the server is not on duty in the room`() {
        every { roomRepository.findById(roomId) } returns mockk<Room>()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns emptySet()

        val result = sut.getRoomStock(GetRoomStockCommand(roomId, serverId, isAdmin = false))

        assertEquals(GetRoomStockResult.NotAuthorized, result)
    }

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.getRoomStock(GetRoomStockCommand(roomId, serverId, isAdmin = true))

        assertEquals(GetRoomStockResult.RoomNotFound, result)
    }
}
