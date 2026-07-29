package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Détail d'une réservation + billet (« ticket ») pour le client propriétaire. */
data class BookingDetailView(
    val bookingId: UUID,
    val roomName: String,
    val roomAddress: String?,
    val roomLatitude: Double?,
    val roomLongitude: Double?,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    val status: BookingStatus,
    val paymentMode: PaymentMode,
    /** Code de billet lisible ; non-null uniquement lorsque la réservation est CONFIRMED. */
    val ticketCode: String?,
)

sealed interface GetBookingDetailResult {
    data class Found(
        val view: BookingDetailView,
    ) : GetBookingDetailResult

    data object NotFound : GetBookingDetailResult

    data object NotOwner : GetBookingDetailResult
}

interface GetBookingDetailUseCase {
    fun getDetail(
        bookingId: BookingId,
        requesterId: UserId,
    ): GetBookingDetailResult
}
