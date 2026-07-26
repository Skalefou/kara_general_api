package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageResult
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesQuery
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListAllConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadCommand
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadResult
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadUseCase
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.ConversationDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.CreateConversationRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.MessageDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.ReactionRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.SendMessageRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val PHOTO_URL_TTL: Duration = Duration.ofMinutes(15)

@Tag(name = "Chat", description = "Messagerie temps réel : conversations, messages, réactions")
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val createConversationUseCase: CreateConversationUseCase,
    private val listConversationsUseCase: ListConversationsUseCase,
    private val listAllConversationsUseCase: ListAllConversationsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val toggleReactionUseCase: ToggleReactionUseCase,
    private val markMessageReadUseCase: MarkMessageReadUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val imageStorage: ImageStoragePort,
) {
    private fun signedPhotoUrl(key: String): String = imageStorage.signedUrl(key, PHOTO_URL_TTL)

    private fun currentUserId(authentication: Authentication): UserId = UserId(UUID.fromString(authentication.name))

    private fun isAdmin(authentication: Authentication): Boolean = authentication.authorities.any { it.authority == "ROLE_ADMIN" }

    @GetMapping("/conversations")
    fun listConversations(authentication: Authentication): ResponseEntity<Any> {
        val conversations =
            listConversationsUseCase
                .listConversations(currentUserId(authentication))
                .map { ConversationDto.from(it, ::signedPhotoUrl) }
        return ResponseEntity.ok(conversations)
    }

    @GetMapping("/admin/conversations")
    fun listAllConversations(authentication: Authentication): ResponseEntity<Any> {
        val conversations =
            listAllConversationsUseCase
                .listAllConversations(currentUserId(authentication))
                .map { ConversationDto.from(it, ::signedPhotoUrl) }
        return ResponseEntity.ok(conversations)
    }

    @PostMapping("/conversations")
    fun createConversation(
        @RequestBody request: CreateConversationRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            CreateConversationCommand(
                currentUserId = currentUserId(authentication),
                participantIds = request.participantIds.map { UserId(UUID.fromString(it)) }.toSet(),
            )
        return when (val result = createConversationUseCase.createConversation(command)) {
            is CreateConversationResult.Success ->
                ResponseEntity.ok(ConversationDto.from(result.conversation, ::signedPhotoUrl))
        }
    }

    @GetMapping("/conversations/{id}/messages")
    fun getMessages(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "30") limit: Int,
        @RequestParam(required = false) before: String?,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val query =
            GetMessagesQuery(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                limit = limit,
                before = before?.let { Instant.parse(it) },
                isAdmin = isAdmin(authentication),
            )
        return when (val result = getMessagesUseCase.getMessages(query)) {
            is GetMessagesResult.Success ->
                ResponseEntity.ok(result.messages.map { MessageDto.from(it, ::signedPhotoUrl) })

            GetMessagesResult.ConversationNotFound -> conversationNotFound()
            GetMessagesResult.NotParticipant -> notParticipant()
        }
    }

    @PostMapping("/conversations/{id}/messages")
    fun sendMessage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendMessageRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            SendMessageCommand(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                text = request.text,
                replyToId = request.replyToId?.let { MessageId(UUID.fromString(it)) },
                isForwarded = request.isForwarded ?: false,
            )
        return when (val result = sendMessageUseCase.sendMessage(command)) {
            is SendMessageResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(MessageDto.from(result.message, ::signedPhotoUrl))

            SendMessageResult.ConversationNotFound -> conversationNotFound()
            SendMessageResult.NotParticipant -> notParticipant()
            SendMessageResult.EmptyText -> emptyText()
            SendMessageResult.ChatClosed -> chatClosed()
        }
    }

    @PostMapping("/conversations/{id}/messages/{mid}/reactions")
    fun toggleReaction(
        @PathVariable id: UUID,
        @PathVariable mid: UUID,
        @Valid @RequestBody request: ReactionRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            ToggleReactionCommand(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                messageId = MessageId(mid),
                emoji = request.emoji,
            )
        return when (val result = toggleReactionUseCase.toggleReaction(command)) {
            is ToggleReactionResult.Success ->
                ResponseEntity.ok(MessageDto.from(result.message, ::signedPhotoUrl))

            ToggleReactionResult.ConversationNotFound -> conversationNotFound()
            ToggleReactionResult.NotParticipant -> notParticipant()
            ToggleReactionResult.MessageNotFound -> messageNotFound()
        }
    }

    @PostMapping("/conversations/{id}/messages/{mid}/read")
    fun markRead(
        @PathVariable id: UUID,
        @PathVariable mid: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            MarkMessageReadCommand(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                messageId = MessageId(mid),
            )
        return when (markMessageReadUseCase.markRead(command)) {
            MarkMessageReadResult.Success -> ResponseEntity.noContent().build()
            MarkMessageReadResult.ConversationNotFound -> conversationNotFound()
            MarkMessageReadResult.NotParticipant -> notParticipant()
            MarkMessageReadResult.MessageNotFound -> messageNotFound()
        }
    }

    @DeleteMapping("/conversations/{id}/messages/{mid}")
    fun deleteMessage(
        @PathVariable id: UUID,
        @PathVariable mid: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            DeleteMessageCommand(
                currentUserId = currentUserId(authentication),
                isAdmin = isAdmin(authentication),
                conversationId = ConversationId(id),
                messageId = MessageId(mid),
            )
        return when (deleteMessageUseCase.deleteMessage(command)) {
            DeleteMessageResult.Success -> ResponseEntity.noContent().build()
            DeleteMessageResult.ConversationNotFound -> conversationNotFound()
            DeleteMessageResult.NotParticipant -> notParticipant()
            DeleteMessageResult.MessageNotFound -> messageNotFound()
            DeleteMessageResult.NotAuthor -> notAuthor()
        }
    }

    private fun notParticipant(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Vous ne participez pas à cette conversation.",
                ).apply {
                    title = "Accès refusé"
                    setProperty("code", "NOT_A_PARTICIPANT")
                },
        )

    private fun notAuthor(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Seul l'auteur du message peut le supprimer.",
                ).apply {
                    title = "Suppression refusée"
                    setProperty("code", "NOT_MESSAGE_AUTHOR")
                },
        )

    private fun conversationNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucune conversation ne correspond à cet identifiant.",
                ).apply {
                    title = "Conversation introuvable"
                    setProperty("code", "CONVERSATION_NOT_FOUND")
                },
        )

    private fun messageNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun message ne correspond à cet identifiant.",
                ).apply {
                    title = "Message introuvable"
                    setProperty("code", "MESSAGE_NOT_FOUND")
                },
        )

    private fun emptyText(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Le message ne peut pas être vide.",
                ).apply {
                    title = "Message vide"
                    setProperty("code", "EMPTY_MESSAGE")
                },
        )

    private fun chatClosed(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Le chat de cette réservation est fermé (30 minutes après la fin de la réservation).",
                ).apply {
                    title = "Chat fermé"
                    setProperty("code", "CHAT_CLOSED")
                },
        )
}
