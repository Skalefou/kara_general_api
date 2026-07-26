package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant

data class GetExtensionOptionsCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
)

data class ExtensionQuote(
    val additionalMinutes: Int,
    val price: BigDecimal,
    val newEndAt: Instant,
)

data class ExtensionOptions(
    val bookingId: BookingId,
    val currentEndAt: Instant,
    val maxAdditionalMinutes: Int,
    val currency: Currency,
    val quotes: List<ExtensionQuote>,
    val settlementDeadline: Instant,
)

sealed interface GetExtensionOptionsResult {
    data class Success(
        val options: ExtensionOptions,
    ) : GetExtensionOptionsResult

    data object BookingNotFound : GetExtensionOptionsResult

    data object NotOwner : GetExtensionOptionsResult

    data object BookingNotConfirmed : GetExtensionOptionsResult

    data object BookingNotActive : GetExtensionOptionsResult

    data object ExtensionAlreadyPending : GetExtensionOptionsResult

    data object RoomNotFound : GetExtensionOptionsResult
}

interface GetExtensionOptionsUseCase {
    fun getOptions(command: GetExtensionOptionsCommand): GetExtensionOptionsResult
}
