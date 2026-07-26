package com.kara.kara_general_api.application.service.order

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuCommand
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetBookingMenuServiceTest {

    private val bookingRepository = mockk<BookingRepository>()
    private val roomStockRepository = mockk<RoomStockRepository>()
    private val sut = GetBookingMenuService(bookingRepository, roomStockRepository)

    private val bookingId = com.kara.kara_general_api.domain.model.booking.BookingId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val ownerId = UserId(UUID.randomUUID())

    private fun booking(): Booking {
        val b = mockk<Booking>()
        every { b.userId } returns ownerId
        every { b.roomId } returns roomId
        return b
    }

    private fun entry(name: String, quantity: Int) =
        RoomStockEntry(
            Product(ProductId(UUID.randomUUID()), name, null, BigDecimal("2.50"), Currency.EUR),
            quantity,
        )

    @Test
    fun `should return only products with a positive quantity for the owner`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { roomStockRepository.findByRoomId(roomId) } returns
            listOf(entry("Coca-Cola 33cl", 12), entry("Eau minérale 50cl", 0), entry("Part de pizza", 3))

        val result = sut.getBookingMenu(GetBookingMenuCommand(bookingId, ownerId))

        val success = assertIs<GetBookingMenuResult.Success>(result)
        assertEquals(listOf("Coca-Cola 33cl", "Part de pizza"), success.entries.map { it.product.name })
    }

    @Test
    fun `should return BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(GetBookingMenuResult.BookingNotFound, sut.getBookingMenu(GetBookingMenuCommand(bookingId, ownerId)))
    }

    @Test
    fun `should return NotOwner when the booking belongs to someone else`() {
        every { bookingRepository.findById(bookingId) } returns booking()

        val result = sut.getBookingMenu(GetBookingMenuCommand(bookingId, UserId(UUID.randomUUID())))

        assertEquals(GetBookingMenuResult.NotOwner, result)
    }
}
