package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingEndReminderRepository
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplyBookingExtensionServiceTest {

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val bookingExtensionRepository = mockk<BookingExtensionRepository>(relaxed = true)
    private val bookingEndReminderRepository = mockk<BookingEndReminderRepository>(relaxed = true)
    private val sut =
        ApplyBookingExtensionService(
            bookingRepository,
            bookingExtensionRepository,
            bookingEndReminderRepository,
        )

    private val bookingId = BookingId(UUID.randomUUID())
    private val endAt: Instant = Instant.parse("2026-07-24T20:00:00Z")

    private val booking =
        Booking(
            id = bookingId,
            roomId = RoomId.generate(),
            userId = UserId(UUID.randomUUID()),
            startAt = endAt.minus(Duration.ofHours(2)),
            endAt = endAt,
            numberOfPeople = 4,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("80.00"),
            currency = Currency.EUR,
            status = BookingStatus.CONFIRMED,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
        )

    private fun extension(status: BookingExtensionStatus = BookingExtensionStatus.PENDING) =
        BookingExtension.create(
            bookingId = bookingId,
            userId = booking.userId,
            additionalMinutes = 60,
            previousEndAt = endAt,
            price = BigDecimal("40.00"),
            currency = Currency.EUR,
            paymentMode = PaymentMode.PAY_ALL,
            now = endAt.minus(Duration.ofMinutes(30)),
        ).copy(status = status)

    @Test
    fun `should push the booking end and add the extension price to the total`() {
        every { bookingRepository.findById(bookingId) } returns booking

        val applied = sut.apply(extension())

        assertTrue(applied)
        verify {
            bookingRepository.updateEndAt(
                bookingId,
                endAt.plus(Duration.ofHours(1)),
                BigDecimal("120.00"),
            )
        }
    }

    @Test
    fun `should clear the already sent end reminders so they fire again on the new end`() {
        every { bookingRepository.findById(bookingId) } returns booking

        sut.apply(extension())

        verify { bookingEndReminderRepository.deleteByBookingId(bookingId) }
    }

    @Test
    fun `should mark the extension confirmed`() {
        every { bookingRepository.findById(bookingId) } returns booking
        val extension = extension()

        sut.apply(extension)

        verify {
            bookingExtensionRepository.updateStatus(extension.id, BookingExtensionStatus.CONFIRMED)
        }
    }

    @Test
    fun `should do nothing when the extension is no longer pending`() {
        val applied = sut.apply(extension(status = BookingExtensionStatus.CANCELLED))

        assertFalse(applied)
        verify(exactly = 0) { bookingRepository.updateEndAt(any(), any(), any()) }
    }
}
