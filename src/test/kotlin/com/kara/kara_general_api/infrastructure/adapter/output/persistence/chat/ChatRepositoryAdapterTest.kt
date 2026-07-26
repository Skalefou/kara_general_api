package com.kara.kara_general_api.infrastructure.adapter.output.persistence.chat

import com.google.firebase.auth.FirebaseAuth
import com.kara.kara_general_api.TestcontainersConfiguration
import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageReaction
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

// Le schéma est généré par Hibernate depuis les @Entity chat (create-drop) pour matérialiser les
// tables dans le conteneur PostgreSQL.
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@ActiveProfiles("test")
class ChatRepositoryAdapterTest {
    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var imageStoragePort: ImageStoragePort

    @MockkBean
    lateinit var paymentGateway: PaymentGateway

    @Autowired
    private lateinit var adapter: ChatRepositoryAdapter

    @Autowired
    private lateinit var jdbc: NamedParameterJdbcTemplate

    private val me = UserId(UUID.randomUUID())
    private val other = UserId(UUID.randomUUID())

    @BeforeEach
    fun cleanUp() {
        jdbc.update("DELETE FROM message_reactions", emptyMap<String, Any>())
        jdbc.update("DELETE FROM messages", emptyMap<String, Any>())
        jdbc.update("DELETE FROM conversation_participants", emptyMap<String, Any>())
        jdbc.update("DELETE FROM conversations", emptyMap<String, Any>())
    }

    private fun newConversation(participants: Set<UserId>): Conversation {
        val conversation = Conversation.create()
        adapter.createConversation(conversation, participants)
        return conversation
    }

    private fun saveMessage(
        conversation: Conversation,
        sender: UserId,
        text: String,
        sentAt: Instant,
    ): Message =
        adapter.saveMessage(
            Message(MessageId.generate(), conversation.id, sender, "text", text, null, false, false, sentAt),
        )

    @Test
    fun `findConversationIdByExactParticipants matches only the exact participant set`() {
        val conversation = newConversation(setOf(me, other))

        assertEquals(conversation.id, adapter.findConversationByExactParticipants(setOf(me, other))?.id)
        assertNull(adapter.findConversationByExactParticipants(setOf(me)))
        assertNull(adapter.findConversationByExactParticipants(setOf(me, other, UserId(UUID.randomUUID()))))
    }

    @Test
    fun `findMessages returns an ascending window bounded by limit and the before cursor`() {
        val conversation = newConversation(setOf(me, other))
        val t1 = Instant.parse("2026-07-21T10:00:00Z")
        val t2 = Instant.parse("2026-07-21T10:01:00Z")
        val t3 = Instant.parse("2026-07-21T10:02:00Z")
        saveMessage(conversation, me, "un", t1)
        saveMessage(conversation, me, "deux", t2)
        saveMessage(conversation, me, "trois", t3)

        val lastTwo = adapter.findMessages(conversation.id, limit = 2, before = null)
        assertEquals(listOf("deux", "trois"), lastTwo.map { it.text })

        val beforeT2 = adapter.findMessages(conversation.id, limit = 10, before = t2)
        assertEquals(listOf("un"), beforeT2.map { it.text })
    }

    @Test
    fun `reactions can be added, detected and removed`() {
        val conversation = newConversation(setOf(me, other))
        val message = saveMessage(conversation, other, "hi", Instant.now())

        assertFalse(adapter.reactionExists(message.id, me, "👍"))
        adapter.addReaction(MessageReaction(message.id, me, "👍"))
        assertTrue(adapter.reactionExists(message.id, me, "👍"))
        assertEquals(1, adapter.findReactions(message.id).size)

        adapter.removeReaction(message.id, me, "👍")
        assertFalse(adapter.reactionExists(message.id, me, "👍"))
    }

    @Test
    fun `countUnread ignores own messages and respects the read cursor`() {
        val conversation = newConversation(setOf(me, other))
        val t1 = Instant.parse("2026-07-21T10:00:00Z")
        val t2 = Instant.parse("2026-07-21T10:01:00Z")
        saveMessage(conversation, other, "un", t1)
        saveMessage(conversation, other, "deux", t2)
        saveMessage(conversation, me, "moi", t2.plusSeconds(30))

        assertEquals(2, adapter.countUnread(conversation.id, me, since = null))
        assertEquals(1, adapter.countUnread(conversation.id, me, since = t1))
    }

    @Test
    fun `markReadUpTo advances the read cursor but never moves it backwards`() {
        val conversation = newConversation(setOf(me, other))
        val early = Instant.parse("2026-07-21T10:00:00Z")
        val late = Instant.parse("2026-07-21T11:00:00Z")

        adapter.markReadUpTo(conversation.id, me, late)
        assertEquals(late, adapter.findLastReadAtByParticipant(conversation.id)[me])

        adapter.markReadUpTo(conversation.id, me, early)
        assertEquals(late, adapter.findLastReadAtByParticipant(conversation.id)[me])
    }
}
