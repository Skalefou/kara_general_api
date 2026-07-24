package com.kara.kara_general_api.application.service.order

import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuCommand
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuResult
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import org.springframework.stereotype.Service

@Service
class GetBookingMenuService(
    private val bookingRepository: BookingRepository,
    private val roomStockRepository: RoomStockRepository,
) : GetBookingMenuUseCase {

    override fun getBookingMenu(command: GetBookingMenuCommand): GetBookingMenuResult {
        val booking = bookingRepository.findById(command.bookingId) ?: return GetBookingMenuResult.BookingNotFound
        if (booking.userId != command.currentUserId) return GetBookingMenuResult.NotOwner
        val orderable = roomStockRepository.findByRoomId(booking.roomId).filter { it.quantity > 0 }
        return GetBookingMenuResult.Success(orderable)
    }
}
