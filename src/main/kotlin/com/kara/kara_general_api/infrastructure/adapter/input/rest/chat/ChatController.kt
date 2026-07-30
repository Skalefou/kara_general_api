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
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailQuery
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailResult
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesQuery
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListAllConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadCommand
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadResult
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadUseCase
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationResult
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminCommand
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminResult
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminUseCase
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.ConversationDetailDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.ConversationDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.CreateConversationRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.MessageDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.ReactionRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.RenameConversationRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.SendMessageRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
    private val renameConversationUseCase: RenameConversationUseCase,
    private val getConversationDetailUseCase: GetConversationDetailUseCase,
    private val setConversationAdminUseCase: SetConversationAdminUseCase,
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

    @GetMapping("/conversations/{id}")
    fun getConversationDetail(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val query =
            GetConversationDetailQuery(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                isAdminRole = isAdmin(authentication),
            )
        return when (val result = getConversationDetailUseCase.getConversationDetail(query)) {
            is GetConversationDetailResult.Success ->
                ResponseEntity.ok(ConversationDetailDto.from(result.conversation, ::signedPhotoUrl))

            GetConversationDetailResult.ConversationNotFound -> conversationNotFound()
            GetConversationDetailResult.NotParticipant -> notParticipant()
        }
    }

    @PutMapping("/conversations/{id}/members/{memberId}/admin")
    fun promoteMember(
        @PathVariable id: UUID,
        @PathVariable memberId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> = setAdmin(id, memberId, true, authentication)

    @DeleteMapping("/conversations/{id}/members/{memberId}/admin")
    fun demoteMember(
        @PathVariable id: UUID,
        @PathVariable memberId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> = setAdmin(id, memberId, false, authentication)

    private fun setAdmin(
        conversationId: UUID,
        memberId: UUID,
        isAdmin: Boolean,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            SetConversationAdminCommand(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(conversationId),
                memberId = UserId(memberId),
                isAdmin = isAdmin,
            )
        return when (val result = setConversationAdminUseCase.setConversationAdmin(command)) {
            is SetConversationAdminResult.Success ->
                ResponseEntity.ok(ConversationDetailDto.from(result.conversation, ::signedPhotoUrl))

            SetConversationAdminResult.ConversationNotFound -> conversationNotFound()
            SetConversationAdminResult.NotParticipant -> notParticipant()
            SetConversationAdminResult.NotAdmin -> notGroupAdmin()
            SetConversationAdminResult.MemberNotParticipant -> memberNotParticipant()
            SetConversationAdminResult.CannotDemoteBookingOwner -> cannotDemoteBookingOwner()
        }
    }

    @PatchMapping("/conversations/{id}")
    fun renameConversation(
        @PathVariable id: UUID,
        @RequestBody request: RenameConversationRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            RenameConversationCommand(
                currentUserId = currentUserId(authentication),
                conversationId = ConversationId(id),
                title = request.title,
            )
        return when (val result = renameConversationUseCase.renameConversation(command)) {
            is RenameConversationResult.Success ->
                ResponseEntity.ok(ConversationDto.from(result.conversation, ::signedPhotoUrl))

            RenameConversationResult.ConversationNotFound -> conversationNotFound()
            RenameConversationResult.NotParticipant -> notParticipant()
            RenameConversationResult.NotRenamable -> notRenamable()
            RenameConversationResult.TitleTooLong -> titleTooLong()
        }
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
            ToggleReactionResult.ChatClosed -> chatClosed()
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

    private fun notGroupAdmin(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Seul un administrateur du groupe gère les droits de ses membres.",
                ).apply {
                    title = "Droits insuffisants"
                    setProperty("code", "CONVERSATION_NOT_GROUP_ADMIN")
                },
        )

    private fun memberNotParticipant(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Ce membre ne fait pas partie de la conversation.",
                ).apply {
                    title = "Membre introuvable"
                    setProperty("code", "CONVERSATION_MEMBER_NOT_FOUND")
                },
        )

    private fun cannotDemoteBookingOwner(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Le client à l'origine de la réservation reste administrateur du groupe.",
                ).apply {
                    title = "Rétrogradation impossible"
                    setProperty("code", "CONVERSATION_OWNER_ALWAYS_ADMIN")
                },
        )

    private fun notRenamable(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Cette conversation ne peut pas être renommée : seul le client à l'origine de la réservation en change le nom.",
                ).apply {
                    title = "Renommage interdit"
                    setProperty("code", "CONVERSATION_NOT_RENAMABLE")
                },
        )

    private fun titleTooLong(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Le titre du groupe ne peut pas dépasser 255 caractères.",
                ).apply {
                    title = "Titre trop long"
                    setProperty("code", "CONVERSATION_TITLE_TOO_LONG")
                },
        )

    private fun chatClosed(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Le chat de cette réservation est fermé (24 heures après la fin de la réservation).",
                ).apply {
                    title = "Chat fermé"
                    setProperty("code", "CHAT_CLOSED")
                },
        )
}
