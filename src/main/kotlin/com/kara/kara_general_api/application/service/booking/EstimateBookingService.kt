package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingEstimator
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingUseCase
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

/**
 * Estimation en lecture seule : charge la salle et ses options, puis délègue le calcul pur au
 * [BookingEstimator]. Aucune réservation n'est persistée.
 */
@Service
class EstimateBookingService(
    private val roomRepository: RoomRepository,
    private val roomOptionRepository: RoomOptionRepository,
) : EstimateBookingUseCase {
    override fun estimate(command: EstimateBookingCommand): EstimateBookingResult {
        val room = roomRepository.findById(command.roomId) ?: return EstimateBookingResult.RoomNotFound
        val options = roomOptionRepository.findByRoomId(command.roomId)
        return BookingEstimator.estimate(
            room = room,
            roomOptions = options,
            numberOfPeople = command.numberOfPeople,
            startAt = command.startAt,
            endAt = command.endAt,
            requestedOptionIds = command.optionIds,
        )
    }
}
