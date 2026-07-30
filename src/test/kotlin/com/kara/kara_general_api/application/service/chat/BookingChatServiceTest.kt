package com.kara.kara_general_api.application.service.chat

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatRepository
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

class BookingChatServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val chatRepository = mockk<ChatRepository>(relaxUnitFun = true)
    private val sut = BookingChatService(bookingRepository, serverShiftRepository, chatRepository)

    private val bookingId = BookingId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val clientId = UserId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())
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

    private fun command(
        userId: UserId,
        isAdmin: Boolean = false,
    ) = OpenBookingConversationCommand(bookingId = bookingId, currentUserId = userId, isAdmin = isAdmin)

    @Test
    fun `should return BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(
            OpenBookingConversationResult.BookingNotFound,
            sut.openBookingConversation(command(serverId)),
        )
    }

    @Test
    fun `should reject a server not assigned to the booking`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, start, end) } returns emptySet()

        val stranger = UserId(UUID.randomUUID())
        assertEquals(
            OpenBookingConversationResult.NotAuthorized,
            sut.openBookingConversation(command(stranger)),
        )
    }

    @Test
    fun `should create the conversation with client and assigned servers when none exists`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, start, end) } returns setOf(serverId)
        every { chatRepository.findConversationByBookingId(bookingId) } returns null

        val result = sut.openBookingConversation(command(serverId))

        assertIs<OpenBookingConversationResult.Success>(result)
        verify {
            chatRepository.createConversation(
                match { it.bookingId == bookingId },
                setOf(serverId, clientId),
            )
        }
    }

    @Test
    fun `should add a late-assigned server as participant of the existing conversation`() {
        val existing = Conversation(ConversationId(UUID.randomUUID()), Instant.now(), bookingId)
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, start, end) } returns setOf(serverId)
        every { chatRepository.findConversationByBookingId(bookingId) } returns existing
        every { chatRepository.isParticipant(existing.id, serverId) } returns false

        val result = sut.openBookingConversation(command(serverId))

        val success = assertIs<OpenBookingConversationResult.Success>(result)
        assertEquals(existing.id, success.conversationId)
        verify { chatRepository.addParticipants(existing.id, setOf(serverId)) }
        verify(exactly = 0) { chatRepository.createConversation(any(), any()) }
    }

    @Test
    fun `should flag the chat as closed past the 24 hour window`() {
        val past = Instant.now().minusSeconds(25 * 3600)
        val closedBooking = booking().copy(startAt = past.minusSeconds(3600), endAt = past)
        every { bookingRepository.findById(bookingId) } returns closedBooking
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, closedBooking.startAt, past) } returns setOf(serverId)
        every { chatRepository.findConversationByBookingId(bookingId) } returns
            Conversation(ConversationId(UUID.randomUUID()), Instant.now(), bookingId).also {
                every { chatRepository.isParticipant(it.id, serverId) } returns true
            }

        val result = sut.openBookingConversation(command(serverId))

        val success = assertIs<OpenBookingConversationResult.Success>(result)
        assertEquals(true, success.closed)
    }
}
