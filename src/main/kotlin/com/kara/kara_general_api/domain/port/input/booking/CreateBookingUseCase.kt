package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant
import java.util.UUID

data class CreateBookingCommand(
    val roomId: RoomId,
    val userId: UserId,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val selectedOptionIds: List<RoomOptionId>,
    val paymentMode: PaymentMode = PaymentMode.PAY_ALL,
)

sealed interface CreateBookingResult {
    data class Created(
        val booking: Booking,
    ) : CreateBookingResult

    data object RoomNotFound : CreateBookingResult

    data object TooFewPeople : CreateBookingResult

    data class CapacityExceeded(
        val maxCapacity: Int,
    ) : CreateBookingResult

    data object InvalidTimeSlot : CreateBookingResult

    /** Au moins un identifiant d'option ne correspond à aucune option de la salle. */
    data class UnknownOptions(
        val optionIds: List<UUID>,
    ) : CreateBookingResult

    /** Le créneau chevauche une réservation existante (PENDING ou CONFIRMED) sur la même salle. */
    data object SlotUnavailable : CreateBookingResult
}

interface CreateBookingUseCase {
    fun createBooking(command: CreateBookingCommand): CreateBookingResult
}
