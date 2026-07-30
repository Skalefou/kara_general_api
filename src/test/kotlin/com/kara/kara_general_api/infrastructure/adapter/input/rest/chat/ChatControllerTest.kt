package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.chat.MESSAGE_STATUS_SENT
import com.kara.kara_general_api.domain.model.chat.MESSAGE_TYPE_TEXT
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.chat.ReactionView
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageResult
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListAllConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadResult
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailUseCase
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminUseCase
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

private const val USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val CONVERSATION_ID = "11111111-1111-1111-1111-111111111111"
private const val MESSAGE_ID = "22222222-2222-2222-2222-222222222222"

@WebMvcTest(ChatController::class)
@Import(SecurityConfig::class)
class ChatControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createConversationUseCase: CreateConversationUseCase

    @MockkBean
    private lateinit var listConversationsUseCase: ListConversationsUseCase

    @MockkBean
    private lateinit var listAllConversationsUseCase: ListAllConversationsUseCase

    @MockkBean
    private lateinit var getMessagesUseCase: GetMessagesUseCase

    @MockkBean
    private lateinit var sendMessageUseCase: SendMessageUseCase

    @MockkBean
    private lateinit var toggleReactionUseCase: ToggleReactionUseCase

    @MockkBean
    private lateinit var markMessageReadUseCase: MarkMessageReadUseCase

    @MockkBean
    private lateinit var deleteMessageUseCase: DeleteMessageUseCase

    @MockkBean
    private lateinit var renameConversationUseCase: RenameConversationUseCase

    @MockkBean
    private lateinit var getConversationDetailUseCase: GetConversationDetailUseCase

    @MockkBean
    private lateinit var setConversationAdminUseCase: SetConversationAdminUseCase

    @MockkBean
    private lateinit var imageStorage: ImageStoragePort

    private fun sampleMessageView(): MessageView =
        MessageView(
            message =
                Message(
                    id = MessageId(UUID.fromString(MESSAGE_ID)),
                    conversationId = ConversationId(UUID.fromString(CONVERSATION_ID)),
                    senderId = UserId(UUID.fromString(USER_ID)),
                    type = MESSAGE_TYPE_TEXT,
                    text = "Bonjour",
                    replyToId = null,
                    isForwarded = false,
                    isPinned = false,
                    sentAt = Instant.parse("2026-07-21T10:15:30Z"),
                ),
            senderName = "Jane Doe",
            senderPhotoKey = null,
            isStaff = false,
            status = MESSAGE_STATUS_SENT,
            replyTo = null,
            reactions = listOf(ReactionView("👍", UserId(UUID.fromString(USER_ID)), "Jane Doe")),
        )

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 201 with the created message`() {
        every { sendMessageUseCase.sendMessage(any()) } returns SendMessageResult.Success(sampleMessageView())

        mockMvc
            .perform(
                post("/api/v1/chat/conversations/$CONVERSATION_ID/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Bonjour"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(MESSAGE_ID))
            .andExpect(jsonPath("$.text").value("Bonjour"))
            .andExpect(jsonPath("$.type").value("text"))
            .andExpect(jsonPath("$.status").value("sent"))
            .andExpect(jsonPath("$.sentAt").value("2026-07-21T10:15:30Z"))
            .andExpect(jsonPath("$.reactions[0].emoji").value("👍"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when the user is not a participant`() {
        every { getMessagesUseCase.getMessages(any()) } returns GetMessagesResult.NotParticipant

        mockMvc
            .perform(get("/api/v1/chat/conversations/$CONVERSATION_ID/messages"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_A_PARTICIPANT"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the message text is blank`() {
        mockMvc
            .perform(
                post("/api/v1/chat/conversations/$CONVERSATION_ID/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "   "}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the updated message after toggling a reaction`() {
        every { toggleReactionUseCase.toggleReaction(any()) } returns ToggleReactionResult.Success(sampleMessageView())

        mockMvc
            .perform(
                post("/api/v1/chat/conversations/$CONVERSATION_ID/messages/$MESSAGE_ID/reactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"emoji": "👍"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.reactions[0].userName").value("Jane Doe"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when marking a message as read`() {
        every { markMessageReadUseCase.markRead(any()) } returns MarkMessageReadResult.Success

        mockMvc
            .perform(post("/api/v1/chat/conversations/$CONVERSATION_ID/messages/$MESSAGE_ID/read"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when a non-author deletes a message`() {
        every { deleteMessageUseCase.deleteMessage(any()) } returns DeleteMessageResult.NotAuthor

        mockMvc
            .perform(delete("/api/v1/chat/conversations/$CONVERSATION_ID/messages/$MESSAGE_ID"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_MESSAGE_AUTHOR"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the conversation list`() {
        every { listConversationsUseCase.listConversations(any()) } returns
            listOf(
                ConversationView(
                    id = ConversationId(UUID.fromString(CONVERSATION_ID)),
                    bookingId = null,
                    title = "John Doe",
                    canRename = false,
                    counterpartName = "John Doe",
                    counterpartPhotoKey = null,
                    lastMessagePreview = "hey",
                    lastMessageAt = Instant.parse("2026-07-21T09:00:00Z"),
                    isLastFromMe = false,
                    unreadCount = 2,
                ),
            )

        mockMvc
            .perform(get("/api/v1/chat/conversations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].counterpartName").value("John Doe"))
            .andExpect(jsonPath("$[0].title").value("John Doe"))
            .andExpect(jsonPath("$[0].unreadCount").value(2))
            .andExpect(jsonPath("$[0].lastMessageAt").value("2026-07-21T09:00:00Z"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should create-or-find a conversation`() {
        every { createConversationUseCase.createConversation(any()) } returns
            CreateConversationResult.Success(
                ConversationView(
                    id = ConversationId(UUID.fromString(CONVERSATION_ID)),
                    bookingId = null,
                    title = "John Doe",
                    canRename = false,
                    counterpartName = "John Doe",
                    counterpartPhotoKey = null,
                    lastMessagePreview = "",
                    lastMessageAt = Instant.parse("2026-07-21T09:00:00Z"),
                    isLastFromMe = false,
                    unreadCount = 0,
                ),
            )

        mockMvc
            .perform(
                post("/api/v1/chat/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"participantIds": ["$USER_ID"]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(CONVERSATION_ID))
    }

    @Test
    fun `should return 401 when listing conversations unauthenticated`() {
        mockMvc.perform(get("/api/v1/chat/conversations")).andExpect(status().isUnauthorized)
    }
}
