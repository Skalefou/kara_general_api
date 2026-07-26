package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.emergency.EmergencyAlert
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyCommand
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmergencyEventPublisher
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TriggerEmergencyServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val emergencyEventPublisher = mockk<EmergencyEventPublisher>(relaxed = true)
    private val sut =
        TriggerEmergencyService(bookingRepository, roomRepository, serverShiftRepository, emergencyEventPublisher)

    private val bookingId = BookingId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val clientId = UserId(UUID.randomUUID())
    private val serverA = UserId(UUID.randomUUID())
    private val serverB = UserId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = start.plusSeconds(3 * 3600)

    private fun booking() =
        Booking(
            id = bookingId,
            roomId = roomId,
            userId = clientId,
            startAt = start,
            endAt = end,
            numberOfPeople = 6,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("180.00"),
            currency = Currency.EUR,
            status = BookingStatus.CONFIRMED,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
        )

    @Test
    fun `should return BookingNotFound when the booking is missing`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(
            TriggerEmergencyResult.BookingNotFound,
            sut.triggerEmergency(TriggerEmergencyCommand(bookingId, clientId, isAdmin = false)),
        )
    }

    @Test
    fun `should reject a caller who is neither the client nor an admin`() {
        every { bookingRepository.findById(bookingId) } returns booking()

        val stranger = UserId(UUID.randomUUID())
        assertEquals(
            TriggerEmergencyResult.NotAuthorized,
            sut.triggerEmergency(TriggerEmergencyCommand(bookingId, stranger, isAdmin = false)),
        )
        verify(exactly = 0) { emergencyEventPublisher.publishEmergency(any(), any()) }
    }

    @Test
    fun `should publish the alert to every assigned server when the client triggers it`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, start, end) } returns setOf(serverA, serverB)

        val result = sut.triggerEmergency(TriggerEmergencyCommand(bookingId, clientId, isAdmin = false))

        val success = assertIs<TriggerEmergencyResult.Success>(result)
        assertEquals(2, success.notifiedServers)
        verify { emergencyEventPublisher.publishEmergency(serverA, any<EmergencyAlert>()) }
        verify { emergencyEventPublisher.publishEmergency(serverB, any<EmergencyAlert>()) }
    }

    @Test
    fun `should allow an admin to trigger the alert`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, start, end) } returns setOf(serverA)

        val admin = UserId(UUID.randomUUID())
        val result = sut.triggerEmergency(TriggerEmergencyCommand(bookingId, admin, isAdmin = true))

        assertIs<TriggerEmergencyResult.Success>(result)
        verify { emergencyEventPublisher.publishEmergency(serverA, any<EmergencyAlert>()) }
    }
}
