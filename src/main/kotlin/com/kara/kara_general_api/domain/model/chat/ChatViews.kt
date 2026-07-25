package com.kara.kara_general_api.domain.model.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/** Statut d'un message renvoyé au front (contrat MVP). */
const val MESSAGE_STATUS_SENT: String = "sent"
const val MESSAGE_STATUS_READ: String = "read"

/** Aperçu d'un message cité (réponse). Le [preview] est le texte tronqué du message d'origine. */
data class ReplyPreview(
    val messageId: MessageId,
    val senderName: String,
    val type: String,
    val preview: String,
)

/** Réaction résolue avec le nom de l'auteur de la réaction. */
data class ReactionView(
    val emoji: String,
    val userId: UserId,
    val userName: String,
)

/**
 * Vue complète d'un message prête à être exposée. Les identités (noms) sont déjà résolues depuis
 * le store utilisateur ; la photo reste une clé opaque ([senderPhotoKey]) résolue en URL signée
 * par l'adaptateur (REST ou WebSocket).
 */
data class MessageView(
    val message: Message,
    val senderName: String,
    val senderPhotoKey: String?,
    val isStaff: Boolean,
    val status: String,
    val replyTo: ReplyPreview?,
    val reactions: List<ReactionView>,
)

/**
 * Vue d'une conversation pour la liste. [counterpartPhotoKey] est une clé opaque résolue en URL
 * signée par l'adaptateur. [lastMessageAt] retombe sur la date de création si aucun message.
 */
data class ConversationView(
    val id: ConversationId,
    val bookingId: BookingId?,
    val counterpartName: String,
    val counterpartPhotoKey: String?,
    val lastMessagePreview: String,
    val lastMessageAt: Instant,
    val isLastFromMe: Boolean,
    val unreadCount: Int,
)

/** Événement « en train d'écrire » rediffusé aux participants. */
data class TypingEvent(
    val conversationId: ConversationId,
    val userId: UserId,
    val userName: String,
    val isTyping: Boolean,
)
