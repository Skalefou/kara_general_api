package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionPlanner
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingResult
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingUseCase
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ExtendBookingService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val extensionFeasibility: ExtensionFeasibility,
) : ExtendBookingUseCase {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun extend(command: ExtendBookingCommand): ExtendBookingResult {
        val booking = bookingRepository.findById(command.bookingId) ?: return ExtendBookingResult.BookingNotFound
        if (booking.userId != command.currentUserId) return ExtendBookingResult.NotOwner
        if (booking.status != BookingStatus.CONFIRMED) return ExtendBookingResult.BookingNotConfirmed

        val now = Instant.now()
        if (now.isBefore(booking.startAt) || !now.isBefore(booking.endAt)) {
            return ExtendBookingResult.BookingNotActive
        }
        if (bookingExtensionRepository.findPendingByBookingId(booking.id) != null) {
            return ExtendBookingResult.ExtensionAlreadyPending
        }

        val room = roomRepository.findById(booking.roomId) ?: return ExtendBookingResult.RoomNotFound
        val max = extensionFeasibility.maxAdditionalMinutes(booking, room, now)
        if (!BookingExtensionPlanner.isValidDuration(command.additionalMinutes, max)) {
            return ExtendBookingResult.SlotUnavailable(max)
        }

        if (!BookingExtension.settlementDeadline(booking.endAt).isAfter(now)) {
            return ExtendBookingResult.SettlementWindowTooShort
        }

        val extension =
            BookingExtension.create(
                bookingId = booking.id,
                userId = booking.userId,
                additionalMinutes = command.additionalMinutes,
                previousEndAt = booking.endAt,
                price = BookingExtensionPlanner.price(room, booking, command.additionalMinutes),
                currency = booking.currency,
                paymentMode = command.paymentMode,
                now = now,
            )

        return try {
            ExtendBookingResult.Created(bookingExtensionRepository.save(extension))
        } catch (e: DataIntegrityViolationException) {
            logger.debug("Concurrent pending extension rejected for booking {}", booking.id.value, e)
            ExtendBookingResult.ExtensionAlreadyPending
        }
    }
}
