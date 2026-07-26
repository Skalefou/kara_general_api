package com.kara.kara_general_api.domain.port.input.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.user.UserId

data class GetBookingMenuCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
)

sealed interface GetBookingMenuResult {
    /** Produits commandables (en stock, quantité > 0) de la salle de la réservation. */
    data class Success(
        val entries: List<RoomStockEntry>,
    ) : GetBookingMenuResult

    data object BookingNotFound : GetBookingMenuResult

    data object NotOwner : GetBookingMenuResult
}

interface GetBookingMenuUseCase {
    fun getBookingMenu(command: GetBookingMenuCommand): GetBookingMenuResult
}
