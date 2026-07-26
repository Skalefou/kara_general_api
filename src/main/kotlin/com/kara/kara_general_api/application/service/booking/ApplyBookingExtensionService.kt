package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.port.output.BookingEndReminderRepository
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import org.springframework.stereotype.Service

@Service
class ApplyBookingExtensionService(
    private val bookingRepository: BookingRepository,
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val bookingEndReminderRepository: BookingEndReminderRepository,
) {
    fun apply(extensionId: BookingExtensionId): Boolean {
        val extension = bookingExtensionRepository.findById(extensionId) ?: return false
        return apply(extension)
    }

    fun apply(extension: BookingExtension): Boolean {
        if (!extension.isPending()) return false
        val booking = bookingRepository.findById(extension.bookingId) ?: return false

        bookingExtensionRepository.updateStatus(extension.id, BookingExtensionStatus.CONFIRMED)
        bookingRepository.updateEndAt(
            id = booking.id,
            endAt = extension.newEndAt,
            totalPrice = booking.totalPrice.add(extension.price),
        )
        bookingEndReminderRepository.deleteByBookingId(booking.id)
        return true
    }
}
