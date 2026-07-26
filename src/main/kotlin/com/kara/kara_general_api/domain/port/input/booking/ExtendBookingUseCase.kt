package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.user.UserId

data class ExtendBookingCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
    val additionalMinutes: Int,
    val paymentMode: PaymentMode,
)

sealed interface ExtendBookingResult {
    data class Created(val extension: BookingExtension) : ExtendBookingResult

    data object BookingNotFound : ExtendBookingResult

    data object NotOwner : ExtendBookingResult

    data object BookingNotConfirmed : ExtendBookingResult

    data object BookingNotActive : ExtendBookingResult

    data object ExtensionAlreadyPending : ExtendBookingResult

    data object RoomNotFound : ExtendBookingResult

    data class SlotUnavailable(val maxAdditionalMinutes: Int) : ExtendBookingResult

    data object SettlementWindowTooShort : ExtendBookingResult
}

interface ExtendBookingUseCase {
    fun extend(command: ExtendBookingCommand): ExtendBookingResult
}
