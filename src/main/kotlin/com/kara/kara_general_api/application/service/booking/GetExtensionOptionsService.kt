package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionPlanner
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.port.input.booking.ExtensionOptions
import com.kara.kara_general_api.domain.port.input.booking.ExtensionQuote
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsCommand
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsResult
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsUseCase
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class GetExtensionOptionsService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val extensionFeasibility: ExtensionFeasibility,
) : GetExtensionOptionsUseCase {

    @Transactional(readOnly = true)
    override fun getOptions(command: GetExtensionOptionsCommand): GetExtensionOptionsResult {
        val booking =
            bookingRepository.findById(command.bookingId) ?: return GetExtensionOptionsResult.BookingNotFound
        if (booking.userId != command.currentUserId) return GetExtensionOptionsResult.NotOwner
        if (booking.status != BookingStatus.CONFIRMED) return GetExtensionOptionsResult.BookingNotConfirmed

        val now = Instant.now()
        if (now.isBefore(booking.startAt) || !now.isBefore(booking.endAt)) {
            return GetExtensionOptionsResult.BookingNotActive
        }
        if (bookingExtensionRepository.findPendingByBookingId(booking.id) != null) {
            return GetExtensionOptionsResult.ExtensionAlreadyPending
        }

        val room = roomRepository.findById(booking.roomId) ?: return GetExtensionOptionsResult.RoomNotFound
        val max = extensionFeasibility.maxAdditionalMinutes(booking, room, now)

        val quotes =
            BookingExtensionPlanner.OFFERED_STEPS
                .filter { it <= max }
                .map { minutes ->
                    ExtensionQuote(
                        additionalMinutes = minutes,
                        price = BookingExtensionPlanner.price(room, booking, minutes),
                        newEndAt = booking.endAt.plus(Duration.ofMinutes(minutes.toLong())),
                    )
                }

        return GetExtensionOptionsResult.Success(
            ExtensionOptions(
                bookingId = booking.id,
                currentEndAt = booking.endAt,
                maxAdditionalMinutes = max,
                currency = booking.currency,
                quotes = quotes,
                settlementDeadline = BookingExtension.settlementDeadline(booking.endAt),
            ),
        )
    }
}
