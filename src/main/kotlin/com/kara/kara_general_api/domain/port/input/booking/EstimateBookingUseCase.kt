package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingEstimate
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import java.time.Instant
import java.util.UUID

data class EstimateBookingCommand(
    val roomId: RoomId,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val optionIds: List<RoomOptionId>,
)

sealed interface EstimateBookingResult {
    data class Success(val estimate: BookingEstimate) : EstimateBookingResult

    data object RoomNotFound : EstimateBookingResult

    data object TooFewPeople : EstimateBookingResult

    data class CapacityExceeded(val maxCapacity: Int) : EstimateBookingResult

    data object InvalidTimeSlot : EstimateBookingResult

    /** Au moins un identifiant d'option ne correspond à aucune option de la salle. */
    data class UnknownOptions(val optionIds: List<UUID>) : EstimateBookingResult
}

interface EstimateBookingUseCase {
    fun estimate(command: EstimateBookingCommand): EstimateBookingResult
}
