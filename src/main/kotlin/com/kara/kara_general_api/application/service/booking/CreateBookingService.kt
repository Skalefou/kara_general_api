package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingEstimator
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Crée une réservation persistée. Réutilise le [BookingEstimator] pour valider la demande ET figer le
 * prix total (aucune logique de prix divergente), puis vérifie l'absence de chevauchement de créneau
 * avant de persister la réservation en statut PENDING.
 */
@Service
class CreateBookingService(
    private val roomRepository: RoomRepository,
    private val roomOptionRepository: RoomOptionRepository,
    private val bookingRepository: BookingRepository,
) : CreateBookingUseCase {
    @Transactional
    override fun createBooking(command: CreateBookingCommand): CreateBookingResult {
        val room = roomRepository.findById(command.roomId) ?: return CreateBookingResult.RoomNotFound
        val options = roomOptionRepository.findByRoomId(command.roomId)

        val estimate =
            when (
                val result =
                    BookingEstimator.estimate(
                        room = room,
                        roomOptions = options,
                        numberOfPeople = command.numberOfPeople,
                        startAt = command.startAt,
                        endAt = command.endAt,
                        requestedOptionIds = command.selectedOptionIds,
                    )
            ) {
                is EstimateBookingResult.Success -> result.estimate
                EstimateBookingResult.RoomNotFound -> return CreateBookingResult.RoomNotFound
                EstimateBookingResult.TooFewPeople -> return CreateBookingResult.TooFewPeople
                is EstimateBookingResult.CapacityExceeded -> return CreateBookingResult.CapacityExceeded(result.maxCapacity)
                EstimateBookingResult.InvalidTimeSlot -> return CreateBookingResult.InvalidTimeSlot
                is EstimateBookingResult.UnknownOptions -> return CreateBookingResult.UnknownOptions(result.optionIds)
            }

        if (bookingRepository.existsOverlapping(command.roomId, command.startAt, command.endAt)) {
            return CreateBookingResult.SlotUnavailable
        }

        val booking =
            Booking.create(
                roomId = command.roomId,
                userId = command.userId,
                startAt = command.startAt,
                endAt = command.endAt,
                numberOfPeople = command.numberOfPeople,
                selectedOptionIds = command.selectedOptionIds,
                totalPrice = estimate.totalPrice,
                currency = estimate.currency,
                paymentMode = command.paymentMode,
            )

        return CreateBookingResult.Created(bookingRepository.save(booking))
    }
}
