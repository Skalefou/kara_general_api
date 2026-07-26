package com.kara.kara_general_api.application.service.chat

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MESSAGE_STATUS_SENT
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageReaction
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesQuery
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatEventPublisher
import com.kara.kara_general_api.domain.port.output.ChatRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ChatServiceTest {
    private val chatRepository = mockk<ChatRepository>(relaxUnitFun = true)
    private val userRepository = mockk<UserRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val eventPublisher = mockk<ChatEventPublisher>(relaxUnitFun = true)
    private val sut = ChatService(chatRepository, userRepository, bookingRepository, eventPublisher)

    private val meId = UserId(UUID.randomUUID())
    private val otherId = UserId(UUID.randomUUID())
    private val conversationId = ConversationId(UUID.randomUUID())

    private fun user(
        id: UserId,
        first: String,
        role: UserRole = UserRole.CLIENT,
    ): User =
        User(
            id = id,
            email = Email("${first.lowercase()}@example.com"),
            hashedPassword = HashedPassword("hashed"),
            firstName = first,
            lastName = "Doe",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = role,
            firebaseUid = "uid-$first",
            createdAt = Instant.now(),
            emailVerified = true,
        )

    private fun stubViewLookups() {
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findLastReadAtByParticipant(conversationId) } returns emptyMap()
        every { userRepository.findById(meId) } returns user(meId, "Jane")
        every { userRepository.findById(otherId) } returns user(otherId, "John")
    }

    @Test
    fun `sendMessage persists the message, broadcasts it and returns the view`() {
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        val saved = slot<Message>()
        every { chatRepository.saveMessage(capture(saved)) } answers { saved.captured }
        every { chatRepository.findReactions(any()) } returns emptyList()
        stubViewLookups()

        val result =
            sut.sendMessage(
                SendMessageCommand(meId, conversationId, "Bonjour", replyToId = null, isForwarded = false),
            )

        val success = assertInstanceOf(SendMessageResult.Success::class.java, result)
        assertEquals("Bonjour", success.message.message.text)
        assertEquals(MESSAGE_STATUS_SENT, success.message.status)
        assertEquals("Jane Doe", success.message.senderName)
        verify(exactly = 1) { chatRepository.saveMessage(any()) }
        verify(exactly = 1) { eventPublisher.publishMessage(conversationId, any()) }
    }

    @Test
    fun `sendMessage rejects a blank text without touching persistence`() {
        val result = sut.sendMessage(SendMessageCommand(meId, conversationId, "   ", null, false))

        assertInstanceOf(SendMessageResult.EmptyText::class.java, result)
        verify(exactly = 0) { chatRepository.saveMessage(any()) }
    }

    @Test
    fun `getMessages returns 403-style NotParticipant when the user is not a participant`() {
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.isParticipant(conversationId, meId) } returns false

        val result = sut.getMessages(GetMessagesQuery(meId, conversationId, 30, null))

        assertInstanceOf(GetMessagesResult.NotParticipant::class.java, result)
    }

    @Test
    fun `toggleReaction adds the emoji when absent`() {
        val messageId = MessageId(UUID.randomUUID())
        val message = Message(messageId, conversationId, otherId, "text", "hi", null, false, false, Instant.now())
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findMessageById(messageId) } returns message
        every { chatRepository.reactionExists(messageId, meId, "👍") } returns false
        every { chatRepository.findReactions(messageId) } returns
            listOf(MessageReaction(messageId, meId, "👍"))
        stubViewLookups()

        val result = sut.toggleReaction(ToggleReactionCommand(meId, conversationId, messageId, "👍"))

        assertInstanceOf(ToggleReactionResult.Success::class.java, result)
        verify(exactly = 1) { chatRepository.addReaction(MessageReaction(messageId, meId, "👍")) }
        verify(exactly = 0) { chatRepository.removeReaction(any(), any(), any()) }
        verify(exactly = 1) { eventPublisher.publishMessage(conversationId, any()) }
    }

    @Test
    fun `toggleReaction removes the emoji when already present`() {
        val messageId = MessageId(UUID.randomUUID())
        val message = Message(messageId, conversationId, otherId, "text", "hi", null, false, false, Instant.now())
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findMessageById(messageId) } returns message
        every { chatRepository.reactionExists(messageId, meId, "👍") } returns true
        every { chatRepository.findReactions(messageId) } returns emptyList()
        stubViewLookups()

        sut.toggleReaction(ToggleReactionCommand(meId, conversationId, messageId, "👍"))

        verify(exactly = 1) { chatRepository.removeReaction(messageId, meId, "👍") }
        verify(exactly = 0) { chatRepository.addReaction(any()) }
    }

    @Test
    fun `listConversations reports the unread count and the counterpart`() {
        val conversation = Conversation(conversationId, Instant.now())
        every { chatRepository.findConversationsForUser(meId) } returns listOf(conversation)
        every { chatRepository.findConversationById(conversationId) } returns conversation
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findLastMessage(conversationId) } returns
            Message(MessageId(UUID.randomUUID()), conversationId, otherId, "text", "hey", null, false, false, Instant.now())
        every { chatRepository.findLastReadAtByParticipant(conversationId) } returns mapOf(meId to null, otherId to null)
        every { chatRepository.countUnread(conversationId, meId, null) } returns 3
        every { userRepository.findById(otherId) } returns user(otherId, "John")

        val conversations = sut.listConversations(meId)

        assertEquals(1, conversations.size)
        assertEquals(3, conversations.first().unreadCount)
        assertEquals("John Doe", conversations.first().counterpartName)
        assertEquals(false, conversations.first().isLastFromMe)
    }

    @Test
    fun `createConversation reuses an existing conversation with the same participants`() {
        every { chatRepository.findConversationByExactParticipants(setOf(meId, otherId)) } returns
            Conversation(conversationId, Instant.now())
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findLastMessage(conversationId) } returns null
        every { chatRepository.findLastReadAtByParticipant(conversationId) } returns emptyMap()
        every { chatRepository.countUnread(conversationId, meId, null) } returns 0
        every { userRepository.findById(otherId) } returns user(otherId, "John")

        val result = sut.createConversation(CreateConversationCommand(meId, setOf(otherId)))

        val success = assertInstanceOf(CreateConversationResult.Success::class.java, result)
        assertEquals(
            conversationId.value.toString(),
            success.conversation.id.value
                .toString(),
        )
        verify(exactly = 0) { chatRepository.createConversation(any(), any()) }
    }

    @Test
    fun `createConversation creates a new conversation when none matches`() {
        every { chatRepository.findConversationByExactParticipants(setOf(meId, otherId)) } returns null
        every { chatRepository.findConversationById(any()) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.findParticipantIds(any()) } returns setOf(meId, otherId)
        every { chatRepository.findLastMessage(any()) } returns null
        every { chatRepository.findLastReadAtByParticipant(any()) } returns emptyMap()
        every { chatRepository.countUnread(any(), any(), any()) } returns 0
        every { userRepository.findById(otherId) } returns user(otherId, "John")

        val result = sut.createConversation(CreateConversationCommand(meId, setOf(otherId)))

        assertInstanceOf(CreateConversationResult.Success::class.java, result)
        verify(exactly = 1) { chatRepository.createConversation(any(), setOf(otherId, meId)) }
    }

    @Test
    fun `message is marked read when every other participant has read past its timestamp`() {
        val sentAt = Instant.parse("2026-07-21T10:00:00Z")
        val message = Message(MessageId(UUID.randomUUID()), conversationId, meId, "text", "hi", null, false, false, sentAt)
        every { chatRepository.findConversationById(conversationId) } returns Conversation(conversationId, Instant.now())
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findLastReadAtByParticipant(conversationId) } returns
            mapOf(otherId to sentAt.plusSeconds(60))
        every { chatRepository.findMessages(conversationId, 30, null) } returns listOf(message)
        every { chatRepository.findReactions(any()) } returns emptyList()
        every { userRepository.findById(meId) } returns user(meId, "Jane")

        val result = sut.getMessages(GetMessagesQuery(meId, conversationId, 30, null))

        val success = assertInstanceOf(GetMessagesResult.Success::class.java, result)
        assertTrue(success.messages.first().status == "read")
    }

    @Test
    fun `sendMessage on a booking conversation is rejected past the 30 min window`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.now().minusSeconds(31 * 60))

        val result = sut.sendMessage(SendMessageCommand(meId, conversationId, "coucou", null, false))

        assertEquals(SendMessageResult.ChatClosed, result)
        verify(exactly = 0) { chatRepository.saveMessage(any()) }
    }

    private fun bookingEndingAt(
        bookingId: BookingId,
        endAt: Instant,
    ): Booking =
        Booking(
            id = bookingId,
            roomId = RoomId(UUID.randomUUID()),
            userId = otherId,
            startAt = endAt.minusSeconds(3600),
            endAt = endAt,
            numberOfPeople = 4,
            selectedOptionIds = emptyList(),
            totalPrice = java.math.BigDecimal("100.00"),
            currency = Currency.EUR,
            status = BookingStatus.CONFIRMED,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
        )
}
