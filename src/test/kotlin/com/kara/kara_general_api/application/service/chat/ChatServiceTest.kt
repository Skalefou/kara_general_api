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
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailQuery
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesQuery
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminCommand
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatEventPublisher
import com.kara.kara_general_api.domain.port.output.ChatRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
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
    private val roomRepository = mockk<RoomRepository>()
    private val eventPublisher = mockk<ChatEventPublisher>(relaxUnitFun = true)
    private val sut =
        ChatService(chatRepository, userRepository, bookingRepository, roomRepository, eventPublisher)

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
    fun `sendMessage on a booking conversation is rejected past the 24 hour window`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.now().minusSeconds(25 * 3600))

        val result = sut.sendMessage(SendMessageCommand(meId, conversationId, "coucou", null, false))

        assertEquals(SendMessageResult.ChatClosed, result)
        verify(exactly = 0) { chatRepository.saveMessage(any()) }
    }

    @Test
    fun `toggleReaction on a booking conversation is rejected past the 24 hour window`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.now().minusSeconds(25 * 3600))

        val result =
            sut.toggleReaction(
                ToggleReactionCommand(meId, conversationId, MessageId(UUID.randomUUID()), "+1"),
            )

        assertEquals(ToggleReactionResult.ChatClosed, result)
        verify(exactly = 0) { chatRepository.addReaction(any()) }
        verify(exactly = 0) { chatRepository.removeReaction(any(), any(), any()) }
    }

    @Test
    fun `listConversations names a booking conversation after the room and its slot`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        val booking =
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(
                roomId = roomId,
                startAt = Instant.parse("2026-07-29T18:00:00Z"),
                userId = meId,
            )
        every { chatRepository.findConversationsForUser(meId) } returns
            listOf(Conversation(conversationId, Instant.now(), bookingId))
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { bookingRepository.findById(bookingId) } returns booking
        every { roomRepository.findById(roomId) } returns room(roomId, "Salle Étoile")
        every { chatRepository.findLastMessage(conversationId) } returns null
        every { chatRepository.countUnread(conversationId, meId, null) } returns 0
        stubViewLookups()

        val views = sut.listConversations(meId)

        assertEquals("Salle Étoile · 29/07 20:00-23:00", views.single().title)
        assertTrue(views.single().canRename)
    }

    @Test
    fun `listConversations keeps a custom title and denies renaming to a guest`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { chatRepository.findConversationsForUser(meId) } returns
            listOf(Conversation(conversationId, Instant.now(), bookingId, "Anniversaire de Bruno"))
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId, "Anniversaire de Bruno")
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(roomId = roomId)
        every { chatRepository.findLastMessage(conversationId) } returns null
        every { chatRepository.countUnread(conversationId, meId, null) } returns 0
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()
        stubViewLookups()

        val view = sut.listConversations(meId).single()

        assertEquals("Anniversaire de Bruno", view.title)
        assertEquals(false, view.canRename)
    }

    @Test
    fun `renameConversation rejects a participant who did not create the booking`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z"))
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()

        val result =
            sut.renameConversation(RenameConversationCommand(meId, conversationId, "Chez Bruno"))

        assertEquals(RenameConversationResult.NotRenamable, result)
        verify(exactly = 0) { chatRepository.updateConversationTitle(any(), any()) }
    }

    @Test
    fun `renameConversation stores the trimmed title for the booking owner`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(
                roomId = roomId,
                userId = meId,
            )
        every { roomRepository.findById(roomId) } returns room(roomId, "Salle Étoile")
        every { chatRepository.findLastMessage(conversationId) } returns null
        every { chatRepository.countUnread(conversationId, meId, null) } returns 0
        stubViewLookups()

        val result =
            sut.renameConversation(
                RenameConversationCommand(meId, conversationId, "  Anniversaire de Bruno  "),
            )

        assertInstanceOf(RenameConversationResult.Success::class.java, result)
        verify {
            chatRepository.updateConversationTitle(conversationId, "Anniversaire de Bruno")
        }
    }

    @Test
    fun `renameConversation clears the title when it is blank`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId, "Anniversaire de Bruno")
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(
                roomId = roomId,
                userId = meId,
            )
        every { roomRepository.findById(roomId) } returns room(roomId, "Salle Étoile")
        every { chatRepository.findLastMessage(conversationId) } returns null
        every { chatRepository.countUnread(conversationId, meId, null) } returns 0
        stubViewLookups()

        sut.renameConversation(RenameConversationCommand(meId, conversationId, "   "))

        verify { chatRepository.updateConversationTitle(conversationId, null) }
    }

    @Test
    fun `getConversationDetail flags the booking owner as admin and lists admins first`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, otherId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(
                roomId = roomId,
                userId = meId,
            )
        every { roomRepository.findById(roomId) } returns room(roomId, "Salle Étoile")
        every { userRepository.findById(meId) } returns user(meId, "Jane")
        every { userRepository.findById(otherId) } returns user(otherId, "John")

        val result =
            sut.getConversationDetail(GetConversationDetailQuery(otherId, conversationId))

        val detail = assertInstanceOf(GetConversationDetailResult.Success::class.java, result).conversation
        assertEquals(listOf("Jane Doe", "John Doe"), detail.members.map { it.displayName })
        assertEquals(listOf(true, false), detail.members.map { it.isAdmin })
        assertEquals(false, detail.isAdmin)
        assertEquals(false, detail.canRename)
        assertTrue(detail.isGroup)
    }

    @Test
    fun `setConversationAdmin promotes a member when the requester owns the booking`() {
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(
                roomId = roomId,
                userId = meId,
            )
        every { roomRepository.findById(roomId) } returns room(roomId, "Salle Étoile")
        every { userRepository.findById(meId) } returns user(meId, "Jane")
        every { userRepository.findById(otherId) } returns user(otherId, "John")

        val result =
            sut.setConversationAdmin(
                SetConversationAdminCommand(meId, conversationId, otherId, isAdmin = true),
            )

        assertInstanceOf(SetConversationAdminResult.Success::class.java, result)
        verify { chatRepository.setParticipantAdmin(conversationId, otherId, true) }
    }

    @Test
    fun `setConversationAdmin refuses a member who is not admin`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, otherId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(userId = meId)

        val result =
            sut.setConversationAdmin(
                SetConversationAdminCommand(otherId, conversationId, meId, isAdmin = false),
            )

        assertEquals(SetConversationAdminResult.NotAdmin, result)
        verify(exactly = 0) { chatRepository.setParticipantAdmin(any(), any(), any()) }
    }

    @Test
    fun `setConversationAdmin never demotes the booking owner`() {
        val bookingId = BookingId(UUID.randomUUID())
        every { chatRepository.findConversationById(conversationId) } returns
            Conversation(conversationId, Instant.now(), bookingId)
        every { chatRepository.isParticipant(conversationId, meId) } returns true
        every { chatRepository.findParticipantIds(conversationId) } returns setOf(meId, otherId)
        every { chatRepository.findAdminIds(conversationId) } returns emptySet()
        every { bookingRepository.findById(bookingId) } returns
            bookingEndingAt(bookingId, Instant.parse("2026-07-29T21:00:00Z")).copy(userId = meId)

        val result =
            sut.setConversationAdmin(
                SetConversationAdminCommand(meId, conversationId, meId, isAdmin = false),
            )

        assertEquals(SetConversationAdminResult.CannotDemoteBookingOwner, result)
        verify(exactly = 0) { chatRepository.setParticipantAdmin(any(), any(), any()) }
    }

    private fun room(
        id: RoomId,
        name: String,
    ): Room =
        Room(
            id = id,
            name = name,
            description = "",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = java.math.BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
            createdAt = Instant.now(),
        )

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
